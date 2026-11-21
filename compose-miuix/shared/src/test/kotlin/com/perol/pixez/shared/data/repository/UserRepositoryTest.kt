package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.cache.IllustMemoryCache
import com.perol.pixez.shared.testutil.CapturedRequest
import com.perol.pixez.shared.testutil.StubEngine
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * 用户仓库测试：用 Ktor MockEngine 打桩外部网络，
 * 验证用户详情/作品/收藏/关注/粉丝/推荐/AI 设置各端点的路径、参数与分页策略。
 */
class UserRepositoryTest {

    // 请求快照（含表单字段）与回包统一由共享夹具维护，本类只保留既有断言所需的别名。
    private val engine = StubEngine()
    private val captured: List<CapturedRequest> get() = engine.requests
    private var responseBody: String
        get() = engine.responseBody
        set(value) {
            engine.responseBody = value
        }

    private lateinit var client: HttpClient
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        client = engine.client()
        repository = UserRepository(client, IllustMemoryCache())
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
    fun `getUserRelated 首页携带 seed_user_id 且分页复用 nextUrl`() = runBlocking {
        responseBody = """{"user_previews":[],"next_url":null}"""

        repository.getUserRelated(12345L)
        repository.getUserRelated(12345L, nextUrl = "https://app-api.pixiv.net/v1/user/related?seed_user_id=12345&offset=20")

        assertEquals("GET", captured[0].method)
        assertEquals("/v1/user/related", captured[0].path)
        assertEquals("12345", captured[0].query["seed_user_id"])
        assertEquals("for_android", captured[0].query["filter"])

        assertEquals("GET", captured[1].method)
        assertEquals("/v1/user/related", captured[1].path)
        assertEquals("20", captured[1].query["offset"])
        assertEquals("12345", captured[1].query["seed_user_id"])
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
