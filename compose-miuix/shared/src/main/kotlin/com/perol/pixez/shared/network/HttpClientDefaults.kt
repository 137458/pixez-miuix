package com.perol.pixez.shared.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logger
import kotlinx.serialization.json.Json

/**
 * Ktor HttpClient 通用默认配置收敛点（架构审查 中-11）。
 *
 * 此前超时/重试/日志器/Json 配置在六个构建点重复手写，调参需多处同步且易漂移；
 * 全部构建点（[PixivHttpClient] 的 api/account/download/oauth 客户端、
 * webClient/boardHttpClient、更新检查客户端）统一改引本对象与同文件的超时/重试扩展。
 * 与 [PixivHosts] 同口径：下层 network 所需默认值收敛于此，不反向依赖 ui 层 AppConstants。
 */
object HttpClientDefaults {
    /**
     * API 档位：app-api / OAuth / 图片下载客户端。
     * 整请求 30s、连接 10s、socket 15s。
     */
    const val API_REQUEST_TIMEOUT_MILLIS = 30_000L
    const val API_CONNECT_TIMEOUT_MILLIS = 10_000L
    const val API_SOCKET_TIMEOUT_MILLIS = 15_000L

    /**
     * Web 档位：网页抓取 / 公告板 / 更新检查客户端。
     * 整请求 15s、连接 10s、socket 10s。
     */
    const val WEB_REQUEST_TIMEOUT_MILLIS = 15_000L
    const val WEB_CONNECT_TIMEOUT_MILLIS = 10_000L
    const val WEB_SOCKET_TIMEOUT_MILLIS = 10_000L

    /** 网络层通用重试次数：连接失败或服务端错误时最多重试 2 次。 */
    const val MAX_RETRIES = 2

    /**
     * 通用 JSON 配置：忽略未知字段、非法输入取默认值、宽松解析。
     * kotlinx.serialization Json 实例不可变，可跨客户端共享。
     */
    val commonJson: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /**
     * Napier 转发的 Ktor 日志器，按 [tag] 区分客户端；
     * 调用方以 LogLevel.INFO 安装，避免打印 Authorization 头或 token 响应体。
     */
    fun napierLogger(tag: String): Logger = object : Logger {
        override fun log(message: String) {
            Napier.d(message, tag = tag)
        }
    }
}

/**
 * 安装 API 档位超时（app-api / OAuth / 图片下载客户端），数值见 [HttpClientDefaults]。
 */
fun HttpClientConfig<*>.apiTimeouts() {
    install(HttpTimeout) {
        requestTimeoutMillis = HttpClientDefaults.API_REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = HttpClientDefaults.API_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = HttpClientDefaults.API_SOCKET_TIMEOUT_MILLIS
    }
}

/**
 * 安装 Web 档位超时（网页抓取 / 公告板 / 更新检查客户端），数值见 [HttpClientDefaults]。
 */
fun HttpClientConfig<*>.webTimeouts() {
    install(HttpTimeout) {
        requestTimeoutMillis = HttpClientDefaults.WEB_REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = HttpClientDefaults.WEB_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = HttpClientDefaults.WEB_SOCKET_TIMEOUT_MILLIS
    }
}

/**
 * 安装网络层通用重试：连接失败或服务端错误时最多重试 [HttpClientDefaults.MAX_RETRIES] 次。
 */
fun HttpClientConfig<*>.defaultRetry() {
    install(HttpRequestRetry) {
        retryOnExceptionOrServerErrors(maxRetries = HttpClientDefaults.MAX_RETRIES)
    }
}
