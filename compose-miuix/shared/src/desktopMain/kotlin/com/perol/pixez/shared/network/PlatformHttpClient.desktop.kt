package com.perol.pixez.shared.network

import com.perol.pixez.shared.ui.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.util.concurrent.TimeUnit

private val sharedConnectionPool = ConnectionPool(
    AppConstants.Network.HTTP_POOL_MAX_IDLE_CONNECTIONS,
    AppConstants.Network.HTTP_POOL_KEEP_ALIVE_DURATION_MINUTES,
    TimeUnit.MINUTES,
)
private val sharedDispatcher = Dispatcher().apply {
    maxRequests = AppConstants.Network.HTTP_DISPATCHER_MAX_REQUESTS
    maxRequestsPerHost = AppConstants.Network.HTTP_DISPATCHER_MAX_REQUESTS_PER_HOST
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
