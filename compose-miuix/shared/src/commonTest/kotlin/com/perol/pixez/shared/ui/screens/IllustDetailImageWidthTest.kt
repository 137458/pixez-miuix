package com.perol.pixez.shared.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class IllustDetailImageWidthTest {

    private fun resolve(
        containerWidthPx: Float = 1200f,
        containerHeightPx: Float = 800f,
        aspectRatio: Float? = 0.7f,
        isWideScreen: Boolean = true,
        contentMaxWidthPx: Float = 760f,
        horizontalPaddingPx: Float = 12f,
        maxHeightFraction: Float = 0.8f,
    ): Float = resolveIllustDetailImageWidthPx(
        containerWidthPx = containerWidthPx,
        containerHeightPx = containerHeightPx,
        illustAspectRatio = aspectRatio,
        isWideScreen = isWideScreen,
        contentMaxWidthPx = contentMaxWidthPx,
        imageHorizontalPaddingPx = horizontalPaddingPx,
        maxHeightFraction = maxHeightFraction,
    )

    @Test
    fun testNarrowScreenKeepsFullBleedContainerWidth() {
        assertEquals(420f, resolve(containerWidthPx = 420f, isWideScreen = false))
    }

    @Test
    fun testWideScreenClampsLandscapeImageToContentColumn() {
        // 列宽 = min(1200, 760) - 24 = 736；横图 1.5 在列宽下高 490 < 视口 640，不受高度限制。
        assertEquals(736f, resolve(aspectRatio = 1.5f))
    }

    @Test
    fun testWideScreenCapsPortraitImageHeight() {
        // 列宽 736，竖图 0.7 全列高 1051 > 视口上限 800*0.8=640 → 宽度收缩到 640*0.7 = 448。
        assertEquals(448f, resolve(aspectRatio = 0.7f))
    }

    @Test
    fun testWideScreenSquareImageWithinHeightLimitKeepsColumnWidth() {
        // 方图列宽下高 736 > 640 → 收缩到 640。
        assertEquals(640f, resolve(aspectRatio = 1f))
    }

    @Test
    fun testUnknownAspectFallsBackToColumnWidth() {
        assertEquals(736f, resolve(aspectRatio = null))
    }

    @Test
    fun testContainerHeightNotMeasuredYetFallsBackToColumnWidth() {
        assertEquals(736f, resolve(containerHeightPx = 0f, aspectRatio = 0.5f))
    }

    @Test
    fun testNarrowContainerClampsColumnToContainerWidth() {
        // 宽屏判定但容器本身只有 600：列宽 = 600 - 24 = 576；竖图 0.7 全列高 823 > 640 → 640*0.7 = 448。
        assertEquals(448f, resolve(containerWidthPx = 600f))
    }
}
