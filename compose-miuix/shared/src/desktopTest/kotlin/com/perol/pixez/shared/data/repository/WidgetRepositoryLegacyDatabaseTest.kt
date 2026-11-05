package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 小组件库名一致性回归测试。
 *
 * 旧实现里小组件实开 `glance_illust_persist.db`，而旧库补列清单与 iOS App Group 特例按
 * `glanceillustpersist.db` 登记：旧 Flutter 用户的小组件库既读不到（数据仍在旧名文件里），
 * 缺列也永远不会补齐（补齐只对另一个文件生效）。本测试在旧名文件里放一行 v1 结构数据，
 * 断言小组件仓库打开后旧数据仍在且缺列已补齐。
 */
class WidgetRepositoryLegacyDatabaseTest {

    private lateinit var testRoot: File
    private lateinit var node: Preferences

    @Before
    fun setUp() {
        testRoot = File(System.getProperty("java.io.tmpdir"), "widget-legacy-db-test-${System.nanoTime()}")
        testRoot.mkdirs()
        System.setProperty("pixez.test.db.root", testRoot.absolutePath)
        node = Preferences.userRoot().node("com/perol/pixez/test/widget-legacy-db")
        node.clear()
    }

    @After
    fun tearDown() {
        System.clearProperty("pixez.test.db.root")
        testRoot.deleteRecursively()
    }

    private fun dbFile(name: String) = File(File(testRoot, "databases").apply { mkdirs() }, name)

    /** 旧 Flutter v1 结构：缺 original_url 与 large_url。 */
    private fun createLegacyGlanceDatabase(file: File) {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        driver.execute(null, LEGACY_GLANCE_V1_SCHEMA, 0, null)
        driver.execute(null, INSERT_LEGACY_GLANCE, 6) {
            bindLong(0, 98765432L)
            bindLong(1, 12345L)
            bindString(2, "https://i.pximg.net/c/360x360_70/img-master/img/1.jpg")
            bindString(3, "home")
            bindString(4, "旧标题")
            bindString(5, "旧画师")
        }
        driver.close()
    }

    private fun tableColumns(file: File, table: String): List<String> {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        val columns = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($table)",
            mapper = { cursor: SqlCursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names.add(cursor.getString(1).orEmpty())
                QueryResult.Value<List<String>>(names)
            },
            parameters = 0,
            binders = null,
        ).value
        driver.close()
        return columns
    }

    private fun rowCount(file: File, table: String): Long {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        val count = driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM $table",
            mapper = { cursor: SqlCursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
            binders = null,
        ).value
        driver.close()
        return count
    }

    @Test
    fun `小组件仓库打开旧库名文件并补齐缺列且保留旧缓存行`() {
        val legacyFile = dbFile("glanceillustpersist.db")
        createLegacyGlanceDatabase(legacyFile)

        val repository = WidgetRepository(
            driverFactory = DriverFactory(),
            settingsRepository = SettingsRepository(PreferencesSettings(node)),
        )
        try {
            val columns = tableColumns(legacyFile, "glanceillustpersist")
            assertTrue("original_url" in columns, "旧库应补齐 original_url 列: $columns")
            assertTrue("large_url" in columns, "旧库应补齐 large_url 列: $columns")
            assertEquals(1L, rowCount(legacyFile, "glanceillustpersist"), "旧库既有缓存行不应丢失")
        } finally {
            repository.close()
        }
    }

    private companion object {
        private const val LEGACY_GLANCE_V1_SCHEMA = """
            CREATE TABLE glanceillustpersist (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                illust_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                picture_url TEXT NOT NULL,
                ctype TEXT NOT NULL,
                title TEXT,
                user_name TEXT,
                ctime INTEGER NOT NULL
            )
        """

        private const val INSERT_LEGACY_GLANCE = """
            INSERT INTO glanceillustpersist (illust_id, user_id, picture_url, ctype, title, user_name, ctime)
            VALUES (?, ?, ?, ?, ?, ?, 0)
        """
    }
}
