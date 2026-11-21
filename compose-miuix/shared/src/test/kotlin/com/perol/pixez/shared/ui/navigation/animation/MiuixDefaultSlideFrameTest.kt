package com.perol.pixez.shared.ui.navigation.animation

import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [resolveMiuixDefaultSlideFrame] 的方向换算验证。
 *
 * Miuix 默认转场（miuix-nav `NavTransitions.MiuixDefault`）要求：
 * - 进/出栈顶的页面全宽滑动，fraction 与栈顶位置的距离线性对应；
 * - 被覆盖/被露出的页面按覆盖进度做 25% 视差与淡出；
 * - 同一时刻顶层与底层帧互补（fraction 之和为 1），两层画面严格同相。
 *
 * Decompose 的 factor 区间与 [CardExpandFrameTest] 相同（四种方向各自带符号），
 * 换算一旦符号或方向搞反，就会出现页面反向滑出、两层错位等明显缺陷，
 * 因此这里逐一锁定四种方向的起点、终点与同相性。
 */
class MiuixDefaultSlideFrameTest {

    @Test
    fun `push 入场页面从完全滑出状态滑动到贴顶`() {
        val start = resolveMiuixDefaultSlideFrame(Direction.ENTER_FRONT, factor = 1f)
        val end = resolveMiuixDefaultSlideFrame(Direction.ENTER_FRONT, factor = 0f)

        assertClose(1f, start.fraction)
        assertClose(0f, end.fraction)
        assertTrue(start.isTopLayer)
    }

    @Test
    fun `push 退到后台的页面从贴顶走到完全被覆盖`() {
        val start = resolveMiuixDefaultSlideFrame(Direction.EXIT_BACK, factor = 0f)
        val end = resolveMiuixDefaultSlideFrame(Direction.EXIT_BACK, factor = -1f)

        assertClose(0f, start.fraction)
        assertClose(1f, end.fraction)
        assertTrue(!start.isTopLayer)
    }

    @Test
    fun `pop 离场的顶层页面从贴顶滑出到完全离屏`() {
        val start = resolveMiuixDefaultSlideFrame(Direction.EXIT_FRONT, factor = 0f)
        val end = resolveMiuixDefaultSlideFrame(Direction.EXIT_FRONT, factor = 1f)

        assertClose(0f, start.fraction)
        assertClose(1f, end.fraction)
        assertTrue(start.isTopLayer)
    }

    @Test
    fun `pop 重新回到前台的页面从完全被覆盖还原到贴顶`() {
        val start = resolveMiuixDefaultSlideFrame(Direction.ENTER_BACK, factor = -1f)
        val end = resolveMiuixDefaultSlideFrame(Direction.ENTER_BACK, factor = 0f)

        assertClose(1f, start.fraction)
        assertClose(0f, end.fraction)
        assertTrue(!start.isTopLayer)
    }

    @Test
    fun `同一时刻顶层与底层帧互补保证两层画面同相`() {
        // push：animationState 从 1 走到 0，入场层 factor = a，退场层 factor = a - 1。
        for (step in 0..10) {
            val a = 1f - step / 10f
            val entering = resolveMiuixDefaultSlideFrame(Direction.ENTER_FRONT, factor = a)
            val exiting = resolveMiuixDefaultSlideFrame(Direction.EXIT_BACK, factor = a - 1f)
            assertClose(1f, entering.fraction + exiting.fraction)
        }
        // pop：入场层 factor = -a，离场层 factor = 1 - a。
        for (step in 0..10) {
            val a = 1f - step / 10f
            val entering = resolveMiuixDefaultSlideFrame(Direction.ENTER_BACK, factor = -a)
            val exiting = resolveMiuixDefaultSlideFrame(Direction.EXIT_FRONT, factor = 1f - a)
            assertClose(1f, entering.fraction + exiting.fraction)
        }
    }

    @Test
    fun `fraction 被钳制在 0 到 1 之间避免溢出滑出容器`() {
        // factor 超出各自方向的理论区间时，fraction 必须钳制而不是把页面推到容器外。
        val belowZero = resolveMiuixDefaultSlideFrame(Direction.EXIT_BACK, factor = 1.4f)
        val aboveOne = resolveMiuixDefaultSlideFrame(Direction.ENTER_BACK, factor = -1.4f)

        assertClose(0f, belowZero.fraction)
        assertClose(1f, aboveOne.fraction)
    }

    @Test
    fun `四个方向的 fraction 端点保持一致避免出入口手感断层`() {
        val endpoints = listOf(
            Direction.ENTER_FRONT to (1f to 0f),
            Direction.EXIT_BACK to (0f to -1f),
            Direction.EXIT_FRONT to (0f to 1f),
            Direction.ENTER_BACK to (-1f to 0f),
        )

        endpoints.forEach { (direction, factors) ->
            val (factorAtStart, factorAtEnd) = factors
            val start = resolveMiuixDefaultSlideFrame(direction, factorAtStart).fraction
            val end = resolveMiuixDefaultSlideFrame(direction, factorAtEnd).fraction
            assertTrue(
                (start == 1f && end == 0f) || (start == 0f && end == 1f),
                "方向 $direction 的 fraction 端点应为 0 与 1 的组合，实际为 $start -> $end",
            )
        }
    }

    @Test
    fun `方向到层级归属与 isFront 判定一致`() {
        Direction.entries.forEach { direction ->
            val frame = resolveMiuixDefaultSlideFrame(direction, factor = 0f)
            assertEquals(direction.isFront, frame.isTopLayer, "方向 $direction 的层级归属错误")
        }
    }

    /**
     * 换算结果为浮点加减，按 [EPSILON] 容差比较，避免表示误差导致的假失败。
     */
    private fun assertClose(expected: Float, actual: Float, epsilon: Float = EPSILON) {
        assertEquals(expected, actual, epsilon)
    }

    private companion object {
        const val EPSILON = 1e-5f
    }
}
