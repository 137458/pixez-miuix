package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

actual fun createPlatformHttpClient(
    modeProvider: (isOauthHost: Boolean) -> String?,
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient = createJvmHttpClient(
    // 高-7：Android 与 Desktop 共用 jvmShared 的 RobustDohDns（含熔断）与连接池构建。
    // Android 端不做桌面式系统代理探测（DoH 经 VPN/代理隧道可达），standard 分流由 modeProvider 承担。
    hasProxyProvider = { false },
    modeProvider = modeProvider,
    block = block,
)
