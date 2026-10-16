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
}
