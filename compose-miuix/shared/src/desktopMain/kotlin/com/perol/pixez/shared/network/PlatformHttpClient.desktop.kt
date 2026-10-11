package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

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

actual fun createPlatformHttpClient(
    modeProvider: (isOauthHost: Boolean) -> String?,
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient = createJvmHttpClient(
    // 高-7：RobustDohDns（含熔断）与连接池构建收敛至 jvmShared，桌面端差异仅保留系统代理探测注入。
    hasProxyProvider = ::detectSystemHasProxy,
    modeProvider = modeProvider,
    block = block,
)
