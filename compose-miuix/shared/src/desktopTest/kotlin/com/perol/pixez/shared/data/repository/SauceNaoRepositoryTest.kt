package com.perol.pixez.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SauceNaoRepositoryTest {

    @Test
    fun `search 发送 multipart 请求并成功返回解析结果`() = runBlocking {
        val mockEngine = MockEngine { request ->
            assertEquals("https://saucenao.com/search.php", request.url.toString())
            assertEquals("POST", request.method.value)

            val jsonResponse = """
            {
              "header": { "status": 0 },
              "results": [
                {
                  "header": {
                    "similarity": "95.5",
                    "thumbnail": "https://img1.saucenao.com/t/test.jpg"
                  },
                  "data": {
                    "pixiv_id": 12345678,
                    "title": "测试插画",
                    "member_name": "测试画师"
                  }
                }
              ]
            }
            """.trimIndent()

            respond(
                content = jsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = HttpClient(mockEngine)
        val repository = SauceNaoRepository(client)
        val results = repository.search(byteArrayOf(1, 2, 3, 4), "test.jpg")

        assertEquals(1, results.size)
        assertEquals(12345678L, results[0].illustId)
        assertEquals("测试插画", results[0].title)
        assertEquals("测试画师", results[0].author)
        assertEquals("95.5", results[0].similarity)
    }

    @Test
    fun `服务端返回异常状态码时抛出异常`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
            )
        }

        val client = HttpClient(mockEngine)
        val repository = SauceNaoRepository(client)

        assertFailsWith<Exception> {
            repository.search(byteArrayOf(1, 2, 3), "error.jpg")
        }
        Unit
    }

    @Test
    fun `search 请求体仅携带图片字段且不包含 API 参数`() {
        val repository = SauceNaoRepository(
            HttpClient(
                MockEngine {
                    respond(
                        content = "<html></html>",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"),
                    )
                }
            )
        )
        val parts = repository.buildUploadForm(byteArrayOf(1, 2, 3, 4), "test.jpg")

        // 匿名请求携带 output_type/db 等 API 参数会被 SauceNAO 以 403 拒绝，
        // 必须与网页表单一致：仅上传 name="file" 的图片字段且带 filename。
        assertEquals(listOf("file"), parts.map { it.name })
        val disposition = parts.single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(disposition.contains("name=\"file\""), disposition)
        assertTrue(disposition.contains("filename=\"test.jpg\""), disposition)
    }

    @Test
    fun `buildUploadForm 剥离文件名中的路径分隔符仅保留安全 basename`() {
        val repository = SauceNaoRepository(
            HttpClient(
                MockEngine {
                    respond(content = "", status = HttpStatusCode.OK)
                }
            )
        )

        val unixPath = repository.buildUploadForm(byteArrayOf(1), "folder/sub/pic.png")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(unixPath.contains("filename=\"pic.png\""), unixPath)
        assertFalse(unixPath.contains('/'), unixPath)

        val windowsPath = repository.buildUploadForm(byteArrayOf(1), "C:\\Users\\test\\pic.png")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(windowsPath.contains("filename=\"pic.png\""), windowsPath)
        assertFalse(windowsPath.contains('\\'), windowsPath)

        val traversal = repository.buildUploadForm(byteArrayOf(1), "../../evil.jpg")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(traversal.contains("filename=\"evil.jpg\""), traversal)
        assertFalse(traversal.contains(".."), traversal)
    }

    @Test
    fun `buildUploadForm 去除引号与控制字符防止 Content-Disposition 注入`() {
        val repository = SauceNaoRepository(
            HttpClient(
                MockEngine {
                    respond(content = "", status = HttpStatusCode.OK)
                }
            )
        )

        val quoted = repository.buildUploadForm(byteArrayOf(1), "we\"ird.jpg")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(quoted.contains("filename=\"weird.jpg\""), quoted)

        val controlChars = repository.buildUploadForm(byteArrayOf(1), "bad\r\nInjected: 1.jpg")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(controlChars.contains("filename=\"badInjected: 1.jpg\""), controlChars)
        assertFalse(controlChars.any { it.isISOControl() }, controlChars)
    }

    @Test
    fun `buildUploadForm 保留非 ASCII 文件名并给空值回退默认名`() {
        val repository = SauceNaoRepository(
            HttpClient(
                MockEngine {
                    respond(content = "", status = HttpStatusCode.OK)
                }
            )
        )

        val nonAscii = repository.buildUploadForm(byteArrayOf(1), "插画_タイトル.png")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(nonAscii.contains("filename=\"插画_タイトル.png\""), nonAscii)

        val nullName = repository.buildUploadForm(byteArrayOf(1), null)
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(nullName.contains("filename=\"image.jpg\""), nullName)

        val blankName = repository.buildUploadForm(byteArrayOf(1), "   ")
            .single().headers[HttpHeaders.ContentDisposition].orEmpty()
        assertTrue(blankName.contains("filename=\"image.jpg\""), blankName)
    }
}