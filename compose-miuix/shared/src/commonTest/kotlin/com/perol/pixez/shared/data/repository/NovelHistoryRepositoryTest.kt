package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.NovelImageUrls
import com.perol.pixez.shared.data.model.NovelProfileImageUrls
import com.perol.pixez.shared.data.model.NovelUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 小说浏览历史仓储测试：验证阅读埋点（insert 去重前置）、降序查询、单删、清空
 * 与导入导出（getAll/replaceAll）往返、导入 id 保留及新旧方法互不破坏。
 */
class NovelHistoryRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: NovelHistoryRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NovelPersistDatabase.Schema.create(driver)
        repository = NovelHistoryRepository(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun novel(id: Long, title: String = "title-$id") = Novel(
        id = id,
        title = title,
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

    private fun item(id: Long?, novelId: Long, time: Long) = NovelHistoryItem(
        id = id,
        novelId = novelId,
        userId = 900L,
        pictureUrl = "p",
        title = "t-$novelId",
        userName = "u-$novelId",
        time = time,
    )

    // ── 阅读埋点：insert / getAllDesc ──

    @Test
    fun `insert 后 getAllDesc 按时间降序返回并映射字段`() = runBlocking {
        repository.insert(novel(1))
        delay(2)
        repository.insert(novel(2))

        val items = repository.getAllDesc()

        assertEquals(2, items.size)
        assertEquals(2L, items[0].novelId)
        assertEquals(1L, items[1].novelId)
        val first = items[0]
        assertEquals("title-2", first.title)
        assertEquals("user-2", first.userName)
        assertEquals(902L, first.userId)
        assertEquals("https://img/2.jpg", first.pictureUrl, "封面应取 squareMedium")
        assertTrue(first.time >= items[1].time)
    }

    @Test
    fun `重复浏览同一小说前置且不产生重复记录`() = runBlocking {
        repository.insert(novel(1))
        delay(2)
        repository.insert(novel(2))
        delay(2)
        repository.insert(novel(1))

        val items = repository.getAllDesc()

        assertEquals(2, items.size, "同小说重复浏览应复用一条记录")
        assertEquals(1L, items[0].novelId, "重复浏览后应前置到最新位置")
        assertEquals(2L, items[1].novelId)
    }

    @Test
    fun `getAllDesc 的 limit 上限生效`() = runBlocking {
        for (id in 1L..5L) {
            repository.insert(novel(id))
            delay(1)
        }

        val limited = repository.getAllDesc(limit = 3)

        assertEquals(3, limited.size)
        assertEquals(5L, limited.first().novelId, "limit 应保留最新的记录")
        assertEquals(3L, limited.last().novelId)
    }

    @Test
    fun `deleteById 仅删除目标主键行`() = runBlocking {
        repository.insert(novel(1))
        delay(2)
        repository.insert(novel(2))
        val target = repository.getAllDesc().first { it.novelId == 1L }
        val targetId = checkNotNull(target.id) { "insert 生成的历史记录应有自增主键" }

        repository.deleteById(targetId)

        val items = repository.getAllDesc()
        assertEquals(1, items.size)
        assertEquals(2L, items.single().novelId)
    }

    @Test
    fun `clearAll 清空全部历史`() = runBlocking {
        repository.insert(novel(1))
        repository.insert(novel(2))

        repository.clearAll()

        assertTrue(repository.getAllDesc().isEmpty())
    }

    // ── 导入导出（既有能力）与新方法互不破坏 ──

    @Test
    fun `replaceAll 保留导入 id 且字段往返完整`() = runBlocking {
        val imported = listOf(
            item(id = 3L, novelId = 11L, time = 200L),
            item(id = null, novelId = 12L, time = 100L),
        )

        repository.replaceAll(imported)

        val all = repository.getAll()
        assertEquals(2, all.size)
        assertEquals(3L, all.first { it.novelId == 11L }.id, "导入的显式 id 应原样保留")
        // 既有契约：无 id 导入按 id=0L 落库（INSERT OR REPLACE 语义，不生成自增 id）
        assertEquals(0L, all.first { it.novelId == 12L }.id)
        assertEquals("t-11", all.first { it.novelId == 11L }.title)
    }

    @Test
    fun `replaceAll 整体替换不叠加旧数据`() = runBlocking {
        repository.replaceAll(listOf(item(id = 1L, novelId = 1L, time = 1L)))
        repository.replaceAll(listOf(item(id = 2L, novelId = 2L, time = 2L)))

        val all = repository.getAll()

        assertEquals(1, all.size, "再次导入应清空旧记录后写入")
        assertEquals(2, all.single().novelId)
    }

    @Test
    fun `getAll 按时间升序且 limit 上限生效`() = runBlocking {
        // 显式 id 避免既有 id=0 折叠语义干扰本用例的行数断言
        val items = (1..5).map { item(id = it.toLong(), novelId = it.toLong(), time = it.toLong()) }
        repository.replaceAll(items)

        val limited = repository.getAll(limit = 2)

        assertEquals(2, limited.size, "limit 应截断返回行数")
        assertEquals(listOf(1L, 2L), limited.map { it.novelId }, "按时间升序保留最早的记录（与既有排序语义一致）")
    }

    @Test
    fun `空导入清空全部历史`() = runBlocking {
        repository.replaceAll(listOf(item(id = 1L, novelId = 1L, time = 1L)))

        repository.replaceAll(emptyList())

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `replaceAll 与 insert 互不破坏`() = runBlocking {
        repository.insert(novel(1))
        repository.replaceAll(
            listOf(
                item(id = 3L, novelId = 11L, time = 200L),
                item(id = 4L, novelId = 12L, time = 300L),
            ),
        )

        // 导入后新埋点仍可用，且按时间降序前置
        repository.insert(novel(2))

        val desc = repository.getAllDesc()
        assertEquals(3, desc.size, "导入数据与新埋点应共存")
        assertEquals(listOf(2L, 12L, 11L), desc.map { it.novelId }, "最新浏览应排在最前")
        // 导出查询（getAll 升序）仍返回全部记录
        assertEquals(3, repository.getAll().size)
    }
}
