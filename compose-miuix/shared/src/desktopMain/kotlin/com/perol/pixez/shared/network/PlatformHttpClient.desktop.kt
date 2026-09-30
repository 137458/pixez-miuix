package com.perol.pixez.shared.network

import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.ui.AppConstants
import io.github.aakira.napier.Napier
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

internal fun detectSystemHasProxy(): Boolean {
    if (!System.getProperty("http.proxyHost").isNullOrBlank() ||
        !System.getProperty("https.proxyHost").isNullOrBlank() ||
        !System.getProperty("socksProxyHost").isNullOrBlank()
    ) {
        return true
    }
    if (!System.getenv("HTTP_PROXY").isNullOrBlank() ||
        !System.getenv("http_proxy").isNullOrBlank() ||
        !System.getenv("HTTPS_PROXY").isNullOrBlank() ||
        !System.getenv("https_proxy").isNullOrBlank() ||
        !System.getenv("ALL_PROXY").isNullOrBlank() ||
        !System.getenv("all_proxy").isNullOrBlank()
    ) {
        return true
    }
    return try {
        val proxies = java.net.ProxySelector.getDefault()?.select(java.net.URI("https://app-api.pixiv.net/"))
        proxies?.any { it.type() != java.net.Proxy.Type.DIRECT } == true
    } catch (_: Throwable) {
        false
    }
}

internal class RobustDohDns(
    private val doh: Dns,
    private val fallback: Dns = Dns.SYSTEM,
    private val hasProxyProvider: () -> Boolean = ::detectSystemHasProxy,
    private val circuitBreakerDurationMillis: Long = 5 * 60 * 1000L,
) : Dns {
    private var circuitOpenUntil = 0L

    val isCircuitOpen: Boolean
        get() = System.currentTimeMillis() < circuitOpenUntil

    override fun lookup(hostname: String): List<InetAddress> {
        val settings = AppDependencies.orNull()?.settingsRepository
        val isOauthHost = hostname.contains("oauth", ignoreCase = true) ||
            hostname.contains("accounts.pixiv.net", ignoreCase = true)
        val mode = if (isOauthHost) settings?.oauthNetworkMode else settings?.apiNetworkMode
        // 1. 若配置为 standard 模式，或检测到当前系统有活跃代理（Windows 注册表/环境变量/JVM 代理），直接走系统 DNS（远端由代理节点解析），跳过 DoH 避免阻塞
        if (mode == AppConstants.Network.MODE_STANDARD || hasProxyProvider()) {
            return fallback.lookup(hostname)
        }
        // 2. 检查熔断状态：若 DoH 处于熔断冷却中，直接回退系统 DNS，防止并发请求逐个卡死 5 秒超时
        if (isCircuitOpen) {
            return fallback.lookup(hostname)
        }
        return try {
            val addresses = doh.lookup(hostname)
            if (addresses.isNotEmpty()) addresses else fallback.lookup(hostname)
        } catch (e: Throwable) {
            circuitOpenUntil = System.currentTimeMillis() + circuitBreakerDurationMillis
            Napier.w("Desktop DoH 解析域名失败: $hostname，触发熔断（${circuitBreakerDurationMillis / 1000}秒），回退至系统 DNS", e, tag = "RobustDohDns")
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
        Napier.w("Desktop 初始化 DoH 客户端失败，回退至系统 DNS", e, tag = "RobustDohDns")
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
