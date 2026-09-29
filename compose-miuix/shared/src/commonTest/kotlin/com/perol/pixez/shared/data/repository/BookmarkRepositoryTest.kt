package com.perol.pixez.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.fullPath
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * 收藏与关注仓库测试：用 Ktor MockEngine 打桩外部网络，
 * 验证四类写操作请求的路径、表单参数与错误传播语义。
 */
class BookmarkRepositoryTest {

    private data class CapturedRequest(
        val path: String,
        val params: Map<String, String>,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var handlerFailure: Throwable? = null

    private lateinit var client: HttpClient
    private lateinit var repository: BookmarkRepository

    @Before
    fun setUp() {
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    handlerFailure?.let { throw it }
                    val params = HashMap<String, String>()
                    val formData = request.body as? FormDataContent
                    if (formData != null) {
                    for (entry in formData.formData.entries()) {
                        params[entry.key] = entry.value.joinToString(",")
                    }
                    }
                    captured.add(CapturedRequest(request.url.fullPath, params))
                    respondOk("{}")
                }
            }
        }
        repository = BookmarkRepository(client)
    }

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun `addBookmark 公开收藏不带 tags`() = runBlocking {
        repository.addBookmark(illustId = 42L)

        val request = captured.single()
        assertEquals("/v2/illust/bookmark/add", request.path)
        assertEquals("42", request.params["illust_id"])
        assertEquals("public", request.params["restrict"])
        assertEquals(null, request.params["tags"], "未传 tags 时不应出现 tags 字段")
    }

    @Test
    fun `addBookmark 私密收藏与 tags 一并提交`() = runBlocking {
        repository.addBookmark(illustId = 42L, isPrivate = true, tags = "R-18,風景")

        val request = captured.single()
        assertEquals("42", request.params["illust_id"])
        assertEquals("private", request.params["restrict"])
        assertEquals("R-18,風景", request.params["tags"])
    }

    @Test
    fun `addBookmark 空白 tags 不提交该字段`() = runBlocking {
        repository.addBookmark(illustId = 1L, tags = "   ")

        val request = captured.single()
        assertEquals(null, request.params["tags"], "空白 tags 视为未传")
    }

    @Test
    fun `deleteBookmark 请求取消收藏端点`() = runBlocking {
        repository.deleteBookmark(7)

        val request = captured.single()
        assertEquals("/v1/illust/bookmark/delete", request.path)
        assertEquals("7", request.params["illust_id"])
    }

    @Test
    fun `followUser 的 restrict 参数映射正确`() = runBlocking {
        repository.followUser(100)
        repository.followUser(101, isPrivate = true)

        assertEquals(listOf("/v1/user/follow/add", "/v1/user/follow/add"), captured.map { it.path })
        assertEquals("100", captured[0].params["user_id"])
        assertEquals("public", captured[0].params["restrict"])
        assertEquals("101", captured[1].params["user_id"])
        assertEquals("private", captured[1].params["restrict"])
    }

    @Test
    fun `unfollowUser 请求取消关注端点`() = runBlocking {
        repository.unfollowUser(100)

        val request = captured.single()
        assertEquals("/v1/user/follow/delete", request.path)
        assertEquals("100", request.params["user_id"])
    }

    @Test
    fun `网络异常经 networkCall 记录后原样向上抛出`() = runBlocking {
        handlerFailure = IllegalStateException("mock network down")

        try {
            repository.addBookmark(1)
            fail("网络异常应向上抛出而非被吞掉")
        } catch (e: IllegalStateException) {
            assertEquals("mock network down", e.message)
        } finally {
            handlerFailure = null
        }
    }
}
