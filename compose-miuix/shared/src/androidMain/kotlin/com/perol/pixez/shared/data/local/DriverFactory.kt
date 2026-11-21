package com.perol.pixez.shared.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * SQLDelight 驱动工厂接口，供生产平台实现与 JVM 测试替身共用。
 */
interface SqlDriverFactory {
    fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver

    fun resolveDatabaseFileName(database: LocalDatabase): String

    fun closeDriver(driver: SqlDriver)
}

/**
 * Android 平台的 SQLDelight 驱动工厂。
 * 每个旧 .db 文件通过 [fileName] 单独打开，保持与旧 Flutter 应用文件路径兼容。
 */
class DriverFactory(private val context: Context) : SqlDriverFactory {
    override fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
        return AndroidSqliteDriver(
            schema,
            context,
            fileName,
            callback = object : AndroidSqliteDriver.Callback(schema) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // 旧 Flutter 数据库通常 user_version 为 0 但表已存在：
                    // AndroidX SQLite 会在 user_version == 0 时触发 onCreate，若直接 super.onCreate 会执行 CREATE TABLE 崩溃。
                    if (db.hasLegacyTables()) {
                        migrateLegacy(db)
                        return
                    }
                    super.onCreate(db)
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {
                    if (oldVersion == 0 && db.hasLegacyTables()) {
                        migrateLegacy(db)
                        return
                    }
                    super.onUpgrade(db, oldVersion, newVersion)
                }

                private fun migrateLegacy(db: SupportSQLiteDatabase) {
                    LegacyDatabaseMigrations.migrateLegacyDatabase(
                        fileName,
                        LegacyDatabaseMigrations.LegacyColumnMigrator(
                            execute = { sql -> db.execSQL(sql) },
                            queryStringList = { sql ->
                                db.query(sql).use { cursor ->
                                    buildList {
                                        while (cursor.moveToNext()) add(cursor.getString(1))
                                    }
                                }
                            },
                        ),
                    )
                }
            },
        )
    }

    /**
     * 解析该库在当前设备上应实际打开的库文件名。
     *
     * 按 [LocalDatabase.candidateFileNames] 顺序返回第一个已存在的文件（兼容旧 Flutter 库名与
     * 过渡版本写出的别名），全部不存在时返回登记主名（新建库走主名）。持库方必须经此解析，
     * 否则补列清单会与实开文件脱节。
     */
    override fun resolveDatabaseFileName(database: LocalDatabase): String =
        database.candidateFileNames.firstOrNull { context.getDatabasePath(it).exists() } ?: database.fileName

    /**
     * 关闭由 [createDriver] 创建的 [SqlDriver]，释放底层文件句柄与连接。
     * Repository / DatabaseHolder 层应在合适的生命周期点统一调用。
     */
    override fun closeDriver(driver: SqlDriver) {
        driver.close()
    }

    private fun SupportSQLiteDatabase.hasLegacyTables(): Boolean {
        query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            val tableNames = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0).orEmpty())
            }
            return LegacyDatabaseMigrations.hasLegacyUserTables(tableNames)
        }
    }
}
