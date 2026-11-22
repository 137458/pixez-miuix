package com.perol.pixez.shared.ui.skin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 皮肤与转场动效语义的绑定关系守护。
 *
 * MIUIX 侧滑是「撑开」语义（底层页随进度左移并轻微淡出），ColorOS 是「覆盖」语义
 * （底层页原地不动、只叠暗色遮罩）。COUI 皮肤下视差位移会让整页每帧重绘，故必须关闭。
 */
class UiSkinCoverParallaxTest {

    @Test
    fun `ColorOS 皮肤关闭底层页视差`() {
        assertFalse(UiSkin.COLOROS.coverParallax, "ColorOS 转场为覆盖语义，底层页必须保持静止")
    }

    @Test
    fun `MIUIX 皮肤保留底层页视差`() {
        assertTrue(UiSkin.HYPEROS.coverParallax, "MIUIX 保留 25% 视差侧滑")
    }
}
