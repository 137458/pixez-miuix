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

        assertEquals(visibleCard, registry.getVisibleInContainer(1, container))
        assertNull(registry.getVisibleInContainer(2, container))
    }
}
