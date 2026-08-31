package com.perol.pixez.shared.data.model

import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class R18FilterTest {

    private fun createDummyIllust(
        id: Int = 1001,
        xRestrict: Int = 0,
        sanityLevel: Int = 2,
        tags: List<IllustTag> = listOf(IllustTag(name = "original")),
    ): Illust {
        return Illust(
            id = id,
            title = "Test Illust",
            type = "illust",
            imageUrls = ImageUrls(
                squareMedium = "https://example.com/sq.jpg",
                medium = "https://example.com/med.jpg",
                large = "https://example.com/large.jpg",
            ),
            caption = "Test Caption",
            restrict = 0,
            user = IllustUser(
                id = 1,
                name = "Artist",
                account = "artist_acc",
                profileImageUrls = IllustProfileImageUrls(medium = "https://example.com/avatar.jpg"),
            ),
            tags = tags,
            tools = emptyList(),
            createDate = "2026-08-31T00:00:00+09:00",
            pageCount = 1,
            width = 1000,
            height = 1000,
            sanityLevel = sanityLevel,
            xRestrict = xRestrict,
            metaSinglePage = MetaSinglePage(originalImageUrl = "https://example.com/orig.jpg"),
            metaPages = emptyList(),
            totalView = 100,
            totalBookmarks = 50,
            isBookmarked = false,
            visible = true,
            isMuted = false,
            illustAIType = 0,
        )
    }

    private fun createDummyNovel(
        id: Int = 2001,
        xRestrict: Int = 0,
        isXRestricted: Boolean = false,
        tags: List<NovelTag> = listOf(NovelTag(name = "original", addedByUploadedUser = false)),
    ): Novel {
        return Novel(
            id = id,
            title = "Test Novel",
            caption = "Novel Caption",
            restrict = 0,
            xRestrict = xRestrict,
            isOriginal = true,
            imageUrls = NovelImageUrls(
                squareMedium = "https://example.com/sq.jpg",
                medium = "https://example.com/med.jpg",
                large = "https://example.com/large.jpg",
            ),
            createDate = "2026-08-31T00:00:00+09:00",
            tags = tags,
            pageCount = 5,
            textLength = 2000,
            user = NovelUser(
                id = 2,
                name = "Writer",
                account = "writer_acc",
                profileImageUrls = NovelProfileImageUrls(medium = "https://example.com/avatar.jpg"),
                isFollowed = false,
            ),
            isBookmarked = false,
            totalBookmarks = 30,
            totalView = 500,
            visible = true,
            totalComments = 5,
            isMuted = false,
            isMypixivOnly = false,
            isXRestricted = isXRestricted,
            novelAIType = 0,
        )
    }

    @Test
    fun testIllustIsR18ReturnsFalseForSafeWork() {
        val safeIllust = createDummyIllust(
            xRestrict = 0,
            sanityLevel = 2,
            tags = listOf(IllustTag(name = "landscape"), IllustTag(name = "anime", translatedName = "animation")),
        )
        assertFalse(safeIllust.isR18())
    }

    @Test
    fun testIllustIsR18ReturnsTrueForXRestrictGreaterThan0() {
        val r18Illust = createDummyIllust(xRestrict = 1, sanityLevel = 2)
        val r18gIllust = createDummyIllust(xRestrict = 2, sanityLevel = 2)
        assertTrue(r18Illust.isR18())
        assertTrue(r18gIllust.isR18())
    }

    @Test
    fun testIllustIsR18ReturnsTrueForSanityLevelGreaterThan4() {
        val r18Sanity = createDummyIllust(xRestrict = 0, sanityLevel = 6)
        val r18gSanity = createDummyIllust(xRestrict = 0, sanityLevel = 18)
        assertTrue(r18Sanity.isR18())
        assertTrue(r18gSanity.isR18())
    }

    @Test
    fun testIllustIsR18ReturnsTrueForSensitiveTagVariations() {
        val tagsToTest = listOf(
            "R-18",
            "r-18",
            "R18",
            "r18",
            "R-18G",
            "R18G",
            "18禁",
            "18+",
        )

        for (tag in tagsToTest) {
            val illustWithTag = createDummyIllust(
                xRestrict = 0,
                sanityLevel = 2,
                tags = listOf(IllustTag(name = tag)),
            )
            assertTrue(illustWithTag.isR18(), "Expected true for tag $tag")
        }
    }

    @Test
    fun testIllustIsR18ReturnsTrueForTranslatedTagVariations() {
        val illustWithTranslatedTag = createDummyIllust(
            xRestrict = 0,
            sanityLevel = 2,
            tags = listOf(IllustTag(name = "オリジナル", translatedName = "R-18")),
        )
        assertTrue(illustWithTranslatedTag.isR18())

        val illustWithTranslated18Plus = createDummyIllust(
            xRestrict = 0,
            sanityLevel = 2,
            tags = listOf(IllustTag(name = "オリジナル", translatedName = "18+")),
        )
        assertTrue(illustWithTranslated18Plus.isR18())
    }

    @Test
    fun testNovelIsR18Detection() {
        val safeNovel = createDummyNovel(xRestrict = 0, isXRestricted = false)
        assertFalse(safeNovel.isR18())

        val r18Novel = createDummyNovel(xRestrict = 1)
        assertTrue(r18Novel.isR18())

        val xRestrictedNovel = createDummyNovel(isXRestricted = true)
        assertTrue(xRestrictedNovel.isR18())

        val tagR18Novel = createDummyNovel(
            tags = listOf(NovelTag(name = "R-18", addedByUploadedUser = false)),
        )
        assertTrue(tagR18Novel.isR18())

        val translatedTagNovel = createDummyNovel(
            tags = listOf(NovelTag(name = "小説", translatedName = "18禁", addedByUploadedUser = false)),
        )
        assertTrue(translatedTagNovel.isR18())
    }

    @Test
    fun testSettingsRepositoryHIsNotAllowTriggersNotifyChanged() {
        val mapSettings = MapSettings()
        val repo = SettingsRepository(mapSettings)
        val initialVersion = repo.changeVersion

        repo.hIsNotAllow = true
        assertEquals(initialVersion + 1, repo.changeVersion)
        assertTrue(repo.hIsNotAllow)

        repo.hIsNotAllow = false
        assertEquals(initialVersion + 2, repo.changeVersion)
        assertFalse(repo.hIsNotAllow)
    }

    @Test
    fun testSettingsRepositoryBanAIIllustAndNsfwMaskTriggerNotifyChanged() {
        val mapSettings = MapSettings()
        val repo = SettingsRepository(mapSettings)
        val initialVersion = repo.changeVersion

        repo.banAIIllust = true
        assertEquals(initialVersion + 1, repo.changeVersion)

        repo.nsfwMask = true
        assertEquals(initialVersion + 2, repo.changeVersion)

        repo.feedAIBadge = false
        assertEquals(initialVersion + 3, repo.changeVersion)
    }
}
