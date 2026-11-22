package com.perol.pixez.shared.ui.coui

import androidx.compose.ui.graphics.vector.ImageVector
import com.perol.pixez.shared.ui.coui.core.shouldUseOfficialCouiNavigationBar
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.navigation.MAIN_TAB_DESCRIPTORS
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.navigation.resolveMainTabIndex
import com.perol.pixez.shared.ui.navigation.resolveVisibleMainTabDescriptors
import com.perol.pixez.shared.ui.skin.couiNavIconsForTab
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CouiBottomBarOfficialStyleTest {

    @Test
    fun `resolveVisibleMainTabDescriptors keeps 5 tabs for HyperOS and 4 tabs without Search for COUI`() {
        val hyperOsTabs = resolveVisibleMainTabDescriptors(isCouiSkin = false)
        assertEquals(MAIN_TAB_DESCRIPTORS.size, hyperOsTabs.size)
        assertEquals(RootComponent.MAIN_TAB_ORDER, hyperOsTabs.map { it.tab })

        val couiTabs = resolveVisibleMainTabDescriptors(isCouiSkin = true)
        assertEquals(
            listOf(
                RootComponent.MainTab.Hello,
                RootComponent.MainTab.Ranking,
                RootComponent.MainTab.New,
                RootComponent.MainTab.Spotlight,
            ),
            couiTabs.map { it.tab },
        )
        assertFalse(couiTabs.any { it.tab == RootComponent.MainTab.Search })
        val newTab = couiTabs.first { it.tab == RootComponent.MainTab.New }
        assertTrue(newTab.carriesUnreadFeedBadge)
    }

    @Test
    fun `resolveMainTabIndex maps visible tabs and clamps missing Search to 0`() {
        val couiTabs = resolveVisibleMainTabDescriptors(isCouiSkin = true)
        assertEquals(0, resolveMainTabIndex(couiTabs, RootComponent.MainTab.Hello))
        assertEquals(1, resolveMainTabIndex(couiTabs, RootComponent.MainTab.Ranking))
        assertEquals(2, resolveMainTabIndex(couiTabs, RootComponent.MainTab.New))
        assertEquals(3, resolveMainTabIndex(couiTabs, RootComponent.MainTab.Spotlight))
        assertEquals(0, resolveMainTabIndex(couiTabs, RootComponent.MainTab.Search))
    }

    @Test
    fun `couiNavIconsForTab provides distinct selected icons for 4 main tabs and Search icon`() {
        val fallback: ImageVector = CouiIcons.More
        val middleTabs = listOf(
            RootComponent.MainTab.Hello,
            RootComponent.MainTab.Ranking,
            RootComponent.MainTab.New,
            RootComponent.MainTab.Spotlight,
        )
        middleTabs.forEach { tab ->
            val (icon, selectedIcon) = couiNavIconsForTab(tab, fallback)
            assertNotEquals(fallback, icon, "tab=$tab should not use fallback icon")
            assertNotEquals(fallback, selectedIcon, "tab=$tab should not use fallback selectedIcon")
            assertNotEquals(icon, selectedIcon, "tab=$tab should have distinct selectedIcon")
        }

        val (searchIcon, searchSelectedIcon) = couiNavIconsForTab(RootComponent.MainTab.Search, fallback)
        assertEquals(CouiIcons.Search, searchIcon)
        assertEquals(CouiIcons.Search, searchSelectedIcon)
    }

    @Test
    fun `shouldUseOfficialCouiNavigationBar requires API 34+, floating, backdrop, and renderer`() {
        assertTrue(
            shouldUseOfficialCouiNavigationBar(
                sdkInt = 34,
                isFloating = true,
                hasBackdrop = true,
                hasRenderer = true,
            ),
        )
        assertTrue(
            shouldUseOfficialCouiNavigationBar(
                sdkInt = 35,
                isFloating = true,
                hasBackdrop = true,
                hasRenderer = true,
            ),
        )
        assertFalse(
            shouldUseOfficialCouiNavigationBar(
                sdkInt = 33,
                isFloating = true,
                hasBackdrop = true,
                hasRenderer = true,
            ),
        )
        assertFalse(
            shouldUseOfficialCouiNavigationBar(
                sdkInt = 34,
                isFloating = false,
                hasBackdrop = true,
                hasRenderer = true,
            ),
        )
        assertFalse(
            shouldUseOfficialCouiNavigationBar(
                sdkInt = 34,
                isFloating = true,
                hasBackdrop = false,
                hasRenderer = true,
            ),
        )
        assertFalse(
            shouldUseOfficialCouiNavigationBar(
                sdkInt = 34,
                isFloating = true,
                hasBackdrop = true,
                hasRenderer = false,
            ),
        )
    }

    @Test
    fun `CouiBottomBar uses official NavigationBar without hand-written floating surface`() {
        val source = locateSharedSourceFile("ui/coui/core/CouiBottomBar.kt").readText()
        assertTrue(source.contains("NavigationBar("))
        assertTrue(source.contains("CouiNavigationItem("))
        assertTrue(source.contains("CouiFloatingRenderer()"))
        assertFalse(source.contains("couiFloatingSurface"))
        assertFalse(source.contains("CouiFloatingRenderer.standalone()"))
        assertFalse(source.contains("androidx.compose.material3"))
        assertFalse(source.contains("top.yukonga.miuix"))
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
