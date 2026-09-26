package com.perol.pixez.shared.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.AsyncImagePainter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 验证加载中间状态的占位画家替换规则：
 * 携带 null 画家的 Loading/Error 会让整个节点什么都不绘制（表现为详情页灰底），
 * 必须由本替换函数兜底；Empty 是请求启动前的初始态，仅在防御性场景流经替换函数。
 */
class PixivAsyncImagePlaceholderTest {

    private class FakePainter : Painter() {
        override val intrinsicSize: Size
            get() = Size.Unspecified

        override fun DrawScope.onDraw() {}
    }

    @Test
    fun emptyStateFallsBackToLoadingWithPlaceholder() {
        val placeholder = FakePainter()

        val result = substitutePixivImagePlaceholder(AsyncImagePainter.State.Empty, placeholder)

        assertTrue(result is AsyncImagePainter.State.Loading, "Empty 应映射为 Loading 以进入绘制树")
        assertSame(placeholder, result.painter, "Loading 态必须携带占位画家")
    }

    @Test
    fun loadingWithNullPainterGetsPlaceholder() {
        val placeholder = FakePainter()

        val result = substitutePixivImagePlaceholder(AsyncImagePainter.State.Loading(null), placeholder)

        assertTrue(result is AsyncImagePainter.State.Loading)
        assertSame(placeholder, result.painter)
    }

    @Test
    fun loadingWithExistingPainterIsUntouched() {
        val existing = FakePainter()
        val placeholder = FakePainter()
        val state = AsyncImagePainter.State.Loading(existing)

        val result = substitutePixivImagePlaceholder(state, placeholder)

        assertSame(state, result, "已有画家的 Loading 不应被占位画家覆盖")
        assertSame(existing, result.painter)
    }

    @Test
    fun nullPlaceholderKeepsStateUntouched() {
        val state = AsyncImagePainter.State.Empty

        val result = substitutePixivImagePlaceholder(state, placeholder = null)

        assertSame(state, result)
    }

    @Test
    fun emptyWithoutPlaceholderYieldsNullPainter() {
        val result = substitutePixivImagePlaceholder(AsyncImagePainter.State.Empty, placeholder = null)

        assertFalse(
            result is AsyncImagePainter.State.Loading && result.painter != null,
            "无占位画家时不应伪造携带画家的状态",
        )
    }
}
