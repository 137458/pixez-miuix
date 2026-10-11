package com.perol.pixez.shared.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 高-7：Android 端 RobustDohDns 熔断分支测试（androidUnitTest，走 Android 编译变体）。
 *
 * 附加要求「Android 补熔断：DoH 不可达时退回系统 DNS，不再每次解析阻塞 5 秒」：
 * DoH 目标（cloudflare-dns.com）在用户网络中不可达时，首次解析等待 bootstrap 5 秒超时后回退；
 * 熔断必须在冷却期内拦截后续解析对 DoH 的重试，否则每次解析都再卡 5 秒。
 */
class AndroidRobustDohDnsTest {

    private class MockDns(private val ip: String, val shouldThrow: Boolean = false) : Dns {
        var callCount = 0
        override fun lookup(hostname: String): List<InetAddress> {
            callCount++
            if (shouldThrow) {
                throw UnknownHostException("Mock network timeout")
            }
            return listOf(InetAddress.getByName(ip))
        }
    }

    @Test
    fun testCircuitBreakerStopsRepeatedDohAttemptsAfterFailure() {
        val failingDoh = MockDns("1.2.3.4", shouldThrow = true)
        val mockFallback = MockDns("5.6.7.8")

        // 按当前 Android 实际构造面构造：ech 模式（非 standard）走 DoH。
        val dns = RobustDohDns(
            doh = failingDoh,
            fallback = mockFallback,
            modeProvider = { "ech" },
        )

        // 第一次解析：DoH 失败，回退系统 DNS 仍要给出结果。
        val first = dns.lookup("first.com")
        assertEquals(1, failingDoh.callCount, "首次解析应尝试 DoH")
        assertEquals("5.6.7.8", first.first().hostAddress, "DoH 失败后应回退系统 DNS")

        // 第二次解析：熔断冷却期内不得再次尝试 DoH（否则每次解析都阻塞 5 秒）。
        val second = dns.lookup("second.com")
        assertEquals(1, failingDoh.callCount, "DoH 失败触发熔断后，后续解析不得再次尝试 DoH")
        assertEquals("5.6.7.8", second.first().hostAddress, "熔断期间应直接走系统 DNS")
    }
}
