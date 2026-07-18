package com.perol.pixez.shared.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.kvpair.KVPairDatabase
import com.perol.pixez.shared.data.local.task.TaskDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M2 旧 Flutter 数据库迁移兼容性测试。
 *
 * 旧 Flutter 应用使用 sqflite / sqflite_common_ffi 创建数据库时通常不设置 `PRAGMA user_version`，
 * 本测试在临时目录中创建与旧版一致的数据库文件（user_version = 0 但表已存在），
 * 验证 [DriverFactory] 打开时不会重复执行 CREATE TABLE，且能正常读取旧数据。
 */
class DriverFactoryMigrationTest {

    private lateinit var tempRoot: File

    @Before
    fun setup() {
        tempRoot = Files.createTempDirectory("pixez-migration-test-").toFile().also { it.deleteOnExit() }
        System.setProperty(TEST_DB_ROOT_PROPERTY, tempRoot.absolutePath)
    }

    @After
    fun tearDown() {
        System.clearProperty(TEST_DB_ROOT_PROPERTY)
        tempRoot.deleteRecursively()
    }

    @Test
    fun `legacy task1 db can be opened without recreate`() {
        val dbFile = dbFile("task1.db")

        // 创建与旧 Flutter 一致的 task 表，不设置 user_version
        val legacyDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        legacyDriver.execute(null, LEGACY_TASK_SCHEMA, 0, null)
        legacyDriver.execute(null, INSERT_TASK, 9) {
            bindString(0, "title")
            bindString(1, "artist")
            bindString(2, "https://i.pximg.net/img-original/img/1.jpg")
            bindLong(3, 2)
            bindLong(4, 98765432)
            bindLong(5, 12345)
            bindLong(6, 0)
            bindString(7, "1.jpg")
            bindString(8, "https://i.pximg.net/c/540x540_70/img-master/img/1.jpg")
        }
        legacyDriver.close()

        // 用 DriverFactory 打开旧数据库
        val factory = DriverFactory()
        val driver = factory.createDriver(TaskDatabase.Schema, dbFile.name)
        val queries = TaskDatabase(driver).taskQueries

        val row = queries.selectByUrl("https://i.pximg.net/img-original/img/1.jpg").executeAsOneOrNull()
        assertNotNull(row)
        assertEquals("artist", row.user_name)
        assertEquals(0, row.status)

        factory.closeDriver(driver)
    }

    @Test
    fun `legacy glanceillustpersist db can be opened without recreate`() {
        val dbFile = dbFile("glanceillustpersist.db")

        val legacyDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        legacyDriver.execute(null, LEGACY_GLANCE_SCHEMA, 0, null)
        legacyDriver.execute(null, INSERT_GLANCE, 9) {
            bindLong(0, 98765432)
            bindLong(1, 12345)
            bindString(2, "https://i.pximg.net/c/360x360_70/img-master/img/1.jpg")
            bindString(3, "home")
            bindString(4, "title")
            bindString(5, "https://i.pximg.net/c/600x1200_90/img-master/img/1.jpg")
            bindString(6, "https://i.pximg.net/img-original/img/1.jpg")
            bindString(7, "artist")
            bindLong(8, System.currentTimeMillis())
        }
        legacyDriver.close()

        val factory = DriverFactory()
        val driver = factory.createDriver(IllustPersistDatabase.Schema, dbFile.name)
        val queries = IllustPersistDatabase(driver).illustPersistQueries

        val rows = queries.selectAll().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("title", rows.first().title)
        assertEquals("home", rows.first().ctype)

        factory.closeDriver(driver)
    }

    @Test
    fun `legacy account db can be opened without recreate`() {
        val dbFile = dbFile("account.db")

        val legacyDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        legacyDriver.execute(null, LEGACY_ACCOUNT_SCHEMA, 0, null)
        legacyDriver.execute(null, INSERT_ACCOUNT, 12) {
            bindString(0, "access")
            bindString(1, "refresh")
            bindString(2, "device")
            bindString(3, "12345")
            bindString(4, "https://i.pximg.net/u/170.jpg")
            bindString(5, "user")
            bindString(6, "pass")
            bindString(7, "account")
            bindString(8, "test@example.com")
            bindLong(9, 0)
            bindLong(10, 0)
            bindLong(11, 1)
        }
        legacyDriver.close()

        val factory = DriverFactory()
        val driver = factory.createDriver(AccountDatabase.Schema, dbFile.name)
        val row = AccountDatabase(driver).accountQueries.selectByUserId("12345").executeAsOne()
        assertEquals("user", row.name)

        factory.closeDriver(driver)
    }

    @Test
    fun `legacy kvpair db can be opened without recreate`() {
        val dbFile = dbFile("kvpair.db")

        val legacyDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        legacyDriver.execute(null, LEGACY_KVPAIR_SCHEMA, 0, null)
        legacyDriver.execute(null, INSERT_KVPAIR, 4) {
            bindString(0, "picture_source")
            bindString(1, "i.pximg.net")
            bindLong(2, 0)
            bindLong(3, System.currentTimeMillis())
        }
        legacyDriver.close()

        val factory = DriverFactory()
        val driver = factory.createDriver(KVPairDatabase.Schema, dbFile.name)
        val rows = KVPairDatabase(driver).kVPairQueries.selectByKey("picture_source").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("i.pximg.net", rows.first().value_)

        factory.closeDriver(driver)
    }

    @Test
    fun `empty database file is recreated with current schema`() {
        val dbFile = dbFile("empty.db")
        dbFile.createNewFile()
        assertTrue(dbFile.exists())

        val factory = DriverFactory()
        val driver = factory.createDriver(TaskDatabase.Schema, dbFile.name)
        val queries = TaskDatabase(driver).taskQueries

        // 空文件应被重新初始化为当前 schema，可以正常写入
        queries.insertOrReplace(
            id = 1,
            title = "title",
            user_name = "artist",
            url = "https://i.pximg.net/img-original/img/1.jpg",
            sanity_level = 0,
            illust_id = 98765432,
            user_id = 12345,
            status = 0,
            file_name = "1.jpg",
            medium = null,
        )
        assertEquals(1, queries.selectAll().executeAsList().size)

        factory.closeDriver(driver)
    }

    private fun dbFile(name: String): File = File(File(tempRoot, "databases").apply { mkdirs() }, name)

    companion object {
        private const val TEST_DB_ROOT_PROPERTY = "pixez.test.db.root"

        private const val LEGACY_TASK_SCHEMA = """
            CREATE TABLE task (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                user_name TEXT NOT NULL,
                url TEXT NOT NULL,
                sanity_level INTEGER,
                illust_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                status INTEGER NOT NULL,
                file_name TEXT NOT NULL,
                medium TEXT
            )
        """

        private const val INSERT_TASK = """
            INSERT INTO task (title, user_name, url, sanity_level, illust_id, user_id, status, file_name, medium)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val LEGACY_GLANCE_SCHEMA = """
            CREATE TABLE glanceillustpersist (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                illust_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                picture_url TEXT NOT NULL,
                ctype TEXT NOT NULL,
                title TEXT,
                large_url TEXT,
                original_url TEXT,
                user_name TEXT,
                ctime INTEGER NOT NULL
            )
        """

        private const val INSERT_GLANCE = """
            INSERT INTO glanceillustpersist (illust_id, user_id, picture_url, ctype, title, large_url, original_url, user_name, ctime)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val LEGACY_ACCOUNT_SCHEMA = """
            CREATE TABLE account (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                access_token TEXT NOT NULL,
                refresh_token TEXT NOT NULL,
                device_token TEXT NOT NULL,
                user_id TEXT NOT NULL,
                user_image TEXT NOT NULL,
                name TEXT NOT NULL,
                password TEXT NOT NULL,
                account TEXT NOT NULL,
                mail_address TEXT NOT NULL,
                is_premium INTEGER NOT NULL,
                x_restrict INTEGER NOT NULL,
                is_mail_authorized INTEGER NOT NULL
            )
        """

        private const val INSERT_ACCOUNT = """
            INSERT INTO account (access_token, refresh_token, device_token, user_id, user_image, name, password, account, mail_address, is_premium, x_restrict, is_mail_authorized)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val LEGACY_KVPAIR_SCHEMA = """
            CREATE TABLE kvpair (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT NOT NULL,
                value TEXT NOT NULL,
                expire_time INTEGER NOT NULL,
                date_time INTEGER NOT NULL
            )
        """

        private const val INSERT_KVPAIR = """
            INSERT INTO kvpair (key, value, expire_time, date_time)
            VALUES (?, ?, ?, ?)
        """
    }
}
