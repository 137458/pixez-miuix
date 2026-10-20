package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.SauceNaoParser
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.platform.FileNamePolicy
import com.perol.pixez.shared.ui.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.isSuccess
import kotlinx.io.Buffer

/** 上传文件名清洗后为空时的默认名，与网页端表单默认行为一致。 */
private const val DEFAULT_UPLOAD_FILE_NAME = "image.jpg"

/**
 * 上传文件名清洗：与下载/保存链路的 [FileNamePolicy.requireSafeBaseName] 同口径。
 * 先取安全 basename（剥除 / 与 \ 分隔的目录段），再去除引号与 ISO 控制字符；
 * 清洗后仍不合法的名字（如 "."、".."）经 [FileNamePolicy.sanitizeSegment] 兜底替换非法字符。
 * 保证拼入 Content-Disposition 的 filename 永不携带目录分隔符、引号与控制字符，头注入不可行。
 */
internal fun sanitizeUploadFileName(raw: String?): String {
    val baseName = raw.orEmpty()
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace("\"", "")
        .filterNot { it.isISOControl() }
        .trim()
    if (baseName.isEmpty()) return DEFAULT_UPLOAD_FILE_NAME
    return runCatching { FileNamePolicy.requireSafeBaseName(baseName) }
        .getOrElse { FileNamePolicy.sanitizeSegment(baseName) }
        .ifBlank { DEFAULT_UPLOAD_FILE_NAME }
}

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
            formData = buildUploadForm(imageBytes, fileName),
        )

        if (!response.status.isSuccess()) {
            throw IllegalStateException("SauceNAO request failed with status: ${response.status}")
        }

        val text = response.bodyAsText()
        return SauceNaoParser.parse(text)
    }

    /**
     * SauceNAO 匿名账号禁止携带 output_type/db 等 API 参数（返回 403）。
     * 请求必须与网页端表单一致：仅上传 name="file" 的图片字段，服务端返回 HTML 结果页。
     */
    internal fun buildUploadForm(
        imageBytes: ByteArray,
        fileName: String?,
    ): List<PartData> = listOf(
        // 直接构造分部以保留 name="file"; filename=… 的完整 Content-Disposition：
        // append(key, ByteArray) 会重写 disposition 丢掉 filename，导致服务端无法识别上传字段。
        PartData.BinaryItem(
            provider = { Buffer().apply { write(imageBytes, 0, imageBytes.size) } },
            dispose = {},
            partHeaders = Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(
                    HttpHeaders.ContentDisposition,
                    "form-data; name=\"file\"; filename=\"${sanitizeUploadFileName(fileName)}\"",
                )
            },
        ),
    )
}