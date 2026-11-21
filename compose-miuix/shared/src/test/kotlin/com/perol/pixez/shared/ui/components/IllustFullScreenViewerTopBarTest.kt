package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaPage
import com.perol.pixez.shared.data.model.MetaPageImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.ui.i18n.ZhCnStrings
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.ListView
import kotlin.test.Test
import kotlin.test.assertEquals

class IllustFullScreenViewerTopBarTest {

    private val strings = ZhCnStrings

    private fun createDummyIllust(id: Long, title: String, pageCount: Int): Illust {
        val metaPages = if (pageCount > 1) {
            (0 until pageCount).map { i ->
                MetaPage(
                    imageUrls = MetaPageImageUrls(
                        squareMedium = "https://example.com/square_$i.jpg",
                        medium = "https://example.com/medium_$i.jpg",
                        large = "https://example.com/large_$i.jpg",
                        original = "https://example.com/original_$i.jpg",
                    )
                )
            }
        } else {
            emptyList()
        }
        return Illust(
            id = id,
            title = title,
            type = "illust",
            imageUrls = ImageUrls(
                squareMedium = "https://example.com/square.jpg",
                medium = "https://example.com/medium.jpg",
                large = "https://example.com/large.jpg",
            ),
            caption = "",
            restrict = 0,
            user = IllustUser(
                id = 1001L,
                name = "Artist",
                account = "artist_acc",
                profileImageUrls = IllustProfileImageUrls(medium = ""),
            ),
            tags = emptyList(),
            tools = emptyList(),
            createDate = "2026-11-11T00:00:00+09:00",
            pageCount = pageCount,
            width = 1920,
            height = 1080,
            sanityLevel = 2,
            xRestrict = 0,
            metaSinglePage = MetaSinglePage(originalImageUrl = ""),
            metaPages = metaPages,
            totalView = 500,
            totalBookmarks = 50,
            isBookmarked = false,
            visible = true,
            isMuted = false,
            totalComments = 0,
            illustAIType = 0,
            illustBookStyle = 0,
        )
    }

    @Test
    fun `单图作品分享信息不携带页码锚点`() {
        val illust = createDummyIllust(id = 123456L, title = "单图作品", pageCount = 1)
        val (link, title) = buildViewerShareInfo(illust = illust, pageCount = 1, currentPage = 0)

        assertEquals("https://www.pixiv.net/artworks/123456", link)
        assertEquals("单图作品", title)
    }

    @Test
    fun `多图作品分享信息正确携带页码锚点与标号`() {
        val illust = createDummyIllust(id = 654321L, title = "图集作品", pageCount = 5)
        val (linkP1, titleP1) = buildViewerShareInfo(illust = illust, pageCount = 5, currentPage = 0)
        assertEquals("https://www.pixiv.net/artworks/654321#page=1", linkP1)
        assertEquals("图集作品 (P1)", titleP1)

        val (linkP3, titleP3) = buildViewerShareInfo(illust = illust, pageCount = 5, currentPage = 2)
        assertEquals("https://www.pixiv.net/artworks/654321#page=3", linkP3)
        assertEquals("图集作品 (P3)", titleP3)
    }

    @Test
    fun `多图作品单页图片URL解析优先选择当前页large画质`() {
        val illust = createDummyIllust(id = 777777L, title = "多图解析", pageCount = 3)
        val urlP1 = resolveViewerTargetImageUrl(illust = illust, pageIndex = 1)
        assertEquals("https://example.com/large_1.jpg", urlP1)

        val urlSingle = createDummyIllust(id = 888888L, title = "单图解析", pageCount = 1)
        val urlP0 = resolveViewerTargetImageUrl(illust = urlSingle, pageIndex = 0)
        assertEquals("https://example.com/large.jpg", urlP0)
    }

    @Test
    fun `阅读模式切换在垂直与水平状态下的图标与文案映射准确`() {
        // 当前是垂直卷轴模式 -> 目标切换到水平切页模式
        val (horizontalTooltip, horizontalIcon) = resolveViewerModeSwitchState(
            isVerticalScrollMode = true,
            strings = strings,
        )
        assertEquals(strings.viewerModeHorizontal, horizontalTooltip)
        assertEquals(MiuixIcons.HorizontalSplit, horizontalIcon)

        // 当前是水平切页模式 -> 目标切换到垂直卷轴模式
        val (verticalTooltip, verticalIcon) = resolveViewerModeSwitchState(
            isVerticalScrollMode = false,
            strings = strings,
        )
        assertEquals(strings.viewerModeVertical, verticalTooltip)
        assertEquals(MiuixIcons.ListView, verticalIcon)
    }
}
