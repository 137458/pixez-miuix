package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [resolveCardExpandTransform] 与底层纵深缩放换算的几何验证。
 *
 * 该函数决定「卡片展开」转场收缩态下顶层页面的缩放系数、平移与圆角补偿，
 * 一旦换算错误就会出现页面从屏幕外飞入、尺寸跳变或停在错误位置等明显缺陷，
 * 因此需要覆盖常规卡片、竖图卡片、被容器裁剪的卡片与非法容器尺寸等输入。
 *
 * 除逐项换算外，此处还锁死两条只能在真机上肉眼观察、却必须成立的合成不变量：
 * 1. 收回终点（expansion = 0）顶层可见窗口与卡片静止态矩形像素级重合；
 * 2. 展开全程顶层可见窗口始终覆盖列表源卡片的实时矩形，底层源卡片才允许被隐藏而不露空洞。
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
    fun `竖图卡片在 scaleY 大于 scaleX 时通过双向视口裁切使退出结束窗口严格重合卡片四边与左上角`() {
        // 容器 1000x2000，长竖图卡片 460x1100 -> scaleX = 0.46, scaleY = 0.55 (scaleY > scaleX)
        val tallCard = Rect(left = 30f, top = 200f, right = 490f, bottom = 1300f)

        val state = assertNotNull(
            resolveCardExpandTransform(
                expansion = 0f,
                sourceBounds = tallCard,
                containerBounds = container,
            ),
        )
        val window = windowRect(state, container)

        // 退出结束位置（expansion = 0）以 max(scaleX, scaleY) = 0.55 等比缩放，并由 visibleWidthFraction 裁切宽度，
        // 保证可见窗口四边与卡片 (30, 200, 490, 1300) 100% 像素级重合。
        assertClose(0.55f, state.uniformScale)
        assertClose(30f, state.transX)
        assertClose(200f, state.transY)
        assertClose(tallCard.width, window.width, epsilon = RECT_EPSILON)
        assertClose(tallCard.height, window.height, epsilon = RECT_EPSILON)
    }

    @Test
    fun `底层列表页面在卡片展开与收回全程保持铺满不缩放不裁切以根除四周白边`() {
        val card = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        val idleBackdrop = resolveBackdropLayerState(
            expansion = 0f,
            sourceBounds = card,
            containerBounds = container,
            containerCornerRadiusDp = 0f,
        )
        assertClose(1.0f, idleBackdrop.scale)
        assertClose(0f, idleBackdrop.localCornerRadiusDp)

        val expandedBackdrop = resolveBackdropLayerState(
            expansion = 1f,
            sourceBounds = card,
            containerBounds = container,
            containerCornerRadiusDp = 32f,
        )
        assertClose(1.0f, expandedBackdrop.scale)
        assertClose(0f, expandedBackdrop.localCornerRadiusDp)
    }

    @Test
    fun `底层页面微缩放锚点对齐源卡片中心在容器内的归一化位置`() {
        val card = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        // 卡片中心为 (300, 600)，容器为 1000x2000 -> 归一化锚点应为 (0.3, 0.3)
        val origin = resolveBackdropTransformOrigin(card, container)

        assertClose(0.3f, origin.pivotFractionX)
        assertClose(0.3f, origin.pivotFractionY)
    }

    @Test
    fun `收回终点顶层可见窗口与卡片矩形像素级重合`() {
        landingCases.forEach { case ->
            val state = assertNotNull(
                resolveCardExpandTransform(
                    expansion = 0f,
                    sourceBounds = case.card,
                    containerBounds = case.container,
                ),
                "${case.name} 在 expansion = 0 必须能解析出可用的变换状态",
            )
            val window = windowRect(state, case.container)

            assertClose(case.card.left, window.left, epsilon = RECT_EPSILON, name = case.name)
            assertClose(case.card.top, window.top, epsilon = RECT_EPSILON, name = case.name)
            assertClose(case.card.right, window.right, epsilon = RECT_EPSILON, name = case.name)
            assertClose(case.card.bottom, window.bottom, epsilon = RECT_EPSILON, name = case.name)
        }
    }

    @Test
    fun `展开全程顶层可见窗口始终覆盖列表源卡片实时矩形`() {
        // 底层源卡片在顶层展开接管后会被隐藏，只有窗口全程覆盖实时卡片，隐藏才不产生空洞。
        landingCases.forEach { case ->
            for (step in 0..20) {
                val expansion = step / 20f
                val state = assertNotNull(
                    resolveCardExpandTransform(
                        expansion = expansion,
                        sourceBounds = case.card,
                        containerBounds = case.container,
                    ),
                )
                val window = windowRect(state, case.container)
                val liveCard = liveCardRect(case.card, expansion)

                assertTrue(
                    window.left <= liveCard.left + RECT_EPSILON &&
                        window.top <= liveCard.top + RECT_EPSILON &&
                        window.right >= liveCard.right - RECT_EPSILON &&
                        window.bottom >= liveCard.bottom - RECT_EPSILON,
                    "${case.name} 在 expansion = $expansion 时窗口 $window 未覆盖实时卡片 $liveCard",
                )
            }
        }
    }

    @Test
    fun `横屏容器下竖图与方图卡片均可触发卡片转场且收回终点与卡片四边像素级重合`() {
        val landscapeContainer = Rect(left = 0f, top = 0f, right = 2400f, bottom = 1080f)
        val card = Rect(left = 300f, top = 220f, right = 600f, bottom = 790f)

        assertTrue(isCardExpandLandable(card, landscapeContainer))

        val state = assertNotNull(
            resolveCardExpandTransform(expansion = 0f, sourceBounds = card, containerBounds = landscapeContainer),
        )
        val window = windowRect(state, landscapeContainer)

        assertClose(card.left, window.left, epsilon = RECT_EPSILON)
        assertClose(card.top, window.top, epsilon = RECT_EPSILON)
        assertClose(card.width, window.width, epsilon = RECT_EPSILON)
        assertClose(card.height, window.height, epsilon = RECT_EPSILON)
    }

    @Test
    fun `方形容器与超长竖图卡片均判定为可落点并精确覆盖卡片全高`() {
        val squareContainer = Rect(left = 0f, top = 0f, right = 1000f, bottom = 1000f)
        val card = Rect(left = 100f, top = 100f, right = 500f, bottom = 850f)

        assertTrue(isCardExpandLandable(card, squareContainer))
        val state = assertNotNull(
            resolveCardExpandTransform(expansion = 0f, sourceBounds = card, containerBounds = squareContainer),
        )
        val window = windowRect(state, squareContainer)
        assertClose(card.width, window.width, epsilon = RECT_EPSILON)
        assertClose(card.height, window.height, epsilon = RECT_EPSILON)
    }

    @Test
    fun `横图卡片在竖屏容器内判定为可落点并精确落位`() {
        val portraitContainer = Rect(left = 100f, top = 50f, right = 1100f, bottom = 2050f)
        val card = Rect(left = 300f, top = 250f, right = 700f, bottom = 850f)

        assertTrue(isCardExpandLandable(card, portraitContainer))
    }

    @Test
    fun `容器或卡片尺寸非法时判定为不可落点`() {
        val card = Rect(left = 100f, top = 100f, right = 500f, bottom = 600f)

        assertFalse(isCardExpandLandable(card, Rect.Zero))
        assertFalse(isCardExpandLandable(card, Rect(left = 0f, top = 0f, right = 1080f, bottom = 0f)))
        assertFalse(isCardExpandLandable(Rect.Zero, container))
        assertFalse(isCardExpandLandable(card.copy(right = card.left), container))
    }

    /**
     * 顶层可见窗口：裁切形状按 [CardExpandTransformState.visibleWidthFraction] 裁宽、
     * 按 [CardExpandTransformState.visibleHeightFraction] 裁高，
     * 再经 `graphicsLayer` 以左上角为原点等比缩放并按 (transX, transY) 平移所得。
     */
    private fun windowRect(state: CardExpandTransformState, containerBounds: Rect): Rect {
        val left = containerBounds.left + state.transX
        val top = containerBounds.top + state.transY
        return Rect(
            left = left,
            top = top,
            right = left + state.uniformScale * state.visibleWidthFraction * containerBounds.width,
            bottom = top + state.uniformScale * state.visibleHeightFraction * containerBounds.height,
        )
    }

    /** 列表源卡片在给定展开度下的实时矩形：围绕自身中心按 [BACKDROP_MIN_SCALE] 随展开度缩放。 */
    private fun liveCardRect(card: Rect, expansion: Float): Rect {
        val scale = 1f + (BACKDROP_MIN_SCALE - 1f) * expansion
        val halfWidth = card.width * scale * 0.5f
        val halfHeight = card.height * scale * 0.5f
        return Rect(
            left = card.center.x - halfWidth,
            top = card.center.y - halfHeight,
            right = card.center.x + halfWidth,
            bottom = card.center.y + halfHeight,
        )
    }

    /**
     * 归一化几何由浮点除法得出，按 [EPSILON] 容差比较，避免 0.70000005 这类
     * 表示误差导致的假失败；容差远小于任何肉眼可辨的动画偏差。
     */
    private fun assertClose(expected: Float, actual: Float?, epsilon: Float = EPSILON, name: String = "") {
        val value = requireNotNull(actual) { "期望 $expected，但几何结果为 null" }
        assertEquals(expected, value, epsilon, "$name 期望 $expected，实际 $value")
    }

    private companion object {
        const val EPSILON = 1e-5f

        /**
         * 矩形换算的容差：测试侧按换算结果反推窗口尺寸会与实现内部的中间量舍入路径不同，
         * 像素级量级上允许 1e-3px 的表示误差（远小于任何肉眼可辨的偏差）。
         */
        const val RECT_EPSILON = 1e-3f

        /** 竖屏容器（含窗口内偏移）与竖图 / 方图 / 横图三种卡片的落点组合。 */
        val landingCases = listOf(
            GeometryCase(
                name = "竖图卡片",
                container = Rect(left = 30f, top = 90f, right = 1110f, bottom = 2430f),
                card = Rect(left = 120f, top = 700f, right = 630f, bottom = 1578f),
            ),
            GeometryCase(
                name = "方图卡片",
                container = Rect(left = 30f, top = 90f, right = 1110f, bottom = 2430f),
                card = Rect(left = 600f, top = 1200f, right = 1080f, bottom = 1900f),
            ),
            GeometryCase(
                name = "横图卡片",
                container = Rect(left = 30f, top = 90f, right = 1110f, bottom = 2430f),
                card = Rect(left = 120f, top = 1800f, right = 630f, bottom = 2150f),
            ),
        )
    }
}

/** 一组用于落点校验的容器与卡片几何。 */
private data class GeometryCase(
    val name: String,
    val container: Rect,
    val card: Rect,
)
