package com.perol.pixez.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 小说仓库测试：用 Ktor MockEngine 打桩 Pixiv API，
 * 验证正文/推荐/排行榜端点参数，以及 nextUrl 分页经 TrustedUrlPolicy 透传（不追加首页参数、拒绝不可信 host）。
 */
class NovelRepositoryTest {

    private data class CapturedRequest(
        val url: String,
        val host: String,
        val path: String,
        val query: Map<String, String>,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var responseBody: String = "{}"

    private val client: HttpClient = HttpClient(MockEngine) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
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
                captured.add(
                    CapturedRequest(
                        url = request.url.toString(),
                        host = request.url.host,
                        path = request.url.encodedPath,
                        query = query,
                    ),
                )
                respond(
                    responseBody,
                    HttpStatusCode.OK,
                    headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }
        }
    }

    private val repository = NovelRepository(client)

    private val novelJson = """
        {"id":11,"title":"推荐小说","caption":"","restrict":0,"x_restrict":0,"is_original":false,
         "image_urls":{"square_medium":"s","medium":"m","large":"l"},
         "create_date":"2026-01-01T00:00:00+09:00",
         "tags":[{"name":"原创","translated_name":"original","added_by_uploaded_user":true}],
         "page_count":1,"text_length":100,
         "user":{"id":2,"name":"u","account":"acc","profile_image_urls":{"medium":"pm"},"is_followed":false},
         "is_bookmarked":false,"visible":true,"is_muted":false,"is_mypixiv_only":false,"is_x_restricted":false,"novel_ai_type":0}
    """.trimIndent()

    private val recomJson = """{"novels":[$novelJson],"next_url":null}"""

    @Test
    fun `getNovelText 请求正文端点携带 novel_id 并解析正文与标记`() = runBlocking {
        responseBody = """{"novel_marker":{"page":3},"novel_text":"正文内容"}"""

        val result = repository.getNovelText(77)

        assertEquals("/v1/novel/text", captured.single().path)
        assertEquals("77", captured.single().query["novel_id"])
        assertEquals("正文内容", result.novelText)
        assertEquals(3, result.novelMarker.page)
    }

    @Test
    fun `getNovelDetail 请求详情端点携带 novel_id 并解析作品与收藏状态`() = runBlocking {
        responseBody = """{"novel":$novelJson}"""
        responseBody = responseBody.replace("\"is_bookmarked\":false", "\"is_bookmarked\":true")

        val result = repository.getNovelDetail(88)

        assertEquals("/v2/novel/detail", captured.single().path)
        assertEquals("88", captured.single().query["novel_id"])
        assertEquals("推荐小说", result.novel.title)
        assertEquals(11L, result.novel.id)
        assertTrue(result.novel.isBookmarked, "详情应解析收藏状态")
    }

    @Test
    fun `getRecommendedNovels 默认携带 for_android 过滤且默认开启排行榜`() = runBlocking {
        responseBody = recomJson

        val result = repository.getRecommendedNovels()

        assertEquals("/v1/novel/recommended", captured.single().path)
        assertEquals("for_android", captured.single().query["filter"])
        assertEquals("true", captured.single().query["include_privacy_policy"])
        assertEquals("true", captured.single().query["include_ranking_novels"])
        assertEquals(1, result.novels.size)
        assertEquals("推荐小说", result.novels.single().title)
    }

    @Test
    fun `getRecommendedNovels 关闭排行榜时 include_ranking_novels 为 false`() = runBlocking {
        responseBody = """{"novels":[],"next_url":null}"""

        val result = repository.getRecommendedNovels(includeRanking = false)

        assertEquals("false", captured.single().query["include_ranking_novels"], "关闭排行榜应显式传 false")
        assertEquals(0, result.novels.size)
    }

    @Test
    fun `getRecommendedNovels 传入 nextUrl 时透传分页 URL 且不追加首页参数`() = runBlocking {
        responseBody = """{"novels":[],"next_url":null}"""
        val nextUrl = "https://app-api.pixiv.net/v1/novel/recommended?offset=30&filter=for_ios"

        val result = repository.getRecommendedNovels(nextUrl = nextUrl)

        val request = captured.single()
        assertEquals("app-api.pixiv.net", request.host, "分页 URL 必须命中可信 API host")
        assertEquals("/v1/novel/recommended", request.path)
        assertEquals("30", request.query["offset"])
        assertEquals("for_ios", request.query["filter"], "分页应使用 nextUrl 自带参数")
        assertFalse(request.query.containsKey("include_ranking_novels"), "透传分支不应追加首页参数")
        assertEquals(0, result.novels.size)
    }

    @Test
    fun `getNovelRanking 强类型枚举映射 mode 参数`() = runBlocking {
        responseBody = recomJson

        val byEnum = repository.getNovelRanking(mode = NovelRankingMode.DayMale)

        assertEquals("/v1/novel/ranking", captured[0].path)
        assertEquals("day_male", captured[0].query["mode"])
        assertEquals("for_android", captured[0].query["filter"])
        assertEquals(1, byEnum.novels.size)

        val byDefault = repository.getNovelRanking(mode = NovelRankingMode.Day)

        assertEquals("day", captured[1].query["mode"], "默认应为日榜")
    }

    @Test
    fun `getNovelRanking 传入 nextUrl 时透传且不追加 mode 与 filter`() = runBlocking {
        responseBody = """{"novels":[],"next_url":null}"""
        val nextUrl = "https://app-api.pixiv.net/v1/novel/ranking?offset=18&mode=week"

        val result = repository.getNovelRanking(mode = NovelRankingMode.Day.value, nextUrl = nextUrl)

        val request = captured.single()
        assertEquals("app-api.pixiv.net", request.host)
        assertEquals("/v1/novel/ranking", request.path)
        assertEquals("18", request.query["offset"])
        assertEquals("week", request.query["mode"])
        assertFalse(request.query.containsKey("filter"), "透传分支不应追加 filter")
        assertEquals(0, result.novels.size)
    }

    @Test
    fun `不可信 host 或非 HTTPS 的 nextUrl 被 TrustedUrlPolicy 拒绝且不发请求`() = runBlocking {
        try {
            repository.getRecommendedNovels(nextUrl = "https://evil.example.com/v1/novel/recommended")
            fail("不可信 host 的分页 URL 必须被拒绝")
        } catch (e: IllegalArgumentException) {
            // 预期路径
        }

        try {
            repository.getNovelRanking(
                mode = NovelRankingMode.Day.value,
                nextUrl = "http://app-api.pixiv.net/v1/novel/ranking?offset=30",
            )
            fail("非 HTTPS 的分页 URL 必须被拒绝")
        } catch (e: IllegalArgumentException) {
            // 预期路径
        }

        assertTrue(captured.isEmpty(), "被拒绝的分页 URL 不应发出任何请求")
    }
}
