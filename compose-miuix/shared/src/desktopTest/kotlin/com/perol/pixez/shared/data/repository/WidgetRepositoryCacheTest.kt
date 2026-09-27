package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.glanceillustpersist.GlanceIllustPersistDatabase
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 小组件缓存策略测试：
 * 1. selectByType 按 ctime 倒序返回最新行（修复旧实现取最旧行导致缓存永不刷新）；
 * 2. deleteByType 仅清理同类型行；
 * 3. 未过期缓存直接返回且不触发网络拉取。
 */
class WidgetRepositoryCacheTest {

    private lateinit var testRoot: File
    private lateinit var repository: WidgetRepository
    private lateinit var node: Preferences

    @Before
    fun setUp() {
        testRoot = File(System.getProperty("java.io.tmpdir"), "widget-cache-test-${System.nanoTime()}")
        testRoot.mkdirs()
        System.setProperty("pixez.test.db.root", testRoot.absolutePath)
        node = Preferences.userRoot().node("com/perol/pixez/test/widget-cache")
        node.clear()
        repository = WidgetRepository(
            driverFactory = DriverFactory(),
            settingsRepository = SettingsRepository(PreferencesSettings(node)),
        )
    }

    @After
    fun tearDown() {
        System.clearProperty("pixez.test.db.root")
        testRoot.deleteRecursively()
    }

    private fun openDb(): JdbcSqliteDriver =
        JdbcSqliteDriver("jdbc:sqlite:${File(File(testRoot, "databases"), "glance_illust_persist.db").absolutePath}")

    private fun insertRow(db: JdbcSqliteDriver, id: Long?, illustId: Long, ctype: String, ctime: Long) {
        GlanceIllustPersistDatabase(db).glanceIllustPersistQueries.insertOrReplace(
            id = id,
            illust_id = illustId,
            user_id = 1,
            picture_url = "p",
            title = "t",
            user_name = "u",
            ctype = ctype,
            original_url = "o",
            large_url = "l",
            ctime = ctime,
        )
    }

    @Test
    fun `selectByType 按 ctime 倒序返回最新缓存行`() {
        val db = openDb()
        insertRow(db, id = null, illustId = 1, ctype = "recom", ctime = 100)
        insertRow(db, id = null, illustId = 2, ctype = "recom", ctime = 300)
        insertRow(db, id = null, illustId = 3, ctype = "recom", ctime = 200)

        val rows = GlanceIllustPersistDatabase(db).glanceIllustPersistQueries.selectByType("recom").executeAsList()

        assertEquals(listOf(2L, 3L, 1L), rows.map { it.illust_id }, "ctime 倒序：最新行排最前，firstOrNull 即最新缓存")
        db.close()
    }

    @Test
    fun `deleteByType 仅清理同类型行`() {
        val db = openDb()
        insertRow(db, id = null, illustId = 1, ctype = "recom", ctime = 100)
        insertRow(db, id = null, illustId = 2, ctype = "recom", ctime = 200)
        insertRow(db, id = null, illustId = 3, ctype = "rank", ctime = 300)

        GlanceIllustPersistDatabase(db).glanceIllustPersistQueries.deleteByType("recom")

        val remain = GlanceIllustPersistDatabase(db).glanceIllustPersistQueries.selectAll().executeAsList()
        assertEquals(listOf(3L), remain.map { it.illust_id }, "其他类型的缓存行不应被清理")
        db.close()
    }

    @Test
    fun `未过期缓存直接返回且不触发网络拉取`() = runBlocking {
        val now = System.currentTimeMillis()
        val db = openDb()
        insertRow(db, id = null, illustId = 42, ctype = "recom", ctime = now)
        db.close()

        val result = repository.getOrFetchWidgetIllust("recom")

        assertEquals(42L, result?.illust_id, "未过期缓存应直接命中且无需网络拉取")
    }

    @Test
    fun `同类型新旧行共存时返回最新一行`() = runBlocking {
        val now = System.currentTimeMillis()
        val db = openDb()
        // 旧行（过期）与新行并存：命中判断取最新行 ctime，未过期则直接返回新行
        insertRow(db, id = null, illustId = 1, ctype = "recom", ctime = now - 48 * 60 * 60 * 1000)
        insertRow(db, id = null, illustId = 2, ctype = "recom", ctime = now)
        db.close()

        val result = repository.getOrFetchWidgetIllust("recom")

        assertEquals(2L, result?.illust_id, "应返回 ctime 最新的缓存行")
        assertTrue(result != null)
    }
}
