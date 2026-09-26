package com.perol.pixez.shared.ui.components

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 图片请求看门狗两阶段策略的验证：
 *
 * 1. 阶段一（既有行为）：请求发出后迟迟收不到任何真实事件（引擎层挂起）→ 重建。
 * 2. 阶段二（本轮新增）：已收到 Loading 等事件但迟迟等不到终态（Success/非取消 Error）→
 *    说明底层管线在事件之后被静默取消（如 DeDupe 去重排队中的请求被上游取消），同样重建。
 *
 * 对应线上症状：详情页图片停留在灰底/占位、无报错、无回调，
 * 左右滑动触发页面重建后从内存缓存秒显。
 */
class PixivAsyncImageWatchdogPolicyTest {

    @Test
    fun `首事件超时且预算内时判定重建`() = runBlocking {
        val restart = awaitImageRequestProgress(
            waitForFirstEvent = { false },
            waitForTerminalEvent = { true },
            restartCount = 0,
            maxRestarts = REQUEST_STALL_MAX_RESTARTS,
        )

        assertTrue(restart, "引擎层完全无事件时必须重建请求")
    }

    @Test
    fun `首事件超时且预算耗尽时放弃重建`() = runBlocking {
        val restart = awaitImageRequestProgress(
            waitForFirstEvent = { false },
            waitForTerminalEvent = { true },
            restartCount = REQUEST_STALL_MAX_RESTARTS,
            maxRestarts = REQUEST_STALL_MAX_RESTARTS,
        )

        assertFalse(restart, "重建预算耗尽后应停止干预，避免无限重启循环")
    }

    @Test
    fun `收到首事件且终态按时到达则不重建`() = runBlocking {
        val restart = awaitImageRequestProgress(
            waitForFirstEvent = { true },
            waitForTerminalEvent = { true },
            restartCount = 0,
            maxRestarts = REQUEST_STALL_MAX_RESTARTS,
        )

        assertFalse(restart, "正常完成（含失败终态）的请求不应被看门狗打扰")
    }

    @Test
    fun `收到首事件后终态停滞超时则判定重建`() = runBlocking {
        val restart = awaitImageRequestProgress(
            waitForFirstEvent = { true },
            waitForTerminalEvent = { false },
            restartCount = 0,
            maxRestarts = REQUEST_STALL_MAX_RESTARTS,
        )

        assertTrue(restart, "Loading 之后被静默取消的请求（无终态事件）必须由看门狗兜底重建")
    }

    @Test
    fun `终态停滞但预算耗尽时放弃重建`() = runBlocking {
        val restart = awaitImageRequestProgress(
            waitForFirstEvent = { true },
            waitForTerminalEvent = { false },
            restartCount = REQUEST_STALL_MAX_RESTARTS,
            maxRestarts = REQUEST_STALL_MAX_RESTARTS,
        )

        assertFalse(restart, "终态停滞但预算耗尽时应停止干预")
    }
}
