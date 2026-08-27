package com.perol.pixez.shared.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.head
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Pixiv API 与 OAuth 的 Ktor HttpClient 工厂。
 *
 * 分别创建：
 * - [apiClient]：用于 app-api.pixiv.net 的业务接口。
 * - [baseOAuthClient]：用于 oauth.secure.pixiv.net 的基础认证接口，被 [oAuthClient] 持有。
 *
 * 注意：[OAuthClient] 内部使用一个不带 token 刷新的基础 HttpClient 执行登录/刷新，
 * 避免与 [TokenRefreshPlugin] 形成循环依赖。
 */
class PixivHttpClient(
    tokenStorage: AuthTokenStorage,
    languageProvider: () -> String = { "zh-CN" },
    enableLogging: Boolean = false,
) {
    /**
     * 用于 OAuth 登录/刷新 token 的基础客户端，不带 TokenRefreshPlugin。
     */
    private val baseOAuthClient: HttpClient = createBaseOAuthClient(languageProvider, enableLogging)

    /**
     * OAuth 业务封装，外部可用它构建登录 URL 或手动刷新 token。
     */
    val oAuthClient: OAuthClient = OAuthClient(baseOAuthClient)

    /**
     * 业务 API 客户端。
     */
    val apiClient: HttpClient = createClient(
        host = APP_API_HOST,
        tokenStorage = tokenStorage,
        oAuthClient = oAuthClient,
        languageProvider = languageProvider,
        enableLogging = enableLogging,
    )

    /**
     * 账号管理客户端，用于 accounts.pixiv.net 的账号信息编辑等接口。
     */
    val accountClient: HttpClient = createClient(
        host = ACCOUNTS_HOST,
        tokenStorage = tokenStorage,
        oAuthClient = oAuthClient,
        languageProvider = languageProvider,
        enableLogging = enableLogging,
    )

    /**
     * 图片下载客户端，用于从 i.pximg.net 下载原图。
     *
     * 不使用 TokenRefreshPlugin，避免图片 401 时触发 OAuth 刷新逻辑；
     * 默认请求头仅包含 Referer，满足 Pixiv 图片防盗链要求。
     */
    val downloadClient: HttpClient = HttpClient {
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 2)
        }
        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(message, tag = "PixivDownload")
                    }
                }
                level = LogLevel.INFO
            }
        }
        defaultRequest {
            headers.append("Referer", "https://app-api.pixiv.net/")
        }
        // 图片下载也需要校验 HTTP 状态，避免把 403/404 的错误页面保存为图片文件。
        HttpResponseValidator {
            validateResponse { response: HttpResponse ->
                if (response.status.value >= 400) {
                    throw PixivApiException(
                        statusCode = response.status.value,
                        message = "下载请求失败: ${response.status}",
                    )
                }
            }
        }
    }

    /**
     * 释放所有 Ktor HttpClient 占用的引擎资源。
     */
    fun close() {
        runCatching { apiClient.close() }
        runCatching { accountClient.close() }
        runCatching { downloadClient.close() }
        runCatching { baseOAuthClient.close() }
    }

    /**
     * 网络连接与 TLS 握手预热。
     * 在后台静默预连接常用 API 与图片域名，消除冷启动首屏网络请求时的 DNS 解析与 TLS 握手延迟。
     */
    suspend fun warmup() {
        runCatching {
            apiClient.head("https://$APP_API_HOST/favicon.ico")
        }
        runCatching {
            downloadClient.head("https://i.pximg.net/favicon.ico")
        }
    }

    companion object {
        private const val APP_API_HOST = "app-api.pixiv.net"
        private const val OAUTH_HOST = "oauth.secure.pixiv.net"
        private const val ACCOUNTS_HOST = "accounts.pixiv.net"

        private val defaultJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        /**
         * 创建不带 TokenRefreshPlugin 的基础 OAuth 客户端，供 [OAuthClient] 内部使用。
         */
        private fun createBaseOAuthClient(
            languageProvider: () -> String,
            enableLogging: Boolean,
        ): HttpClient = HttpClient {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = OAUTH_HOST
                }
                PixivHeaders.commonHeaders(languageProvider()).forEach { (key, value) ->
                    headers[key] = value
                }
            }
            install(ContentNegotiation) {
                json(defaultJson)
            }
            if (enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Napier.d(message, tag = "PixivOAuth")
                        }
                    }
                    // 避免打印 Authorization 头或 token 响应体。
                    level = LogLevel.INFO
                }
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 2)
            }
            HttpResponseValidator {
                validateResponse { response: HttpResponse ->
                    if (response.status.value >= 400) {
                        val errorDetail = try {
                            val body = response.bodyAsText()
                            if (body.isNotBlank()) {
                                try {
                                    val parsed = defaultJson
                                        .decodeFromString<com.perol.pixez.shared.data.model.LoginErrorResponse>(body)
                                    parsed.errors.system.message
                                } catch (_: Exception) {
                                    body.take(200)
                                }
                            } else {
                                null
                            }
                        } catch (_: Exception) {
                            null
                        }

                        val message = if (!errorDetail.isNullOrBlank()) {
                            "OAuth 认证失败: $errorDetail (${response.status})"
                        } else {
                            "OAuth 请求失败: ${response.status}"
                        }

                        throw PixivApiException(
                            statusCode = response.status.value,
                            message = message,
                        )
                    }
                }
            }
        }

        fun createClient(
            host: String,
            tokenStorage: AuthTokenStorage,
            oAuthClient: OAuthClient,
            languageProvider: () -> String,
            enableLogging: Boolean,
        ): HttpClient = HttpClient {
            // 统一使用 HTTPS 与固定 Host。
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    this.host = host
                }
                PixivHeaders.commonHeaders(languageProvider()).forEach { (key, value) ->
                    headers.append(key, value)
                }
            }

            // JSON 序列化：忽略未知字段，宽松解析。
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        isLenient = true
                    },
                )
            }

            // 日志：仅打印请求方法与 URL，避免泄露 Authorization 头。
            if (enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Napier.d(message, tag = "PixivHttp")
                        }
                    }
                    level = LogLevel.INFO
                }
            }

            // 网络层通用重试：连接失败时最多重试 2 次。
            // 先安装 HttpRequestRetry，再安装 TokenRefreshPlugin，确保 401 刷新逻辑在外层，
            // 避免刷新失败时被外层重试反复触发。
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 2)
            }

            // Token 注入与刷新。
            install(TokenRefreshPlugin) {
                this.tokenStorage = tokenStorage
                this.oAuthClient = oAuthClient
            }

            // 响应校验：非 2xx 统一抛出异常。
            HttpResponseValidator {
                validateResponse { response: HttpResponse ->
                    // 401 已由 TokenRefreshPlugin 处理（刷新或抛异常），此处不再重复处理。
                    if (response.status.value >= 400 && response.status != HttpStatusCode.Unauthorized) {
                        val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
                        Napier.e("Pixiv API 请求失败 status=${response.status}, url=${response.call.request.url}, body=$bodyText")
                        throw PixivApiException(
                            statusCode = response.status.value,
                            message = if (bodyText.isNotBlank()) "请求失败: ${response.status} ($bodyText)" else "请求失败: ${response.status}",
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pixiv API 返回的非 2xx 响应异常。
 */
class PixivApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)
