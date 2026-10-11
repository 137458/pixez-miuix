package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 创建用于检查 GitHub Release 的 HttpClient，复用平台网络引擎（含 DoH / 代理支持）。
 *
 * @param modeProvider DoH 网络模式提供者，语义见 [createPlatformHttpClient]；
 *   由 AppDependencies 注入，独立惰性单例 [defaultUpdateCheckClient] 无容器可达、走默认策略。
 */
internal fun createUpdateCheckClient(
    modeProvider: (isOauthHost: Boolean) -> String? = { null },
): HttpClient = createPlatformHttpClient(modeProvider) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            },
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 10_000
    }
}

/**
 * 复用的 GitHub API HttpClient。
 */
internal val defaultUpdateCheckClient: HttpClient by lazy {
    createUpdateCheckClient()
}
