package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TrustedRedirectPluginTest {

    @Test
    fun `downloadClient 重定向至非受信任域名或 HTTP 明文被逐跳拦截`() = runTest {
        val evilEngine = MockEngine { request ->
            if (request.url.host == "i.pximg.net") {
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "https://evil.example/leak.jpg"),
                )
            } else {
                respond("leaked", HttpStatusCode.OK)
            }
        }
        val evilClient = HttpClient(evilEngine) {
            followRedirects = false
            install(TrustedRedirectPlugin) {
                validateTarget = { currentUrl, targetUrl ->
                    validateDownloadRedirectTarget(currentUrl, targetUrl, mirrorHost = null)
                }
            }
        }
        assertFailsWith<IllegalArgumentException>("应阻止重定向至 evil.example") {
            evilClient.get("https://i.pximg.net/img-original/a.jpg")
        }
        evilClient.close()

        val httpEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.MovedPermanently,
                headers = headersOf(HttpHeaders.Location, "http://i.pximg.net/a.jpg"),
            )
        }
        val downgradeClient = HttpClient(httpEngine) {
            followRedirects = false
            install(TrustedRedirectPlugin) {
                validateTarget = { currentUrl, targetUrl ->
                    validateDownloadRedirectTarget(currentUrl, targetUrl, mirrorHost = null)
                }
            }
        }
        assertFailsWith<IllegalArgumentException>("应阻止 HTTPS -> HTTP 明文降级") {
            downgradeClient.get("https://i.pximg.net/img-original/a.jpg")
        }
        downgradeClient.close()
    }

    @Test
    fun `downloadClient 允许跳转到受信任镜像 CDN 且限制最大跳数`() = runTest {
        val okEngine = MockEngine { request ->
            if (request.url.host == "i.pximg.net") {
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "https://i.pixiv.re/img-original/a.jpg"),
                )
            } else {
                respond("ok-image", HttpStatusCode.OK)
            }
        }
        val okClient = HttpClient(okEngine) {
            followRedirects = false
            install(TrustedRedirectPlugin) {
                validateTarget = { currentUrl, targetUrl ->
                    validateDownloadRedirectTarget(currentUrl, targetUrl, mirrorHost = null)
                }
            }
        }
        assertEquals("ok-image", okClient.get("https://i.pximg.net/img-original/a.jpg").bodyAsText())
        okClient.close()

        val loopEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "/loop"),
            )
        }
        val loopClient = HttpClient(loopEngine) {
            followRedirects = false
            install(TrustedRedirectPlugin) {
                maxRedirects = 3
                validateTarget = { currentUrl, targetUrl ->
                    validateDownloadRedirectTarget(currentUrl, targetUrl, mirrorHost = null)
                }
            }
        }
        assertFailsWith<IllegalArgumentException>("超过最大重定向次数应失败") {
            loopClient.get("https://i.pximg.net/loop")
        }
        loopClient.close()
    }

    @Test
    fun `webClient 仅允许 Pixivision 和 SauceNAO 各自可信域内跳转`() = runTest {
        val badEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "https://attacker.com/phish"),
            )
        }
        val client = HttpClient(badEngine) {
            followRedirects = false
            install(TrustedRedirectPlugin) {
                validateTarget = { currentUrl, targetUrl ->
                    validateWebRedirectTarget(currentUrl, targetUrl)
                }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            client.get("https://www.pixivision.net/zh/a/123")
        }
        client.close()
    }

    @Test
    fun `sanitizeRemoteImageModel 放行本地与可信源并阻断不安全远端地址`() {
        assertEquals(
            "file:///C:/pic/1.png",
            sanitizeRemoteImageModel("file:///C:/pic/1.png", null),
        )
        assertEquals(
            "https://i.pximg.net/a.jpg",
            sanitizeRemoteImageModel("https://i.pximg.net/a.jpg", null),
        )
        assertEquals(
            "https://mirror.example.org/a.jpg",
            sanitizeRemoteImageModel("https://mirror.example.org/a.jpg", "mirror.example.org"),
        )
        assertNull(sanitizeRemoteImageModel("http://i.pximg.net/a.jpg", null))
        assertNull(sanitizeRemoteImageModel("https://evil.example/a.jpg", null))
        assertNull(sanitizeRemoteImageModel("https://user@i.pximg.net/a.jpg", null))
        assertEquals(
            "https://www.pixivision.net/cover.jpg",
            sanitizeRemoteImageModel("https://www.pixivision.net/cover.jpg", null),
        )
    }

    @Test
    fun `resolveRedirectUrl 正确解析相对路径与协议相对路径`() {
        val base = Url("https://www.pixivision.net/zh/a/100?ref=list")
        assertEquals("https://www.pixivision.net/en/a/100", resolveRedirectUrl(base, "/en/a/100"))
        assertEquals("https://i.pximg.net/c/a.jpg", resolveRedirectUrl(base, "//i.pximg.net/c/a.jpg"))
        assertEquals("https://www.pixivision.net/zh/a/200", resolveRedirectUrl(base, "200"))
    }
}
