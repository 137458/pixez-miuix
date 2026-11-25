package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.network.HttpClientDefaults
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 更新检查兜底链路测试：GitHub REST API 触发限流（403）时改走 releases.atom +
 * expanded_assets（无配额），并把两条链路各自的失败原因合并回失败消息。
 */
class UpdateRepositoryFallbackTest {

    private data class CapturedRequest(val path: String, val ifNoneMatch: String?)

    private val captured = mutableListOf<CapturedRequest>()

    private var apiStatus: HttpStatusCode = HttpStatusCode.Forbidden
    private var apiBody: String = "{\"message\":\"API rate limit exceeded\"}"
    private var apiEtag: String? = null

    private var feedStatus: HttpStatusCode = HttpStatusCode.OK
    private var feedBody: String = ""

    private var assetsStatus: HttpStatusCode = HttpStatusCode.OK
    private var assetsBody: String = ""

    private lateinit var client: HttpClient
    private lateinit var repository: UpdateRepository

    @Before
    fun setUp() {
        feedBody = feedXml(tagName = "v99.0.0")
        assetsBody = expandedAssetsHtml(tagName = "v99.0.0")
        client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(HttpClientDefaults.commonJson)
            }
            engine {
                addHandler { request ->
                    val path = request.url.fullPath
                    captured.add(CapturedRequest(path, request.headers[HttpHeaders.IfNoneMatch]))
                    when {
                        path.endsWith("/releases/latest") -> respond(
                            content = apiBody,
                            status = apiStatus,
                            headers = if (apiEtag != null) {
                                headersOf(
                                    HttpHeaders.ContentType to listOf("application/json"),
                                    HttpHeaders.ETag to listOf(apiEtag.orEmpty()),
                                )
                            } else {
                                headersOf(HttpHeaders.ContentType to listOf("application/json"))
                            },
                        )

                        path.endsWith("/releases.atom") -> respond(
                            content = feedBody,
                            status = feedStatus,
                            headers = headersOf(HttpHeaders.ContentType to listOf("application/atom+xml")),
                        )

                        path.contains("/releases/expanded_assets/") -> respond(
                            content = assetsBody,
                            status = assetsStatus,
                            headers = headersOf(HttpHeaders.ContentType to listOf("text/html")),
                        )

                        else -> respond("unexpected path", HttpStatusCode.NotFound)
                    }
                }
            }
        }
        repository = UpdateRepository(client)
    }

    @After
    fun tearDown() {
        client.close()
    }

    private fun feedXml(tagName: String): String = """
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <id>tag:github.com,2008:Repository/1304646975/TAG</id>
            <updated>2026-10-01T00:00:00Z</updated>
            <link rel="alternate" type="text/html" href="https://github.com/137458/pixez-miuix/releases/tag/TAG"/>
            <title>PixEz MIUIX TAG</title>
            <content type="html">&lt;h2&gt;新增&lt;/h2&gt;&lt;ul&gt;&lt;li&gt;Atom 兜底可用&lt;/li&gt;&lt;/ul&gt;</content>
          </entry>
        </feed>
    """.trimIndent().replace("TAG", tagName)

    private fun expandedAssetsHtml(tagName: String): String {
        val version = tagName.removePrefix("v")
        return """
            <ul>
              <li class="Box-row">
                <a href="/137458/pixez-miuix/releases/download/TAG/PixEz-MIUIX-vVERSION.apk" rel="nofollow">PixEz-MIUIX-vVERSION.apk</a>
                <span>sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee</span>
              </li>
            </ul>
        """.trimIndent().replace("VERSION", version).replace("TAG", tagName)
    }

    @Test
    fun `API 限流 403 时回退 Atom 源并给出可下载安装包`() = runBlocking {
        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isSuccess, "API 403 时 Atom 兜底成功，整体应成功")
        val info = result.getOrThrow()
        assertEquals("v99.0.0", info.tagName)
        assertEquals("99.0.0", info.versionName)
        assertEquals("PixEz MIUIX v99.0.0", info.title)
        assertTrue(info.isNew, "99.0.0 应比当前已安装版本新")
        assertEquals("2026-10-01T00:00:00Z", info.publishedAt)
        assertEquals("## 新增\n- Atom 兜底可用", info.changelog)
        assertEquals(
            "https://github.com/137458/pixez-miuix/releases/download/v99.0.0/PixEz-MIUIX-v99.0.0.apk",
            info.downloadUrl,
        )
        assertEquals("PixEz-MIUIX-v99.0.0.apk", info.fileName)
        assertEquals(
            "sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
            info.digest,
        )
        assertEquals(
            "https://github.com/137458/pixez-miuix/releases/tag/v99.0.0",
            info.releaseUrl,
        )
        assertEquals(info, repository.cachedReleaseInfo, "兜底成功也应写入缓存")

        val paths = captured.map { it.path }
        assertEquals(3, paths.size, "应依次请求 API、atom、expanded_assets，实际为：$paths")
        assertTrue(paths[0].endsWith("/releases/latest"))
        assertTrue(paths[1].endsWith("/137458/pixez-miuix/releases.atom"))
        assertTrue(paths[2].contains("/releases/expanded_assets/v99.0.0"))
    }

    @Test
    fun `API 限流且 Atom 源同样失败时合并两侧失败原因`() = runBlocking {
        feedStatus = HttpStatusCode.InternalServerError

        val result = repository.fetchLatestReleaseInfo()

        assertFalse(result.isSuccess)
        val error = result.exceptionOrNull()
        assertTrue(error is IllegalStateException, "兜底链失败应返回 IllegalStateException")
        val message = error.message.orEmpty()
        assertTrue(message.contains("403"), "消息应保留原始 API 状态码，实际为：$message")
        assertTrue(message.contains("Atom"), "消息应说明 Atom 兜底也失败，实际为：$message")
    }

    @Test
    fun `Atom 兜底资产列表不可用时仍返回版本信息且无下载直链`() = runBlocking {
        assetsStatus = HttpStatusCode.NotFound

        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isSuccess, "资产列表失败不应整体失败，版本信息仍可用")
        val info = result.getOrThrow()
        assertEquals("v99.0.0", info.tagName)
        assertNull(info.downloadUrl, "拿不到资产直链时应为 null")
        assertNull(info.fileName)
    }

    @Test
    fun `Atom 兜底遇到非官方仓库直链时整体失败`() = runBlocking {
        assetsBody = """
            <ul>
              <li class="Box-row">
                <a href="/attacker/malware/releases/download/v99.0.0/PixEz-MIUIX-v99.0.0.apk">PixEz-MIUIX-v99.0.0.apk</a>
              </li>
            </ul>
        """.trimIndent()

        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isFailure, "asset 直链不属于官方仓库时必须整体失败（fail closed）")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("官方仓库") == true,
            "失败消息应说明非官方仓库，实际为：${result.exceptionOrNull()?.message}",
        )
    }

    @Test
    fun `API 成功响应携带 ETag 时后续请求改发条件请求并在 304 命中缓存`() = runBlocking {
        apiStatus = HttpStatusCode.OK
        apiEtag = "\"etag-1\""
        apiBody = """
            {
              "tag_name": "v99.0.0",
              "name": "PixEz MIUIX v99.0.0",
              "body": "- 新增功能",
              "html_url": "https://github.com/137458/pixez-miuix/releases/tag/v99.0.0",
              "published_at": "2026-10-01T00:00:00Z",
              "assets": []
            }
        """.trimIndent()

        val first = repository.fetchLatestReleaseInfo().getOrThrow()
        assertEquals("v99.0.0", first.tagName)

        apiStatus = HttpStatusCode.NotModified
        apiBody = ""
        val second = repository.fetchLatestReleaseInfo().getOrThrow()

        assertEquals("v99.0.0", second.tagName, "304 应返回缓存内容")
        assertEquals(2, captured.size, "304 命中后不再回退 Atom 源，实际请求：${captured.map { it.path }}")
        assertNull(captured[0].ifNoneMatch, "首次请求尚无 ETag，不应带 If-None-Match")
        assertEquals("\"etag-1\"", captured[1].ifNoneMatch, "缓存 ETag 后应改发条件请求")
    }

    @Test
    fun `API 返回 200 但缺少 tag_name 时不回退 Atom 源`() = runBlocking {
        apiStatus = HttpStatusCode.OK
        apiBody = "{\"tag_name\": null}"

        val result = repository.fetchLatestReleaseInfo()

        assertFalse(result.isSuccess)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("tag_name") == true,
            "应保留载荷错误原因，实际为：${result.exceptionOrNull()?.message}",
        )
        assertEquals(
            listOf("/repos/137458/pixez-miuix/releases/latest"),
            captured.map { it.path },
            "载荷错误不是配额问题，不应触发 Atom 兜底",
        )
    }

    @Test
    fun `Atom 兜底清除估算 fileSize 且使旧 API ETag 失效并对非官方 releaseUrl 回退`() = runBlocking {
        // 1. 先经 API 缓存 v98.0.0 与 ETag
        apiStatus = HttpStatusCode.OK
        apiEtag = "\"etag-v98\""
        apiBody = """
            {
              "tag_name": "v98.0.0",
              "name": "PixEz MIUIX v98.0.0",
              "body": "old",
              "html_url": "https://evil.example.com/phishing",
              "published_at": "2026-09-01T00:00:00Z",
              "assets": []
            }
        """.trimIndent()
        val apiInfo = repository.fetchLatestReleaseInfo().getOrThrow()
        assertEquals("https://github.com/137458/pixez-miuix/releases", apiInfo.releaseUrl)

        // 2. API 403 回退 Atom（带 8.04 MB 文本估算体积），应产出 fileSize = null 且清空 apiCache
        apiStatus = HttpStatusCode.Forbidden
        assetsBody = """
            <ul>
              <li class="Box-row">
                <a href="/137458/pixez-miuix/releases/download/v99.0.0/PixEz-MIUIX-v99.0.0.apk">PixEz-MIUIX-v99.0.0.apk</a>
                <span>8.04 MB</span>
                <span>sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee</span>
              </li>
            </ul>
        """.trimIndent()
        val atomInfo = repository.fetchLatestReleaseInfo().getOrThrow()
        assertNull(atomInfo.fileSize, "Atom 估算体积不应写入 fileSize，避免重启后精确字节比对误删已完成包")

        // 3. 再次请求 API 时不应携带已失效的 etag-v98
        apiStatus = HttpStatusCode.OK
        apiEtag = "\"etag-v100\""
        apiBody = """
            {
              "tag_name": "v100.0.0",
              "name": "PixEz MIUIX v100.0.0",
              "body": "new",
              "html_url": "https://github.com/137458/pixez-miuix/releases/tag/v100.0.0",
              "published_at": "2026-10-02T00:00:00Z",
              "assets": []
            }
        """.trimIndent()
        val third = repository.fetchLatestReleaseInfo().getOrThrow()
        assertEquals("v100.0.0", third.tagName)
        assertNull(captured.last().ifNoneMatch, "Atom 覆盖缓存后不应继续发送旧 API ETag")
    }
}
