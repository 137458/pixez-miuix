package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.settings.SettingsRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pixivision 特辑仓库测试（中-7 拆分后）：用 Ktor MockEngine 打桩外部网络，
 * 验证 REST Spotlight 列表的端点参数与缓存语义、novel 分类的 pixivision HTML 抓取
 * （语言路径 + UA 伪装）、特辑详情抓取解析与 clearMemoryCache 失效行为。
 */
class PixivisionRepositoryTest {

    private data class CapturedRequest(
        val path: String,
        val query: Map<String, String>,
        val headers: Map<String, String>,
    )

    private val apiCaptured = mutableListOf<CapturedRequest>()
    private val webCaptured = mutableListOf<CapturedRequest>()
    private var apiResponseBody: String = "{}"
    private var webBody: String = ""

    private lateinit var apiClient: HttpClient
    private lateinit var webClient: HttpClient
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: PixivisionRepository

    @Before
    fun setUp() {
        apiClient = mockClient(apiCaptured, ContentType.Application.Json, isApi = true)
        webClient = mockClient(webCaptured, ContentType.Text.Html, isApi = false)
        settingsRepository = SettingsRepository(InMemorySettings())
        repository = PixivisionRepository(
            apiClient = apiClient,
            webClient = webClient,
            settingsRepository = settingsRepository,
        )
    }

    private fun mockClient(
        sink: MutableList<CapturedRequest>,
        contentType: ContentType,
        isApi: Boolean,
    ): HttpClient = HttpClient(MockEngine) {
        if (isApi) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        isLenient = true
                    },
                )
            }
        }
        engine {
            addHandler { request ->
                val query = LinkedHashMap<String, String>()
                for (name in request.url.parameters.names()) {
                    query[name] = request.url.parameters[name] ?: ""
                }
                val headers = LinkedHashMap<String, String>()
                for (name in request.headers.names()) {
                    headers[name] = request.headers.getAll(name)?.joinToString(",") ?: ""
                }
                sink.add(CapturedRequest(request.url.encodedPath, query, headers))
                respond(
                    if (isApi) apiResponseBody else webBody,
                    HttpStatusCode.OK,
                    headersOf("Content-Type", contentType.toString()),
                )
            }
        }
    }

    @After
    fun tearDown() {
        apiClient.close()
        webClient.close()
    }

    private fun spotlightJson(ids: List<Long>, nextUrl: String?): String {
        val articles = ids.joinToString(",") { id ->
            """{"id":$id,"title":"特辑$id","pure_title":"特辑$id","thumbnail":"https://s.pximg.net/t$id","article_url":"https://www.pixivision.net/zh/a/$id","publish_date":"2026-01-01"}"""
        }
        return """{"spotlight_articles":[$articles],"next_url":${if (nextUrl == null) "null" else "\"$nextUrl\""}}"""
    }

    private val novelsHtml = """
        <html><body><div>
            <article class="_article-card spotlight">
                <div class="arc__thumbnail-container">
                    <a href="/zh/a/9001"><div class="_thumbnail" style="background-image: url(https://i.pximg.net/c/1200x630/9001.jpg)"></div></a>
                </div>
                <div class="arc__title-container"><h2 class="arc__title"><a href="/zh/a/9001">【小说】测试特辑甲</a></h2></div>
                <div class="arc__footer-date-pr"><time datetime="2026-02-01">2026.02.01</time></div>
            </article>
            <article class="_article-card spotlight">
                <div class="arc__thumbnail-container">
                    <a href="/zh/a/9002"><div class="_thumbnail" style="background-image: url(https://i.pximg.net/c/1200x630/9002.jpg)"></div></a>
                </div>
                <div class="arc__title-container"><h2 class="arc__title"><a href="/zh/a/9002">测试特辑乙</a></h2></div>
                <div class="arc__footer-date-pr"><time datetime="2026-03-01">2026.03.01</time></div>
            </article>
        </div></body></html>
    """.trimIndent()

    private val detailHtml = """
        <html><head><meta property="og:title" content="特辑详情测试"></head>
        <body>
            <article class="am__work">
                <a href="https://www.pixiv.net/artworks/777">作品</a>
                <img src="https://i.pximg.net/c/600x600/777.jpg">
            </article>
        </body></html>
    """.trimIndent()

    @Test
    fun `REST Spotlight 首页请求参数正确且写入缓存`() = runBlocking {
        apiResponseBody = spotlightJson(listOf(1, 2), nextUrl = null)

        val response = repository.getSpotlightArticles("all")

        assertEquals("/v1/spotlight/articles", apiCaptured.single().path)
        assertEquals("for_android", apiCaptured.single().query["filter"])
        assertEquals("all", apiCaptured.single().query["category"])
        assertEquals(listOf(1L, 2L), response.spotlightArticles.map { it.id })
        assertEquals(listOf(1L, 2L), repository.getCachedSpotlightArticles("all")?.spotlightArticles?.map { it.id })
        assertNull(repository.getCachedSpotlightArticles("illust"), "不同分类的首页快照互不共享")
    }

    @Test
    fun `REST Spotlight 首页默认命中缓存且 forceRefresh 覆盖旧快照`() = runBlocking {
        apiResponseBody = spotlightJson(listOf(1, 2), nextUrl = null)

        repository.getSpotlightArticles("all")
        repository.getSpotlightArticles("all")

        assertEquals(1, apiCaptured.size, "第二次调用应命中内存缓存，不再发请求")

        apiResponseBody = spotlightJson(listOf(3), nextUrl = null)
        repository.getSpotlightArticles("all", forceRefresh = true)

        assertEquals(2, apiCaptured.size, "forceRefresh 应绕过缓存重新请求")
        assertEquals(
            listOf(3L),
            repository.getCachedSpotlightArticles("all")?.spotlightArticles?.map { it.id },
            "forceRefresh 首页应以覆盖而非合并写入缓存",
        )
    }

    @Test
    fun `REST Spotlight 翻页只返回当页响应且不合并进仓库缓存`() = runBlocking {
        val nextPageUrl = "https://app-api.pixiv.net/v1/spotlight/articles?offset=30"
        apiResponseBody = spotlightJson(listOf(1, 2), nextUrl = nextPageUrl)
        repository.getSpotlightArticles("all")

        apiResponseBody = spotlightJson(listOf(2, 3), nextUrl = null)
        val page2 = repository.getSpotlightArticles("all", nextUrl = nextPageUrl)

        assertEquals(listOf(2L, 3L), page2.spotlightArticles.map { it.id }, "翻页应原样返回当页响应，累积与去重由 UI 层单点负责")
        assertEquals(
            listOf(1L, 2L),
            repository.getCachedSpotlightArticles("all")?.spotlightArticles?.map { it.id },
            "仓库缓存只存最近首页快照，翻页不得把当页合并回缓存",
        )
    }

    @Test
    fun `Novel 分类走 pixivision HTML 抓取并按语言设置拼接路径`() = runBlocking {
        webBody = novelsHtml

        val response = repository.getSpotlightArticles("novel")

        val request = webCaptured.single()
        assertEquals("/en/c/novels", request.path, "默认语言（非 zh/zh-tw/ja/ko）应走 en 语言路径")
        assertTrue(
            request.headers["User-Agent"]?.contains("Mozilla/5.0") == true,
            "网页抓取必须携带浏览器 UA 伪装",
        )
        assertEquals("https://www.pixivision.net/", request.headers["Referer"])
        assertEquals(listOf(9001L, 9002L), response.spotlightArticles.map { it.id })
        assertEquals("测试特辑甲", response.spotlightArticles.first().pureTitle, "解析应剥离【】标记生成纯标题")
        assertNull(response.nextUrl, "novel 分类无翻页")
        assertEquals(listOf(9001L, 9002L), repository.getCachedSpotlightArticles("novel")?.spotlightArticles?.map { it.id })

        // 语言设置变化后强制刷新应切换语言路径（zh-tw）
        settingsRepository.languageNum = 2
        repository.getSpotlightArticles("novel", forceRefresh = true)

        assertEquals("/zh-tw/c/novels", webCaptured.last().path)
    }

    @Test
    fun `Novel 分类翻页返回空响应且不发起任何请求`() = runBlocking {
        val response = repository.getSpotlightArticles("novel", nextUrl = "https://www.pixivision.net/en/c/novels?offset=30")

        assertTrue(response.spotlightArticles.isEmpty())
        assertNull(response.nextUrl)
        assertTrue(webCaptured.isEmpty(), "novel 分类无翻页能力，不应发起请求")
        assertNull(repository.getCachedSpotlightArticles("novel"), "空翻页响应不得写入首页快照缓存")
    }

    @Test
    fun `特辑详情走网页抓取解析且缓存与 forceRefresh 语义正确`() = runBlocking {
        val articleUrl = "https://www.pixivision.net/zh/a/1234"
        webBody = detailHtml

        val detail = repository.getSpotlightArticleDetail(articleUrl)

        val request = webCaptured.single()
        assertEquals("/zh/a/1234", request.path)
        assertEquals("https://www.pixivision.net/zh/", request.headers["Referer"])
        assertTrue(
            request.headers["Accept-Language"]?.startsWith("zh-CN") == true,
            "详情抓取应声明中文语言偏好",
        )
        assertEquals("特辑详情测试", detail.title)
        assertEquals(1, detail.works.size, "详情解析应提取收录画作")
        assertEquals("https://www.pixiv.net/artworks/777", detail.works.single().arworkLink)

        // 二次调用命中缓存
        repository.getSpotlightArticleDetail(articleUrl)
        assertEquals(1, webCaptured.size)

        // forceRefresh 重新抓取
        val refreshed = repository.getSpotlightArticleDetail(articleUrl, forceRefresh = true)
        assertEquals(2, webCaptured.size)
        assertEquals("特辑详情测试", refreshed.title)
        assertEquals("特辑详情测试", repository.getCachedSpotlightDetail(articleUrl)?.title)
    }

    @Test
    fun `clearMemoryCache 同时失效 Spotlight 列表与详情缓存`() = runBlocking {
        apiResponseBody = spotlightJson(listOf(5), nextUrl = null)
        webBody = detailHtml

        repository.getSpotlightArticles("manga")
        repository.getSpotlightArticleDetail("https://www.pixivision.net/zh/a/1234")
        assertEquals(5L, repository.getCachedSpotlightArticles("manga")?.spotlightArticles?.single()?.id)
        assertEquals("特辑详情测试", repository.getCachedSpotlightDetail("https://www.pixivision.net/zh/a/1234")?.title)

        repository.clearMemoryCache()

        assertNull(repository.getCachedSpotlightArticles("manga"), "清内存缓存必须失效 Spotlight 列表 LRU")
        assertNull(repository.getCachedSpotlightDetail("https://www.pixivision.net/zh/a/1234"), "清内存缓存必须失效特辑详情 LRU")
    }
}
