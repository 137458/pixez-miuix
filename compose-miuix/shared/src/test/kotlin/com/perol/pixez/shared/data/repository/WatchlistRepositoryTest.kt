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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 追更仓库测试：用 Ktor MockEngine 打桩外部网络，
 * 验证三方法端点/参数（列表 GET、追更/取消追更表单 POST）、响应解析与 nextUrl 分页透传。
 * JSON 字段以旧 Flutter `watchlist_manga_model.dart` 头部注释样例为准。
 */
class WatchlistRepositoryTest {

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val formParams: Map<String, String>,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var responseBody: String = "{}"
    private var responseStatus: HttpStatusCode = HttpStatusCode.OK

    private lateinit var client: HttpClient
    private lateinit var repository: WatchlistRepository

    @Before
    fun setUp() {
        client = HttpClient(MockEngine) {
            // 镜像生产 apiClient 的 4xx/5xx 抛错语义（PixivHttpClient HttpResponseValidator）。
            expectSuccess = true
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
                        responseStatus,
                        headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            }
        }
        repository = WatchlistRepository(apiClient = client)
    }

    @After
    fun tearDown() {
        client.close()
    }

    // 旧 Flutter watchlist_manga_model.dart 头部注释样例（裁剪无关展示字段后保留可空容错）。
    private val watchlistJson = """
        {"series":[
          {"mask_text":null,
           "user":{"id":4004637,"account":"9365710x","name":"幻羽",
                   "profile_image_urls":{"medium":"https://i.pximg.net/user-profile/img/medium.jpg"}},
           "latest_content_id":125547965,"id":266067,"title":"X'max Present",
           "last_published_content_datetime":"2024-12-26T01:27:48+09:00",
           "published_content_count":2,
           "url":"https://i.pximg.net/c/240x480/img-master/master1200.jpg"}
        ],
        "next_url":null}
    """.trimIndent()

    @Test
    fun `getMangaWatchlist 请求端点正确并解析系列字段`() = runBlocking {
        responseBody = watchlistJson

        val response = repository.getMangaWatchlist()

        val request = captured.single()
        assertEquals("GET", request.method)
        assertEquals("/v1/watchlist/manga", request.path)
        val series = response.series.single()
        assertEquals(266067L, series.id)
        assertEquals("X'max Present", series.title)
        assertEquals(4004637L, series.user?.id)
        assertEquals("幻羽", series.user?.name)
        assertEquals("https://i.pximg.net/user-profile/img/medium.jpg", series.user?.profileImageUrls?.medium)
        assertEquals(125547965L, series.latestContentId)
        assertEquals("2024-12-26T01:27:48+09:00", series.lastPublishedContentDatetime)
        assertEquals(2, series.publishedContentCount)
        assertEquals("https://i.pximg.net/c/240x480/img-master/master1200.jpg", series.url)
        assertNull(response.nextUrl, "样例 next_url 为 null，不应虚构分页")
    }

    @Test
    fun `getMangaWatchlist nextUrl 非空时透传分页地址且不追加查询参数`() = runBlocking {
        responseBody = """{"series":[],"next_url":null}"""

        repository.getMangaWatchlist(nextUrl = "https://app-api.pixiv.net/v1/watchlist/manga?offset=30")

        val request = captured.single()
        assertEquals("/v1/watchlist/manga", request.path, "分页应请求 next_url 指向的路径")
        assertEquals("30", request.query["offset"], "分页请求应携带 next_url 自带的查询参数")
        assertEquals(1, captured.size, "透传分页时不应再请求首页")
    }

    @Test
    fun `getMangaWatchlist 解析下一页 next_url`() = runBlocking {
        responseBody = """{"series":[],"next_url":"https://app-api.pixiv.net/v1/watchlist/manga?offset=30"}"""

        val response = repository.getMangaWatchlist()

        assertEquals("https://app-api.pixiv.net/v1/watchlist/manga?offset=30", response.nextUrl)
        assertTrue(response.series.isEmpty())
    }

    @Test
    fun `getMangaWatchlist 空列表响应解析为空 series`() = runBlocking {
        responseBody = """{"series":[],"next_url":null}"""

        val response = repository.getMangaWatchlist()

        assertTrue(response.series.isEmpty())
        assertNull(response.nextUrl)
    }

    @Test
    fun `addMangaWatchlist 以表单 series_id 请求追更端点`() = runBlocking {
        responseBody = "{}"

        repository.addMangaWatchlist(seriesId = 266067L)

        val request = captured.single()
        assertEquals("POST", request.method)
        assertEquals("/v1/watchlist/manga/add", request.path)
        assertEquals("266067", request.formParams["series_id"], "追更应以表单字段 series_id 传参")
    }

    @Test
    fun `deleteMangaWatchlist 以表单 series_id 请求取消追更端点`() = runBlocking {
        responseBody = "{}"

        repository.deleteMangaWatchlist(seriesId = 266067L)

        val request = captured.single()
        assertEquals("POST", request.method)
        assertEquals("/v1/watchlist/manga/delete", request.path)
        assertEquals("266067", request.formParams["series_id"], "取消追更应以表单字段 series_id 传参")
    }

    @Test
    fun `getMangaWatchlist 服务端错误时向上抛出异常而非吞掉`() = runBlocking {
        responseStatus = HttpStatusCode.InternalServerError

        val result = runCatching { repository.getMangaWatchlist() }

        assertTrue(result.isFailure, "服务端 500 时仓库必须向上抛出异常")
        assertEquals(1, captured.size)
    }
}
