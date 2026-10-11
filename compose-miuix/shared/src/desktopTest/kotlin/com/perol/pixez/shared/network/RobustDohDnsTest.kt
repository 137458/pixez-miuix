package com.perol.pixez.shared.network

import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.ui.AppConstants
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
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
    fun testLookupWithoutModeProviderIgnoresAppDependencies() {
        // 高-2：DNS 策略必须切断对 AppDependencies 静态单例的反查。
        // 构造真实容器并把网络模式置为 standard：旧实现反查单例会命中 standard 分支走系统 DNS；
        // 切断反查后，未注入 modeProvider 的 DNS 按默认策略（mode=null）继续走 DoH，
        // 不受容器在场与容器内设置值影响。容器设置先存后还原，避免污染真实用户偏好。
        val tempRoot = Files.createTempDirectory("pixez-robustdohdns-test-").toFile()
        System.setProperty("pixez.test.db.root", tempRoot.absolutePath)
        val dependencies = AppDependencies(DriverFactory(), SettingsFactory())
        val previousApiMode = dependencies.settingsRepository.apiNetworkMode
        val previousOauthMode = dependencies.settingsRepository.oauthNetworkMode
        try {
            dependencies.settingsRepository.apiNetworkMode = AppConstants.Network.MODE_STANDARD
            dependencies.settingsRepository.oauthNetworkMode = AppConstants.Network.MODE_STANDARD

            val mockDoh = MockDns("1.2.3.4")
            val mockFallback = MockDns("5.6.7.8")
            val dns = RobustDohDns(
                doh = mockDoh,
                fallback = mockFallback,
                hasProxyProvider = { false },
            )

            val result = dns.lookup("app-api.pixiv.net")

            assertEquals("1.2.3.4", result.first().hostAddress, "未注入 modeProvider 时不应反查容器单例，应走 DoH")
            assertEquals(1, mockDoh.callCount, "切断反查后应由 DoH 解析")
            assertEquals(0, mockFallback.callCount, "未注入 modeProvider 且无代理时不应走系统 DNS")
        } finally {
            dependencies.settingsRepository.apiNetworkMode = previousApiMode
            dependencies.settingsRepository.oauthNetworkMode = previousOauthMode
            dependencies.close()
            System.clearProperty("pixez.test.db.root")
            tempRoot.deleteRecursively()
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

    @Test
    fun testStandardModeProviderSkipsDoh() {
        val mockDoh = MockDns("1.2.3.4")
        val mockFallback = MockDns("5.6.7.8")

        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            modeProvider = { AppConstants.Network.MODE_STANDARD },
        )

        val result = dns.lookup("app-api.pixiv.net")

        assertEquals(0, mockDoh.callCount, "standard 模式应跳过 DoH")
        assertEquals(1, mockFallback.callCount)
        assertEquals("5.6.7.8", result.first().hostAddress)
    }

    @Test
    fun testNonStandardModeProviderUsesDoh() {
        val mockDoh = MockDns("1.2.3.4")
        val mockFallback = MockDns("5.6.7.8")

        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            // "ech" 为应用内网络模式真实默认值（SettingsRepository.DEFAULT_NETWORK_MODE）。
            modeProvider = { "ech" },
        )

        val result = dns.lookup("app-api.pixiv.net")

        assertEquals(1, mockDoh.callCount, "非 standard 模式应由 DoH 解析")
        assertEquals(0, mockFallback.callCount)
        assertEquals("1.2.3.4", result.first().hostAddress)
    }

    @Test
    fun testNullModeProviderUsesDoh() {
        val mockDoh = MockDns("1.2.3.4")
        val mockFallback = MockDns("5.6.7.8")

        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            modeProvider = { null },
        )

        val result = dns.lookup("app-api.pixiv.net")

        assertEquals(1, mockDoh.callCount, "mode=null（容器缺省时的历史取值）应继续走 DoH 而非回退")
        assertEquals(0, mockFallback.callCount)
        assertEquals("1.2.3.4", result.first().hostAddress)
    }

    @Test
    fun testModeProviderRoutesOauthAndApiHosts() {
        val mockDoh = MockDns("1.2.3.4")
        val mockFallback = MockDns("5.6.7.8")
        val capturedFlags = mutableListOf<Boolean>()

        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            modeProvider = { isOauthHost ->
                capturedFlags += isOauthHost
                // OAuth 域配置为 standard（走系统 DNS），API 域配置为 ech（走 DoH）。
                if (isOauthHost) AppConstants.Network.MODE_STANDARD else "ech"
            },
        )

        val oauthResult = dns.lookup("oauth.secure.pixiv.net")
        assertEquals("5.6.7.8", oauthResult.first().hostAddress, "OAuth 域应按 oauthNetworkMode=standard 走系统 DNS")
        val accountsResult = dns.lookup("accounts.pixiv.net")
        assertEquals("5.6.7.8", accountsResult.first().hostAddress, "accounts 域应视为 OAuth 域")
        val apiResult = dns.lookup("app-api.pixiv.net")
        assertEquals("1.2.3.4", apiResult.first().hostAddress, "API 域应按 apiNetworkMode=ech 走 DoH")

        assertEquals(listOf(true, true, false), capturedFlags, "主机启发式应正确区分 OAuth 域与 API 域")
        assertEquals(1, mockDoh.callCount)
        assertEquals(2, mockFallback.callCount)
    }

    @Test
    fun testCircuitStateSharedAcrossDnsInstances() {
        // 全部 client 的 DNS 实例共享一份熔断状态，保持改造前单例 DNS 的全局熔断语义：
        // 任一实例的 DoH 失败应使其他实例在冷却期内直接走系统 DNS。
        val circuitState = DohCircuitState()
        val failingDoh = MockDns("1.2.3.4", shouldThrow = true)
        val healthyDoh = MockDns("9.9.9.9")
        val mockFallback = MockDns("5.6.7.8")

        val first = RobustDohDns(
            doh = failingDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            modeProvider = { "ech" },
            circuitBreakerDurationMillis = 60_000L,
            circuitState = circuitState,
        )
        first.lookup("first.com")
        assertTrue(first.isCircuitOpen, "首个实例 DoH 失败应触发熔断")

        val second = RobustDohDns(
            doh = healthyDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            modeProvider = { "ech" },
            circuitBreakerDurationMillis = 60_000L,
            circuitState = circuitState,
        )

        assertTrue(second.isCircuitOpen, "共享熔断状态下，另一实例的失败应使本实例处于熔断开启")
        val result = second.lookup("second.com")
        assertEquals(0, healthyDoh.callCount, "共享熔断开启时不应再次尝试 DoH")
        assertEquals("5.6.7.8", result.first().hostAddress)
    }
}
