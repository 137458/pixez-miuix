package com.perol.pixez.shared.network

import com.perol.pixez.shared.ui.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
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

private class RobustDohDns(private val doh: Dns, private val fallback: Dns = Dns.SYSTEM) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            val addresses = doh.lookup(hostname)
            if (addresses.isNotEmpty()) addresses else fallback.lookup(hostname)
        } catch (_: Throwable) {
            fallback.lookup(hostname)
        }
    }
}

private val robustDns: Dns by lazy {
    try {
        val bootstrapClient = OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val doh = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
                InetAddress.getByName("223.5.5.5"),
                InetAddress.getByName("223.6.6.6"),
            )
            .includeIPv6(false)
            .build()
        RobustDohDns(doh, Dns.SYSTEM)
    } catch (_: Throwable) {
        Dns.SYSTEM
    }
}

actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                connectionPool(sharedConnectionPool)
                dispatcher(sharedDispatcher)
                dns(robustDns)
                followRedirects(true)
                retryOnConnectionFailure(true)
            }
        }
        block()
    }

