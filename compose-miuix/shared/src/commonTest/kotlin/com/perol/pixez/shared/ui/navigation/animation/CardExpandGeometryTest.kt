package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [resolveCardExpandGeometry] 的几何换算验证。
 *
 * 该函数决定「卡片展开」转场收缩态下顶层页面的缩放系数与缩放锚点，
 * 一旦换算错误就会出现页面从屏幕外飞入、尺寸跳变或停在错误位置等明显缺陷，
 * 因此需要覆盖常规卡片、被容器裁剪的卡片与非法容器尺寸三类输入。
 */
class CardExpandGeometryTest {

    private val container = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)

    @Test
    fun `容器左上角的半宽半高卡片得到半屏缩放与零平移`() {
        val card = Rect(left = 0f, top = 0f, right = 500f, bottom = 1000f)

        val geometry = resolveCardExpandGeometry(card, container)

        assertClose(0.5f, geometry?.scaleX)
        assertClose(0.5f, geometry?.scaleY)
        assertClose(0f, geometry?.transX)
        assertClose(0f, geometry?.transY)
    }

    @Test
    fun `偏置卡片按相对偏移换算出正确的平移起点`() {
        // 卡片位于容器右下区域：左 400、上 1200，尺寸 200x400。
        val card = Rect(left = 400f, top = 1200f, right = 600f, bottom = 1600f)

        val geometry = resolveCardExpandGeometry(card, container)

        assertClose(0.2f, geometry?.scaleX)
        assertClose(0.2f, geometry?.scaleY)
        assertClose(400f, geometry?.transX)
        assertClose(1200f, geometry?.transY)
    }

    @Test
    fun `容器带偏移时按容器左上角换算相对几何`() {
        // 容器整体右移 100、下移 50（例如宽屏下左侧有 NavigationRail）。
        val offsetContainer = Rect(left = 100f, top = 50f, right = 1100f, bottom = 2050f)
        val card = Rect(left = 300f, top = 250f, right = 700f, bottom = 850f)

        val geometry = resolveCardExpandGeometry(card, offsetContainer)

        assertClose(0.4f, geometry?.scaleX)
        assertClose(0.3f, geometry?.scaleY)
        assertClose(200f, geometry?.transX)
        assertClose(200f, geometry?.transY)
    }

    @Test
    fun `卡片部分超出容器上边界时平移允许为负`() {
        val card = Rect(left = 0f, top = -200f, right = 400f, bottom = 200f)

        val geometry = resolveCardExpandGeometry(card, container)

        assertClose(0.4f, geometry?.scaleX)
        assertClose(0.2f, geometry?.scaleY)
        assertClose(0f, geometry?.transX)
        assertClose(-200f, geometry?.transY)
    }

    @Test
    fun `卡片与容器等大时缩放为 1 且零平移`() {
        val geometry = resolveCardExpandGeometry(container, container)

        assertClose(1f, geometry?.scaleX)
        assertClose(1f, geometry?.scaleY)
        assertClose(0f, geometry?.transX)
        assertClose(0f, geometry?.transY)
    }

    @Test
    fun `退化卡片尺寸被抬升到缩放下限避免页面不可见`() {
        val card = Rect(left = 0f, top = 0f, right = 0f, bottom = 0f)

        val geometry = resolveCardExpandGeometry(card, container)

        assertClose(0.05f, geometry?.scaleX)
        assertClose(0.05f, geometry?.scaleY)
        assertClose(0f, geometry?.transX)
        assertClose(0f, geometry?.transY)
    }

    @Test
    fun `超宽卡片缩放被钳制为 1 不放大`() {
        val card = Rect(left = -500f, top = -1000f, right = 1500f, bottom = 4000f)

        val geometry = resolveCardExpandGeometry(card, container)

        assertClose(1f, geometry?.scaleX)
        assertClose(1f, geometry?.scaleY)
        assertClose(-500f, geometry?.transX)
        assertClose(-1000f, geometry?.transY)
    }

    @Test
    fun `容器尺寸为零时不产生几何结果`() {
        val degenerate = Rect(left = 0f, top = 0f, right = 0f, bottom = 0f)

        assertNull(resolveCardExpandGeometry(container, degenerate))
    }

    /**
     * 归一化几何由浮点除法得出，按 [EPSILON] 容差比较，避免 0.70000005 这类
     * 表示误差导致的假失败；容差远小于任何肉眼可辨的动画偏差。
     */
    private fun assertClose(expected: Float, actual: Float?, epsilon: Float = EPSILON) {
        val value = requireNotNull(actual) { "期望 $expected，但几何结果为 null" }
        assertEquals(expected, value, epsilon)
    }

    private companion object {
        const val EPSILON = 1e-5f
    }
}
