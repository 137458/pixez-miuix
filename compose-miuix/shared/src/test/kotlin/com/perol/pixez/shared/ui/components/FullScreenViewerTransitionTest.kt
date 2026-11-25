package com.perol.pixez.shared.ui.components

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FullScreenViewerTransitionTest {

    @Test
    fun `DetailImageBoundsTracker 过滤视口外卡片并返回可见页矩形`() {
        val tracker = DetailImageBoundsTracker()
        tracker.viewportBounds = Rect(0f, 0f, 1080f, 2400f)

        val page0 = Rect(0f, 120f, 1080f, 1200f)
        val offscreen = Rect(0f, -1500f, 1080f, -100f)
        tracker.updatePageBounds(0, page0)
        tracker.updatePageBounds(1, offscreen)

        assertEquals(page0, tracker.visibleBoundsFor(0))
        assertNull(tracker.visibleBoundsFor(1))

        tracker.removePageBounds(0)
        assertNull(tracker.visibleBoundsFor(0))
    }

    @Test
    fun `resolveViewerDismissOutcome 按纵向位移与释放速度判定提交或取消`() {
        assertEquals(
            ViewerDismissOutcome.COMMIT,
            resolveViewerDismissOutcome(dragOffsetY = 280f, velocityY = 0f, containerHeightPx = 2000f),
        )
        assertEquals(
            ViewerDismissOutcome.COMMIT,
            resolveViewerDismissOutcome(dragOffsetY = 60f, velocityY = 1150f, containerHeightPx = 2000f),
        )
        assertEquals(
            ViewerDismissOutcome.COMMIT,
            resolveViewerDismissOutcome(dragOffsetY = -450f, velocityY = 0f, containerHeightPx = 2000f),
        )
        assertEquals(
            ViewerDismissOutcome.CANCEL,
            resolveViewerDismissOutcome(dragOffsetY = 120f, velocityY = 200f, containerHeightPx = 2000f),
        )
    }

    @Test
    fun `resolveViewerOriginEndpoint 精确将居中大图映射到原图位置与缩放`() {
        val container = Rect(0f, 0f, 1000f, 2000f)
        val origin = Rect(100f, 200f, 900f, 1000f) // 800x800, center=(500,600)
        val endpoint = resolveViewerOriginEndpoint(
            originRect = origin,
            containerBounds = container,
            imageAspectRatio = 1f,
        )
        // 1:1 图片在 1000x2000 中 ContentScale.Fit 后宽度为 1000，缩放比为 800 / 1000 = 0.8
        assertEquals(0f, endpoint.translationX, 1e-3f)
        assertEquals(-400f, endpoint.translationY, 1e-3f)
        assertEquals(0.8f, endpoint.scale, 1e-3f)
        assertEquals(0f, endpoint.backgroundAlpha, 1e-3f)
    }

    @Test
    fun `lerpViewerTransform 线性插值起止态`() {
        val start = FullScreenViewerTransform(
            translationX = 100f,
            translationY = -200f,
            scale = 0.6f,
            cornerRadiusPx = 20f,
            backgroundAlpha = 0f,
            contentAlpha = 0.5f,
            controlsAlpha = 0f,
        )
        val end = FullScreenViewerTransform()
        val mid = lerpViewerTransform(start, end, 0.5f)
        assertEquals(50f, mid.translationX, 1e-3f)
        assertEquals(-100f, mid.translationY, 1e-3f)
        assertEquals(0.8f, mid.scale, 1e-3f)
        assertEquals(10f, mid.cornerRadiusPx, 1e-3f)
        assertEquals(0.5f, mid.backgroundAlpha, 1e-3f)
        assertEquals(0.75f, mid.contentAlpha, 1e-3f)
        assertEquals(0.5f, mid.controlsAlpha, 1e-3f)
    }
}
