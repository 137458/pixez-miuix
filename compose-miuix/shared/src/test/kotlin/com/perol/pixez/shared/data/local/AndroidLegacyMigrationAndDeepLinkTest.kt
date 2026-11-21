package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.navigation.DeepLinkParser
import com.perol.pixez.shared.navigation.ParsedDeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Android 端专属单元测试（运行于 androidUnitTest 变体）：
 * 1. 验证旧版 Flutter Android SQLite 表（user_version=0，缺少新列及索引）的列补齐、索引补建与幂等性。
 * 2. 验证 Android Manifest 放开 host 约束后，DeepLinkParser 的业务深链解析与 OAuth 白名单约束。
 */
class AndroidLegacyMigrationAndDeepLinkTest {

    @Test
    fun `旧版 Flutter illustpersist 与 task 数据库迁移补齐列和索引且可幂等重复执行`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            // 模拟 Android 端旧版 Flutter 建库（user_version=0，缺 title/user_name/medium 与查询索引）
            driver.execute(
                null,
                "CREATE TABLE illustpersist (id INTEGER PRIMARY KEY AUTOINCREMENT, illust_id INTEGER NOT NULL, user_id INTEGER NOT NULL, picture_url TEXT NOT NULL, time INTEGER NOT NULL)",
                0,
                null,
            )
            driver.execute(
                null,
                "CREATE TABLE task (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT, file_name TEXT, status INTEGER)",
                0,
                null,
            )

            val migrator = LegacyDatabaseMigrations.sqlDriverMigrator(driver)
            LegacyDatabaseMigrations.migrateLegacyDatabase("illustpersist.db", migrator)
            LegacyDatabaseMigrations.migrateLegacyDatabase("task.db", migrator)
            // 幂等重跑不得因重复列或索引报错
            LegacyDatabaseMigrations.migrateLegacyDatabase("illustpersist.db", migrator)
            LegacyDatabaseMigrations.migrateLegacyDatabase("task.db", migrator)

            val illustColumns = migrator.tableColumns("illustpersist")
            assertTrue("title" in illustColumns)
            assertTrue("user_name" in illustColumns)

            val taskColumns = migrator.tableColumns("task")
            assertTrue("medium" in taskColumns)

            val indexNames = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='index' AND sql IS NOT NULL",
                mapper = { cursor: SqlCursor ->
                    QueryResult.Value(
                        buildList {
                            while (cursor.next().value) {
                                add(cursor.getString(0).orEmpty())
                            }
                        },
                    )
                },
                parameters = 0,
            ).value
            assertTrue("idx_illustpersist_illust_id" in indexNames)
            assertTrue("idx_illustpersist_time" in indexNames)
            assertTrue("idx_task_status_id" in indexNames)
            assertTrue("idx_task_url" in indexNames)
        } finally {
            driver.close()
        }
    }

    @Test
    fun `Android 端自定义 scheme 深链各主机与官方 OAuth 回调白名单校验`() {
        val customHostIllust = DeepLinkParser.parse("pixez://illust/123456")
        assertIs<ParsedDeepLink.IllustDetail>(customHostIllust)
        assertEquals(123456L, customHostIllust.illustId)

        val downloads = DeepLinkParser.parse("pixez://downloads")
        assertEquals(ParsedDeepLink.DownloadTasks, downloads)

        val user = DeepLinkParser.parse("pixiv://pixiv.net/users/7890")
        assertIs<ParsedDeepLink.UserDetail>(user)
        assertEquals(7890L, user.userId)

        val validOAuth = DeepLinkParser.parse("https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback?code=abc123")
        assertIs<ParsedDeepLink.OAuthLogin>(validOAuth)
        assertEquals("abc123", validOAuth.code)

        val spoofingOAuth = DeepLinkParser.parse("https://evil.example.com/callback?code=stolen")
        assertFalse(spoofingOAuth is ParsedDeepLink.OAuthLogin)
    }
}
