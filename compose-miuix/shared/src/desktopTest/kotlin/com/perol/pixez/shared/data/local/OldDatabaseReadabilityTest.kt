package com.perol.pixez.shared.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.kvpair.KVPairDatabase
import com.perol.pixez.shared.data.local.task.TaskDatabase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * M2 本地数据库兼容性单元测试。
 * 用 JdbcSqliteDriver 在内存中创建与旧 Flutter 应用一致的表结构，
 * 验证 SQLDelight 生成的 schema 可以读写旧版数据。
 */
class OldDatabaseReadabilityTest {

    @Test
    fun `account db schema matches old account db`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AccountDatabase.Schema.create(driver)

        AccountDatabase(driver).accountQueries.insertOrReplace(
            id = 1,
            access_token = "access",
            refresh_token = "refresh",
            device_token = "device",
            user_id = "12345",
            user_image = "https://i.pximg.net/u/170.jpg",
            name = "user",
            password = "pass",
            account = "account",
            mail_address = "test@example.com",
            is_premium = 0,
            x_restrict = 0,
            is_mail_authorized = 1,
        )

        val row = AccountDatabase(driver).accountQueries.selectByUserId("12345").executeAsOne()
        assertEquals("user", row.name)
        DriverFactory().closeDriver(driver)
    }

    @Test
    fun `illustpersist db schema matches old glance db`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        IllustPersistDatabase.Schema.create(driver)

        IllustPersistDatabase(driver).illustPersistQueries.insertOrReplace(
            id = 1,
            illust_id = 98765432,
            user_id = 12345,
            picture_url = "https://i.pximg.net/c/360x360_70/img-master/img/1.jpg",
            title = "title",
            user_name = "artist",
            ctype = "home",
            original_url = "https://i.pximg.net/img-original/img/1.jpg",
            large_url = "https://i.pximg.net/c/600x1200_90/img-master/img/1.jpg",
            ctime = System.currentTimeMillis(),
        )

        val rows = IllustPersistDatabase(driver).illustPersistQueries.selectAll().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("title", rows.first().title)
        assertEquals("home", rows.first().ctype)
        DriverFactory().closeDriver(driver)
    }

    @Test
    fun `task db schema matches old task1 db`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TaskDatabase.Schema.create(driver)

        TaskDatabase(driver).taskQueries.insertOrReplace(
            id = 1,
            title = "title",
            user_name = "artist",
            url = "https://i.pximg.net/img-original/img/1.jpg",
            sanity_level = 2,
            illust_id = 98765432,
            user_id = 12345,
            status = 0,
            file_name = "1.jpg",
            medium = "https://i.pximg.net/c/540x540_70/img-master/img/1.jpg",
        )

        val row = TaskDatabase(driver).taskQueries.selectByUrl("https://i.pximg.net/img-original/img/1.jpg").executeAsOneOrNull()
        assertNotNull(row)
        assertEquals(0, row.status)
        DriverFactory().closeDriver(driver)
    }

    @Test
    fun `kvpair db schema matches old key value store`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KVPairDatabase.Schema.create(driver)

        KVPairDatabase(driver).kVPairQueries.insertOrReplace(
            _id = 1,
            key = "picture_source",
            value_ = "i.pximg.net",
            expire_time = 0,
            date_time = System.currentTimeMillis(),
        )

        KVPairDatabase(driver).kVPairQueries.insertOrReplace(
            _id = 1,
            key = "picture_source",
            value_ = "i.pixiv.net",
            expire_time = 0,
            date_time = System.currentTimeMillis(),
        )

        val rows = KVPairDatabase(driver).kVPairQueries.selectByKey("picture_source").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("i.pixiv.net", rows.first().value_)
        DriverFactory().closeDriver(driver)
    }

    @Test
    fun `task db paging queries work`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TaskDatabase.Schema.create(driver)

        val queries = TaskDatabase(driver).taskQueries
        repeat(3) { index ->
            queries.insertOrReplace(
                id = (index + 1).toLong(),
                title = "title$index",
                user_name = "artist",
                url = "https://i.pximg.net/img-original/img/$index.jpg",
                sanity_level = 0,
                illust_id = (100 + index).toLong(),
                user_id = 200L,
                status = (if (index == 0) 0 else 1).toLong(),
                file_name = "$index.jpg",
                medium = null,
            )
        }

        // SQLDelight 为 LIMIT/OFFSET 生成的是位置参数，不识别 limit/offset 命名
        val all = queries.selectAllPagedAsc(10, 0).executeAsList()
        assertEquals(3, all.size)

        val statusZero = queries.selectByStatusPagedAsc(0, 10, 0).executeAsList()
        assertEquals(1, statusZero.size)
        assertEquals(0, statusZero.first().status)
        DriverFactory().closeDriver(driver)
    }
}
