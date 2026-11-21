package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
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
import kotlin.test.assertNull

/**
 * 小说评论仓库测试：Ktor MockEngine 打桩外部网络，
 * 验证小说评论三端点的请求路径 / query / 表单字段、nextUrl 分页透传与响应解析
 * （端点对齐旧 Flutter 版 api_client.dart 的 getNovelComments / getNovelCommentsReplies /
 * postNovelComment；响应结构与插画评论同构，复用 CommentResponse 模型）。
 */
class NovelCommentsRepositoryTest {

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val formParams: Map<String, String>,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var responseBody: String = "{}"

    private lateinit var client: HttpClient
    private lateinit var repository: IllustRepository

    @Before
    fun setUp() {
        client = mockClient()
        repository = IllustRepository(
            apiClient = client,
            downloadClient = client,
            settingsRepository = SettingsRepository(InMemorySettings()),
        )
    }

    private fun mockClient(): HttpClient = HttpClient(MockEngine) {
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
                val formParams = LinkedHashMap<String, String>()
                val formData = request.body as? FormDataContent
                if (formData != null) {
                    for (entry in formData.formData.entries()) {
                        formParams[entry.key] = entry.value.joinToString(",")
                    }
                }
                captured.add(CapturedRequest(request.method.value, request.url.encodedPath, query, formParams))
                respond(
                    responseBody,
                    HttpStatusCode.OK,
                    headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }
        }
    }

    @After
    fun tearDown() {
        client.close()
    }

    private val novelCommentsJson = """
        {"total_comments":2,
         "comments":[
           {"id":1001,"comment":"写得真好","date":"2026-01-02 03:04:05",
            "user":{"id":7,"name":"作者","account":"author","profile_image_urls":{"medium":"pm"}},
            "has_replies":true},
           {"id":1002,"comment":"回复内容","date":"2026-01-03 03:04:05",
            "user":{"id":8,"name":"读者","account":"reader","profile_image_urls":{"medium":"pm"}},
            "parent_comment":{"id":1001,"comment":"写得真好","date":"2026-01-02 03:04:05",
              "user":{"id":7,"name":"作者","account":"author","profile_image_urls":{"medium":"pm"}}}}
         ],
         "next_url":null}
    """.trimIndent()

    @Test
    fun `getNovelCommentsResponse 请求小说评论端点且 query 为 novel_id 并解析评论`() = runBlocking {
        responseBody = novelCommentsJson

        val response = repository.getNovelCommentsResponse(novelId = 55L)

        assertEquals(1, captured.size)
        assertEquals("/v3/novel/comments", captured.single().path)
        assertEquals("55", captured.single().query["novel_id"])
        assertEquals(2, response.totalComments)
        assertEquals(2, response.comments.size)
        assertEquals(1001L, response.comments[0].id)
        assertEquals("写得真好", response.comments[0].comment)
        assertEquals("作者", response.comments[0].user?.name)
        assertEquals(true, response.comments[0].hasReplies)
        assertEquals(1001L, response.comments[1].parentComment?.id, "回复评论应携带父评论")
        assertNull(response.nextUrl)
    }

    @Test
    fun `getNovelCommentsResponse nextUrl 走分页透传且不再重复构造 novel_id query`() = runBlocking {
        responseBody = """{"comments":[],"next_url":null}"""

        repository.getNovelCommentsResponse(
            novelId = 55L,
            nextUrl = "https://app-api.pixiv.net/v3/novel/comments?novel_id=55&offset=30",
        )

        assertEquals(1, captured.size)
        assertEquals("/v3/novel/comments", captured.single().path)
        assertEquals("30", captured.single().query["offset"])
    }

    @Test
    fun `postNovelComment 表单参数完整且回复时携带父评论 ID`() = runBlocking {
        responseBody = "{}"

        repository.postNovelComment(novelId = 55L, comment = "追更了")
        repository.postNovelComment(novelId = 55L, comment = "回复", parentCommentId = 1001L)

        assertEquals("/v1/novel/comment/add", captured[0].path)
        assertEquals("55", captured[0].formParams["novel_id"])
        assertEquals("追更了", captured[0].formParams["comment"])
        assertNull(captured[0].formParams["parent_comment_id"], "普通评论不应携带父评论字段")
        assertEquals("/v1/novel/comment/add", captured[1].path)
        assertEquals("1001", captured[1].formParams["parent_comment_id"])
    }
}
