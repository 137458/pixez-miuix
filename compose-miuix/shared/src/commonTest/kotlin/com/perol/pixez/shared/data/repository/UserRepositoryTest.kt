package com.perol.pixez.shared.data.repository

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

/**
 * 用户仓库测试：用 Ktor MockEngine 打桩外部网络，
 * 验证用户详情/作品/收藏/关注/粉丝/推荐/AI 设置各端点的路径、参数与分页策略。
 */
class UserRepositoryTest {

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val formParams: Map<String, String>,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var responseBody: String = "{}"

    private lateinit var client: HttpClient
    private lateinit var repository: UserRepository

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
                    val formParams = LinkedHashMap<String, String>()
                    (request.body as? FormDataContent)?.formData?.entries()?.forEach { entry ->
                        formParams[entry.key] = entry.value.joinToString(",")
                    }
                    captured.add(CapturedRequest(request.method.value, request.url.encodedPath, query, formParams))
                    respond(responseBody, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
                }
            }
        }
        repository = UserRepository(client)
    }

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun `getUserDetail 请求用户详情端点`() = runBlocking {
        responseBody = """{"user":{"id":9,"name":"u","account":"a","profile_image_urls":{"medium":"m"}},"profile":{"is_premium":false,"is_using_custom_profile_image":false},"profile_publicity":{"gender":"public","region":"public","birth_day":"public","birth_year":"public","job":"public","pawoo":true},"workspace":{}}"""

        repository.getUserDetail(9)

        val request = captured.single()
        assertEquals("GET", request.method)
        assertEquals("/v1/user/detail", request.path)
        assertEquals("9", request.query["user_id"])
        assertEquals("for_android", request.query["filter"])
    }

    @Test
    fun `getUserIllustsResponse 首页携带 type 参数且分页复用 nextUrl`() = runBlocking {
        responseBody = """{"illusts":[],"next_url":null}"""

        repository.getUserIllustsResponse(9)
        repository.getUserIllustsResponse(9, type = "manga")
        repository.getUserIllustsResponse(9, nextUrl = "https://app-api.pixiv.net/v1/user/illusts?user_id=9&offset=30")

        assertEquals("/v1/user/illusts", captured[0].path)
        assertEquals("illust", captured[0].query["type"])
        assertEquals("manga", captured[1].query["type"])
        assertEquals("30", captured[2].query["offset"], "分页应复用 nextUrl 自带参数")
        assertEquals(null, captured[2].query["filter"], "分页请求不应重建查询参数")
    }

    @Test
    fun `getUserBookmarksResponse 的 restrict 参数映射`() = runBlocking {
        responseBody = """{"illusts":[],"next_url":null}"""

        repository.getUserBookmarksResponse(9)
        repository.getUserBookmarksResponse(9, restrict = "private")

        assertEquals("/v1/user/bookmarks/illust", captured[0].path)
        assertEquals("public", captured[0].query["restrict"])
        assertEquals("private", captured[1].query["restrict"])
    }

    @Test
    fun `关注与粉丝列表端点路径正确`() = runBlocking {
        responseBody = """{"user_previews":[],"next_url":null}"""

        repository.getUserFollowingResponse(9)
        repository.getUserFollowersResponse(9)

        assertEquals("/v1/user/following", captured[0].path)
        assertEquals("/v1/user/follower", captured[1].path)
    }

    @Test
    fun `getRecommendedUsers 首页走推荐端点分页复用 nextUrl`() = runBlocking {
        responseBody = """{"user_previews":[],"next_url":null}"""

        repository.getRecommendedUsers()
        repository.getRecommendedUsers("https://app-api.pixiv.net/v1/user/recommended?offset=18")

        assertEquals("/v1/user/recommended", captured[0].path)
        assertEquals("18", captured[1].query["offset"])
    }

    @Test
    fun `AI 显示设置读取与更新端点及表单参数正确`() = runBlocking {
        responseBody = """{"show_ai":true}"""
        assertEquals(true, repository.getUserAISettings().showAI)
        assertEquals("/v1/user/ai-show-settings", captured[0].path)

        responseBody = """{"show_ai":false}"""
        assertEquals(false, repository.updateUserAISettings(false).showAI)

        val update = captured[1]
        assertEquals("POST", update.method)
        assertEquals("/v1/user/ai-show-settings/edit", update.path)
        assertEquals("false", update.formParams["show_ai"])
    }
}
