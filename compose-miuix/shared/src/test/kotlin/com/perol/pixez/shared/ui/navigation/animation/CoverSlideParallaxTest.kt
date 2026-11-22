package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 底层页视差位移开关的行为守护。
 *
 * 背景：ColorOS 皮肤下底层页挂着窗口级 Backdrop 采样源与瀑布流大图，转场期间整页位移会让
 * 每帧把整页重新合成进离屏层，实测「作品列表 -> 一级设置页」稳态帧时长从 20ms 涨到 30ms、
 * 首帧从 100ms 涨到 145ms；关掉位移后回到与「卡片 -> 详情页」同级的 19.8ms。
 */
class CoverSlideParallaxTest {

    @Test
    fun `视差比例为 0 时底层列表不做位移登记`() {
        val registry = SharedBoundsRegistry()
        registry.updateSlideParallax(widthPx = 1000f, fraction = 0.5f, parallaxFraction = 0f)

        // 覆盖语义下底层列表静止，新登记的卡片坐标必须原样保留
        val rect = Rect(left = 24f, top = 300f, right = 524f, bottom = 950f)
        registry.put(illustId = 88L, rect = rect)
        assertEquals(rect, registry.get(88L), "关闭视差时底层卡片坐标不得被逆变换")
    }

    @Test
    fun `视差比例非零时底层列表按比例逆变换登记坐标`() {
        val registry = SharedBoundsRegistry()
        registry.updateSlideParallax(widthPx = 1000f, fraction = 0.5f, parallaxFraction = 0.25f)

        // 左侧位移 = -1000 * 0.25 * 0.5 = -125px，静止坐标需加回 125px
        registry.put(illustId = 88L, rect = Rect(left = 24f, top = 300f, right = 524f, bottom = 950f))
        assertEquals(149f, registry.get(88L)?.left, "视差侧滑期间登记坐标必须逆变换回静止态")
    }
}
