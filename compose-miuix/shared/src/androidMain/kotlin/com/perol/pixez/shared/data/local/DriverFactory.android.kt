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
                    super.onCreate(db)
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {
                    // 旧 Flutter 数据库通常 user_version 为 0 但表已存在，
                    // 此时直接让框架设置新版本号，不执行 SQLDelight 的 migration/create。
                    if (oldVersion == 0 && db.hasLegacyTables()) return
                    super.onUpgrade(db, oldVersion, newVersion)
                }
            },
        )
    }

    actual fun closeDriver(driver: SqlDriver) {
        driver.close()
    }

    private fun SupportSQLiteDatabase.hasLegacyTables(): Boolean {
        query("SELECT count(*) FROM sqlite_master WHERE type='table'").use { cursor ->
            return cursor.moveToFirst() && cursor.getLong(0) > 0
        }
    }
}
