package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.AppInfo
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
import kotlin.test.fail

/**
 * 更新检查仓库测试：用 Ktor MockEngine 打桩 GitHub Release API（外部网络），
 * 验证构造注入 client 的请求端点与 UA、成功解析、失败路径与本地日志回退语义。
 */
class UpdateRepositoryTest {

    private data class CapturedRequest(
        val path: String,
        val userAgent: String?,
    )

    private val captured = mutableListOf<CapturedRequest>()

    /** 由用例改写的桩响应：状态码与 JSON 体。 */
    private var respondStatus: HttpStatusCode = HttpStatusCode.OK
    private var respondBody: String = ""

    private lateinit var client: HttpClient
    private lateinit var repository: UpdateRepository

    @Before
    fun setUp() {
        client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(HttpClientDefaults.commonJson)
            }
            engine {
                addHandler { request ->
                    captured.add(
                        CapturedRequest(
                            path = request.url.fullPath,
                            userAgent = request.headers[HttpHeaders.UserAgent],
                        )
                    )
                    respond(
                        content = respondBody,
                        status = respondStatus,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        repository = UpdateRepository(client)
    }

    @After
    fun tearDown() {
        client.close()
    }

    private fun releaseJson(
        tagName: String?,
        body: String? = "- 新增功能\n- 修复问题",
        assetsJson: String = """
            [
              {"name":"PixEz-windows-x64-99.0.0.exe","browser_download_url":"https://github.com/137458/pixez-miuix/releases/download/v99.0.0/PixEz-windows-x64-99.0.0.exe","size":68000000,"digest":"sha256:abcd1234"},
              {"name":"pixez-miuix-99.0.0.apk","browser_download_url":"https://github.com/137458/pixez-miuix/releases/download/v99.0.0/pixez-miuix-99.0.0.apk","size":25000000}
            ]
        """.trimIndent(),
    ): String = """
        {
          "tag_name": ${tagName?.let { "\"$it\"" } ?: "null"},
          "name": "PixEz MIUIX ${tagName ?: ""}",
          "body": ${body?.let { "\"$it\"" } ?: "null"},
          "html_url": "https://github.com/137458/pixez-miuix/releases/tag/v99.0.0",
          "published_at": "2026-10-01T00:00:00Z",
          "assets": $assetsJson
        }
    """.trimIndent()

    @Test
    fun `fetchLatestReleaseInfo 成功解析并选用 apk 安装包与摘要`() = runBlocking {
        respondBody = releaseJson(
            tagName = "v99.0.0",
            assetsJson = """
                [
                  {"name":"PixEz-windows-x64-99.0.0.exe","browser_download_url":"https://github.com/137458/pixez-miuix/releases/download/v99.0.0/PixEz-windows-x64-99.0.0.exe","size":68000000,"digest":"sha256:ignored"},
                  {"name":"pixez-miuix-99.0.0.apk","browser_download_url":"https://github.com/137458/pixez-miuix/releases/download/v99.0.0/pixez-miuix-99.0.0.apk","size":25000000,"digest":"sha256:abcd1234"}
                ]
            """.trimIndent(),
        )

        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isSuccess, "GitHub 正常响应应返回成功结果")
        val info = result.getOrThrow()
        assertEquals("v99.0.0", info.tagName)
        assertEquals("99.0.0", info.versionName, "versionName 应去掉 tag 的 v 前缀")
        assertTrue(info.isNew, "99.0.0 应比当前已安装版本新")
        assertEquals("PixEz MIUIX v99.0.0", info.title)
        assertEquals("- 新增功能\n- 修复问题", info.changelog)
        assertEquals("2026-10-01T00:00:00Z", info.publishedAt)
        assertEquals(
            "https://github.com/137458/pixez-miuix/releases/download/v99.0.0/pixez-miuix-99.0.0.apk",
            info.downloadUrl,
        )
        assertEquals("pixez-miuix-99.0.0.apk", info.fileName)
        assertEquals(25_000_000L, info.fileSize)
        assertEquals("sha256:abcd1234", info.digest)

        val request = captured.single()
        assertEquals("/repos/137458/pixez-miuix/releases/latest", request.path)
        assertEquals("PixEz-MIUIX/${AppInfo.VERSION_NAME}", request.userAgent)
    }

    @Test
    fun `fetchLatestReleaseInfo 缺失 digest 时透传 null`() = runBlocking {
        respondBody = releaseJson(tagName = "v99.0.0")

        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isSuccess, "GitHub 正常响应应返回成功结果")
        val info = result.getOrThrow()
        assertEquals(
            "https://github.com/137458/pixez-miuix/releases/download/v99.0.0/pixez-miuix-99.0.0.apk",
            info.downloadUrl,
        )
        assertEquals("pixez-miuix-99.0.0.apk", info.fileName)
        assertEquals(25_000_000L, info.fileSize)
        assertNull(info.digest, "该 apk 资产未提供 digest，应透传为 null")
    }

    @Test
    fun `fetchLatestReleaseInfo 非 2xx 返回失败并携带状态码`() = runBlocking {
        respondStatus = HttpStatusCode.Forbidden
        respondBody = "{\"message\":\"API rate limit exceeded\"}"

        val result = repository.fetchLatestReleaseInfo()

        assertFalse(result.isSuccess, "403 应返回失败结果而非抛出")
        val error = result.exceptionOrNull()
        assertTrue(error is IllegalStateException, "失败原因应为 IllegalStateException")
        assertTrue(
            error.message?.contains("403") == true,
            "失败消息应携带 HTTP 状态码，实际为: ${error.message}",
        )
    }

    @Test
    fun `fetchLatestReleaseInfo 缺失 tag_name 返回失败`() = runBlocking {
        respondBody = releaseJson(tagName = null)

        val result = repository.fetchLatestReleaseInfo()

        assertFalse(result.isSuccess, "缺失 tag_name（未认证速率限制场景）应返回失败结果")
        val error = result.exceptionOrNull()
        assertTrue(
            error?.message?.contains("tag_name") == true,
            "失败消息应说明 tag_name 缺失，实际为: ${error?.message}",
        )
    }

    @Test
    fun `fetchLatestReleaseInfo 远端日志空白且非新版本时回退本地内置日志`() = runBlocking {
        respondBody = releaseJson(
            tagName = "v${AppInfo.VERSION_NAME}",
            body = "   ",
            assetsJson = "[]",
        )

        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isSuccess)
        val info = result.getOrThrow()
        assertFalse(info.isNew, "tag 与当前版本一致时不应判定为新版本")
        assertEquals(AppInfo.CURRENT_CHANGELOG, info.changelog, "空白远端日志应回退本地内置日志")
        assertNull(info.downloadUrl, "无资产时 downloadUrl 应为 null")
        assertNull(info.fileName)
    }

    @Test
    fun `fetchLatestReleaseInfo 拒绝指向非官方仓库的 asset 直链`() = runBlocking {
        respondBody = releaseJson(
            tagName = "v99.0.0",
            assetsJson = """
                [
                  {"name":"pixez-miuix-99.0.0.apk","browser_download_url":"https://github.com/attacker/malware/releases/download/v99.0.0/pixez-miuix-99.0.0.apk","size":25000000}
                ]
            """.trimIndent(),
        )

        val result = repository.fetchLatestReleaseInfo()

        assertTrue(result.isFailure, "asset 直链不属于官方仓库时必须整体失败（fail closed）")
        val error = result.exceptionOrNull()
        assertTrue(
            error?.message?.contains("官方仓库") == true,
            "失败消息应说明非官方仓库，实际为: ${error?.message}",
        )
    }

    @Test
    fun `fetchLatestReleaseInfo 成功后可通过 cachedReleaseInfo 同步读取`() = runBlocking {
        assertNull(repository.cachedReleaseInfo, "初始缓存应为 null")
        respondBody = releaseJson(tagName = "v99.0.0")

        val info = repository.fetchLatestReleaseInfo().getOrThrow()

        assertEquals(info, repository.cachedReleaseInfo, "请求成功后应同步更新 cachedReleaseInfo")
    }
}
