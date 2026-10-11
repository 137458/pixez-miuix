package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.network.PixivApiException
import com.perol.pixez.shared.ui.i18n.EnStrings
import com.perol.pixez.shared.ui.i18n.ZhCnStrings
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
}
