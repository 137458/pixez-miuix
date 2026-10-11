package com.perol.pixez.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 公告板仓库测试：用 Ktor MockEngine 打桩多镜像端点（经 urls 参数注入假地址），
 * 验证按序重试 → 内存缓存回退 → 全失败抛错三分支，以及默认构造仍指向内置镜像。
 */
class BoardRepositoryTest {

    private data class StubResponse(val status: HttpStatusCode, val body: String)

    private val capturedUrls = mutableListOf<String>()
    private val stubbed = LinkedHashMap<String, StubResponse>()

    private fun mockClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                val url = request.url.toString()
                capturedUrls.add(url)
                val stub = stubbed[url]
                when {
                    stub == null -> respondError(HttpStatusCode.InternalServerError)
                    stub.status == HttpStatusCode.OK -> respond(
                        stub.body,
                        HttpStatusCode.OK,
                        headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                    else -> respondError(stub.status)
                }
            }
        }
    }

    private fun repository(vararg urls: String): BoardRepository =
        BoardRepository(mockClient(), urls = urls.toList())

    private val primaryUrl = "https://board.example.test/primary.json"
    private val secondaryUrl = "https://mirror.example.test/secondary.json"

    private val primaryJson = """
        [{"title":"主源公告","content":"来自主源","startDate":"2026-10-01"}]
    """.trimIndent()

    private val secondaryJson = """
        [{"title":"镜像公告","content":"来自镜像","startDate":"2026-10-02","endDate":"2026-10-31","extra":"未知键"}]
    """.trimIndent()

    @Test
    fun `首源成功时直接返回列表且不再请求其余源`() = runBlocking {
        stubbed[primaryUrl] = StubResponse(HttpStatusCode.OK, primaryJson)

        val boards = repository(primaryUrl, secondaryUrl).loadBoardList()

        assertEquals(1, boards.size, "应解析出主源公告列表")
        assertEquals("主源公告", boards.single().title)
        assertEquals(listOf(primaryUrl), capturedUrls, "首源成功后不应继续请求次源")
    }

    @Test
    fun `首源失败时按序重试次源并返回次源数据`() = runBlocking {
        stubbed[primaryUrl] = StubResponse(HttpStatusCode.InternalServerError, "")
        stubbed[secondaryUrl] = StubResponse(HttpStatusCode.OK, secondaryJson)

        val boards = repository(primaryUrl, secondaryUrl).loadBoardList()

        assertEquals(1, boards.size)
        assertEquals("镜像公告", boards.single().title, "应返回次源镜像的数据")
        assertEquals(listOf(primaryUrl, secondaryUrl), capturedUrls, "应按注入顺序重试")
        assertEquals("2026-10-31", boards.single().endDate, "endDate 应正常解析")
    }

    @Test
    fun `远端全失败时回退内存缓存`() = runBlocking {
        stubbed[primaryUrl] = StubResponse(HttpStatusCode.OK, primaryJson)
        val repository = repository(primaryUrl)

        val first = repository.loadBoardList()

        stubbed.clear()
        val fallback = repository.loadBoardList()

        assertEquals(first, fallback, "远端全失败应回退为上次成功数据")
        assertEquals(2, capturedUrls.size, "回退前仍应尝试远端请求")
        assertEquals("主源公告", fallback.single().title)
    }

    @Test
    fun `全源失败且无缓存时抛出最后一个错误`() = runBlocking {
        stubbed[primaryUrl] = StubResponse(HttpStatusCode.InternalServerError, "")
        stubbed[secondaryUrl] = StubResponse(HttpStatusCode.OK, "not-a-json")

        try {
            repository(primaryUrl, secondaryUrl).loadBoardList()
            fail("全源失败且无缓存必须向上抛出异常")
        } catch (e: kotlinx.serialization.SerializationException) {
            assertEquals(listOf(primaryUrl, secondaryUrl), capturedUrls, "抛错前应重试完所有镜像")
        }

        Unit
    }

    @Test
    fun `注入空源列表时抛出兜底异常且不发请求`() = runBlocking {
        try {
            repository().loadBoardList()
            fail("无可用源必须抛出兜底异常")
        } catch (e: RuntimeException) {
            assertEquals("加载公告板失败", e.message, "无最后错误时应抛兜底消息")
            assertTrue(capturedUrls.isEmpty(), "空源列表不应发出任何请求")
        }

        Unit
    }

    @Test
    fun `默认构造仍指向内置 GitHub Raw 主镜像`() = runBlocking {
        val primary = "https://raw.githubusercontent.com/137458/pixez-miuix/refs/heads/master/.github/board/android.json"
        stubbed[primary] = StubResponse(HttpStatusCode.OK, primaryJson)

        val boards = BoardRepository(mockClient()).loadBoardList()

        assertEquals(listOf(primary), capturedUrls, "默认 urls 应保持内置镜像顺序且主源成功即短路")
        assertEquals("主源公告", boards.single().title)
    }
}
