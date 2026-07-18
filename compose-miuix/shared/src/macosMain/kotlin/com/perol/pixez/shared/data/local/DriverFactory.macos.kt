package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask

/**
 * macOS 平台的 SQLDelight 驱动工厂。
 *
 * 旧 Flutter 桌面端使用 sqflite_common_ffi，其 `getDatabasesPath()` 在 macOS 上对应
 * `Application Support/databases`。本实现沿用该路径，确保能直接打开旧数据库。
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

        val dbDir = "$supportDir/databases"
        fileManager.createDirectoryAtPath(
            path = dbDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        val dbPath = "$dbDir/$fileName"
        return NativeSqliteDriver(schema, dbPath)
    }
}
