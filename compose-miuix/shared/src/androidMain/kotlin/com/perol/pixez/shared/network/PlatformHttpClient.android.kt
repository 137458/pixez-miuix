package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.util.concurrent.TimeUnit

private val sharedConnectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)
private val sharedDispatcher = Dispatcher().apply {
    maxRequests = 128
    maxRequestsPerHost = 32
}

actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                connectionPool(sharedConnectionPool)
                dispatcher(sharedDispatcher)
                followRedirects(true)
                retryOnConnectionFailure(true)
            }
        }
        block()
    }

