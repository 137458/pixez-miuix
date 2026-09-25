package com.perol.pixez.shared.ui.navigation.animation

import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [resolveCardExpandFrame] 的方向换算验证。
 *
 * Decompose 的 `StackAnimator` 对四种方向给出不同符号的 factor（见 `StackAnimator.kt` 文档）：
 * - `ENTER_FRONT`：1F -> 0F（push 时新顶层页面入场）
 * - `EXIT_FRONT`：0F -> 1F（pop 时原顶层页面离场）
 * - `ENTER_BACK`：-1F -> 0F（pop 时被覆盖的页面重新回到前台）
 * - `EXIT_BACK`：0F -> -1F（push 时原顶层页面退到后台）
 *
 * 换算必须让作品卡片的「展开度」始终从 0（收缩在卡片内）单调走到 1（铺满容器），
 * 一旦符号或方向搞反，就会出现详情页从整屏缩回卡片、返回时反向弹出等明显缺陷。
 * 因此这里逐一锁定四种方向的起点、终点与单调性。
 */
class CardExpandFrameTest {

    @Test
    fun `push 入场页面从卡片收缩态展开到铺满`() {
        val start = resolveCardExpandFrame(Direction.ENTER_FRONT, factor = 1f)
        val end = resolveCardExpandFrame(Direction.ENTER_FRONT, factor = 0f)

        assertClose(0f, start.expansion)
        assertClose(1f, end.expansion)
        assertTrue(start.isTopLayer)
    }

    @Test
    fun `push 退到后台的页面铺满状态保持不动并以遮罩描述纵深`() {
        val start = resolveCardExpandFrame(Direction.EXIT_BACK, factor = 0f)
        val end = resolveCardExpandFrame(Direction.EXIT_BACK, factor = -1f)

        assertClose(0f, start.expansion)
        assertClose(1f, end.expansion)
        assertTrue(!start.isTopLayer)
    }

    @Test
    fun `pop 离场的顶层页面从铺满收缩回卡片`() {
        val start = resolveCardExpandFrame(Direction.EXIT_FRONT, factor = 0f)
        val end = resolveCardExpandFrame(Direction.EXIT_FRONT, factor = 1f)

        assertClose(1f, start.expansion)
        assertClose(0f, end.expansion)
        assertTrue(start.isTopLayer)
    }

    @Test
    fun `pop 重新回到前台的页面按顶层展开度从铺满回到卡片收缩态`() {
        // pop 起手时离场的详情页铺满容器，该层无遮罩；pop 结束时详情页已收回卡片，遮罩最强。
        val start = resolveCardExpandFrame(Direction.ENTER_BACK, factor = -1f)
        val end = resolveCardExpandFrame(Direction.ENTER_BACK, factor = 0f)

        assertClose(1f, start.expansion)
        assertClose(0f, end.expansion)
        assertTrue(!start.isTopLayer)
    }

    @Test
    fun `同一时刻两种方向算出的展开度一致避免两层页面错位`() {
        // push：animationState 从 1 走到 0，入场层 factor = a，退场层 factor = a - 1。
        for (step in 0..10) {
            val a = 1f - step / 10f
            val entering = resolveCardExpandFrame(Direction.ENTER_FRONT, factor = a)
            val exiting = resolveCardExpandFrame(Direction.EXIT_BACK, factor = a - 1f)
            assertClose(entering.expansion, exiting.expansion)
        }
        // pop：入场层 factor = -a，离场层 factor = 1 - a。
        for (step in 0..10) {
            val a = 1f - step / 10f
            val entering = resolveCardExpandFrame(Direction.ENTER_BACK, factor = -a)
            val exiting = resolveCardExpandFrame(Direction.EXIT_FRONT, factor = 1f - a)
            assertClose(entering.expansion, exiting.expansion)
        }
    }

    @Test
    fun `展开度被钳制在 0 到 1 之间避免负圆角`() {
        // factor 超出各自方向的理论区间时，展开度必须钳制而不是溢出。
        val belowZero = resolveCardExpandFrame(Direction.EXIT_BACK, factor = 1.4f)
        val aboveOne = resolveCardExpandFrame(Direction.ENTER_BACK, factor = -1.4f)

        assertClose(0f, belowZero.expansion)
        assertClose(1f, aboveOne.expansion)
    }

    @Test
    fun `预测性返回手势进度换算为顶层页面展开度`() {
        // 手势刚开始（progress = 0）时详情页铺满，拖到底（progress = 1）时收进卡片。
        assertClose(1f, predictiveBackCardExpandExpansion(progress = 0f))
        assertClose(0.4f, predictiveBackCardExpandExpansion(progress = 0.6f))
        assertClose(0f, predictiveBackCardExpandExpansion(progress = 1f))
        assertClose(1f, predictiveBackCardExpandExpansion(progress = -0.2f))
        assertClose(0f, predictiveBackCardExpandExpansion(progress = 1.3f))
    }

    @Test
    fun `四个方向的展开度端点保持一致避免出入口手感断层`() {
        val endpoints = listOf(
            Direction.ENTER_FRONT to (1f to 0f),
            Direction.EXIT_BACK to (0f to -1f),
            Direction.EXIT_FRONT to (0f to 1f),
            Direction.ENTER_BACK to (-1f to 0f),
        )

        endpoints.forEach { (direction, factors) ->
            val (factorAtStart, factorAtEnd) = factors
            val start = resolveCardExpandFrame(direction, factorAtStart).expansion
            val end = resolveCardExpandFrame(direction, factorAtEnd).expansion
            assertTrue(
                (start == 1f && end == 0f) || (start == 0f && end == 1f),
                "方向 $direction 的展开度端点应为 0 与 1 的组合，实际为 $start -> $end",
            )
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
