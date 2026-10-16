package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.SauceNaoParser
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.ui.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/**
 * SauceNAO 以图搜图数据仓库。
 * 负责组装 multipart/form-data 二进制表单并提交至 SauceNAO 接口，交由 [SauceNaoParser] 解析结果。
 */
class SauceNaoRepository(
    private val client: HttpClient,
) {
    suspend fun search(
        imageBytes: ByteArray,
        fileName: String? = null,
    ): List<SauceNaoResult> {
        val response = client.submitFormWithBinaryData(
            url = AppConstants.Urls.SAUCE_NAO_SEARCH,
            formData = formData {
                append("output_type", "2")
                append("db", "999")
                append(
                    key = "file",
                    value = imageBytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, """filename="${fileName ?: "image.jpg"}"""")
                    },
                )
            },
        )

        if (!response.status.isSuccess()) {
            throw IllegalStateException("SauceNAO request failed with status: ${response.status}")
        }

        val text = response.bodyAsText()
        return SauceNaoParser.parse(text)
    }
}
