package com.perol.pixez.shared.network

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

/**
 * DoH 熔断共享状态。历史上全部 client 复用同一个 RobustDohDns 单例、共享一份熔断状态；
 * 改为按 client 注入 modeProvider 构造 DNS 实例后，通过显式传递本对象保持
 * 「任一 client 的 DoH 失败即对全部 client 生效」的原有全局熔断语义。
 */
internal class DohCircuitState {
    @Volatile
    var openUntilMillis: Long = 0L
}

internal class RobustDohDns(
    private val doh: Dns,
    private val fallback: Dns = Dns.SYSTEM,
    private val hasProxyProvider: () -> Boolean = ::detectSystemHasProxy,
    private val modeProvider: (isOauthHost: Boolean) -> String? = { null },
    private val circuitBreakerDurationMillis: Long = 5 * 60 * 1000L,
    private val circuitState: DohCircuitState = DohCircuitState(),
) : Dns {
    val isCircuitOpen: Boolean
        get() = System.currentTimeMillis() < circuitState.openUntilMillis

    override fun lookup(hostname: String): List<InetAddress> {
        val isOauthHost = hostname.contains("oauth", ignoreCase = true) ||
            hostname.contains("accounts.pixiv.net", ignoreCase = true)
        val mode = modeProvider(isOauthHost)
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
            circuitState.openUntilMillis = System.currentTimeMillis() + circuitBreakerDurationMillis
            Napier.w("Desktop DoH 解析域名失败: $hostname，触发熔断（${circuitBreakerDurationMillis / 1000}秒），回退至系统 DNS", e, tag = "RobustDohDns")
            fallback.lookup(hostname)
        }
    }
}

/** 底层 DoH 解析器（初始化失败回退系统 DNS），全部 client 共用；网络模式由各 client 的 modeProvider 决定。 */
private val dohResolver: Dns by lazy {
    try {
        val bootstrapClient = OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .connectTimeout(AppConstants.Network.DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.Network.DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val bootstrapIps = AppConstants.Network.DOH_BOOTSTRAP_HOSTS.map { InetAddress.getByName(it) }
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(AppConstants.Network.DOH_URL.toHttpUrl())
            .bootstrapDnsHosts(bootstrapIps)
            .includeIPv6(false)
            .build()
    } catch (e: Throwable) {
        Napier.w("Desktop 初始化 DoH 客户端失败，回退至系统 DNS", e, tag = "RobustDohDns")
        Dns.SYSTEM
    }
}

/** 全部 client 共享的熔断状态，保持原单例 DNS 的全局熔断语义。 */
private val sharedCircuitState = DohCircuitState()

actual fun createPlatformHttpClient(
    modeProvider: (isOauthHost: Boolean) -> String?,
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                connectionPool(sharedConnectionPool)
                dispatcher(sharedDispatcher)
                dns(
                    RobustDohDns(
                        doh = dohResolver,
                        fallback = Dns.SYSTEM,
                        modeProvider = modeProvider,
                        circuitState = sharedCircuitState,
                    )
                )
                followRedirects(true)
                retryOnConnectionFailure(true)
            }
        }
        block()
    }
