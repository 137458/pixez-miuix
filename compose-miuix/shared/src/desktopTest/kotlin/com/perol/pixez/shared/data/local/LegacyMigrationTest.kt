package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.glanceillustpersist.GlanceIllustPersistDatabase
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.task.TaskDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 旧 Flutter 数据库（user_version=0 且表已存在）升级修复测试。
 *
 * 旧 v1 结构缺少 1.sqm 声明的列（illustpersist.title/user_name、task.medium、
 * glanceillustpersist.original_url/large_url），历史实现在该分支只同步版本号不补列，
 * 导致历史/下载/小组件页面 "no such column" 全挂。修复后按列探测补 ALTER。
 *
 * 回归只能走 desktopTest JVM 单测：VerifyMigrationTask 在 Windows 默认禁用。
 */
class LegacyMigrationTest {

    private lateinit var testRoot: File

    @Before
    fun setUp() {
        testRoot = File(System.getProperty("java.io.tmpdir"), "legacy-migration-test-${System.nanoTime()}")
        testRoot.mkdirs()
        System.setProperty("pixez.test.db.root", testRoot.absolutePath)
    }

    @After
    fun tearDown() {
        System.clearProperty("pixez.test.db.root")
        testRoot.deleteRecursively()
    }

    /** 在测试根目录构造一个旧 v1 结构的数据库文件（缺 1.sqm 新增列，user_version 保持 0）。 */
    private fun createLegacyV1Database(fileName: String, legacyDdl: List<String>) {
        val dbFile = File(File(testRoot, "databases"), fileName)
        dbFile.parentFile.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        legacyDdl.forEach { driver.execute(null, it, 0, null) }
        driver.close()
    }

    private fun readUserVersion(dbFile: File): Long {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        val version = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor: SqlCursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
            binders = null,
        ).value
        driver.close()
        return version
    }

    private fun tableColumns(dbFile: File, table: String): List<String> {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        val columns = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($table)",
            mapper = { cursor: SqlCursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) {
                    names.add(cursor.getString(1).orEmpty())
                }
                QueryResult.Value(names)
            },
            parameters = 0,
            binders = null,
        ).value
        driver.close()
        return columns
    }

    @Test
    fun `illustpersist 旧库补齐 title 与 user_name 列并写入版本号`() {
        createLegacyV1Database(
            "illustpersist.db",
            listOf(
                "CREATE TABLE illustpersist (id INTEGER PRIMARY KEY AUTOINCREMENT, illust_id INTEGER, user_id INTEGER, picture_url TEXT, time INTEGER)",
                "INSERT INTO illustpersist (illust_id, user_id, picture_url, time) VALUES (42, 7, 'p', 111)",
            ),
        )
        val factory = DriverFactory()

        val driver = factory.createDriver(IllustPersistDatabase.Schema, "illustpersist.db")

        val columns = tableColumns(File(File(testRoot, "databases"), "illustpersist.db"), "illustpersist")
        assertTrue("title" in columns, "应补齐 title 列: $columns")
        assertTrue("user_name" in columns, "应补齐 user_name 列: $columns")
        assertEquals(2L, readUserVersion(File(File(testRoot, "databases"), "illustpersist.db")), "版本号应同步到 schema 版本")

        // 旧数据仍在且可通过新列查询
        val row = IllustPersistDatabase(driver).illustPersistQueries.selectAll(limit = 1000).executeAsList().single()
        assertEquals(42L, row.illust_id)
        assertEquals("p", row.picture_url)
        factory.closeDriver(driver)
    }

    @Test
    fun `task 旧库补齐 medium 列`() {
        createLegacyV1Database(
            "task.db",
            listOf(
                "CREATE TABLE task (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, user_name TEXT NOT NULL, url TEXT NOT NULL, sanity_level INTEGER, illust_id INTEGER NOT NULL, user_id INTEGER NOT NULL, status INTEGER NOT NULL, file_name TEXT NOT NULL)",
                "INSERT INTO task (title, user_name, url, illust_id, user_id, status, file_name) VALUES ('t', 'u', 'url', 1, 2, 0, 'f')",
            ),
        )
        val factory = DriverFactory()

        val driver = factory.createDriver(TaskDatabase.Schema, "task.db")

        val columns = tableColumns(File(File(testRoot, "databases"), "task.db"), "task")
        assertTrue("medium" in columns, "应补齐 medium 列: $columns")
        assertEquals(1, TaskDatabase(driver).taskQueries.selectAll().executeAsList().size, "旧数据应保留")
        factory.closeDriver(driver)
    }

    @Test
    fun `glanceillustpersist 旧库补齐 original_url 与 large_url 列`() {
        createLegacyV1Database(
            "glanceillustpersist.db",
            listOf(
                "CREATE TABLE glanceillustpersist (id INTEGER PRIMARY KEY AUTOINCREMENT, illust_id INTEGER, user_id INTEGER, picture_url TEXT, title TEXT, user_name TEXT, ctype TEXT, ctime INTEGER)",
                "INSERT INTO glanceillustpersist (illust_id, user_id, picture_url, title, user_name, ctype, ctime) VALUES (1, 2, 'p', 't', 'u', 'home', 123)",
            ),
        )
        val factory = DriverFactory()

        val driver = factory.createDriver(GlanceIllustPersistDatabase.Schema, "glanceillustpersist.db")

        val columns = tableColumns(File(File(testRoot, "databases"), "glanceillustpersist.db"), "glanceillustpersist")
        assertTrue("original_url" in columns, "应补齐 original_url 列: $columns")
        assertTrue("large_url" in columns, "应补齐 large_url 列: $columns")
        factory.closeDriver(driver)
    }

    @Test
    fun `结构已完整的旧库迁移为无操作且数据保留`() {
        createLegacyV1Database(
            "illustpersist.db",
            listOf(
                "CREATE TABLE illustpersist (id INTEGER PRIMARY KEY AUTOINCREMENT, illust_id INTEGER, user_id INTEGER, picture_url TEXT, title TEXT, user_name TEXT, time INTEGER)",
                "INSERT INTO illustpersist (illust_id, user_id, picture_url, title, user_name, time) VALUES (42, 7, 'p', '标题', '画师', 111)",
            ),
        )
        val factory = DriverFactory()

        val driver = factory.createDriver(IllustPersistDatabase.Schema, "illustpersist.db")

        val row = IllustPersistDatabase(driver).illustPersistQueries.selectAll(limit = 1000).executeAsList().single()
        assertEquals("标题", row.title, "已有列的数据在迁移后不应丢失")
        assertEquals("画师", row.user_name)
        factory.closeDriver(driver)
    }
}
