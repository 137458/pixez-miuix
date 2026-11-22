package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Novelpersist 库 1 → 2 迁移测试。
 *
 * 旧版（v1）库只有小说浏览历史表，v2 新增阅读进度表。Windows 宿主的
 * `verify<Variant>NovelPersistDatabaseMigration` 任务被 onlyIf 跳过，
 * 这里直接用内存驱动执行 [NovelPersistDatabase.Schema.migrate]，
 * 校验迁移脚本本身可执行、旧数据不丢、新表可读写。
 */
class NovelPersistMigrationTest {

    private lateinit var driver: JdbcSqliteDriver

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    /** 按旧版 Flutter 结构重建 v1 库。 */
    private fun createVersion1Schema() {
        driver.execute(
            null,
            """
            CREATE TABLE novelpersist (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                novel_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                picture_url TEXT NOT NULL,
                title TEXT NOT NULL,
                user_name TEXT NOT NULL,
                time INTEGER NOT NULL
            )
            """.trimIndent(),
            0,
        )
        driver.execute(null, "CREATE INDEX idx_novelpersist_novel_id ON novelpersist(novel_id)", 0)
        driver.execute(null, "CREATE INDEX idx_novelpersist_time ON novelpersist(time)", 0)
    }

    private fun executeSql(sql: String) {
        driver.execute(null, sql, 0)
    }

    private fun queryLong(sql: String): Long {
        var value = 0L
        driver.executeQuery(
            null,
            sql,
            { cursor ->
                if (cursor.next().value) value = cursor.getLong(0) ?: 0L
                QueryResult.Unit
            },
            0,
        )
        return value
    }

    @Test
    fun migrateFromVersion1KeepsHistoryAndCreatesProgressTable() {
        createVersion1Schema()
        executeSql(
            "INSERT INTO novelpersist (novel_id, user_id, picture_url, title, user_name, time) " +
                "VALUES (11, 22, 'p', 't', 'u', 33)",
        )

        NovelPersistDatabase.Schema.migrate(driver, 1L, 2L)

        val history = NovelPersistDatabase(driver).novelPersistQueries.selectAllDesc(10).executeAsList()
        assertEquals(1, history.size)
        assertEquals(11L, history.single().novel_id)

        executeSql(
            "INSERT INTO novel_reading_progress (novel_id, block_index, block_offset, updated_at) " +
                "VALUES (11, 7, 120, 99)",
        )
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM novel_reading_progress"))
        assertEquals(
            7L,
            queryLong("SELECT block_index FROM novel_reading_progress WHERE novel_id = 11"),
        )
    }

    @Test
    fun migrateFromVersion1IsIdempotentForExistingData() {
        createVersion1Schema()
        executeSql(
            "INSERT INTO novelpersist (novel_id, user_id, picture_url, title, user_name, time) " +
                "VALUES (11, 22, 'p', 't', 'u', 33)",
        )

        NovelPersistDatabase.Schema.migrate(driver, 1L, 2L)

        // 迁移不得重复插入或清空既有历史
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM novelpersist"))
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM novel_reading_progress"))
    }
}
