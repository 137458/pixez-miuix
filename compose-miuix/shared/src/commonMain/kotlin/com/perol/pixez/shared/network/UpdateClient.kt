package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 创建用于检查 GitHub Release 的 HttpClient，复用平台网络引擎（含 DoH / 代理支持）。
 */
internal fun createUpdateCheckClient(): HttpClient = createPlatformHttpClient {
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
