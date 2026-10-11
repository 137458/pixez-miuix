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

/**
 * 自动注入 Bearer Token 并在凭证失效时刷新 token 的 Ktor 插件。
 *
 * 设计要点：
 * - 除 walkthrough 等匿名接口外，所有请求自动附加 Authorization: Bearer <access_token>。
 * - 收到 401（响应体含 OAuth 相关特征）或 400 invalid_grant 时，串行执行一次 token 刷新，然后重试原请求。
 * - 刷新失败或没有登录账号时，直接向上抛异常，避免无限重试。
 */
internal enum class AuthRefreshDecision { REFRESH, PASS_THROUGH }

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
) {
    private val mutex = Mutex()

    private suspend fun getAuthorizationHeader(): String? {
        val account = tokenStorage.getCurrentAccount() ?: return null
        return "Bearer ${account.access_token}"
    }

    private suspend fun refreshTokenOrThrow() {
        val staleAccessToken = tokenStorage.getCurrentAccount()?.access_token
        mutex.withLock {
            val currentAccount = tokenStorage.getCurrentAccount()
                ?: throw IllegalStateException("No logged-in account to refresh token")
            if (currentAccount.access_token != staleAccessToken && currentAccount.access_token.isNotBlank()) {
                Napier.i("检测到 Token 已由前置并发请求完成刷新，复用最新凭证")
                return@withLock
            }
            try {
                val refreshed = oAuthClient.refreshToken(currentAccount.refresh_token)
                tokenStorage.updateTokens(
                    accessToken = refreshed.response.accessToken,
                    refreshToken = refreshed.response.refreshToken,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("刷新 token 失败", e)
                throw e
            }
        }
    }

    companion object : HttpClientPlugin<Config, TokenRefreshPlugin> {
        override val key: AttributeKey<TokenRefreshPlugin> = AttributeKey("TokenRefreshPlugin")

        override fun prepare(block: Config.() -> Unit): TokenRefreshPlugin {
            val config = Config().apply(block)
            return TokenRefreshPlugin(
                tokenStorage = checkNotNull(config.tokenStorage) { "必须设置 tokenStorage" },
                oAuthClient = checkNotNull(config.oAuthClient) { "必须设置 oAuthClient" },
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
    }
}
