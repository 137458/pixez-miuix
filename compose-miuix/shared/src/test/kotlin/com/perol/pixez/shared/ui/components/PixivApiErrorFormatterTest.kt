package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.network.AppErrorKind
import com.perol.pixez.shared.network.PixivApiException
import com.perol.pixez.shared.ui.i18n.EnStrings
import com.perol.pixez.shared.ui.i18n.JaStrings
import com.perol.pixez.shared.ui.i18n.ZhCnStrings
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * PixivApiException.statusCode → 差异化错误文案语义键 的映射测试：
 * 403/429/404 三类可识别状态码各映射到独立语义键；未收录状态码（0/500）、
 * 非 PixivApiException 与 null 一律回退 null，由调用方保持「加载失败」兜底文案。
 *
 * format(error, strings) 测试覆盖报告「高-6」要求的差异化数据：
 * HTTP 错误码（403/429/404 差异化 + 未收录 500）、网络错误（SocketTimeoutException）、
 * 解析失败（SerializationException）、取消（CancellationException）与 null，
 * 全部以 AppStrings 具体语言实例断言精确文案，且不泄露原始异常消息。
 */
class PixivApiErrorFormatterTest {

    // ---------- 常规：可识别 statusCode 的差异化映射 ----------

    @Test
    fun `403 映射到 FORBIDDEN 语义键`() {
        val key = PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 403, message = "请求失败: 403 Forbidden"))
        assertEquals(PixivApiErrorKey.FORBIDDEN, key)
    }

    @Test
    fun `429 映射到 RATE_LIMITED 语义键`() {
        val key = PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 429, message = "请求失败: 429 Too Many Requests"))
        assertEquals(PixivApiErrorKey.RATE_LIMITED, key)
    }

    @Test
    fun `404 映射到 NOT_FOUND 语义键`() {
        val key = PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 404, message = "请求失败: 404 Not Found"))
        assertEquals(PixivApiErrorKey.NOT_FOUND, key)
    }

    @Test
    fun `三个语义键两两互异`() {
        val forbidden = PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 403, message = "f"))
        val rateLimited = PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 429, message = "r"))
        val notFound = PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 404, message = "n"))
        assertEquals(3, setOf(forbidden, rateLimited, notFound).size)
    }

    // ---------- 边界：不可识别输入回退 null ----------

    @Test
    fun `statusCode 为 0 时回退 null`() {
        assertNull(PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 0, message = "请求失败")))
    }

    @Test
    fun `未收录的 statusCode 如 500 回退 null`() {
        assertNull(PixivApiErrorFormatter.resolve(PixivApiException(statusCode = 500, message = "请求失败: 500")))
    }

    @Test
    fun `非 PixivApiException 的 Throwable 回退 null`() {
        assertNull(PixivApiErrorFormatter.resolve(IllegalStateException("非 API 异常")))
        assertNull(PixivApiErrorFormatter.resolve(RuntimeException("运行时异常")))
    }

    @Test
    fun `error 为 null 时回退 null`() {
        assertNull(PixivApiErrorFormatter.resolve(null))
    }

    // ---------- format：可识别 HTTP 错误码 → 差异化本地化文案 ----------

    @Test
    fun `format 403 返回 FORBIDDEN 本地化文案且随传入语言变化`() {
        val error = PixivApiException(statusCode = 403, message = "请求失败: 403 (截断的响应体)")
        assertEquals(EnStrings.loadFailedForbidden, PixivApiErrorFormatter.format(error, EnStrings))
        assertEquals(ZhCnStrings.loadFailedForbidden, PixivApiErrorFormatter.format(error, ZhCnStrings))
    }

    @Test
    fun `format 429 返回 RATE_LIMITED 本地化文案`() {
        val error = PixivApiException(statusCode = 429, message = "请求失败: 429 Too Many Requests")
        assertEquals(EnStrings.loadFailedRateLimited, PixivApiErrorFormatter.format(error, EnStrings))
        assertEquals(ZhCnStrings.loadFailedRateLimited, PixivApiErrorFormatter.format(error, ZhCnStrings))
    }

    @Test
    fun `format 404 返回 NOT_FOUND 本地化文案`() {
        val error = PixivApiException(statusCode = 404, message = "请求失败: 404 Not Found")
        assertEquals(EnStrings.loadFailedNotFound, PixivApiErrorFormatter.format(error, EnStrings))
        assertEquals(ZhCnStrings.loadFailedNotFound, PixivApiErrorFormatter.format(error, ZhCnStrings))
    }

    // ---------- format：未收录 HTTP 错误码与消息泄露防护 ----------

    @Test
    fun `format 未收录状态码 500 回退 loadFailed`() {
        val error = PixivApiException(statusCode = 500, message = "请求失败: 500 Internal Server Error")
        assertEquals(EnStrings.loadFailed, PixivApiErrorFormatter.format(error, EnStrings))
        assertEquals(ZhCnStrings.loadFailed, PixivApiErrorFormatter.format(error, ZhCnStrings))
    }

    @Test
    fun `format 结果不包含原始异常消息`() {
        val error = PixivApiException(statusCode = 403, message = "请求失败: 403 (截断的响应体)")
        val formatted = PixivApiErrorFormatter.format(error, EnStrings)
        assertFalse(formatted.contains("请求失败"))
        assertFalse(formatted.contains("截断的响应体"))
    }

    // ---------- format：非 PixivApiException 差异化数据回退 loadFailed ----------

    @Test
    fun `format 网络错误回退 loadFailed`() {
        val formatted = PixivApiErrorFormatter.format(SocketTimeoutException("Connect timed out"), EnStrings)
        assertEquals(EnStrings.loadFailed, formatted)
    }

    @Test
    fun `format 解析失败回退 loadFailed`() {
        val formatted = PixivApiErrorFormatter.format(
            SerializationException("Unexpected JSON token at offset 0"),
            ZhCnStrings,
        )
        assertEquals(ZhCnStrings.loadFailed, formatted)
    }

    @Test
    fun `format 取消异常回退 loadFailed`() {
        val formatted = PixivApiErrorFormatter.format(CancellationException("Job was cancelled"), EnStrings)
        assertEquals(EnStrings.loadFailed, formatted)
    }

    @Test
    fun `format error 为 null 时回退 loadFailed`() {
        assertEquals(ZhCnStrings.loadFailed, PixivApiErrorFormatter.format(null, ZhCnStrings))
    }

    // ---------- AppErrorKind：PixivApiException.errorKind 稳定映射 ----------

    @Test
    fun `PixivApiException 403 的 errorKind 为 FORBIDDEN`() {
        assertSame(AppErrorKind.FORBIDDEN, PixivApiException(statusCode = 403, message = "f").errorKind)
    }

    @Test
    fun `PixivApiException 429 的 errorKind 为 RATE_LIMITED`() {
        assertSame(AppErrorKind.RATE_LIMITED, PixivApiException(statusCode = 429, message = "r").errorKind)
    }

    @Test
    fun `PixivApiException 404 的 errorKind 为 NOT_FOUND`() {
        assertSame(AppErrorKind.NOT_FOUND, PixivApiException(statusCode = 404, message = "n").errorKind)
    }

    @Test
    fun `PixivApiException 未收录状态码的 errorKind 为 API_ERROR`() {
        assertSame(AppErrorKind.API_ERROR, PixivApiException(statusCode = 500, message = "5").errorKind)
        assertSame(AppErrorKind.API_ERROR, PixivApiException(statusCode = 401, message = "u").errorKind)
        assertSame(AppErrorKind.API_ERROR, PixivApiException(statusCode = 0, message = "0").errorKind)
    }

    @Test
    fun `kindOf 按异常类型归一：PixivApiException 取 errorKind 其余为 UNKNOWN`() {
        assertSame(AppErrorKind.FORBIDDEN, PixivApiErrorFormatter.kindOf(PixivApiException(statusCode = 403, message = "f")))
        assertSame(AppErrorKind.UNKNOWN, PixivApiErrorFormatter.kindOf(IllegalStateException("io")))
        assertSame(AppErrorKind.UNKNOWN, PixivApiErrorFormatter.kindOf(null))
    }

    // ---------- format(AppErrorKind)：错误码到本地化文案的差异化映射 ----------

    @Test
    fun `format FORBIDDEN 返回差异化文案且随语言变化`() {
        assertEquals(EnStrings.loadFailedForbidden, PixivApiErrorFormatter.format(AppErrorKind.FORBIDDEN, EnStrings))
        assertEquals(ZhCnStrings.loadFailedForbidden, PixivApiErrorFormatter.format(AppErrorKind.FORBIDDEN, ZhCnStrings))
    }

    @Test
    fun `format RATE_LIMITED 返回限流文案`() {
        assertEquals(EnStrings.loadFailedRateLimited, PixivApiErrorFormatter.format(AppErrorKind.RATE_LIMITED, EnStrings))
        assertEquals(ZhCnStrings.loadFailedRateLimited, PixivApiErrorFormatter.format(AppErrorKind.RATE_LIMITED, ZhCnStrings))
    }

    @Test
    fun `format NOT_FOUND 返回不存在文案`() {
        assertEquals(EnStrings.loadFailedNotFound, PixivApiErrorFormatter.format(AppErrorKind.NOT_FOUND, EnStrings))
        assertEquals(ZhCnStrings.loadFailedNotFound, PixivApiErrorFormatter.format(AppErrorKind.NOT_FOUND, ZhCnStrings))
    }

    @Test
    fun `format DOWNLOAD_FAILED 返回下载失败文案`() {
        assertEquals(EnStrings.downloadFailed, PixivApiErrorFormatter.format(AppErrorKind.DOWNLOAD_FAILED, EnStrings))
        assertEquals(ZhCnStrings.downloadFailed, PixivApiErrorFormatter.format(AppErrorKind.DOWNLOAD_FAILED, ZhCnStrings))
        assertEquals(JaStrings.downloadFailed, PixivApiErrorFormatter.format(AppErrorKind.DOWNLOAD_FAILED, JaStrings))
    }

    @Test
    fun `format UGOIRA_SAVE_FAILED 返回动图保存失败文案`() {
        assertEquals(EnStrings.ugoiraSaveFailed, PixivApiErrorFormatter.format(AppErrorKind.UGOIRA_SAVE_FAILED, EnStrings))
        assertEquals(ZhCnStrings.ugoiraSaveFailed, PixivApiErrorFormatter.format(AppErrorKind.UGOIRA_SAVE_FAILED, ZhCnStrings))
        assertEquals(JaStrings.ugoiraSaveFailed, PixivApiErrorFormatter.format(AppErrorKind.UGOIRA_SAVE_FAILED, JaStrings))
    }

    @Test
    fun `format API_ERROR 与 UNKNOWN 回退 loadFailed`() {
        assertEquals(EnStrings.loadFailed, PixivApiErrorFormatter.format(AppErrorKind.API_ERROR, EnStrings))
        assertEquals(ZhCnStrings.loadFailed, PixivApiErrorFormatter.format(AppErrorKind.UNKNOWN, ZhCnStrings))
    }

    // ---------- format(Throwable) 与 AppErrorKind 通道口径一致 ----------

    @Test
    fun `format Throwable 与对应 errorKind 的文案一致`() {
        val error = PixivApiException(statusCode = 429, message = "请求失败: 429")
        assertEquals(
            PixivApiErrorFormatter.format(AppErrorKind.RATE_LIMITED, EnStrings),
            PixivApiErrorFormatter.format(error, EnStrings),
        )
    }

    // ---------- toLocalizedDownloadError：task.error 错误码（含遗留数据）解码 ----------

    @Test
    fun `task error 错误码解码为本地化文案`() {
        assertEquals(EnStrings.downloadFailed, "download_failed".toLocalizedDownloadError(EnStrings))
        assertEquals(ZhCnStrings.loadFailedForbidden, "forbidden".toLocalizedDownloadError(ZhCnStrings))
        assertEquals(ZhCnStrings.ugoiraSaveFailed, "ugoira_save_failed".toLocalizedDownloadError(ZhCnStrings))
        assertEquals(ZhCnStrings.loadFailedRateLimited, "rate_limited".toLocalizedDownloadError(ZhCnStrings))
        assertEquals(EnStrings.loadFailedNotFound, "not_found".toLocalizedDownloadError(EnStrings))
    }

    @Test
    fun `task error 遗留中文与异常消息回退 loadFailed 不泄露细节`() {
        assertEquals(ZhCnStrings.loadFailed, "下载失败".toLocalizedDownloadError(ZhCnStrings))
        assertEquals(EnStrings.loadFailed, "Connect timed out".toLocalizedDownloadError(EnStrings))
        assertEquals(EnStrings.loadFailed, "forbidden (legacy exception message)".toLocalizedDownloadError(EnStrings))
    }

    @Test
    fun `task error 为 null 回退 loadFailed`() {
        assertEquals(ZhCnStrings.loadFailed, null.toLocalizedDownloadError(ZhCnStrings))
        assertEquals(ZhCnStrings.loadFailed, (null as String?).toLocalizedDownloadError(ZhCnStrings))
    }
}
