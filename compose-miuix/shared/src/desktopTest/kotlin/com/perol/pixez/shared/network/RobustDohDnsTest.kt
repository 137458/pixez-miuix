package com.perol.pixez.shared.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RobustDohDnsTest {

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
    fun testHasProxySkipsDoh() {
        val mockDoh = MockDns("1.2.3.4")
        val mockFallback = MockDns("5.6.7.8")

        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { true },
        )

        val result = dns.lookup("example.com")
        assertEquals(0, mockDoh.callCount, "有代理时应跳过 DoH")
        assertEquals(1, mockFallback.callCount)
        assertEquals("5.6.7.8", result.first().hostAddress)
    }

    @Test
    fun testCircuitBreakerTripsOnFailure() {
        val mockDoh = MockDns("1.2.3.4", shouldThrow = true)
        val mockFallback = MockDns("5.6.7.8")

        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            circuitBreakerDurationMillis = 60_000L,
        )

        assertFalse(dns.isCircuitOpen)

        // 第一次查询失败，触发熔断
        val result1 = dns.lookup("first.com")
        assertEquals(1, mockDoh.callCount)
        assertEquals(1, mockFallback.callCount)
        assertEquals("5.6.7.8", result1.first().hostAddress)
        assertTrue(dns.isCircuitOpen, "DoH 失败后应进入熔断开启状态")

        // 紧接着的第二次查询应该直接走 fallback，不再次调用 mockDoh
        val result2 = dns.lookup("second.com")
        assertEquals(1, mockDoh.callCount, "熔断期间不应再次调用 DoH 造成额外超时卡死")
        assertEquals(2, mockFallback.callCount)
        assertEquals("5.6.7.8", result2.first().hostAddress)
    }
}
