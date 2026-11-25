package com.perol.pixez.shared.ui.navigation.animation

import android.os.Build
import androidx.compose.ui.unit.LayoutDirection
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.ui.navigation.Config
import com.perol.pixez.shared.ui.navigation.isModalRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationTransitionsPolishTest {

    @Test
    fun `resolveNavigationBlurBlendFrame 按明暗主题量化半径并仅在非零进度启用`() {
        assertEquals(
            NavigationBlurBlendFrame.None,
            resolveNavigationBlurBlendFrame(progress = 0f, isLightBackground = false, sdkInt = Build.VERSION_CODES.S),
        )
        assertEquals(
            NavigationBlurBlendFrame.None,
            resolveNavigationBlurBlendFrame(progress = 0.8f, isLightBackground = false, sdkInt = Build.VERSION_CODES.R),
        )
        assertEquals(
            NavigationBlurBlendFrame.None,
            resolveNavigationBlurBlendFrame(progress = 0.8f, enabled = false, sdkInt = Build.VERSION_CODES.S),
        )

        val dark = resolveNavigationBlurBlendFrame(progress = 0.5f, isLightBackground = false, sdkInt = Build.VERSION_CODES.S)
        assertEquals(28f, dark.radiusPx)
        assertEquals(0.5f, dark.alpha)
        assertEquals(0f, dark.separationTintAlpha)

        val light = resolveNavigationBlurBlendFrame(progress = 0.5f, isLightBackground = true, sdkInt = Build.VERSION_CODES.S)
        assertEquals(24f, light.radiusPx)
        assertEquals(0.5f, light.alpha)
        assertEquals(0.025f, light.separationTintAlpha, 1e-5f)
    }

    @Test
    fun `NavVelocityHandoffEasing 保持端点精确与单调有界并接续释放速度`() {
        val easing = NavVelocityHandoffEasing(
            distance = 0.5f,
            initialVelocity = 3f,
            durationMillis = 200,
        )
        assertEquals(0f, easing.transform(0f))
        assertEquals(1f, easing.transform(1f))
        var prev = 0f
        for (step in 1..20) {
            val current = easing.transform(step / 20f)
            assertTrue(current in 0f..1f, "Expected clamped value, got $current")
            assertTrue(current >= prev, "Expected monotonic increase: prev=$prev current=$current")
            prev = current
        }

        val fastEarly = easing.transform(0.1f)
        val neutralEarly = HyperOSDecelerateEasing.transform(0.1f)
        assertTrue(fastEarly >= 0.1f, "Fast handoff should preserve initial velocity: $fastEarly vs $neutralEarly")
    }

    @Test
    fun `resolveModalDismissOutcome 与 isModalRoute 规则一致`() {
        assertEquals(DragBackOutcome.COMMIT, resolveModalDismissOutcome(progress = 0.22f, velocityYPxPerSec = 0f))
        assertEquals(DragBackOutcome.CANCEL, resolveModalDismissOutcome(progress = 0.21f, velocityYPxPerSec = 0f))
        assertEquals(DragBackOutcome.COMMIT, resolveModalDismissOutcome(progress = 0.05f, velocityYPxPerSec = 1400f))
        assertEquals(DragBackOutcome.CANCEL, resolveModalDismissOutcome(progress = 0.05f, velocityYPxPerSec = 1399f))

        // 评论页已切为以评论卡片为锚点的卡片展开转场
        assertFalse(Config.Comments(illustId = 1L).isModalRoute())
        assertFalse(Config.Comments(illustId = 1L, isNovel = true).isModalRoute())
        assertFalse(Config.IllustDetail(1L).isModalRoute())
        assertFalse(Config.NovelViewer(1L).isModalRoute())
        assertFalse(
            Config.SpotlightDetail(
                SpotlightArticle(1L, "t", "t", "u", "a", "d"),
            ).isModalRoute(),
        )
    }

    @Test
    fun `resolveVelocityHandoffSpec 异号速度退回默认减速曲线且 RTL 拖拽进度与速度方向正确`() {
        val mismatched = resolveVelocityHandoffSpec(
            remainingDistance = -0.3f,
            initialVelocityPerSec = 1.5f,
            durationMillis = 200,
        )
        assertEquals(HyperOSDecelerateEasing, mismatched.easing)

        // RTL 下从右缘向左滑动产生正进度，且左甩负 velocityX 映射为正返回速度
        val rtlProgress = resolveDragBackProgress(
            downX = 1060f,
            currentX = 760f,
            containerWidthPx = 1000f,
            layoutDirection = LayoutDirection.Rtl,
        )
        assertEquals(0.3f, rtlProgress, 1e-4f)
        assertEquals(
            2.0f,
            resolveDragBackSignedVelocityFraction(
                velocityX = -2000f,
                containerWidthPx = 1000f,
                layoutDirection = LayoutDirection.Rtl,
            ),
            1e-4f,
        )
    }
}
