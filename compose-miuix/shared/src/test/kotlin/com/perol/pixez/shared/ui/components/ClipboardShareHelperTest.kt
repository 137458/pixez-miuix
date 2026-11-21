package com.perol.pixez.shared.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 高-6b：复制/分享失败 Toast 不得把原始异常消息拼接进用户可见文案。
 * 异常细节仅留在返回的 Result 中供调用方记录日志。
 */
class ClipboardShareHelperTest {

    @Test
    fun `同步失败 Toast 文案为 failurePrefix 且不包含原始异常消息`() {
        var toast: ToastData? = null
        val result = runClipboardShare(
            success = ToastData("ok", ToastType.Success),
            failurePrefix = "复制加载失败",
            onToast = { toast = it },
        ) { throw IllegalStateException("secret-internal-message") }

        assertTrue(result.isFailure, "动作失败时 Result 必须保留失败状态供调用方记日志")
        assertEquals("复制加载失败", toast?.message)
        assertFalse(toast?.message?.contains("secret-internal-message") == true, "失败 Toast 不得透出异常消息")
        assertEquals(ToastType.Error, toast?.type)
    }

    @Test
    fun `挂起失败 Toast 文案为 failurePrefix 且不包含原始异常消息`() = runBlocking {
        var toast: ToastData? = null
        val result = suspendRunClipboardShare(
            success = ToastData("ok", ToastType.Success),
            failurePrefix = "分享失败",
            onToast = { toast = it },
        ) { throw RuntimeException("disk-full-detail") }

        assertTrue(result.isFailure)
        assertEquals("分享失败", toast?.message)
        assertFalse(toast?.message?.contains("disk-full-detail") == true, "挂起失败 Toast 同样不得透出异常消息")
        Unit
    }

    @Test
    fun `成功路径恰好触发一次成功 Toast 且保留动作结果`() {
        var toast: ToastData? = null
        var calls = 0
        var captured = 0
        val result = runClipboardShare(
            success = ToastData("copied", ToastType.Success),
            failurePrefix = "复制加载失败",
            onToast = { toast = it; calls++ },
        ) { captured = 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, captured, "动作副作用必须真实执行")
        assertEquals(1, calls, "结果回调无论成功失败必须恰好触发一次")
        assertEquals("copied", toast?.message)
        assertEquals(ToastType.Success, toast?.type)
    }

    @Test
    fun `取消异常原样抛出不并入失败 Toast`() {
        var toast: ToastData? = null
        val thrown = try {
            runClipboardShare(
                success = ToastData("ok", ToastType.Success),
                failurePrefix = "复制加载失败",
                onToast = { toast = it },
            ) { throw CancellationException("scope cancelled") }
            null
        } catch (e: CancellationException) {
            e
        }

        assertTrue(thrown != null, "CancellationException 必须原样抛出")
        assertEquals(null, toast, "取消路径不得触发失败 Toast")
    }
}
