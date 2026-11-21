package com.perol.pixez.shared.network

import io.github.aakira.napier.Napier
import okhttp3.Dns
import java.net.InetAddress

/**
 * DoH 熔断共享状态。全部 client 共用一份 DNS 实例池，通过显式传递本对象保持
 * 「任一 client 的 DoH 失败即对全部 client 生效」的全局熔断语义。
 */
internal class DohCircuitState {
    @Volatile
    var openUntilMillis: Long = 0L
}

/**
 * Android / Desktop 双端共用的 DoH + 系统 DNS 兜底解析器（高-7 收敛自两份平台实现）。
 *
 * @param doh 底层 DoH 解析器
 * @param fallback DoH 失败/跳过时的系统 DNS 兜底
 * @param hasProxyProvider 系统代理探测策略，由平台 actual 注入：
 *   desktop 探测 JVM/环境变量代理（远端由代理节点解析，跳过 DoH 避免阻塞）；
 *   Android 恒为 false（DoH 经 VPN/代理隧道可达，standard 分流由 modeProvider 承担）。
 * @param modeProvider 网络模式提供者（standard 走系统 DNS，其余走 DoH），由装配层注入
 * @param circuitBreakerDurationMillis 熔断冷却时长；冷却期内直接回退系统 DNS，
 *   防止 DoH 不可达时并发请求逐个卡死 5 秒超时（Android 端补齐的保护，双端一致为 5 分钟）
 */
internal class RobustDohDns(
    private val doh: Dns,
    private val fallback: Dns = Dns.SYSTEM,
    private val hasProxyProvider: () -> Boolean = { false },
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
        // 1. 若配置为 standard 模式，或平台注入的代理策略命中，直接走系统 DNS（远端由代理节点解析），跳过 DoH 避免阻塞
        if (mode == HttpEngineDefaults.MODE_STANDARD || hasProxyProvider()) {
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
            Napier.w("DoH 解析域名失败: $hostname，触发熔断（${circuitBreakerDurationMillis / 1000}秒），回退至系统 DNS", e, tag = "RobustDohDns")
            fallback.lookup(hostname)
        }
    }
}
