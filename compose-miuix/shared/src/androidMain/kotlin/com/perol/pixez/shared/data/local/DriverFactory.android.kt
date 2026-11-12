package com.perol.pixez.shared.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
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

    actual fun resolveDatabaseFileName(database: LocalDatabase): String =
        database.candidateFileNames.firstOrNull { context.getDatabasePath(it).exists() } ?: database.fileName

    actual fun closeDriver(driver: SqlDriver) {
        driver.close()
    }

    private fun SupportSQLiteDatabase.hasLegacyTables(): Boolean {
        query("SELECT count(*) FROM sqlite_master WHERE type='table'").use { cursor ->
            return cursor.moveToFirst() && cursor.getLong(0) > 0
        }
    }
}
