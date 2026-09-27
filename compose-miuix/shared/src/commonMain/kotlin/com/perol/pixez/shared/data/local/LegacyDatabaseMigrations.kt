package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver

/**
 * 旧 Flutter 数据库（user_version=0 且表已存在）的补列迁移。
 *
 * 旧 v1 结构缺少 SQLDelight 1.sqm 声明的列（task.medium、illustpersist.title/user_name、
 * glanceillustpersist.original_url/large_url）。历史实现遇到旧库只同步版本号不补列，
 * 导致历史/下载/小组件页面 "no such column" 全挂。此工具按 PRAGMA table_info 探测缺列，
 * 逐条 ALTER TABLE ADD COLUMN（新增列均为可空 TEXT，与 1.sqm 一致）。
 *
 * 回归验证只能走 desktopTest JVM 单测（LegacyMigrationTest）：
 * VerifyMigrationTask 在 Windows 默认禁用，Android onUpgrade 分支无法本机运行。
 */
object LegacyDatabaseMigrations {

    /** 各数据库文件的表 → 旧 v1 缺失列清单（均为可空 TEXT）。 */
    private val REQUIRED_COLUMNS_BY_TABLE: Map<String, Map<String, List<String>>> = mapOf(
        "task.db" to mapOf("task" to listOf("medium")),
        "illustpersist.db" to mapOf("illustpersist" to listOf("title", "user_name")),
        "glanceillustpersist.db" to mapOf("glanceillustpersist" to listOf("original_url", "large_url")),
    )

    /**
     * 平台无关的列迁移执行器：屏蔽 SqlDriver 与 Android SupportSQLiteDatabase 的 API 差异。
     */
    class LegacyColumnMigrator(
        private val execute: (String) -> Unit,
        private val queryStringList: (String) -> List<String>,
    ) {
        fun tableColumns(table: String): List<String> = queryStringList("PRAGMA table_info($table)")

        fun addColumn(table: String, column: String) = execute("ALTER TABLE $table ADD COLUMN $column TEXT")
    }

    /**
     * 按 [fileName] 补齐旧库缺失列；结构已完整时为无操作，已存在的数据不受影响。
     * 不在清单内的数据库文件不做任何处理。
     */
    fun migrateLegacyDatabase(fileName: String, migrator: LegacyColumnMigrator) {
        REQUIRED_COLUMNS_BY_TABLE[fileName]?.forEach { (table, requiredColumns) ->
            val existing = migrator.tableColumns(table)
            requiredColumns
                .filter { it !in existing }
                .forEach { column -> migrator.addColumn(table, column) }
        }
    }

    /** SqlDriver 侧执行器（desktop 与测试使用）。 */
    fun sqlDriverMigrator(driver: SqlDriver): LegacyColumnMigrator = LegacyColumnMigrator(
        execute = { sql -> driver.execute(null, sql, 0, null) },
        queryStringList = { sql ->
            driver.executeQuery(
                identifier = null,
                sql = sql,
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
        },
    )
}
