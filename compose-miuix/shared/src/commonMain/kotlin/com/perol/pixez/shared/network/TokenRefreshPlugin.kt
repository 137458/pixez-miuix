package com.perol.pixez.shared.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
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
 * 自动注入 Bearer Token 并在 401 时刷新 token 的 Ktor 插件。
 *
 * 设计要点：
 * - 除 walkthrough 等匿名接口外，所有请求自动附加 Authorization: Bearer <access_token>。
 * - 收到 401 且错误信息包含 OAuth 时，串行执行一次 token 刷新，然后重试原请求。
 * - 刷新失败或没有登录账号时，直接向上抛异常，避免无限重试。
 */
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
                ?: throw IllegalStateException("没有登录账号，无法刷新 token")
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
                    val bodyText = try {
                        call.response.bodyAsText()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.w("读取认证异常响应体失败", e)
                        ""
                    }
                    val shouldRefresh = bodyText.contains("OAuth", ignoreCase = true) ||
                            bodyText.contains("invalid_grant", ignoreCase = true) ||
                            bodyText.contains("Access Token", ignoreCase = true) ||
                            bodyText.contains("token", ignoreCase = true)
                    if (shouldRefresh) {
                        Napier.i("检测到 Pixiv Access Token 失效/过期 (HTTP $status)，正在自动执行 Token 刷新并重试...")
                        plugin.refreshTokenOrThrow()
                        // 移除旧的 Authorization，重新注入刷新后的 token。
                        request.headers.remove("Authorization")
                        plugin.getAuthorizationHeader()?.let {
                            request.headers.append("Authorization", it)
                        }
                        call = execute(request)
                    } else if (status == HttpStatusCode.Unauthorized) {
                        // 非 OAuth 相关的 401（如账号被禁用、token 被撤销）直接抛异常
                        throw PixivApiException(
                            statusCode = call.response.status.value,
                            message = "请求未授权: ${call.response.status}, body=$bodyText",
                        )
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
