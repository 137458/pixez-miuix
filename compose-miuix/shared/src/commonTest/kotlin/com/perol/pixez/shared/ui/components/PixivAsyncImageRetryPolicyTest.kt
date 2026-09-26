package com.perol.pixez.shared.ui.components

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 图片加载自愈两条纯逻辑的验证：
 *
 * 1. [shouldCountAsLoadProgress]——停滞看门狗的「事件 = 进展」判定。取消类错误必须不算进展，
 *    否则被静默取消的请求会让看门狗提前放行，详情页灰底永远不恢复（CHANGELOG 所称修复的漏洞）。
 * 2. [resolveSilentErrorDecision]——静默重试与调用方错误回调的分配规则。
 */
class PixivAsyncImageRetryPolicyTest {

    @Test
    fun `无异常的状态一律算加载进展`() {
        assertTrue(shouldCountAsLoadProgress(throwable = null))
    }

    @Test
    fun `普通错误也算加载进展由静默重试或错误回调处理`() {
        assertTrue(shouldCountAsLoadProgress(RuntimeException("network broken")))
    }

    @Test
    fun `取消类错误不算加载进展交由看门狗超时重建`() {
        assertFalse(
            shouldCountAsLoadProgress(CancellationException("request cancelled silently")),
            "取消类 Error 若算进展，看门狗会放行而灰底自愈失效",
        )
    }

    @Test
    fun `调用方未接管错误且未达重试预算时静默重试`() {
        val decision = resolveSilentErrorDecision(
            throwable = RuntimeException("transient"),
            onError = null,
            silentRetryCount = 0,
        )

        assertEquals(SilentErrorDecision.Retry, decision)
    }

    @Test
    fun `重试预算耗尽后不再静默重试`() {
        val throwable = RuntimeException("still broken")
        val decision = resolveSilentErrorDecision(
            throwable = throwable,
            onError = null,
            silentRetryCount = SILENT_ERROR_MAX_RETRIES,
        )

        assertEquals(SilentErrorDecision.Report(throwable), decision)
    }

    @Test
    fun `调用方已接管错误回调时永不静默重试`() {
        val throwable = RuntimeException("caller handled")
        val decision = resolveSilentErrorDecision(
            throwable = throwable,
            onError = {},
            silentRetryCount = 0,
        )

        assertEquals(SilentErrorDecision.Report(throwable), decision)
    }
}
