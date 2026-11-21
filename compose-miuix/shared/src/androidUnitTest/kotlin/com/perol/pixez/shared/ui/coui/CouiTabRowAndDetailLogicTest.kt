package com.perol.pixez.shared.ui.coui

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.clampCouiTabIndex
import com.perol.pixez.shared.ui.coui.component.resolveCouiChipTabKey
import com.perol.pixez.shared.ui.coui.page.CouiIllustBadge
import com.perol.pixez.shared.ui.coui.page.CouiIllustBadgeKind
import com.perol.pixez.shared.ui.coui.page.resolveCouiIllustBadges
import com.perol.pixez.shared.ui.coui.page.resolveDisplayedBookmarkCount
import com.perol.pixez.shared.ui.coui.page.shouldShowArtistFollowButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CouiTabRowAndDetailLogicTest {

    @Test
    fun clampCouiTabIndexHandlesEmptyAndOutOfRangeValues() {
        assertEquals(-1, clampCouiTabIndex(0, 0))
        assertEquals(-1, clampCouiTabIndex(2, -1))
        assertEquals(0, clampCouiTabIndex(-3, 3))
        assertEquals(1, clampCouiTabIndex(1, 3))
        assertEquals(2, clampCouiTabIndex(9, 3))
    }

    @Test
    fun resolveCouiChipTabKeyFallsBackToFirstWhenMissing() {
        val chips = listOf(
            CouiFilterChip(key = "day", label = "Day"),
            CouiFilterChip(key = "week", label = "Week"),
        )
        assertNull(resolveCouiChipTabKey("day", emptyList()))
        assertEquals("week", resolveCouiChipTabKey("week", chips))
        assertEquals("day", resolveCouiChipTabKey("unknown", chips))
    }

    @Test
    fun resolveCouiIllustBadgesPreservesSemanticOrder() {
        val normalSingle = sampleIllust()
        assertEquals(emptyList<CouiIllustBadge>(), resolveCouiIllustBadges(normalSingle))

        val richIllust = sampleIllust(
            type = "ugoira",
            pageCount = 5,
            illustAIType = 2,
            xRestrict = 1,
        )
        assertEquals(
            listOf(
                CouiIllustBadge("R-18", CouiIllustBadgeKind.R18),
                CouiIllustBadge("AI", CouiIllustBadgeKind.AI),
                CouiIllustBadge("GIF", CouiIllustBadgeKind.UGOIRA),
                CouiIllustBadge("5P", CouiIllustBadgeKind.MULTI_PAGE),
            ),
            resolveCouiIllustBadges(richIllust),
        )
    }

    @Test
    fun shouldShowArtistFollowButtonHidesOnlyForOwnArtwork() {
        assertTrue(shouldShowArtistFollowButton(illustUserId = 100L, currentAccountUserId = null))
        assertTrue(shouldShowArtistFollowButton(illustUserId = 100L, currentAccountUserId = 200L))
        assertFalse(shouldShowArtistFollowButton(illustUserId = 100L, currentAccountUserId = 100L))
    }

    @Test
    fun resolveDisplayedBookmarkCountReflectsLocalToggleAndFloorsAtZero() {
        assertEquals(10, resolveDisplayedBookmarkCount(10, initiallyBookmarked = false, currentlyBookmarked = false))
        assertEquals(11, resolveDisplayedBookmarkCount(10, initiallyBookmarked = false, currentlyBookmarked = true))
        assertEquals(9, resolveDisplayedBookmarkCount(10, initiallyBookmarked = true, currentlyBookmarked = false))
        assertEquals(0, resolveDisplayedBookmarkCount(0, initiallyBookmarked = true, currentlyBookmarked = false))
    }

    @Test
    fun couiSourceTreeContainsZeroMiuixImportsOrReferences() {
        val couiRoot = locateCouiSourceDir()
        val ktFiles = couiRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("Expected COUI source files under ${couiRoot.absolutePath}", ktFiles.isNotEmpty())

        val violations = mutableListOf<String>()
        val forbidden = Regex("""\b(top\.yukonga\.miuix|MiuixIcons|MiuixTheme)\b""")
        ktFiles.forEach { file ->
            file.readLines().forEachIndexed { idx, line ->
                if (forbidden.containsMatchIn(line)) {
                    violations += "${file.name}:${idx + 1}: ${line.trim()}"
                }
            }
        }
        assertEquals(
            "COUI source tree must not contain MIUIX imports or icon/theme references",
            emptyList<String>(),
            violations,
        )
    }

    private fun locateCouiSourceDir(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val suffix = "src/main/kotlin/com/perol/pixez/shared/ui/coui"
        while (dir != null) {
            val direct = File(dir, suffix)
            if (direct.isDirectory) return direct
            val nested = File(dir, "shared/$suffix")
            if (nested.isDirectory) return nested
            val rootNested = File(dir, "compose-miuix/shared/$suffix")
            if (rootNested.isDirectory) return rootNested
            dir = dir.parentFile
        }
        error("Unable to locate COUI source directory from ${System.getProperty("user.dir")}")
    }

    private fun sampleIllust(
        type: String = "illust",
        pageCount: Int = 1,
        illustAIType: Int = 1,
        xRestrict: Int = 0,
    ): Illust = Illust(
        id = 12345L,
        title = "Sample",
        type = type,
        imageUrls = ImageUrls(
            squareMedium = "",
            medium = "",
            large = "",
        ),
        caption = "",
        restrict = 0,
        user = IllustUser(
            id = 100L,
            name = "Artist",
            account = "artist",
            profileImageUrls = IllustProfileImageUrls(medium = ""),
        ),
        tags = listOf(IllustTag(name = "tag")),
        tools = emptyList(),
        createDate = "2026-01-01T00:00:00+09:00",
        pageCount = pageCount,
        width = 800,
        height = 1200,
        sanityLevel = 2,
        xRestrict = xRestrict,
        metaPages = emptyList(),
        totalView = 100,
        totalBookmarks = 10,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = illustAIType,
    )
}
