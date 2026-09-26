package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [resolveCardExpandTransform] 与底层纵深缩放换算的几何验证。
 *
 * 该函数决定「卡片展开」转场收缩态下顶层页面的缩放系数、平移与圆角补偿，
 * 一旦换算错误就会出现页面从屏幕外飞入、尺寸跳变或停在错误位置等明显缺陷，
 * 因此需要覆盖常规卡片、竖图卡片、被容器裁剪的卡片与非法容器尺寸等输入。
 */
class CardExpandGeometryTest {

    private val container = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)

    @Test
    fun `偏置卡片按相对偏移换算出正确的平移起点`() {
        // 卡片位于容器右下区域：左 400、上 1200，尺寸 200x400。
        val card = Rect(left = 400f, top = 1200f, right = 600f, bottom = 1600f)

        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
        )

        // expansion = 0 时 backdropScale = 1，uniformScale = 卡片宽/容器宽 = 0.2，左上角严格对齐卡片。
        assertClose(0.2f, state?.uniformScale)
        assertClose(400f, state?.transX)
        assertClose(1200f, state?.transY)
    }

    @Test
    fun `容器带偏移时按容器左上角换算相对几何`() {
        // 容器整体右移 100、下移 50（例如宽屏下左侧有 NavigationRail）。
        val offsetContainer = Rect(left = 100f, top = 50f, right = 1100f, bottom = 2050f)
        val card = Rect(left = 300f, top = 250f, right = 700f, bottom = 850f)

        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = offsetContainer,
        )

        assertClose(0.4f, state?.uniformScale)
        assertClose(200f, state?.transX)
        assertClose(200f, state?.transY)
    }

    @Test
    fun `卡片部分超出容器上边界时平移允许为负`() {
        val card = Rect(left = 0f, top = -200f, right = 400f, bottom = 200f)

        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
        )

        assertClose(0.4f, state?.uniformScale)
        assertClose(0f, state?.transX)
        assertClose(-200f, state?.transY)
    }

    @Test
    fun `卡片与容器等大时收缩态即铺满`() {
        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = container,
            containerBounds = container,
        )

        assertClose(1f, state?.uniformScale)
        assertClose(0f, state?.transX)
        assertClose(0f, state?.transY)
    }

    @Test
    fun `退化卡片尺寸被抬升到缩放下限避免页面不可见`() {
        val card = Rect(left = 0f, top = 0f, right = 0f, bottom = 0f)

        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
        )

        assertClose(0.05f, state?.uniformScale)
        assertClose(0f, state?.transX)
        assertClose(0f, state?.transY)
    }

    @Test
    fun `超宽卡片缩放被钳制为 1 不放大`() {
        val card = Rect(left = -500f, top = -1000f, right = 1500f, bottom = 4000f)

        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
        )

        assertClose(1f, state?.uniformScale)
        assertClose(-500f, state?.transX)
        assertClose(-1000f, state?.transY)
    }

    @Test
    fun `容器尺寸为零时不产生变换结果`() {
        val degenerate = Rect(left = 0f, top = 0f, right = 0f, bottom = 0f)

        assertNull(resolveCardExpandTransform(expansion = 0f, sourceBounds = container, containerBounds = degenerate))
    }

    @Test
    fun `收缩态本地圆角按缩放比例逆向补偿以保证屏幕物理圆角等于卡片圆角`() {
        // 卡片宽度为容器的 0.25 倍（例如平板 4 列瀑布流），卡片圆角 16dp，屏幕圆角 32dp。
        val card = Rect(left = 100f, top = 200f, right = 350f, bottom = 500f)
        val startState = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
            cardCornerRadiusDp = 16f,
            containerCornerRadiusDp = 32f,
        )
        val endState = resolveCardExpandTransform(
            expansion = 1f,
            sourceBounds = card,
            containerBounds = container,
            cardCornerRadiusDp = 16f,
            containerCornerRadiusDp = 32f,
        )

        // progress = 0 时，uniformScale = 0.25，本地圆角必须为 16 / 0.25 = 64dp，缩放后屏幕视觉圆角才精确等于 16dp。
        assertClose(0.25f, startState?.uniformScale)
        assertClose(64f, startState?.localCornerRadiusDp)
        // progress = 1 时，uniformScale = 1.0，本地圆角等于屏幕圆角 32dp。
        assertClose(1f, endState?.uniformScale)
        assertClose(32f, endState?.localCornerRadiusDp)
    }

    @Test
    fun `竖图卡片在 scaleY 大于 scaleX 时退出结束位置严格对齐卡片宽度与左上角避免水平放大跳变`() {
        // 常见竖屏或宽屏场景：容器 1000x2000，长竖图卡片 460x1100 -> scaleX = 0.46, scaleY = 0.55 (scaleY > scaleX)
        val tallCard = Rect(left = 30f, top = 200f, right = 490f, bottom = 1300f)

        val state = resolveCardExpandTransform(
            expansion = 0f,
            sourceBounds = tallCard,
            containerBounds = container,
        )

        // 退出结束位置（expansion = 0）必须严格以 scaleX (0.46) 缩放，保证详情页顶部图片宽度 100% 等于卡片宽度 460px，
        // 左上角严格落在 (30, 200)，绝不能放大到 0.55 并向左偏移。
        assertClose(0.46f, state?.uniformScale)
        assertClose(30f, state?.transX)
        assertClose(200f, state?.transY)
        assertClose(1.0f, state?.visibleHeightFraction)
    }

    @Test
    fun `底层列表页面在展开缩小后具备经过缩放逆补偿的圆角以消除四边直角`() {
        val card = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        // 未展开（expansion = 0）时列表铺满屏幕不缩小，本地圆角为 0
        val idleBackdrop = resolveBackdropLayerState(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
            containerCornerRadiusDp = 0f,
        )
        assertClose(1.0f, idleBackdrop.scale)
        assertClose(0f, idleBackdrop.localCornerRadiusDp)

        // 完全展开（expansion = 1）时列表缩小到 0.96，即使设备未上报物理圆角（0dp），
        // 也必须启用兜底圆角（28dp）并除以 0.96 逆向补偿（28 / 0.96 = 29.166668dp），防止露出四边直角。
        val shrunkBackdrop = resolveBackdropLayerState(
            expansion = 1f,
            sourceBounds = card,
            containerBounds = container,
            containerCornerRadiusDp = 0f,
        )
        assertClose(0.96f, shrunkBackdrop.scale)
        assertClose(28f / 0.96f, shrunkBackdrop.localCornerRadiusDp, epsilon = 1e-4f)
    }

    @Test
    fun `底层页面微缩放锚点对齐源卡片中心在容器内的归一化位置`() {
        val card = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        // 卡片中心为 (300, 600)，容器为 1000x2000 -> 归一化锚点应为 (0.3, 0.3)
        val origin = resolveBackdropTransformOrigin(card, container)

        assertClose(0.3f, origin.pivotFractionX)
        assertClose(0.3f, origin.pivotFractionY)
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
