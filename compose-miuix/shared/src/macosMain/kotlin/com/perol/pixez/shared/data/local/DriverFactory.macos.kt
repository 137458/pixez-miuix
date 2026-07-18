package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask

/**
 * macOS 平台的 SQLDelight 驱动工厂。
 *
 * 旧 Flutter 桌面端使用 path_provider_foundation，其 `getApplicationSupportDirectory()` 在 macOS 上
 * 返回 `Application Support/<bundle_identifier>`，旧 Flutter 项目 bundle id 为 `com.perol.pixezFlutter`，
 * 再在其下追加 `databases`。本实现沿用该路径，确保能直接打开旧数据库。
 */
@OptIn(ExperimentalForeignApi::class)
actual class DriverFactory {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
        val fileManager = NSFileManager.defaultManager
        val supportDir = fileManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            appropriateForURL = null,
            inDomain = NSUserDomainMask,
            create = true,
            error = null,
        )?.path
            ?: throw IllegalStateException("无法获取 macOS ApplicationSupport 目录")

        // 旧 Flutter 项目 macOS bundle identifier 为 com.perol.pixezFlutter，
        // path_provider_foundation 会在 Application Support 下追加该 bundle id。
        val dbDir = "$supportDir/$LEGACY_MACOS_BUNDLE_ID/databases"
        fileManager.createDirectoryAtPath(
            path = dbDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        val dbPath = "$dbDir/$fileName"
        val configuration = DatabaseConfiguration(
            name = dbPath,
            version = schema.version.toInt(),
            create = { connection ->
                wrapConnection(connection) { driver ->
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

    companion object {
        // 旧 Flutter 项目 macOS bundle identifier
        private const val LEGACY_MACOS_BUNDLE_ID = "com.perol.pixezFlutter"
    }
}
