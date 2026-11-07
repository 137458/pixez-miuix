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
import com.perol.pixez.shared.network.PixivApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
    private val mutex = Mutex()

    /** 已确认失效的账号与时间点；同账号窗口内直接熔断。 */
    private var expiredUserId: String? = null
    private var expiredAt: TimeMark? = null

    private suspend fun getAuthorizationHeader(): String? {
        val account = tokenStorage.getCurrentAccount() ?: return null
        if (account.access_token.isBlank()) {
            // 凭证已被清理（解密失败或被判定吊销）：不再发送空 Bearer，直接提示重新登录。
            authExpirySignal?.emit(account.user_id)
            return null
        }
        return "Bearer ${account.access_token}"
    }

    private suspend fun refreshTokenOrThrow() {
        val staleAccessToken = tokenStorage.getCurrentAccount()?.access_token
        mutex.withLock {
            val currentAccount = tokenStorage.getCurrentAccount()
                ?: throw IllegalStateException("No logged-in account to refresh token")
            // 同账号近期已确认失效：熔断，避免每个请求重复发起注定失败的刷新。
            val breakerActive = expiredUserId == currentAccount.user_id &&
                expiredAt?.let { it.elapsedNow() < REFRESH_BREAKER_WINDOW } == true
            if (breakerActive) {
                authExpirySignal?.emit(currentAccount.user_id)
                throw AuthExpiredException(currentAccount.user_id)
            }
            if (currentAccount.access_token != staleAccessToken && currentAccount.access_token.isNotBlank()) {
                Napier.i("检测到 Token 已由前置并发请求完成刷新，复用最新凭证")
                return@withLock
            }
            if (currentAccount.refresh_token.isBlank()) {
                expireCredentials(currentAccount.user_id)
            }
            try {
                val refreshed = oAuthClient.refreshToken(currentAccount.refresh_token)
                // 锁内已捕获发起刷新的账号，新凭证只写回该账号行；
                // 刷新往返期间若活跃账号被切换，不得写入切换后的账号（跨账号凭证污染）。
                tokenStorage.updateTokensForUser(
                    userId = currentAccount.user_id,
                    accessToken = refreshed.response.accessToken,
                    refreshToken = refreshed.response.refreshToken,
                )
                expiredUserId = null
                expiredAt = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: PixivApiException) {
                // OAuth 端点明确拒绝（400 invalid_grant / 401）才是凭证失效；其它状态码原样抛出。
                if (e.statusCode == 400 || e.statusCode == 401) {
                    expireCredentials(currentAccount.user_id)
                }
                Napier.e("刷新 token 失败", e)
                throw e
            } catch (e: IllegalArgumentException) {
                // refresh_token 格式非法，等价于凭证失效。
                expireCredentials(currentAccount.user_id)
            } catch (e: Exception) {
                // 连接失败/超时等网络异常不得清理凭证，否则离线用户会被误判为登出。
                Napier.e("刷新 token 失败", e)
                throw e
            }
        }
    }

    /**
     * 确认凭证失效：清理账号 token、标记熔断并发信号，随后抛 [AuthExpiredException]。
     * 必须在持有 [mutex] 的上下文中调用。
     */
    private suspend fun expireCredentials(userId: String): Nothing {
        expiredUserId = userId
        expiredAt = TimeSource.Monotonic.markNow()
        tokenStorage.invalidateCredentials(userId)
        authExpirySignal?.emit(userId)
        throw AuthExpiredException(userId)
    }

    companion object : HttpClientPlugin<Config, TokenRefreshPlugin> {
        override val key: AttributeKey<TokenRefreshPlugin> = AttributeKey("TokenRefreshPlugin")

        override fun prepare(block: Config.() -> Unit): TokenRefreshPlugin {
            val config = Config().apply(block)
            return TokenRefreshPlugin(
                tokenStorage = checkNotNull(config.tokenStorage) { "必须设置 tokenStorage" },
                oAuthClient = checkNotNull(config.oAuthClient) { "必须设置 oAuthClient" },
                authExpirySignal = config.authExpirySignal,
            )
        }

        override fun install(plugin: TokenRefreshPlugin, scope: HttpClient) {
            // 请求阶段注入 Authorization（walkthrough 等匿名接口除外）。
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                if (!context.url.encodedPath.contains("v1/walkthrough/illusts")) {
                    val header = plugin.getAuthorizationHeader()
                    if (header != null) {
                        context.headers {
                            if (!contains("Authorization")) {
                                append("Authorization", header)
                            }
                        }
                    }
                }
            }

            // 使用 HttpSend 拦截器实现 Token 过期自动刷新与重试。
            // Pixiv API 在 access_token 过期时可能返回 400 (invalid_grant) 或 401 (Unauthorized)。
            scope.plugin(HttpSend).intercept { request ->
                var call = execute(request)
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
                            Napier.i("检测到 Pixiv Access Token 失效/过期 (HTTP $status)，正在自动执行 Token 刷新并重试...")
                            plugin.refreshTokenOrThrow()
                            // 移除旧的 Authorization，重新注入刷新后的 token。
                            request.headers.remove("Authorization")
                            plugin.getAuthorizationHeader()?.let {
                                request.headers.append("Authorization", it)
                            }
                            call = execute(request)
                            val retryStatus = call.response.status
                            if (retryStatus == HttpStatusCode.Unauthorized) {
                                call = call.save()
                                val retryBody = runCatching { call.response.bodyAsText() }.getOrDefault("")
                                // 异常 message 仅承载技术细节供日志，不进入用户可见文案（高-6b：UI 走 PixivApiErrorFormatter）。
                                throw PixivApiException(
                                    statusCode = retryStatus.value,
                                    message = "Still unauthorized after token refresh: $retryStatus, body=$retryBody",
                                )
                            }
                        }

                        AuthRefreshDecision.PASS_THROUGH -> if (status == HttpStatusCode.Unauthorized) {
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
