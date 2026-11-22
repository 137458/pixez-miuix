package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.NovelImageUrls
import com.perol.pixez.shared.data.model.NovelProfileImageUrls
import com.perol.pixez.shared.data.model.NovelUser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 小说阅读进度读写测试：覆盖空进度、覆盖写、跨小说隔离、单本清除，
 * 以及与浏览历史（insert / clearAll）互不干扰。
 */
class NovelReadingProgressTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: NovelHistoryRepository

    private var fakeNowMillis = 1_000L

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NovelPersistDatabase.Schema.create(driver)
        fakeNowMillis = 1_000L
        repository = NovelHistoryRepository(driver) { fakeNowMillis++ }
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun novel(id: Long) = Novel(
        id = id,
        title = "title-$id",
        restrict = 0,
        xRestrict = 0,
        isOriginal = false,
        imageUrls = NovelImageUrls(
            squareMedium = "https://img/$id.jpg",
            medium = "https://img/$id-m.jpg",
            large = "https://img/$id-l.jpg",
        ),
        createDate = "2026-01-01T00:00:00+09:00",
        tags = emptyList(),
        pageCount = 1,
        textLength = 100,
        user = NovelUser(
            id = 900L + id,
            name = "user-$id",
            account = "acc-$id",
            profileImageUrls = NovelProfileImageUrls(medium = "pm"),
            isFollowed = false,
        ),
        isBookmarked = false,
        visible = true,
        isMuted = false,
        isMypixivOnly = false,
        isXRestricted = false,
        novelAIType = 0,
    )

    @Test
    fun readingProgressIsNullWhenNeverSaved() = runBlocking {
        assertNull(repository.getReadingProgress(42L))
    }

    @Test
    fun savedReadingProgressRoundTrips() = runBlocking {
        repository.saveReadingProgress(novelId = 42L, blockIndex = 7, blockOffset = 123)

        val progress = repository.getReadingProgress(42L)
        assertNotNull(progress)
        assertEquals(42L, progress.novelId)
        assertEquals(7, progress.blockIndex)
        assertEquals(123, progress.blockOffset)
        assertEquals(1_000L, progress.updatedAt)
    }

    @Test
    fun savingAgainOverwritesPreviousPosition() = runBlocking {
        repository.saveReadingProgress(novelId = 42L, blockIndex = 7, blockOffset = 123)
        repository.saveReadingProgress(novelId = 42L, blockIndex = 19, blockOffset = 0)

        val progress = repository.getReadingProgress(42L)
        assertNotNull(progress)
        assertEquals(19, progress.blockIndex)
        assertEquals(0, progress.blockOffset)
        // 第二次写入使用递增时钟，说明记录被覆盖而不是保留旧值
        assertEquals(1_001L, progress.updatedAt)
    }

    @Test
    fun readingProgressIsIsolatedPerNovel() = runBlocking {
        repository.saveReadingProgress(novelId = 1L, blockIndex = 3, blockOffset = 10)
        repository.saveReadingProgress(novelId = 2L, blockIndex = 8, blockOffset = 20)

        assertEquals(3, repository.getReadingProgress(1L)?.blockIndex)
        assertEquals(8, repository.getReadingProgress(2L)?.blockIndex)
    }

    @Test
    fun clearReadingProgressRemovesOnlyThatNovel() = runBlocking {
        repository.saveReadingProgress(novelId = 1L, blockIndex = 3, blockOffset = 10)
        repository.saveReadingProgress(novelId = 2L, blockIndex = 8, blockOffset = 20)

        repository.clearReadingProgress(1L)

        assertNull(repository.getReadingProgress(1L))
        assertEquals(8, repository.getReadingProgress(2L)?.blockIndex)
    }

    @Test
    fun historyWritesKeepReadingProgress() = runBlocking {
        repository.saveReadingProgress(novelId = 42L, blockIndex = 5, blockOffset = 60)

        repository.insert(novel(42L))

        val progress = repository.getReadingProgress(42L)
        assertNotNull(progress)
        assertEquals(5, progress.blockIndex)
        assertEquals(60, progress.blockOffset)
    }

    @Test
    fun clearingHistoryKeepsReadingProgress() = runBlocking {
        repository.insert(novel(42L))
        repository.saveReadingProgress(novelId = 42L, blockIndex = 5, blockOffset = 60)

        repository.clearAll()

        assertNull(repository.getReadingProgress(99L))
        assertEquals(5, repository.getReadingProgress(42L)?.blockIndex)
    }
}
