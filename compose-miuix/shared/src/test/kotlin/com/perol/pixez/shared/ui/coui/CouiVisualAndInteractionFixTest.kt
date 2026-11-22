package com.perol.pixez.shared.ui.coui

import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.perol.pixez.shared.ui.coui.core.BoundedObjectPool
import com.perol.pixez.shared.ui.coui.core.shouldCommitCouiBottomBarTap
import com.perol.pixez.shared.ui.coui.page.resolveCouiDetailTopBarBlurAlpha
import com.perol.pixez.shared.ui.navigation.animation.resolveStackLayerFactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CouiVisualAndInteractionFixTest {

    @Test
    fun boundedObjectPoolReusesReleasedInstancesAndGuardsCapacityAndDuplicates() {
        var created = 0
        val pool = BoundedObjectPool(maxSize = 2) {
            created += 1
            Any()
        }

        val first = pool.acquire()
        val second = pool.acquire()
        val third = pool.acquire()
        assertEquals(3, created)
        assertEquals(0, pool.availableCount)

        assertTrue(pool.release(first))
        assertEquals(1, pool.availableCount)
        // 同一实例重复归还不得入池两次，否则会被两个控件同时借出
        assertFalse(pool.release(first))
        assertEquals(1, pool.availableCount)

        assertTrue(pool.release(second))
        assertEquals(2, pool.availableCount)
        // 超出容量上限的归还直接丢弃
        assertFalse(pool.release(third))
        assertEquals(2, pool.availableCount)

        val reused1 = pool.acquire()
        val reused2 = pool.acquire()
        assertEquals(3, created)
        assertEquals(0, pool.availableCount)
        assertNotSame(reused1, reused2)
        assertTrue(reused1 === first || reused1 === second)
        assertTrue(reused2 === first || reused2 === second)

        val fourth = pool.acquire()
        assertEquals(4, created)
        assertNotSame(fourth, first)
        assertNotSame(fourth, second)
    }

    @Test
    fun boundedObjectPoolWithZeroCapacityAlwaysCreatesFreshInstances() {
        var created = 0
        val pool = BoundedObjectPool(maxSize = 0) {
            created += 1
            Any()
        }
        val a = pool.acquire()
        assertFalse(pool.release(a))
        assertEquals(0, pool.availableCount)
        val b = pool.acquire()
        assertNotSame(a, b)
        assertEquals(2, created)
    }

    @Test
    fun couiIllustDetailTopBarUsesScrollGatedGradientBlurAndGlareTopAppBarButtons() {
        val source = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        assertTrue(
            "CouiIllustDetailScreen must record content with rememberCouiBackdrop() and couiBackdrop(...)",
            source.contains("rememberCouiBackdrop(") && source.contains(".couiBackdrop("),
        )
        assertTrue(
            "CouiIllustDetailFloatingTopBar must use TopAppBarSurface with scroll-driven backgroundAlpha and resolveCouiDetailTopBarBlurAlpha",
            source.contains("TopAppBarSurface(") &&
                source.contains("backgroundAlpha = ") &&
                source.contains("resolveCouiDetailTopBarBlurAlpha("),
        )
        assertTrue(
            "CouiIllustDetailFloatingTopBar must use CouiTopBarIconButton and TopAppBarButtonGroup for glare buttons",
            source.contains("CouiTopBarIconButton(") && source.contains("TopAppBarButtonGroup"),
        )
        assertFalse(
            "CouiIllustDetailFloatingTopBar must not use flat alpha solid background or custom dark circle button",
            source.contains("CouiDetailTopCircleButton") || source.contains("scheme.background.copy(alpha = 0.94f)"),
        )
    }

    @Test
    fun couiBottomBarPreventsSlideOverSelection() {
        val source = locateSharedSourceFile("ui/coui/core/CouiBottomBar.kt").readText()
        assertTrue(
            "CouiOfficialNavigationBar must install CouiCenterCapsuleTapOverlay and validate tap-only commits via shouldCommitCouiBottomBarTap",
            source.contains("CouiCenterCapsuleTapOverlay(") &&
                source.contains("shouldCommitCouiBottomBarTap("),
        )
    }

    @Test
    fun couiMenuSelectRowUsesDropdownIndicatorInsteadOfHorizontalNavigationChevron() {
        val rowsSource = locateSharedSourceFile("ui/coui/core/CouiRows.kt").readText()
        val settingsSource = locateSharedSourceFile("ui/coui/page/CouiSettingsScreen.kt").readText()

        assertTrue(
            "CouiRows must define CouiRowIndicator.Dropdown and CouiDropdownIndicator",
            rowsSource.contains("enum class CouiRowIndicator") &&
                rowsSource.contains("Dropdown") &&
                rowsSource.contains("fun CouiDropdownIndicator("),
        )
        assertTrue(
            "CouiMenuSelectRow must pass trailingIndicator = CouiRowIndicator.Dropdown",
            settingsSource.contains("trailingIndicator = CouiRowIndicator.Dropdown"),
        )
    }

    @Test
    fun couiSegmentedControlCommitsSelectionOnRelease() {
        val tabRowSource = locateSharedSourceFile("ui/coui/component/CouiTabRow.kt").readText()
        assertTrue(
            "CouiLibrarySegmentedControl must pass commitOnRelease = true so sliding over options does not immediately select",
            tabRowSource.contains("commitOnRelease = commitOnRelease") || tabRowSource.contains("commitOnRelease = true"),
        )
    }

    @Test
    fun couiPageTransitionsAvoidRedundantBackdropsShaderRebuildsAndPerFrameRecomposition() {
        val rootSource = locateSharedSourceFile("ui/navigation/RootContent.kt").readText()
        val scaffoldSource = locateSharedSourceFile("ui/coui/core/CouiScaffold.kt").readText()
        val tabRowSource = locateSharedSourceFile("ui/coui/component/CouiTabRow.kt").readText()
        val detailSource = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        val transitionSource = locateSharedSourceFile("ui/navigation/animation/MiuixExpandTransition.kt").readText()
        val stackAnimSource = locateSharedSourceFile("ui/navigation/animation/SeekableStackAnimation.kt").readText()

        assertTrue(
            "RootContent must disable MIUIX LayerBackdrop when COUI skin is active",
            rootSource.contains("rememberBlurBackdrop(enableBlur = uiSkin != UiSkin.COLOROS)"),
        )
        assertFalse(
            "CouiScaffold must not wrap Scaffold content in a second nested contentBackdrop",
            scaffoldSource.contains("LocalCouiContentBackdrop"),
        )
        assertTrue(
            "CouiScaffold must share a single CouiBackdrop via CouiSingleBackdropScaffold and ProvideCouiBackdrop",
            scaffoldSource.contains("CouiSingleBackdropScaffold(") &&
                scaffoldSource.contains("ProvideCouiBackdrop("),
        )
        assertTrue(
            "CouiTabRow must consume the single shared backdrop LocalCouiSharedBackdrop.current",
            tabRowSource.contains("LocalCouiSharedBackdrop.current"),
        )
        assertTrue(
            "CouiScaffold and CouiTabRow must reuse pooled CouiFloatingRenderer instances",
            scaffoldSource.contains("rememberPooledCouiFloatingRenderer(") &&
                tabRowSource.contains("rememberPooledCouiFloatingRenderer(") &&
                tabRowSource.contains("rememberPooledCouiFloatingRenderers("),
        )
        assertFalse(
            "CouiIllustDetailSingle must not read activeTransitionExpansion directly during composition",
            detailSource.contains("cardExpandDetailChromeAlpha(sharedBoundsRegistry.activeTransitionExpansion)"),
        )
        assertFalse(
            "miuixDefaultSlideLayer must keep graphicsLayer attached in steady state instead of returning this",
            transitionSource.contains("if (clampedFraction <= 0.001f) return this"),
        )
        assertFalse(
            "SeekableStackAnimation must defer reading progress.value out of composition body",
            stackAnimSource.contains("Direction.ENTER_FRONT -> progress.value"),
        )
    }

    @Test
    fun resolveStackLayerFactorMapsAllDirectionsConsistently() {
        assertEquals(1f, resolveStackLayerFactor(Direction.ENTER_FRONT, 1f), 1e-5f)
        assertEquals(0.25f, resolveStackLayerFactor(Direction.ENTER_FRONT, 0.25f), 1e-5f)
        assertEquals(0f, resolveStackLayerFactor(Direction.EXIT_FRONT, 1f), 1e-5f)
        assertEquals(0.75f, resolveStackLayerFactor(Direction.EXIT_FRONT, 0.25f), 1e-5f)
        assertEquals(-1f, resolveStackLayerFactor(Direction.ENTER_BACK, 1f), 1e-5f)
        assertEquals(-0.25f, resolveStackLayerFactor(Direction.ENTER_BACK, 0.25f), 1e-5f)
        assertEquals(0f, resolveStackLayerFactor(Direction.EXIT_BACK, 1f), 1e-5f)
        assertEquals(-0.75f, resolveStackLayerFactor(Direction.EXIT_BACK, 0.25f), 1e-5f)
    }

    @Test
    fun resolveCouiDetailTopBarBlurAlphaStaysZeroOnEntryAndFadesInAfterScrollingDown() {
        assertEquals(0f, resolveCouiDetailTopBarBlurAlpha(0f), 1e-5f)
        assertEquals(0f, resolveCouiDetailTopBarBlurAlpha(0.18f), 1e-5f)
        val midAlpha = resolveCouiDetailTopBarBlurAlpha(0.44f)
        assertTrue("Expected fade progress in (0, 1), got $midAlpha", midAlpha in 0.49f..0.51f)
        assertEquals(1f, resolveCouiDetailTopBarBlurAlpha(0.70f), 1e-5f)
        assertEquals(1f, resolveCouiDetailTopBarBlurAlpha(1.25f), 1e-5f)
    }

    @Test
    fun shouldCommitCouiBottomBarTapCommitsOnlyInsideItemWithinTouchSlop() {
        // 正常轻点同一条目：位移 <= touchSlop 且落在 [0..100]x[0..80] 内 -> 提交
        assertTrue(
            shouldCommitCouiBottomBarTap(
                startX = 50f,
                startY = 40f,
                endX = 56f,
                endY = 44f,
                widthPx = 100f,
                heightPx = 80f,
                touchSlopPx = 12f,
            ),
        )
        // 滑动离开当前条目到邻近图标处抬手 -> 即使距离或跨界也不得提交
        assertFalse(
            shouldCommitCouiBottomBarTap(
                startX = 95f,
                startY = 40f,
                endX = 104f,
                endY = 40f,
                widthPx = 100f,
                heightPx = 80f,
                touchSlopPx = 12f,
            ),
        )
        // 条目内拖拽位移超过 touchSlop -> 视作滑动而非点击
        assertFalse(
            shouldCommitCouiBottomBarTap(
                startX = 20f,
                startY = 40f,
                endX = 45f,
                endY = 40f,
                widthPx = 100f,
                heightPx = 80f,
                touchSlopPx = 12f,
            ),
        )
        // 无效尺寸兜底 -> 不提交
        assertFalse(
            shouldCommitCouiBottomBarTap(
                startX = 0f,
                startY = 0f,
                endX = 0f,
                endY = 0f,
                widthPx = 0f,
                heightPx = 80f,
                touchSlopPx = 12f,
            ),
        )
    }

    private fun locateSharedSourceFile(relativePath: String): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val suffix = "src/main/kotlin/com/perol/pixez/shared/$relativePath"
        while (dir != null) {
            val direct = File(dir, suffix)
            if (direct.isFile) return direct
            val nested = File(dir, "shared/$suffix")
            if (nested.isFile) return nested
            val rootNested = File(dir, "compose-miuix/shared/$suffix")
            if (rootNested.isFile) return rootNested
            dir = dir.parentFile
        }
        error("Unable to locate $relativePath from ${System.getProperty("user.dir")}")
    }
}
