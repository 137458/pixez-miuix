package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BanRepositoryTest {

    private lateinit var illustDriver: JdbcSqliteDriver
    private lateinit var userDriver: JdbcSqliteDriver
    private lateinit var tagDriver: JdbcSqliteDriver
    private lateinit var repository: BanRepository

    @Before
    fun setUp() {
        illustDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        userDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        tagDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        BanIllustIdDatabase.Schema.create(illustDriver)
        BanUserIdDatabase.Schema.create(userDriver)
        BanTagDatabase.Schema.create(tagDriver)

        repository = BanRepository(illustDriver, userDriver, tagDriver)
    }

    @After
    fun tearDown() {
        illustDriver.close()
        userDriver.close()
        tagDriver.close()
    }

    private fun createMockIllust(
        id: Int,
        userId: Int = 100,
        tags: List<String> = emptyList(),
        isAi: Boolean = false,
        xRestrict: Int = 0,
    ): Illust {
        return Illust(
            id = id,
            title = "Test Illust $id",
            type = "illust",
            imageUrls = ImageUrls(squareMedium = "", medium = "", large = ""),
            caption = "caption",
            restrict = 0,
            user = IllustUser(
                id = userId,
                name = "Artist $userId",
                account = "artist_$userId",
                profileImageUrls = IllustProfileImageUrls(medium = ""),
            ),
            tags = tags.map { IllustTag(name = it, translatedName = null) },
            tools = emptyList(),
            createDate = "2026-01-01",
            pageCount = 1,
            width = 1000,
            height = 1000,
            sanityLevel = 2,
            xRestrict = xRestrict,
            metaSinglePage = null,
            metaPages = emptyList(),
            totalView = 100,
            totalBookmarks = 50,
            isBookmarked = false,
            visible = true,
            isMuted = false,
            illustAIType = if (isAi) 2 else 0,
        )
    }

    @Test
    fun `test ban illust id and in-memory cache`() = runBlocking {
        assertFalse(repository.isBanIllust(10001))
        assertTrue(repository.getBannedIllustIds().isEmpty())

        repository.insertBanIllust(10001, "Test Work")
        assertTrue(repository.isBanIllust(10001))
        assertEquals(setOf(10001), repository.getBannedIllustIds())

        // Cached hit
        assertTrue(repository.isBanIllust(10001))

        repository.clearAllBanIllusts()
        assertFalse(repository.isBanIllust(10001))
        assertTrue(repository.getBannedIllustIds().isEmpty())
    }

    @Test
    fun `test ban user id`() = runBlocking {
        repository.insertBanUser(20002, "Banned User")
        assertTrue(repository.isBanUser(20002))
        assertEquals(setOf(20002), repository.getBannedUserIds())

        val allUsers = repository.getAllBanUsers()
        assertEquals(1, allUsers.size)
        assertEquals("20002", allUsers.first().userId)
    }

    @Test
    fun `test ban tag literal and regex`() = runBlocking {
        repository.insertBanTag("testTag", "测试标签")
        repository.insertBanTag("r'^bad_.*'", "正则过滤")

        val tags = repository.getAllBanTags()
        assertEquals(2, tags.size)

        val literalTag = tags.first { it.name == "testTag" }
        assertFalse(literalTag.isRegexMatcher)

        val regexTag = tags.first { it.name.startsWith("r'") }
        assertTrue(regexTag.isRegexMatcher)
        assertNotNull(regexTag.regex)
        assertTrue(regexTag.regex!!.containsMatchIn("bad_content"))
        assertFalse(regexTag.regex!!.containsMatchIn("good_content"))
    }

    @Test
    fun `test filterIllusts with multiple criteria`() = runBlocking {
        repository.insertBanIllust(1, "Ban Illust 1")
        repository.insertBanUser(999, "Ban Artist 999")
        repository.insertBanTag("r'^guro.*'", "Guro Regex")
        repository.insertBanTag("spoiler", "Spoiler Tag")

        val illusts = listOf(
            createMockIllust(id = 1, userId = 100), // Banned illust id
            createMockIllust(id = 2, userId = 999), // Banned user id
            createMockIllust(id = 3, userId = 100, tags = listOf("guro_art")), // Banned by regex tag
            createMockIllust(id = 4, userId = 100, tags = listOf("spoiler")), // Banned by literal tag
            createMockIllust(id = 5, userId = 100, isAi = true), // AI illust
            createMockIllust(id = 6, userId = 100, xRestrict = 1), // R18 illust
            createMockIllust(id = 7, userId = 100, tags = listOf("safe", "landscape")), // Safe
        )

        // Filter without banAI or hideR18
        val result1 = repository.filterIllusts(illusts, banAIIllust = false, hideR18 = false)
        assertEquals(listOf(5, 6, 7), result1.map { it.id })

        // Filter with banAI and hideR18
        val result2 = repository.filterIllusts(illusts, banAIIllust = true, hideR18 = true)
        assertEquals(listOf(7), result2.map { it.id })
    }

    private fun assertNotNull(value: Any?) {
        assertTrue(value != null)
    }
}
