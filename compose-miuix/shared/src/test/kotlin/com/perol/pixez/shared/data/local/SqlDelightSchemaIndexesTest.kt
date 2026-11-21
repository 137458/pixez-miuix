package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.local.glanceillustpersist.GlanceIllustPersistDatabase
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.kvpair.KVPairDatabase
import com.perol.pixez.shared.data.local.task.TaskDatabase
import org.junit.Test
import kotlin.test.assertTrue

class SqlDelightSchemaIndexesTest {

    private fun indexNames(driver: JdbcSqliteDriver, table: String): Set<String> =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA index_list($table)",
            mapper = { cursor: SqlCursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(1)?.let { names.add(it) }
                }
                QueryResult.Value(names)
            },
            parameters = 0,
            binders = null,
        ).value

    @Test
    fun `新库创建与 v1 升级迁移后核心查询索引均存在`() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { taskDriver ->
            TaskDatabase.Schema.create(taskDriver)
            val idx = indexNames(taskDriver, "task")
            assertTrue("idx_task_status_id" in idx, "task 表缺少 idx_task_status_id: $idx")
            assertTrue("idx_task_url" in idx, "task 表缺少 idx_task_url: $idx")
        }

        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { glanceDriver ->
            GlanceIllustPersistDatabase.Schema.create(glanceDriver)
            val idx = indexNames(glanceDriver, "glanceillustpersist")
            assertTrue("idx_glance_ctype_ctime" in idx, "glanceillustpersist 缺少 idx_glance_ctype_ctime: $idx")
        }

        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { accountDriver ->
            AccountDatabase.Schema.create(accountDriver)
            val idx = indexNames(accountDriver, "account")
            assertTrue("idx_account_user_id" in idx, "account 缺少 idx_account_user_id: $idx")
        }

        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { kvDriver ->
            KVPairDatabase.Schema.create(kvDriver)
            val idx = indexNames(kvDriver, "kvpair")
            assertTrue("idx_kvpair_key" in idx, "kvpair 缺少 idx_kvpair_key: $idx")
        }

        // 模拟 v1 旧库经 1.sqm 升级到 v2：illustpersist 索引也应通过迁移建立
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { legacyIllustDriver ->
            legacyIllustDriver.execute(
                null,
                "CREATE TABLE illustpersist (id INTEGER PRIMARY KEY AUTOINCREMENT, illust_id INTEGER NOT NULL, user_id INTEGER NOT NULL, picture_url TEXT NOT NULL, time INTEGER NOT NULL);",
                0,
                null,
            )
            IllustPersistDatabase.Schema.migrate(legacyIllustDriver, 1, 2)
            val idx = indexNames(legacyIllustDriver, "illustpersist")
            assertTrue("idx_illustpersist_illust_id" in idx, "illustpersist v1->v2 迁移应建立 idx_illustpersist_illust_id: $idx")
            assertTrue("idx_illustpersist_time" in idx, "illustpersist v1->v2 迁移应建立 idx_illustpersist_time: $idx")
        }
    }
}
