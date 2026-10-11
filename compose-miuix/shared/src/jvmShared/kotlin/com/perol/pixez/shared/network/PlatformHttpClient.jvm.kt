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
        Napier.w("初始化 DoH 客户端失败，回退至系统 DNS", e, tag = "RobustDohDns")
        Dns.SYSTEM
    }
}

/** 全部 client 共享的熔断状态，保持单例 DNS 时代的全局熔断语义。 */
private val sharedCircuitState = DohCircuitState()

/**
 * 双端（Android/Desktop）共用的 OkHttp 深度优化 client 构建（高-7 收敛自两份平台 actual）：
 * 连接池/并发上限/DoH 熔断 DNS 逻辑单点维护，仅平台差异经 [hasProxyProvider] 注入。
 *
 * @param hasProxyProvider 平台代理探测策略（desktop 探测系统代理；Android 恒 false）
 * @param modeProvider DoH 网络模式提供者，语义见 [createPlatformHttpClient]
 */
internal fun createJvmHttpClient(
    hasProxyProvider: () -> Boolean,
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
                        hasProxyProvider = hasProxyProvider,
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
