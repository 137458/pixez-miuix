package com.perol.pixez.shared.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodedPath
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.network.PixivApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 自动注入 Bearer Token 并在凭证失效时刷新 token 的 Ktor 插件。
 *
 * 设计要点：
 * - 除 walkthrough 等匿名接口外，所有请求自动附加 Authorization: Bearer <access_token>。
 * - 收到 401（响应体含 OAuth 相关特征）或 400 invalid_grant 时，串行执行一次 token 刷新，然后重试原请求。
 * - 刷新失败或没有登录账号时，直接向上抛异常，避免无限重试。
 * - 确认 refresh_token 已被吊销/为空时清理凭证并发 [AuthExpirySignal]，同一账号 30 秒内熔断，
 *   避免每个请求都重复一次注定失败的刷新。
 */
internal enum class AuthRefreshDecision { REFRESH, PASS_THROUGH }

/** 已确认凭证失效后的熔断窗口：窗口内同账号直接快速失败，不再发起 OAuth 刷新。 */
private val REFRESH_BREAKER_WINDOW = 30.seconds

/** 401 场景的刷新特征宽匹配：认证失败本身是强信号，沿用既有口径（中-16 仅收窄 400 路径）。 */
private val UNAUTHORIZED_REFRESH_HINTS = listOf("OAuth", "invalid_grant", "Access Token", "token")

/**
 * 依据响应状态与响应体判定是否触发 OAuth 刷新（中-16 收窄后的触发条件）：
 * - 401：响应体含 OAuth 相关特征即刷新，不含则透传（账号被禁用、token 被撤销等非 OAuth 401 不刷新）；
 * - 400：仅当命中 Pixiv OAuth 过期专属标识 invalid_grant 才刷新，错误消息含裸 "token" 字样的业务 400 不再误触发；
 * - 其余状态码一律不刷新。
 */
internal fun resolveAuthRefreshDecision(status: HttpStatusCode, bodyText: String): AuthRefreshDecision =
    when (status) {
        HttpStatusCode.Unauthorized ->
            if (UNAUTHORIZED_REFRESH_HINTS.any { bodyText.contains(it, ignoreCase = true) }) {
                AuthRefreshDecision.REFRESH
            } else {
                AuthRefreshDecision.PASS_THROUGH
            }

        HttpStatusCode.BadRequest ->
            if (bodyText.contains("invalid_grant", ignoreCase = true)) {
                AuthRefreshDecision.REFRESH
            } else {
                AuthRefreshDecision.PASS_THROUGH
            }

        else -> AuthRefreshDecision.PASS_THROUGH
    }
class TokenRefreshPlugin(
    private val tokenStorage: AuthTokenStorage,
    private val oAuthClient: OAuthClient,
    private val authExpirySignal: AuthExpirySignal? = null,
) {
    private data class RequestAuthSnapshot(
        val userId: String,
        val header: String,
    )

    private suspend fun getAuthorizationSnapshot(): RequestAuthSnapshot? {
        val account = tokenStorage.getCurrentAccount() ?: return null
        if (account.access_token.isBlank()) {
            // 不再发送空 Bearer。软失效（access 清空但 refresh 尚在）留给启动自愈静默恢复，
            // 不在这里每个请求都提示一次登录失效；两个 token 都没了才是确认失效，直接提示重新登录。
            if (account.refresh_token.isBlank()) {
                authExpirySignal?.emit(account.user_id)
            }
            return null
        }
        return RequestAuthSnapshot(
            userId = account.user_id,
            header = "Bearer ${account.access_token}",
        )
    }

    /** 检查当前活跃账号是否正处于失效熔断窗口内（供无凭证 401/400 快失败判断使用）。 */
    private suspend fun activeBreakerUserIdOrNull(): String? {
        val current = tokenStorage.getCurrentAccount() ?: return null
        return withRefreshLock {
            if (isRefreshBreakerActive(current.user_id)) current.user_id else null
        }
    }

    private suspend fun resolveAccount(targetUserId: String?): Account? {
        val current = tokenStorage.getCurrentAccount()
        if (targetUserId == null || current?.user_id == targetUserId) {
            return current
        }
        return tokenStorage.getAllAccounts().firstOrNull { it.user_id == targetUserId }
    }

    /**
     * 为发起本次请求的 [targetUserId]（若未记录则回退当前活跃账号）刷新 token，
     * 并返回应重新注入到重试请求头的 `Bearer <access_token>`。
     */
    private suspend fun refreshTokenOrThrow(
        targetUserId: String? = null,
        staleAccessToken: String? = null,
    ): String {
        val preLockAccessToken = resolveAccount(targetUserId)?.access_token
        val initialAccessToken = staleAccessToken ?: preLockAccessToken
        return mutex.withLock {
            val targetAccount = resolveAccount(targetUserId)
                ?: throw IllegalStateException("No logged-in account to refresh token")
            // 同账号近期已确认失效：熔断，避免每个请求重复发起注定失败的刷新；不同账号各自独立。
            // 仅在首次进入熔断时（expireCredentials）发射 authExpirySignal，同一窗口内不再重复发射。
            if (isRefreshBreakerActive(targetAccount.user_id)) {
                throw AuthExpiredException(targetAccount.user_id)
            }
            if (targetAccount.access_token != initialAccessToken && targetAccount.access_token.isNotBlank()) {
                Napier.i("检测到 Token 已由前置并发请求完成刷新，复用最新凭证")
                return@withLock "Bearer ${targetAccount.access_token}"
            }
            if (targetAccount.refresh_token.isBlank()) {
                expireCredentials(
                    userId = targetAccount.user_id,
                    soft = false,
                    reason = "refresh_token 为空，无可保留的续期凭证",
                )
            }
            val refreshed = try {
                oAuthClient.refreshToken(targetAccount.refresh_token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PixivApiException) {
                // OAuth 端点明确拒绝（400 invalid_grant / 401）才是凭证失效；其它状态码原样抛出。
                if (e.statusCode == 400 || e.statusCode == 401) {
                    expireCredentials(
                        userId = targetAccount.user_id,
                        soft = true,
                        reason = "OAuth 端点拒绝：HTTP ${e.statusCode} ${e.message}",
                    )
                }
                Napier.e("刷新 token 失败", e)
                throw e
            } catch (e: IllegalArgumentException) {
                // refresh_token 格式非法，等价于凭证失效。
                expireCredentials(
                    userId = targetAccount.user_id,
                    soft = true,
                    reason = "refresh_token 格式非法：${e.message}",
                )
            } catch (e: Exception) {
                // 连接失败/超时等网络异常不得清理凭证，否则离线用户会被误判为登出。
                Napier.e("刷新 token 失败", e)
                throw e
            }
            // 写回放在网络异常捕获之外：加密/落库失败是本地故障，既不能被当成网络异常静默吞掉，
            // 也不能据此判定凭证失效。写回失败时本次请求仍使用新 Bearer，账号留待下次启动再续期。
            try {
                // 锁内按发起刷新的账号精准写回；期间若切换活跃账号，绝不污染新账号也绝不回写错误 token。
                tokenStorage.updateTokensForUser(
                    userId = targetAccount.user_id,
                    accessToken = refreshed.response.accessToken,
                    refreshToken = refreshed.response.refreshToken,
                )
                expiredUsers.remove(targetAccount.user_id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("刷新 token 成功但凭证写回本地失败，本次请求使用新 Bearer，下次启动需重新续期", e)
            }
            "Bearer ${refreshed.response.accessToken}"
        }
    }

    /**
     * 确认凭证失效：标记熔断、清理本地凭证、发信号，随后抛 [AuthExpiredException]。
     * 必须在持有 [mutex] 的上下文中调用。
     *
     * @param soft true 为软失效：只清 access_token、保留 refresh_token 供下次启动静默恢复；
     *   false 为凭据已无可保留（refresh_token 为空），整体清空。
     * @param reason 日志用原因，含状态码与脱敏响应消息，便于事后定位被拒的具体成因。
     */
    private suspend fun expireCredentials(userId: String, soft: Boolean, reason: String): Nothing {
        Napier.w(
            "账号($userId)凭证续期失败，" +
                if (soft) {
                    "软失效：清空 access_token 并保留 refresh_token 待启动自愈（$reason）"
                } else {
                    "确认失效：清空全部凭证（$reason）"
                },
        )
        expiredUsers[userId] = TimeSource.Monotonic.markNow()
        if (soft) {
            tokenStorage.expireAccessToken(userId)
        } else {
            tokenStorage.invalidateCredentials(userId)
        }
        authExpirySignal?.emit(userId)
        throw AuthExpiredException(userId)
    }

    companion object : HttpClientPlugin<Config, TokenRefreshPlugin> {
        override val key: AttributeKey<TokenRefreshPlugin> = AttributeKey("TokenRefreshPlugin")
        private val REQUEST_USER_ID_KEY = AttributeKey<String>("TokenRefreshPluginUserId")

        /**
         * 刷新互斥与熔断表挂在 companion 上，跨 client 共享。
         *
         * apiClient 与 accountClient 各 install 一次本插件；若锁随实例走，同一账号的 401 会被两条
         * client 并发刷新，而 pixiv 的 refresh_token 是轮换式的——后到者必然 invalid_grant，
         * 进而清凭证并触发登录失效信号，用户被强制登出。
         */
        private val mutex = Mutex()

        /** 已确认失效的账号与时间点（按 userId 隔离）；同账号窗口内直接熔断，不阻塞切号后的新账号。 */
        private val expiredUsers = ConcurrentHashMap<String, TimeMark>()

        /**
         * 跨 HttpClient 与 AccountRepository 共享刷新互斥锁，
         * 确保启动静默恢复与并发 401 刷新不会同时消耗同一个轮换型 refresh_token。
         */
        internal suspend fun <T> withRefreshLock(block: suspend () -> T): T =
            mutex.withLock { block() }

        /** 检查指定账号是否仍处于失效熔断窗口内（超期条目顺带清理）。 */
        internal fun isRefreshBreakerActive(userId: String): Boolean {
            val expiredAt = expiredUsers[userId] ?: return false
            if (expiredAt.elapsedNow() < REFRESH_BREAKER_WINDOW) {
                return true
            }
            expiredUsers.remove(userId, expiredAt)
            return false
        }

        /** 标记账号进入失效熔断窗口。 */
        internal fun markRefreshBreaker(userId: String) {
            expiredUsers[userId] = TimeSource.Monotonic.markNow()
        }

        /** 清除指定账号的失效熔断标记（刷新成功或重新登录后调用）。 */
        internal fun clearRefreshBreaker(userId: String) {
            expiredUsers.remove(userId)
        }

        /**
         * 清空熔断表。熔断表是进程级共享状态，单测必须逐用例复位，
         * 否则前一个用例判定的失效会泄漏到后一个用例（生产代码不调用）。
         */
        internal fun resetRefreshBreaker() {
            expiredUsers.clear()
        }

        override fun prepare(block: Config.() -> Unit): TokenRefreshPlugin {
            val config = Config().apply(block)
            return TokenRefreshPlugin(
                tokenStorage = checkNotNull(config.tokenStorage) { "必须设置 tokenStorage" },
                oAuthClient = checkNotNull(config.oAuthClient) { "必须设置 oAuthClient" },
                authExpirySignal = config.authExpirySignal,
            )
        }

        private fun isAnonymousEndpoint(encodedPath: String): Boolean =
            encodedPath.contains("v1/walkthrough/illusts")

        override fun install(plugin: TokenRefreshPlugin, scope: HttpClient) {
            // 请求阶段注入 Authorization（walkthrough 等匿名接口除外），并绑定发起账号 ID。
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                if (!isAnonymousEndpoint(context.url.encodedPath)) {
                    val snapshot = plugin.getAuthorizationSnapshot()
                    if (snapshot != null) {
                        context.headers {
                            if (!contains("Authorization")) {
                                append("Authorization", snapshot.header)
                                context.attributes.put(REQUEST_USER_ID_KEY, snapshot.userId)
                            }
                        }
                    }
                }
            }

            // 使用 HttpSend 拦截器实现 Token 过期自动刷新与重试。
            // Pixiv API 在 access_token 过期时可能返回 400 (invalid_grant) 或 401 (Unauthorized)。
            scope.plugin(HttpSend).intercept { request ->
                var call = execute(request)
                // 显式匿名接口直接放行原响应，绝不进入刷新或熔断逻辑。
                if (isAnonymousEndpoint(request.url.encodedPath)) {
                    return@intercept call
                }
                val hasRequestCredentials = request.attributes.contains(REQUEST_USER_ID_KEY) ||
                    !request.headers["Authorization"].isNullOrBlank()
                val status = call.response.status
                if (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.BadRequest) {
                    call = call.save()
                    val bodyText = try {
                        call.response.bodyAsText()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.w("读取认证异常响应体失败", e)
                        ""
                    }
                    when (resolveAuthRefreshDecision(status, bodyText)) {
                        AuthRefreshDecision.REFRESH -> {
                            if (!hasRequestCredentials) {
                                // 未携带凭证时：仅当当前活跃账号仍在 30s 熔断期内才快失败，
                                // 其余状态（冷启动软失效 / 冷却期已过）直接放行原响应，不刷新不熔断不发信号。
                                val activeExpiredUserId = plugin.activeBreakerUserIdOrNull()
                                if (activeExpiredUserId != null) {
                                    throw AuthExpiredException(activeExpiredUserId)
                                }
                                return@intercept call
                            }
                            Napier.i("检测到 Pixiv Access Token 失效/过期 (HTTP $status)，正在自动执行 Token 刷新并重试...")
                            val targetUserId = request.attributes.getOrNull(REQUEST_USER_ID_KEY)
                            val staleAccessToken = request.headers["Authorization"]?.removePrefix("Bearer ")
                            val freshHeader = plugin.refreshTokenOrThrow(
                                targetUserId = targetUserId,
                                staleAccessToken = staleAccessToken,
                            )
                            // 移除旧的 Authorization，仅注入本次发起账号刷新后的新 token。
                            request.headers.remove("Authorization")
                            request.headers.append("Authorization", freshHeader)
                            call = execute(request)
                            val retryStatus = call.response.status
                            if (retryStatus == HttpStatusCode.Unauthorized) {
                                call = call.save()
                                val retryBody = suspendRunCatchingNonCancel { call.response.bodyAsText() }.getOrDefault("")
                                // 异常 message 仅承载技术细节供日志，不进入用户可见文案（高-6b：UI 走 PixivApiErrorFormatter）。
                                throw PixivApiException(
                                    statusCode = retryStatus.value,
                                    message = "Still unauthorized after token refresh: $retryStatus, body=$retryBody",
                                )
                            }
                        }

                        AuthRefreshDecision.PASS_THROUGH -> if (hasRequestCredentials && status == HttpStatusCode.Unauthorized) {
                            // 非 OAuth 相关的 401（如账号被禁用、token 被撤销）直接抛异常
                            // 异常 message 仅承载技术细节供日志，不进入用户可见文案（高-6b：UI 走 PixivApiErrorFormatter）。
                            throw PixivApiException(
                                statusCode = call.response.status.value,
                                message = "Request unauthorized: ${call.response.status}, body=$bodyText",
                            )
                        }
                    }
                }
                call
            }
        }
    }

    class Config {
        var tokenStorage: AuthTokenStorage? = null
        var oAuthClient: OAuthClient? = null
        var authExpirySignal: AuthExpirySignal? = null
    }
}
