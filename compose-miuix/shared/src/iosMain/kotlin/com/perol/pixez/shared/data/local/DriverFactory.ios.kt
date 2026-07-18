package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

/**
 * iOS 平台的 SQLDelight 驱动工厂。
 *
 * 旧 Flutter 应用使用 sqflite，其 `getDatabasesPath()` 在 iOS 上对应
 * `Documents/databases`。本实现沿用该路径，确保能直接打开旧数据库。
 *
 * 对于 `glanceillustpersist.db`，旧版使用 App Group `group.pixez` 下的
 * `DB/` 目录，优先尝试该路径，失败时回退到 `Documents/databases`。
 */
@OptIn(ExperimentalForeignApi::class)
actual class DriverFactory {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
        val fileManager = NSFileManager.defaultManager
        val dbDir = when (fileName) {
            GLANCE_DB_NAME -> legacyAppGroupDbDir(fileManager) ?: legacyDocumentsDbDir(fileManager)
            else -> legacyDocumentsDbDir(fileManager)
        }

        val dbPath = "$dbDir/$fileName"
        val configuration = DatabaseConfiguration(
            name = dbPath,
            version = schema.version.toInt(),
            create = { connection ->
                wrapConnection(connection) { driver ->
                    // 旧 Flutter 数据库可能已有表但 user_version 为 0，
                    // 仅同步版本号，避免 CREATE TABLE 冲突。
                    if (driver.hasLegacyTables()) {
                        driver.execute(null, "PRAGMA user_version = ${schema.version}", 0, null)
                    } else {
                        schema.create(driver)
                    }
                }
            },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) { driver ->
                    schema.migrate(driver, oldVersion.toLong(), newVersion.toLong())
                }
            },
        )
        return NativeSqliteDriver(configuration)
    }

    actual fun closeDriver(driver: SqlDriver) {
        driver.close()
    }

    private fun SqlDriver.hasLegacyTables(): Boolean {
        return executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM sqlite_master WHERE type='table'",
            mapper = { cursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
            binders = null,
        ).value > 0L
    }

    private fun legacyDocumentsDbDir(fileManager: NSFileManager): String {
        val documentsDir = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            appropriateForURL = null,
            inDomain = NSUserDomainMask,
            create = true,
            error = null,
        )?.path
            ?: throw IllegalStateException("无法获取 iOS Documents 目录")

        val dbDir = "$documentsDir/databases"
        fileManager.createDirectoryAtPath(
            path = dbDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return dbDir
    }

    private fun legacyAppGroupDbDir(fileManager: NSFileManager): String? {
        val groupUrl = fileManager.containerURLForSecurityApplicationGroupIdentifier(APP_GROUP_ID)
        val dbDir = groupUrl?.path?.let { "$it/DB" } ?: return null
        val created = fileManager.createDirectoryAtPath(
            path = dbDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return if (created) dbDir else null
    }

    companion object {
        private const val GLANCE_DB_NAME = "glanceillustpersist.db"
        private const val APP_GROUP_ID = "group.pixez"
    }
}
