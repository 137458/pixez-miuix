package com.perol.pixez.shared.ui.coui

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaPage
import com.perol.pixez.shared.data.model.MetaPageImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.ui.components.resolveViewerThumbnailUrl
import com.perol.pixez.shared.ui.components.resolveViewerZoomUrl
import com.perol.pixez.shared.ui.coui.page.resolveCouiDetailTopBarTitleAlpha
import com.perol.pixez.shared.ui.coui.page.resolveCouiPictureSourceOptionIndex
import com.perol.pixez.shared.ui.coui.page.resolveCouiWidgetFeedIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CouiDetailDropdownAndViewerTest {

    @Test
    fun resolveCouiDetailTopBarTitleAlphaStaysZeroAtTopAndFadesInAfterThreshold() {
        assertEquals(0f, resolveCouiDetailTopBarTitleAlpha(-0.2f), 1e-5f)
        assertEquals(0f, resolveCouiDetailTopBarTitleAlpha(0f), 1e-5f)
        assertEquals(0f, resolveCouiDetailTopBarTitleAlpha(0.35f), 1e-5f)
        assertEquals(0.5f, resolveCouiDetailTopBarTitleAlpha(0.675f), 1e-4f)
        assertEquals(1f, resolveCouiDetailTopBarTitleAlpha(1f), 1e-5f)
        assertEquals(1f, resolveCouiDetailTopBarTitleAlpha(1.5f), 1e-5f)
    }

    @Test
    fun resolveViewerZoomUrlHandlesSingleAndMultiPageQualities() {
        val single = sampleIllust(
            large = "https://example.com/single_large.jpg",
            medium = "https://example.com/single_med.jpg",
            singleOriginal = "https://example.com/single_orig.png",
        )
        assertEquals("https://example.com/single_orig.png", resolveViewerZoomUrl(single, 0, zoomQuality = 0))
        assertEquals("https://example.com/single_large.jpg", resolveViewerZoomUrl(single, 0, zoomQuality = 1))
        assertEquals("https://example.com/single_med.jpg", resolveViewerZoomUrl(single, 0, zoomQuality = 2))

        val multi = sampleIllust(
            metaPages = listOf(
                MetaPage(
                    imageUrls = MetaPageImageUrls(
                        squareMedium = "https://example.com/p0_sq.jpg",
                        medium = "https://example.com/p0_med.jpg",
                        large = "https://example.com/p0_large.jpg",
                        original = "https://example.com/p0_orig.png",
                    ),
                ),
                MetaPage(
                    imageUrls = MetaPageImageUrls(
                        squareMedium = "https://example.com/p1_sq.jpg",
                        medium = "",
                        large = "https://example.com/p1_large.jpg",
                        original = "",
                    ),
                ),
            ),
        )
        assertEquals("https://example.com/p0_orig.png", resolveViewerZoomUrl(multi, 0, zoomQuality = 0))
        assertEquals("https://example.com/p0_large.jpg", resolveViewerZoomUrl(multi, 0, zoomQuality = 1))
        assertEquals("https://example.com/p0_med.jpg", resolveViewerZoomUrl(multi, 0, zoomQuality = 2))
        assertEquals("https://example.com/p1_large.jpg", resolveViewerZoomUrl(multi, 1, zoomQuality = 0))
    }

    @Test
    fun resolveViewerThumbnailUrlUsesPreviewUrlOnlyOnInitialPage() {
        val multi = sampleIllust(
            large = "https://example.com/cover_large.jpg",
            medium = "https://example.com/cover_med.jpg",
            metaPages = listOf(
                MetaPage(
                    imageUrls = MetaPageImageUrls(
                        squareMedium = "",
                        medium = "https://example.com/p0_med.jpg",
                        large = "https://example.com/p0_large.jpg",
                        original = "https://example.com/p0_orig.png",
                    ),
                ),
                MetaPage(
                    imageUrls = MetaPageImageUrls(
                        squareMedium = "",
                        medium = "https://example.com/p1_med.jpg",
                        large = "",
                        original = "",
                    ),
                ),
            ),
        )
        assertEquals(
            "https://example.com/preview_cached.jpg",
            resolveViewerThumbnailUrl(multi, pageIndex = 1, initialPage = 1, previewUrl = "https://example.com/preview_cached.jpg"),
        )
        assertEquals(
            "https://example.com/p0_large.jpg",
            resolveViewerThumbnailUrl(multi, pageIndex = 0, initialPage = 1, previewUrl = "https://example.com/preview_cached.jpg"),
        )
        assertEquals(
            "https://example.com/p1_med.jpg",
            resolveViewerThumbnailUrl(multi, pageIndex = 1, initialPage = 0, previewUrl = null),
        )
    }

    @Test
    fun resolveCouiPictureSourceOptionIndexMapsPresetsAndCustomHost() {
        assertEquals(0, resolveCouiPictureSourceOptionIndex(PixivHosts.HOST_PXIMG))
        assertEquals(1, resolveCouiPictureSourceOptionIndex(PixivHosts.HOST_PIXIV_RE))
        assertEquals(2, resolveCouiPictureSourceOptionIndex("i.pixiv.nl"))
    }

    @Test
    fun resolveCouiWidgetFeedIndexNormalizesLegacyRankToDay() {
        val types = listOf("recom", "day", "week", "month")
        assertEquals(1, resolveCouiWidgetFeedIndex("rank", types))
        assertEquals(1, resolveCouiWidgetFeedIndex("day", types))
        assertEquals(2, resolveCouiWidgetFeedIndex("week", types))
        assertEquals(0, resolveCouiWidgetFeedIndex("unknown", types))
    }

    @Test
    fun couiIllustDetailScreenRemovesTopBarStaticTitleAndUsesImmersiveCollapse() {
        val source = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        assertFalse(
            "CouiIllustDetailScreen must not display static artwork title in CouiCompactScaffold top bar",
            source.contains("CouiCompactScaffold(\n            title = illust?.title") ||
                source.contains("CouiCompactScaffold(\r\n            title = illust?.title"),
        )
        assertTrue(
            "CouiIllustDetailScreen must compute scroll-driven title alpha via resolveCouiDetailTopBarTitleAlpha",
            source.contains("resolveCouiDetailTopBarTitleAlpha("),
        )
    }

    @Test
    fun couiFullScreenViewerIsRewrittenInPureCouiWithoutMiuixDelegation() {
        val source = locateSharedSourceFile("ui/coui/component/CouiFullScreenViewer.kt").readText()
        assertFalse(
            "CouiFullScreenViewer must not delegate to MIUIX IllustDetailFullScreenOverlay",
            source.contains("IllustDetailFullScreenOverlay"),
        )
        assertFalse(
            "CouiFullScreenViewer must not delegate to MIUIX IllustFullScreenViewer",
            source.contains("IllustFullScreenViewer"),
        )
        assertFalse(
            "CouiFullScreenViewer must not delegate to MIUIX ZoomableImage",
            Regex("""\bZoomableImage\(""").containsMatchIn(source),
        )
        assertTrue(
            "CouiFullScreenViewer must implement COUI zoomable image handling directly",
            source.contains("CouiZoomableImage("),
        )
    }

    @Test
    fun couiSettingPagesUseMenuDropdownSelectInsteadOfInlineRadioLists() {
        val targetPages = listOf(
            "ui/coui/page/CouiQualitySettingScreen.kt",
            "ui/coui/page/CouiThemeSettingScreen.kt",
            "ui/coui/page/CouiWelcomePageScreen.kt",
            "ui/coui/page/CouiWidgetRecommendScreen.kt",
            "ui/coui/page/CouiNetworkSettingScreen.kt",
            "ui/coui/page/CouiLanguageSettingScreen.kt",
            "ui/coui/page/CouiAiSettingScreen.kt",
        )
        targetPages.forEach { path ->
            val source = locateSharedSourceFile(path).readText()
            assertFalse(
                "$path must replace CouiRadioOptionRow with CouiMenuSelectRow",
                source.contains("CouiRadioOptionRow"),
            )
            assertTrue(
                "$path must use CouiMenuSelectRow dropdown popup",
                source.contains("CouiMenuSelectRow("),
            )
        }
        val settingsSource = locateSharedSourceFile("ui/coui/page/CouiSettingsScreen.kt").readText()
        assertTrue(
            "CouiMenuSelectRow must pass selected state to CouiMenuItem",
            settingsSource.contains("selected = index == selectedIndex"),
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

    private fun sampleIllust(
        large: String = "",
        medium: String = "",
        singleOriginal: String? = null,
        metaPages: List<MetaPage> = emptyList(),
    ): Illust = Illust(
        id = 1L,
        title = "Sample",
        type = "illust",
        imageUrls = ImageUrls(
            squareMedium = "",
            medium = medium,
            large = large,
        ),
        caption = "",
        restrict = 0,
        user = IllustUser(
            id = 1L,
            name = "Artist",
            account = "artist",
            profileImageUrls = IllustProfileImageUrls(medium = ""),
        ),
        tags = emptyList(),
        tools = emptyList(),
        createDate = "2026-01-01T00:00:00+09:00",
        pageCount = metaPages.size.coerceAtLeast(1),
        width = 800,
        height = 1200,
        sanityLevel = 2,
        xRestrict = 0,
        metaSinglePage = singleOriginal?.let { MetaSinglePage(originalImageUrl = it) },
        metaPages = metaPages,
        totalView = 10,
        totalBookmarks = 5,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = 1,
    )
}
