package com.perol.pixez.shared.ui.coui

import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.coui.page.resolveCouiSearchExpandedForResults
import com.perol.pixez.shared.ui.coui.page.shouldInterceptCouiSearchBack
import com.perol.pixez.shared.ui.screens.resolveSyncedDetailToggle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CouiSearchAndDetailFixTest {

    @Test
    fun resolveCouiSearchExpandedForResultsExpandsWhenEnteringResultsAndPreservesClearState() {
        assertFalse(resolveCouiSearchExpandedForResults(currentExpanded = false, showingResults = false))
        assertTrue(resolveCouiSearchExpandedForResults(currentExpanded = false, showingResults = true))
        assertTrue(resolveCouiSearchExpandedForResults(currentExpanded = true, showingResults = false))
        assertTrue(resolveCouiSearchExpandedForResults(currentExpanded = true, showingResults = true))
    }

    @Test
    fun shouldInterceptCouiSearchBackHonorsActiveTabDialogsAndSearchState() {
        assertFalse(
            shouldInterceptCouiSearchBack(
                isActive = false,
                showingResults = true,
                searchExpanded = true,
                hasBlockingDialog = false,
            ),
        )
        assertFalse(
            shouldInterceptCouiSearchBack(
                isActive = true,
                showingResults = true,
                searchExpanded = true,
                hasBlockingDialog = true,
            ),
        )
        assertFalse(
            shouldInterceptCouiSearchBack(
                isActive = true,
                showingResults = false,
                searchExpanded = false,
                hasBlockingDialog = false,
            ),
        )
        assertTrue(
            shouldInterceptCouiSearchBack(
                isActive = true,
                showingResults = true,
                searchExpanded = false,
                hasBlockingDialog = false,
            ),
        )
        assertTrue(
            shouldInterceptCouiSearchBack(
                isActive = true,
                showingResults = false,
                searchExpanded = true,
                hasBlockingDialog = false,
            ),
        )
        assertTrue(
            shouldInterceptCouiSearchBack(
                isActive = true,
                showingResults = true,
                searchExpanded = true,
                hasBlockingDialog = false,
            ),
        )
    }

    @Test
    fun resolveSyncedDetailToggleAlignsRemoteWhenUntouchedAndPreservesLocalOverride() {
        assertFalse(resolveSyncedDetailToggle(currentValue = false, hasLocalChange = false, remoteValue = null))
        assertTrue(resolveSyncedDetailToggle(currentValue = true, hasLocalChange = false, remoteValue = null))
        assertTrue(resolveSyncedDetailToggle(currentValue = false, hasLocalChange = false, remoteValue = true))
        assertFalse(resolveSyncedDetailToggle(currentValue = true, hasLocalChange = false, remoteValue = false))

        assertTrue(resolveSyncedDetailToggle(currentValue = true, hasLocalChange = true, remoteValue = false))
        assertFalse(resolveSyncedDetailToggle(currentValue = false, hasLocalChange = true, remoteValue = true))
        assertTrue(resolveSyncedDetailToggle(currentValue = true, hasLocalChange = true, remoteValue = null))
    }

    @Test
    fun couiIconsDirectionalVectorsEnableAutoMirror() {
        assertTrue(CouiIcons.Back.autoMirror)
        assertTrue(CouiIcons.ChevronForward.autoMirror)
        assertFalse(CouiIcons.ExpandLess.autoMirror)
        assertFalse(CouiIcons.ExpandMore.autoMirror)
    }

    @Test
    fun couiSearchScreenInterceptsBackAndSyncsExpandedState() {
        val source = locateSharedSourceFile("ui/coui/page/CouiSearchScreen.kt").readText()
        assertTrue(
            "CouiSearchScreen must register PlatformBackHandler to exit search results or expanded state",
            source.contains("PlatformBackHandler("),
        )
        assertTrue(
            "CouiSearchScreen must sync searchExpanded to true when showingResults becomes true",
            source.contains("LaunchedEffect(showingResults)"),
        )
    }

    @Test
    fun detailScreensUseSyncedToggleInsteadOfWholeIllustRememberSaveableKey() {
        val couiSource = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        val miuixSource = locateSharedSourceFile("ui/screens/IllustDetailScreen.kt").readText()

        assertFalse(
            "CouiIllustDetailSingle must not key rememberSaveable on whole Illust object",
            couiSource.contains("rememberSaveable(illust)"),
        )
        assertFalse(
            "CouiIllustDetailSingle must not key isFollowed rememberSaveable on illust?.user?.isFollowed",
            couiSource.contains("rememberSaveable(illust?.user?.id, illust?.user?.isFollowed)"),
        )
        assertTrue(
            "CouiIllustDetailSingle must wire rememberSyncedDetailToggle",
            couiSource.contains("rememberSyncedDetailToggle("),
        )
        assertFalse(
            "IllustDetailSingleContent must not key rememberSaveable on whole Illust object",
            miuixSource.contains("rememberSaveable(illust)"),
        )
        assertTrue(
            "IllustDetailSingleContent must wire rememberSyncedDetailToggle",
            miuixSource.contains("rememberSyncedDetailToggle("),
        )
    }

    @Test
    fun couiMinorFixesAreWiredInSource() {
        val rowsSource = locateSharedSourceFile("ui/coui/core/CouiRows.kt").readText()
        val iconsSource = locateSharedSourceFile("ui/coui/icon/CouiIcons.kt").readText()
        val overlaysSource = locateSharedSourceFile("ui/coui/component/CouiRootOverlays.kt").readText()
        val skinPagesSource = locateSharedSourceFile("ui/skin/CouiSkinPages.kt").readText()
        val languageSource = locateSharedSourceFile("ui/coui/page/CouiLanguageSettingScreen.kt").readText()

        assertFalse(
            "CouiActionRow destructive branch must not use CouiTheme.colorScheme.accent",
            rowsSource.contains("destructive -> CouiTheme.colorScheme.accent"),
        )
        assertTrue(
            "CouiActionRow destructive branch must use CouiDialogDefaults destructive color",
            rowsSource.contains("CouiDialogDefaults.colors(CouiTheme.isDark).destructive"),
        )
        assertTrue(
            "CouiChevron must support LayoutDirection.Rtl mirroring",
            rowsSource.contains("LayoutDirection.Rtl"),
        )
        assertTrue(
            "CouiIcons.Back must enable autoMirror",
            iconsSource.contains("""strokeIcon("Back", autoMirror = true)"""),
        )
        assertTrue(
            "CouiIcons.ChevronForward must enable autoMirror",
            iconsSource.contains("""strokeIcon("ChevronForward", autoMirror = true)"""),
        )
        assertTrue(
            "CouiNavigationRail must consumeWindowInsets after windowInsetsPadding",
            overlaysSource.contains(".consumeWindowInsets("),
        )
        assertFalse(
            "CouiSkinPages must key reselectFlow on both component and tab",
            skinPagesSource.contains("remember(tab) { component.reselectFlowOf(tab) }"),
        )
        assertFalse(
            "CouiLanguageSettingScreen comment must not claim no in-app live language switching",
            languageSource.contains("不做应用内实时切换"),
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
