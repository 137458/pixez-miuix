package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.ui.AppConstants
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
import okio.FileSystem
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 插画仓库测试：用 Ktor MockEngine 打桩外部网络，
 * 验证内存缓存语义（推荐/匿名推荐的 forceRefresh）、端点参数、评论表单与动图 Zip 下载的安全校验。
 * Spotlight/特辑链路测试见 PixivisionRepositoryTest（中-7 拆分）。
 */
class IllustRepositoryTest {

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val headers: Map<String, String>,
        val formParams: Map<String, String>,
    )

    private val apiCaptured = mutableListOf<CapturedRequest>()
    private val downloadCaptured = mutableListOf<CapturedRequest>()
    private var apiResponseBody: String = "{}"
    private var downloadBody: ByteArray = ByteArray(0)
    private var downloadHandlerFailure: Throwable? = null

    private lateinit var client: HttpClient
    private lateinit var downloadClient: HttpClient
    private lateinit var repository: IllustRepository

    @Before
    fun setUp() {
        client = mockClient(apiCaptured, isDownload = false)
        downloadClient = mockClient(downloadCaptured, isDownload = true)
        repository = IllustRepository(
            apiClient = client,
            downloadClient = downloadClient,
            settingsRepository = SettingsRepository(InMemorySettings()),
        )
    }

    private fun mockClient(
        sink: MutableList<CapturedRequest>,
        isDownload: Boolean,
    ): HttpClient = HttpClient(MockEngine) {
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
                if (isDownload && downloadHandlerFailure != null) {
                    throw downloadHandlerFailure!!
                }
                val query = LinkedHashMap<String, String>()
                for (name in request.url.parameters.names()) {
                    query[name] = request.url.parameters[name] ?: ""
                }
                val headers = LinkedHashMap<String, String>()
                for (name in request.headers.names()) {
                    headers[name] = request.headers.getAll(name)?.joinToString(",") ?: ""
                }
                val formParams = LinkedHashMap<String, String>()
                val formData = request.body as? FormDataContent
                if (formData != null) {
                    for (entry in formData.formData.entries()) {
                        formParams[entry.key] = entry.value.joinToString(",")
                    }
                }
                sink.add(CapturedRequest(request.method.value, request.url.encodedPath, query, headers, formParams))
                if (isDownload) {
                    respond(
                        downloadBody,
                        HttpStatusCode.OK,
                        headersOf("Content-Type", ContentType.Application.OctetStream.toString()),
                    )
                } else {
                    respond(
                        apiResponseBody,
                        HttpStatusCode.OK,
                        headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            }
        }
    }

    @After
    fun tearDown() {
        client.close()
        downloadClient.close()
    }

    private val detailJson = """
        {"illust":{"id":77,"title":"t","type":"illust",
          "image_urls":{"square_medium":"s","medium":"m","large":"l"},
          "restrict":0,
          "user":{"id":2,"name":"u","account":"acc","profile_image_urls":{"medium":"pm"}},
          "tags":[],
          "create_date":"2026-01-01T00:00:00+09:00","page_count":1,"width":100,"height":100,
          "sanity_level":2,"x_restrict":0,"meta_pages":[],"is_bookmarked":false,"visible":true,
          "is_muted":false,"illust_ai_type":0}}
    """.trimIndent()

    @Test
    fun `详情接口请求参数正确并顺带写入内存缓存`() = runBlocking {
        apiResponseBody = detailJson

        val illust = repository.getIllustDetail(77)

        assertEquals("/v1/illust/detail", apiCaptured.single().path)
        assertEquals("77", apiCaptured.single().query["illust_id"])
        assertEquals("for_android", apiCaptured.single().query["filter"])
        assertEquals("t", illust.title)
        assertEquals(77, repository.getCachedIllust(77)?.id, "详情应写入内存缓存供详情页复用")
        assertNull(repository.getCachedIllust(78))
    }

    @Test
    fun `推荐响应默认命中内存缓存且 forceRefresh 强制刷新`() = runBlocking {
        apiResponseBody = """{"illusts":[],"next_url":null}"""

        repository.getRecommended()
        repository.getRecommended()

        assertEquals(1, apiCaptured.size, "第二次调用应命中内存缓存，不再发请求")

        repository.getRecommended(forceRefresh = true)

        assertEquals(2, apiCaptured.size, "forceRefresh 应绕过缓存重新请求")
        assertEquals("/v1/illust/recommended", apiCaptured.last().path)
        assertEquals("for_ios", apiCaptured.last().query["filter"])
        assertEquals("true", apiCaptured.last().query["include_ranking_label"])
    }

    @Test
    fun `匿名推荐响应同样具备缓存与强制刷新语义`() = runBlocking {
        apiResponseBody = """{"illusts":[]}"""

        repository.getWalkthroughIllusts()
        repository.getWalkthroughIllusts()
        repository.getWalkthroughIllusts(forceRefresh = true)

        assertEquals(2, apiCaptured.size)
        assertEquals("/v1/walkthrough/illusts", apiCaptured.last().path)
    }

    @Test
    fun `postComment 表单参数完整且回复时携带父评论 ID`() = runBlocking {
        apiResponseBody = "{}"

        repository.postComment(illustId = 7L, comment = "好看")
        repository.postComment(illustId = 7L, comment = "回复", parentCommentId = 99)

        assertEquals("/v1/illust/comment/add", apiCaptured[0].path)
        assertEquals("/v1/illust/comment/add", apiCaptured[1].path)
        assertEquals("7", apiCaptured[0].formParams["illust_id"])
        assertEquals("好看", apiCaptured[0].formParams["comment"])
        assertEquals(null, apiCaptured[0].formParams["parent_comment_id"], "普通评论不应携带父评论字段")
        assertEquals("99", apiCaptured[1].formParams["parent_comment_id"])
    }

    @Test
    fun `downloadUgoiraZip 拒绝非图片 host 并对受信任 host 携带 Referer`() = runBlocking {
        try {
            repository.downloadUgoiraZip("https://evil.example.com/ugoira.zip")
            fail("非受信任 host 的 Zip 下载必须被 TrustedUrlPolicy 拒绝")
        } catch (e: IllegalArgumentException) {
            // 预期路径
        }

        downloadBody = byteArrayOf(1, 2, 3)
        val bytes = repository.downloadUgoiraZip("https://i.pximg.net/img-zip-ugoira/v1/77.zip")

        assertEquals(3, bytes.size)
        val request = downloadCaptured.single()
        assertEquals("/img-zip-ugoira/v1/77.zip", request.path)
        assertEquals(
            AppConstants.Urls.PIXIV_APP_API,
            request.headers["Referer"],
            "动图 Zip 下载应携带 app-api Referer",
        )
    }

    // ---------- 中-15：ugoira zip 流式落盘版下载 ----------

    @Test
    fun `downloadUgoiraZipToFile 将响应体流式写入目标文件并携带 Referer`() = runBlocking {
        downloadBody = byteArrayOf(1, 2, 3, 4, 5)
        val target = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_ugoira_file_${Random.nextLong()}.zip"
        try {
            val result = repository.downloadUgoiraZipToFile("https://i.pximg.net/img-zip-ugoira/v1/88.zip", target)

            assertEquals(target, result, "应返回目标路径本身")
            assertTrue(FileSystem.SYSTEM.exists(target), "目标文件应已落盘")
            val written = FileSystem.SYSTEM.read(target) { readByteArray() }
            assertTrue(downloadBody.contentEquals(written), "落盘内容应与响应体一致")

            val request = downloadCaptured.single()
            assertEquals("/img-zip-ugoira/v1/88.zip", request.path)
            assertEquals(
                AppConstants.Urls.PIXIV_APP_API,
                request.headers["Referer"],
                "动图 Zip 下载应携带 app-api Referer",
            )
        } finally {
            runCatching { FileSystem.SYSTEM.delete(target) }
        }
    }

    @Test
    fun `downloadUgoiraZipToFile 拒绝非图片 host 且不产生目标文件`() = runBlocking {
        val target = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_ugoira_file_${Random.nextLong()}.zip"
        try {
            try {
                repository.downloadUgoiraZipToFile("https://evil.example.com/ugoira.zip", target)
                fail("非受信任 host 的 Zip 下载必须被 TrustedUrlPolicy 拒绝")
            } catch (e: IllegalArgumentException) {
                // 预期路径
            }
            assertFalse(FileSystem.SYSTEM.exists(target), "被拒绝的下载不应产生目标文件")
        } finally {
            runCatching { FileSystem.SYSTEM.delete(target) }
        }
    }

    @Test
    fun `downloadUgoiraZipToFile 下载失败时清理目标残留文件`() = runBlocking {
        downloadHandlerFailure = RuntimeException("mock download failure")
        val target = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_ugoira_file_${Random.nextLong()}.zip"
        try {
            // 预置残留文件，验证失败路径会清掉半成品而非留下损坏 zip。
            FileSystem.SYSTEM.write(target) { writeUtf8("stale") }

            try {
                repository.downloadUgoiraZipToFile("https://i.pximg.net/img-zip-ugoira/v1/99.zip", target)
                fail("下载失败必须向上抛出异常")
            } catch (e: Exception) {
                // 预期路径
            }

            assertFalse(FileSystem.SYSTEM.exists(target), "失败路径必须清理目标文件残留")
        } finally {
            downloadHandlerFailure = null
            runCatching { FileSystem.SYSTEM.delete(target) }
        }
    }
}
