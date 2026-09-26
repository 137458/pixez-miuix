package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [SharedBoundsRegistry] 的登记 / 读取 / 注销行为验证。
 *
 * 该登记表是卡片展开转场的几何来源，若残留陈旧矩形，
 * 详情页就会从「上一次点击的卡片」或已销毁卡片的旧位置错误地展开。
 */
class SharedBoundsRegistryTest {

    private val card = Rect(left = 10f, top = 20f, right = 110f, bottom = 220f)

    @Test
    fun `登记后可按作品 ID 取回矩形`() {
        val registry = SharedBoundsRegistry()

        registry.put(illustId = 42, rect = card)

        assertEquals(card, registry.get(42))
    }

    @Test
    fun `未登记的作品 ID 返回空`() {
        val registry = SharedBoundsRegistry()

        assertNull(registry.get(42))
    }

    @Test
    fun `重复登记以最后一次为准`() {
        val registry = SharedBoundsRegistry()
        val moved = Rect(left = 300f, top = 400f, right = 500f, bottom = 700f)

        registry.put(illustId = 42, rect = card)
        registry.put(illustId = 42, rect = moved)

        assertEquals(moved, registry.get(42))
    }

    @Test
    fun `登记空矩形等价于注销`() {
        val registry = SharedBoundsRegistry()
        registry.put(illustId = 42, rect = card)

        registry.put(illustId = 42, rect = null)

        assertNull(registry.get(42))
    }

    @Test
    fun `零尺寸矩形视为无效并不予登记`() {
        val registry = SharedBoundsRegistry()

        registry.put(illustId = 42, rect = Rect(left = 5f, top = 5f, right = 5f, bottom = 5f))

        assertNull(registry.get(42))
    }

    @Test
    fun `注销只影响目标作品不影响其他登记`() {
        val registry = SharedBoundsRegistry()
        registry.put(illustId = 1, rect = card)
        registry.put(illustId = 2, rect = card)

        registry.remove(1)

        assertNull(registry.get(1))
        assertEquals(card, registry.get(2))
    }

    @Test
    fun `清空移除全部登记`() {
        val registry = SharedBoundsRegistry()
        registry.put(illustId = 1, rect = card)
        registry.put(illustId = 2, rect = card)

        registry.clear()

        assertNull(registry.get(1))
        assertNull(registry.get(2))
    }

    @Test
    fun `详情页左右滑动切换作品后优先使用当前实际展示的作品 ID 匹配收回卡片`() {
        val registry = SharedBoundsRegistry()
        registry.activeDetailIllustId = 99

        assertEquals(99, registry.resolveEffectiveIllustId(routeIllustId = 42))
        registry.activeDetailIllustId = null
        assertEquals(42, registry.resolveEffectiveIllustId(routeIllustId = 42))
    }

    @Test
    fun `滚出容器可视范围超过阈值的卡片返回空以回退为侧滑动画`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        // 完全在容器内的卡片
        val visibleCard = Rect(left = 16f, top = 200f, right = 520f, bottom = 800f)
        // 绝大部分已滚出容器顶部的卡片（仅露出底部 20px，总高 600px，可见占比 < 5%）
        val mostlyScrolledOutCard = Rect(left = 16f, top = -580f, right = 520f, bottom = 20f)

        registry.put(illustId = 1, rect = visibleCard)
        registry.put(illustId = 2, rect = mostlyScrolledOutCard)

        assertEquals(visibleCard, registry.getVisibleInContainer(1, container)?.rect)
        assertNull(registry.getVisibleInContainer(2, container))
    }

    @Test
    fun `转场动画进行中禁止用被缩放的后台列表坐标覆盖卡片静止态真实坐标`() {
        val registry = SharedBoundsRegistry()
        val stationaryBounds = Rect(left = 24f, top = 300f, right = 524f, bottom = 950f)
        val shrunkBoundsFromBackdrop = Rect(left = 38f, top = 320f, right = 518f, bottom = 944f)

        registry.put(illustId = 42, rect = stationaryBounds)
        // 转场开始（expansion = 0.6f），底层列表被 0.96x 缩放触发 onGloballyPositioned
        registry.updateTransitionState(illustId = 42, expansion = 0.6f)
        registry.put(illustId = 42, rect = shrunkBoundsFromBackdrop)

        // 取出退出终点坐标时，必须保持未缩放的静止态真实坐标 stationaryBounds
        assertEquals(stationaryBounds, registry.get(42))

        // 转场结束后（expansion = 0f），允许正常列表滚动更新坐标
        registry.updateTransitionState(illustId = 42, expansion = 0f)
        val scrolledBounds = Rect(left = 24f, top = 180f, right = 524f, bottom = 830f)
        registry.put(illustId = 42, rect = scrolledBounds)
        assertEquals(scrolledBounds, registry.get(42))
    }

    @Test
    fun `登记时记录卡片圆角并随可视矩形一起返回`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)

        registry.put(illustId = 42, rect = card, cornerRadiusDp = 12f)

        val visible = registry.getVisibleInContainer(42, container)
        assertEquals(12f, visible?.cornerRadiusDp, "转场终点圆角必须取卡片自身圆角（如历史卡 12dp）")
        assertEquals(card, visible?.rect)
    }

    @Test
    fun `转场激活期首次登记的卡片按底层纵深缩放逆变换归一为静止态坐标`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        val sourceCard = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        // expansion = 0.5 → backdropScale = lerp(1, 0.96, 0.5) = 0.98，缩放锚点 = 源卡片中心 (300, 600)。
        registry.updateTransitionState(
            illustId = 42,
            expansion = 0.5f,
            sourceBounds = sourceCard,
            containerBounds = container,
        )

        // 转场中新进入组合的卡片上报的是被 0.98x 缩放污染的瞬时坐标：
        // q = pivot + (p - pivot) * 0.98，静止态 p = Rect(200, 500, 600, 900) → q = Rect(202, 502, 594, 894)。
        val pollutedFromBackdrop = Rect(left = 202f, top = 502f, right = 594f, bottom = 894f)
        registry.put(illustId = 7, rect = pollutedFromBackdrop, cornerRadiusDp = 12f)

        assertEquals(
            Rect(left = 200f, top = 500f, right = 600f, bottom = 900f),
            registry.get(7),
            "转场期首次登记的坐标必须归一为静止态真实坐标，否则退出动画终点跳变",
        )
    }

    @Test
    fun `转场结束后登记坐标不再做逆变换`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        val sourceCard = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        registry.updateTransitionState(illustId = 42, expansion = 0.5f, sourceBounds = sourceCard, containerBounds = container)
        registry.updateTransitionState(illustId = null, expansion = 0f)

        val stationary = Rect(left = 202f, top = 502f, right = 594f, bottom = 894f)
        registry.put(illustId = 7, rect = stationary)

        assertEquals(stationary, registry.get(7))
    }
}
