package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 小说浏览历史仓储测试：验证 getAll/replaceAll 的往返、导入 id 保留与查询上限。
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

    private fun item(id: Long?, novelId: Int, time: Long) = NovelHistoryItem(
        id = id,
        novelId = novelId,
        userId = 900,
        pictureUrl = "p",
        title = "t-$novelId",
        userName = "u-$novelId",
        time = time,
    )

    @Test
    fun `replaceAll 保留导入 id 且字段往返完整`() = runBlocking {
        val imported = listOf(
            item(id = 3L, novelId = 11, time = 200L),
            item(id = null, novelId = 12, time = 100L),
        )

        repository.replaceAll(imported)

        val all = repository.getAll()
        assertEquals(2, all.size)
        assertEquals(3L, all.first { it.novelId == 11 }.id, "导入的显式 id 应原样保留")
        // 既有契约：无 id 导入按 id=0L 落库（INSERT OR REPLACE 语义，不生成自增 id）
        assertEquals(0L, all.first { it.novelId == 12 }.id)
        assertEquals("t-11", all.first { it.novelId == 11 }.title)
    }

    @Test
    fun `replaceAll 整体替换不叠加旧数据`() = runBlocking {
        repository.replaceAll(listOf(item(id = 1L, novelId = 1, time = 1L)))
        repository.replaceAll(listOf(item(id = 2L, novelId = 2, time = 2L)))

        val all = repository.getAll()

        assertEquals(1, all.size, "再次导入应清空旧记录后写入")
        assertEquals(2, all.single().novelId)
    }

    @Test
    fun `getAll 按时间升序且 limit 上限生效`() = runBlocking {
        // 显式 id 避免既有 id=0 折叠语义干扰本用例的行数断言
        val items = (1..5).map { item(id = it.toLong(), novelId = it, time = it.toLong()) }
        repository.replaceAll(items)

        val limited = repository.getAll(limit = 2)

        assertEquals(2, limited.size, "limit 应截断返回行数")
        assertEquals(listOf(1, 2), limited.map { it.novelId }, "按时间升序保留最早的记录（与既有排序语义一致）")
    }

    @Test
    fun `空导入清空全部历史`() = runBlocking {
        repository.replaceAll(listOf(item(id = 1L, novelId = 1, time = 1L)))

        repository.replaceAll(emptyList())

        assertTrue(repository.getAll().isEmpty())
    }
}
