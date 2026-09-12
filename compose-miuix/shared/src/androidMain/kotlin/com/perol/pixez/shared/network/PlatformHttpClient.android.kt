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
import io.github.aakira.napier.Napier
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
        } catch (e: Throwable) {
            Napier.w("DoH 解析域名失败: $hostname，回退至系统 DNS", e, tag = "RobustDohDns")
            fallback.lookup(hostname)
        }
    }
}

private val robustDns: Dns by lazy {
    try {
        val bootstrapClient = OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .connectTimeout(AppConstants.Network.DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.Network.DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val bootstrapIps = AppConstants.Network.DOH_BOOTSTRAP_HOSTS.map { InetAddress.getByName(it) }
        val doh = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(AppConstants.Network.DOH_URL.toHttpUrl())
            .bootstrapDnsHosts(bootstrapIps)
            .includeIPv6(false)
            .build()
        RobustDohDns(doh, Dns.SYSTEM)
    } catch (e: Throwable) {
        Napier.w("初始化 DoH 客户端失败，回退至系统 DNS", e, tag = "RobustDohDns")
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

