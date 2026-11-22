package com.perol.pixez.shared.ui.coui

import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.perol.pixez.shared.ui.coui.core.BoundedObjectPool
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
    fun couiIllustDetailTopBarUsesOfficialGradientBlurAndGlareTopAppBarButtons() {
        val source = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        assertTrue(
            "CouiIllustDetailScreen must record content with rememberCouiBackdrop() and couiBackdrop(...)",
            source.contains("rememberCouiBackdrop(") && source.contains(".couiBackdrop("),
        )
        assertTrue(
            "CouiIllustDetailFloatingTopBar must use official COUI TopAppBar for gradient blur",
            source.contains("TopAppBar(") && source.contains("CouiTopAppBarDefaults.colors("),
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
