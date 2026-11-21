package com.perol.pixez.shared.network

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * HttpClientDefaults 收敛配置契约（架构审查 中-11）：
 * 超时/重试数值与收敛前各构建点手写字面量等价，commonJson 三项配置真实生效，
 * napierLogger 经 Napier 以给定 tag 转发。
 */
class HttpClientDefaultsTest {

    @Serializable
    private data class Payload(
        val id: Int = 0,
        val name: String = "",
    )

    // —— 超时/重试数值与收敛前字面量等价：API 档位原为 download/oauth/api 三处 30s/10s/15s ——

    @Test
    fun apiTimeoutPresetMatchesLegacyValues() {
        assertEquals(30_000L, HttpClientDefaults.API_REQUEST_TIMEOUT_MILLIS)
        assertEquals(10_000L, HttpClientDefaults.API_CONNECT_TIMEOUT_MILLIS)
        assertEquals(15_000L, HttpClientDefaults.API_SOCKET_TIMEOUT_MILLIS)
    }

    // —— Web 档位原为 web/board/update 三处 15s/10s/10s ——

    @Test
    fun webTimeoutPresetMatchesLegacyValues() {
        assertEquals(15_000L, HttpClientDefaults.WEB_REQUEST_TIMEOUT_MILLIS)
        assertEquals(10_000L, HttpClientDefaults.WEB_CONNECT_TIMEOUT_MILLIS)
        assertEquals(10_000L, HttpClientDefaults.WEB_SOCKET_TIMEOUT_MILLIS)
    }

    @Test
    fun maxRetriesMatchesLegacyValue() {
        assertEquals(2, HttpClientDefaults.MAX_RETRIES)
    }

    // —— commonJson 三项配置逐项验证：ignoreUnknownKeys / coerceInputValues / isLenient ——

    @Test
    fun commonJsonIgnoresUnknownKeys() {
        val payload = HttpClientDefaults.commonJson.decodeFromString<Payload>(
            """{"id":1,"name":"a","futureField":{"nested":42}}""",
        )
        assertEquals(Payload(id = 1, name = "a"), payload)
    }

    @Test
    fun commonJsonCoercesNullInputToDefaults() {
        val payload = HttpClientDefaults.commonJson.decodeFromString<Payload>(
            """{"id":null,"name":"a"}""",
        )
        assertEquals(Payload(id = 0, name = "a"), payload)
    }

    @Test
    fun commonJsonAcceptsLenientUnquotedString() {
        val payload = HttpClientDefaults.commonJson.decodeFromString<Payload>(
            """{"id":1,"name":unquoted_value}""",
        )
        assertEquals(Payload(id = 1, name = "unquoted_value"), payload)
    }

    // —— napierLogger 经 Napier 以指定 tag 转发消息 ——

    private class CapturingAntilog : Antilog() {
        val records = mutableListOf<Pair<String?, String?>>()

        override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
            records += tag to message
        }
    }

    @Test
    fun napierLoggerForwardsMessageWithGivenTag() {
        val antilog = CapturingAntilog()
        Napier.base(antilog)

        HttpClientDefaults.napierLogger("PixivTest").log("request line")

        val last = antilog.records.last()
        assertEquals("PixivTest", last.first)
        assertEquals("request line", last.second)
    }
}
