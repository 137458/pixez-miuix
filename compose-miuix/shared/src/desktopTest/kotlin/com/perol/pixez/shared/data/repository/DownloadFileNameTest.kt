package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import org.junit.Before
import org.junit.Test
import java.util.prefs.Preferences
import kotlin.test.assertEquals

class DownloadFileNameTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var downloadRepository: DownloadRepository

    @Before
    fun setUp() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/dl_filename_${System.currentTimeMillis()}")
        node.clear()
        settingsRepository = SettingsRepository(PreferencesSettings(node))
        downloadRepository = DownloadRepository(
            settingsRepository = settingsRepository,
        )
    }

    private fun createDummyIllust(
        id: Int = 123456,
        title: String = "Test Artwork",
        userId: Int = 7890,
        userName: String = "TestArtist",
        createDate: String = "2026-03-29T12:00:00+09:00",
        width: Int = 1920,
        height: Int = 1080,
        pageCount: Int = 1,
    ): Illust = Illust(
        id = id,
        title = title,
        type = "illust",
        imageUrls = ImageUrls(
            squareMedium = "https://i.pximg.net/sq.jpg",
            medium = "https://i.pximg.net/med.jpg",
            large = "https://i.pximg.net/large.jpg",
        ),
        caption = "caption",
        restrict = 0,
        user = IllustUser(
            id = userId,
            name = userName,
            account = "test_artist_account",
            profileImageUrls = IllustProfileImageUrls(""),
            isFollowed = false,
        ),
        tags = emptyList(),
        tools = emptyList(),
        createDate = createDate,
        pageCount = pageCount,
        width = width,
        height = height,
        sanityLevel = 2,
        xRestrict = 0,
        series = null,
        metaSinglePage = null,
        metaPages = emptyList(),
        totalView = 100,
        totalBookmarks = 50,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = 1,
        illustBookStyle = 0,
    )

    @Test
    fun testDefaultFormat() {
        val illust = createDummyIllust(id = 999888)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/img-original/999888_p0.jpg",
        )
        assertEquals("999888_p0.jpg", fileName)
    }

    @Test
    fun testIllustIdPlaceholder() {
        settingsRepository.format = "illust_{illust_id}"
        val illust = createDummyIllust(id = 123456)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("illust_123456.jpg", fileName)
    }

    @Test
    fun testTitlePlaceholder() {
        settingsRepository.format = "artwork_{title}"
        val illust = createDummyIllust(title = "Sunset Glow")
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.png",
        )
        assertEquals("artwork_Sunset Glow.png", fileName)
    }

    @Test
    fun testUserIdPlaceholder() {
        settingsRepository.format = "user_{user_id}"
        val illust = createDummyIllust(userId = 8888)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("user_8888.jpg", fileName)
    }

    @Test
    fun testUserNamePlaceholder() {
        settingsRepository.format = "artist_{user_name}"
        val illust = createDummyIllust(userName = "MikuMaster")
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("artist_MikuMaster.jpg", fileName)
    }

    @Test
    fun testAuthorPlaceholder() {
        settingsRepository.format = "by_{author}"
        val illust = createDummyIllust(userName = "MikuMaster")
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("by_MikuMaster.jpg", fileName)
    }

    @Test
    fun testPartPlaceholder() {
        settingsRepository.format = "{illust_id}_page_{part}"
        val illust = createDummyIllust(id = 123456, pageCount = 3)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 2,
            remoteUrl = "https://i.pximg.net/123456_p2.png",
        )
        assertEquals("123456_page_2.png", fileName)
    }

    @Test
    fun testCreateDatePlaceholder() {
        settingsRepository.format = "{create_date}_{illust_id}"
        val illust = createDummyIllust(id = 123456, createDate = "2026-03-29T12:34:56+09:00")
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("2026-03-29_123456.jpg", fileName)
    }

    @Test
    fun testWidthAndHeightPlaceholders() {
        settingsRepository.format = "{illust_id}_w{width}_h{height}"
        val illust = createDummyIllust(id = 123456, width = 3840, height = 2160)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("123456_w3840_h2160.jpg", fileName)
    }

    @Test
    fun testWidthXHeightPlaceholder() {
        settingsRepository.format = "{illust_id}_{width}x{height}"
        val illust = createDummyIllust(id = 123456, width = 1920, height = 1080)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("123456_1920x1080.jpg", fileName)
    }

    @Test
    fun testCombinedAllPlaceholders() {
        settingsRepository.format = "{illust_id}_{title}_{user_id}_{user_name}_{author}_{part}_{create_date}_{width}x{height}_{width}_{height}"
        val illust = createDummyIllust(
            id = 100,
            title = "Masterpiece",
            userId = 200,
            userName = "Painter",
            createDate = "2026-03-29T00:00:00Z",
            width = 1920,
            height = 1080,
            pageCount = 2,
        )
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 1,
            remoteUrl = "https://i.pximg.net/sample.webp",
        )
        assertEquals("100_Masterpiece_200_Painter_Painter_1_2026-03-29_1920x1080_1920_1080.webp", fileName)
    }

    @Test
    fun testMultiPageWithoutPartAppendsPart() {
        settingsRepository.format = "{illust_id}_{title}"
        val illust = createDummyIllust(id = 555, title = "MultiPageArt", pageCount = 3)
        val fileName = downloadRepository.buildFileName(
            illust = illust,
            pageIndex = 1,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("555_MultiPageArt_p1.jpg", fileName)
    }
}
