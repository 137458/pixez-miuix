package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.network.PixivApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * PixivApiException.statusCode → 差异化错误文案语义键 的映射测试：
 * 403/429/404 三类可识别状态码各映射到独立语义键；未收录状态码（0/500）、
 * 非 PixivApiException 与 null 一律回退 null，由调用方保持「加载失败」兜底文案。
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
}
