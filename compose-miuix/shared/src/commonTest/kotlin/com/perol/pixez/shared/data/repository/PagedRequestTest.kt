package com.perol.pixez.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 分页接缝语义：空白 nextUrl 走首页构造块、非空原样直取且必须先过 URL 白名单。
 * 22 处生产调用点共用本语义，故用一份用例守护两条分支（含校验失败不得发出请求）。
 */
class PagedRequestTest {

    @Serializable
    private data class Page(val marker: String)

    private val requestedUrls = mutableListOf<String>()

    private fun client(): HttpClient {
        val engine = MockEngine { request ->
            requestedUrls.add(request.url.toString())
            respond(
                content = "{\"marker\":\"ok\"}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `空白 nextUrl 走首页构造块且不请求分页地址`() = runBlocking {
        val client = client()
        val page: Page = client.pagedRequest("   ") {
            client.get("https://app-api.pixiv.net/v1/illust/recommended").body<Page>()
        }

        assertEquals("ok", page.marker)
        assertEquals(listOf("https://app-api.pixiv.net/v1/illust/recommended"), requestedUrls)
        client.close()
    }

    @Test
    fun `非空 nextUrl 原样直取并保留查询参数`() = runBlocking {
        val client = client()
        val nextUrl = "https://app-api.pixiv.net/v1/illust/recommended?filter=for_ios&offset=30"
        val page: Page = client.pagedRequest(nextUrl) {
            error("非空 nextUrl 不应走首页构造块")
        }

        assertEquals("ok", page.marker)
        assertEquals(listOf(nextUrl), requestedUrls, "next_url 自带全部查询参数，必须原样请求")
        client.close()
    }

    @Test
    fun `不受信任 host 的 nextUrl 被拒绝且不发起请求`() = runBlocking {
        val client = client()
        val failure = runCatching {
            client.pagedRequest<Page>("https://evil.example.com/v1/illust/recommended") {
                error("不应走首页构造块")
            }
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException, "非受信任 host 必须抛 IllegalArgumentException，实际: " + failure)
        assertEquals(emptyList<String>(), requestedUrls, "校验失败必须在发请求前中止")
        client.close()
    }

    @Test
    fun `pagedGet 拒绝非 HTTPS 与外部 host`() = runBlocking {
        val client = client()

        val httpFailure = runCatching { client.pagedGet<Page>("http://app-api.pixiv.net/v1/x") }.exceptionOrNull()
        val hostFailure = runCatching { client.pagedGet<Page>("https://evil.example.com/v1/x") }.exceptionOrNull()

        assertTrue(httpFailure is IllegalArgumentException, "http 明文分页地址必须被拒绝，实际: " + httpFailure)
        assertTrue(hostFailure is IllegalArgumentException, "外部 host 分页地址必须被拒绝，实际: " + hostFailure)
        assertEquals(emptyList<String>(), requestedUrls, "校验失败不得发出任何请求")
        client.close()
    }
}
