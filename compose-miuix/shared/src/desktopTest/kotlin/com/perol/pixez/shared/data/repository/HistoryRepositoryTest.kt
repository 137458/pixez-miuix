package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 插画浏览历史仓储测试：验证记录、去重前置、分页上限、单删、清空与导入替换的外部行为。
 */
class HistoryRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        IllustPersistDatabase.Schema.create(driver)
        repository = HistoryRepository(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun illust(id: Int, title: String = "title-$id", medium: String = "https://img/$id.jpg") = Illust(
        id = id,
        title = title,
        type = "illust",
        imageUrls = ImageUrls(squareMedium = "s", medium = medium, large = "l"),
        restrict = 0,
        user = IllustUser(
            id = 900 + id,
            name = "user-$id",
            account = "acc-$id",
            profileImageUrls = IllustProfileImageUrls(medium = "pm"),
        ),
        tags = emptyList(),
        createDate = "2026-01-01T00:00:00+09:00",
        pageCount = 1,
        width = 100,
        height = 100,
        sanityLevel = 2,
        xRestrict = 0,
        metaPages = emptyList(),
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = 0,
    )

    @Test
    fun `insert 后 getAll 按时间降序返回并映射字段`() = runBlocking {
        repository.insert(illust(1))
        Thread.sleep(2)
        repository.insert(illust(2))

        val items = repository.getAll()

        assertEquals(2, items.size)
        assertEquals(2L, items[0].illustId)
        assertEquals(1L, items[1].illustId)
        val first = items[0]
        assertEquals("title-2", first.title)
        assertEquals("user-2", first.userName)
        assertEquals(902L, first.userId)
        assertEquals("https://img/2.jpg", first.pictureUrl)
        assertTrue(first.time >= items[1].time)
    }

    @Test
    fun `重复浏览同一作品前置且不产生重复记录`() = runBlocking {
        repository.insert(illust(1))
        Thread.sleep(2)
        repository.insert(illust(2))
        Thread.sleep(2)
        repository.insert(illust(1))

        val items = repository.getAll()

        assertEquals(2, items.size, "同作品重复浏览应复用一条记录")
        assertEquals(1L, items[0].illustId, "重复浏览后应前置到最新位置")
        assertEquals(2L, items[1].illustId)
    }

    @Test
    fun `getAll 的 limit 上限生效`() = runBlocking {
        for (id in 1..5) {
            repository.insert(illust(id))
            Thread.sleep(1)
        }

        val limited = repository.getAll(limit = 3)

        assertEquals(3, limited.size)
        assertEquals(5L, limited.first().illustId, "limit 应保留最新的记录")
        assertEquals(3L, limited.last().illustId)
    }

    @Test
    fun `deleteById 仅删除目标主键行`() = runBlocking {
        repository.insert(illust(1))
        Thread.sleep(2)
        repository.insert(illust(2))
        val target = repository.getAll().first { it.illustId == 1L }

        repository.deleteById(target.id)

        val items = repository.getAll()
        assertEquals(1, items.size)
        assertEquals(2L, items.single().illustId)
    }

    @Test
    fun `clearAll 清空全部历史`() = runBlocking {
        repository.insert(illust(1))
        repository.insert(illust(2))

        repository.clearAll()

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `replaceAll 原子替换且保留导入 id 与字段`() = runBlocking {
        repository.insert(illust(1))
        val imported = listOf(
            HistoryItem(
                id = 5L,
                illustId = 77L,
                userId = 88L,
                pictureUrl = "p",
                title = null,
                userName = null,
                time = 1234567890L,
            ),
            HistoryItem(
                id = 9L,
                illustId = 66L,
                userId = 55L,
                pictureUrl = "p2",
                title = "t2",
                userName = "u2",
                time = 987654321L,
            ),
        )

        repository.replaceAll(imported)

        val items = repository.getAll()
        assertEquals(2, items.size, "导入应整体替换而非叠加既有记录")
        assertEquals(listOf(5L, 9L), items.map { it.id }.sorted())
        val withNulls = items.single { it.id == 5L }
        assertEquals(77L, withNulls.illustId)
        assertEquals(88L, withNulls.userId)
        assertEquals(null, withNulls.title, "导入数据的空标题应原样保留为 null")
        assertEquals(1234567890L, withNulls.time)
    }
}
