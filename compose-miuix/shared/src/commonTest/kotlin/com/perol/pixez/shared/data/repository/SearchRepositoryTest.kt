package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Search
import com.perol.pixez.shared.data.model.UserPreviewsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 搜索仓库测试：用 Ktor MockEngine 打桩外部网络，
 * 验证热门标签、插画/画师搜索的端点、查询参数、分页 URL 策略与日期格式转换。
 */
class SearchRepositoryTest {

    private data class CapturedRequest(
        val path: String,
        val query: Map<String, String>,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var responseBody: String = "{}"

    private lateinit var client: HttpClient
    private lateinit var repository: SearchRepository

    @Before
    fun setUp() {
        client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        isLenient = true
                    },
                )
            }
            engine {
                addHandler { request ->
                    val query = LinkedHashMap<String, String>()
                    for (name in request.url.parameters.names()) {
                        query[name] = request.url.parameters[name] ?: ""
                    }
                    captured.add(CapturedRequest(request.url.encodedPath, query))
                    respond(responseBody, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
                }
            }
        }
        repository = SearchRepository(client)
    }

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun `getTrendTags 请求热门标签端点并解析列表`() = runBlocking {
        responseBody = """{"trend_tags":[{"tag":"オリジナル","translated_name":null,"illust":{"id":1,"image_urls":{"square_medium":"s","medium":"m","large":"l"}}}]}"""

        val tags = repository.getTrendTags()

        assertEquals("/v1/trending-tags/illust", captured.single().path)
        assertEquals("for_android", captured.single().query["filter"])
        assertEquals(1, tags.size)
        assertEquals("オリジナル", tags.single().tag)
    }

    @Test
    fun `searchIllustResponse 首页请求携带全部查询参数`() = runBlocking {
        responseBody = """{"illusts":[],"next_url":null}"""

        val response = repository.searchIllustResponse(
            word = "風景",
            sort = "date_desc",
            searchTarget = "exact_match_for_tags",
            searchAiType = 1,
            startDate = "2026-01-05",
            endDate = "2026-02-08",
        )

        val request = captured.single()
        assertEquals("/v1/search/illust", request.path)
        assertEquals("風景", request.query["word"])
        assertEquals("date_desc", request.query["sort"])
        assertEquals("exact_match_for_tags", request.query["search_target"])
        assertEquals("1", request.query["search_ai_type"])
        assertEquals("true", request.query["merge_plain_keyword_results"])
        assertEquals("for_android", request.query["filter"])
        // Pixiv API 期望 YYYY-M-D（无前导零），由 toPixivDateFormat 转换
        assertEquals("2026-1-5", request.query["start_date"])
        assertEquals("2026-2-8", request.query["end_date"])
        assertEquals(null, response.nextUrl)
        assertTrue(response.illusts.isEmpty())
    }

    @Test
    fun `searchIllustResponse 缺省日期不发送日期参数`() = runBlocking {
        responseBody = """{"illusts":[]}"""

        repository.searchIllustResponse(word = "w")

        val request = captured.single()
        assertEquals(null, request.query["start_date"])
        assertEquals(null, request.query["end_date"])
    }

    @Test
    fun `searchIllustResponse 的 nextUrl 走分页直取而不重建查询参数`() = runBlocking {
        responseBody = """{"illusts":[],"next_url":null}"""

        repository.searchIllustResponse(word = "w", nextUrl = "https://app-api.pixiv.net/v1/search/illust?word=w&offset=30")

        val request = captured.single()
        assertTrue(request.path.startsWith("/v1/search/illust"), "分页应复用 nextUrl 路径: ${request.path}")
        assertEquals("w", request.query["word"], "nextUrl 自带的查询参数应保留")
        assertEquals("30", request.query["offset"])
    }

    @Test
    fun `searchIllustResponse 非信任 host 的 nextUrl 被拒绝`() = runBlocking {
        try {
            repository.searchIllustResponse(word = "w", nextUrl = "https://evil.example.com/v1/search/illust?word=w")
            fail("非信任 host 的分页 URL 必须被 TrustedUrlPolicy 拒绝")
        } catch (e: IllegalArgumentException) {
            // TrustedUrlPolicy 以 IllegalArgumentException 拒绝伪造 host
        }
    }

    @Test
    fun `searchUserResponse 首页与分页路径正确`() = runBlocking {
        responseBody = """{"user_previews":[],"next_url":null}"""

        repository.searchUserResponse(word = "u")
        repository.searchUserResponse(word = "u", nextUrl = "https://app-api.pixiv.net/v1/search/user?word=u&offset=30")

        assertEquals("/v1/search/user", captured[0].path)
        assertEquals("u", captured[0].query["word"])
        assertTrue(captured[1].path.startsWith("/v1/search/user"), "分页应复用 nextUrl 路径: ${captured[1].path}")
        assertEquals("30", captured[1].query["offset"])
    }

    @Test
    fun `searchIllust 兼容方法仅返回列表`() = runBlocking {
        responseBody = """{"illusts":[],"next_url":"https://app-api.pixiv.net/x"}"""

        val illusts = repository.searchIllust(word = "w")

        assertTrue(illusts.isEmpty())
        assertEquals("https://app-api.pixiv.net/x", repository.searchIllustResponse(word = "w").nextUrl)
    }

    @Test
    fun `响应反序列化容忍未知字段`() = runBlocking {
        responseBody = """{"illusts":[],"next_url":null,"unknown_future_field":{"a":1}}"""

        val response: Search = repository.searchIllustResponse(word = "w")
        val userResponse: UserPreviewsResponse = run {
            responseBody = """{"user_previews":[],"next_url":null,"unknown_future_field":{"a":1}}"""
            repository.searchUserResponse(word = "w")
        }

        assertTrue(response.illusts.isEmpty() && userResponse.userPreviews.isEmpty())
    }
}
