package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Desktop(JVM) 平台的 SQLDelight 驱动工厂。
 *
 * 数据库路径沿用旧 Flutter 桌面端（sqflite_common_ffi + path_provider）的默认位置：
 * - Windows：%APPDATA%/com.perol.pixez/databases
 * - macOS：~/Library/Application Support/com.perol.pixez/databases
 * - Linux：~/.local/share/com.perol.pixez/databases
 *
 * 打开后通过 `PRAGMA user_version` 判断是创建新库还是迁移旧库，
 * 避免每次启动都对已有数据库执行 CREATE TABLE 导致崩溃。
 */
actual class DriverFactory {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
        val dbDir = File(legacyDatabaseRoot(), "databases").apply { mkdirs() }
        val dbFile = File(dbDir, fileName)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        val currentVersion = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
            binders = null,
        ).value
        val targetVersion = schema.version
        val hasTables = driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM sqlite_master WHERE type='table'",
            mapper = { cursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
            binders = null,
        ).value > 0L

        when {
            // 全新数据库：文件不存在且版本号为 0
            currentVersion == 0L && !dbFile.exists() -> {
                schema.create(driver)
                driver.execute(null, "PRAGMA user_version = $targetVersion", 0, null)
            }
            // 空或损坏文件：文件存在但无表且版本号为 0
            currentVersion == 0L && dbFile.exists() && !hasTables -> {
                schema.create(driver)
                driver.execute(null, "PRAGMA user_version = $targetVersion", 0, null)
            }
            // 旧 Flutter 数据库：文件存在、有表但 user_version 为 0
            currentVersion == 0L && dbFile.exists() && hasTables -> {
                // 旧版 sqflite 未设置 user_version，直接同步到目标版本，避免 SQLDelight 重复 CREATE
                driver.execute(null, "PRAGMA user_version = $targetVersion", 0, null)
            }
            // 正常升级
            currentVersion < targetVersion -> {
                schema.migrate(driver, currentVersion, targetVersion)
                driver.execute(null, "PRAGMA user_version = $targetVersion", 0, null)
            }
        }

        return driver
    }

    actual fun closeDriver(driver: SqlDriver) {
        driver.close()
    }

    /**
     * 返回与旧 Flutter 桌面端 `path_provider.getApplicationSupportDirectory()` 一致的目录。
     * path_provider 在该目录下会追加应用包名，因此这里需要同步追加 `com.perol.pixez`。
     *
     * 测试可通过设置系统属性 `pixez.test.db.root` 覆盖根目录，避免污染真实用户数据路径。
     */
    private fun legacyDatabaseRoot(): File {
        System.getProperty(TEST_DB_ROOT_PROPERTY)?.let { return File(it) }

        val home = System.getProperty("user.home") ?: error("无法获取 user.home")
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val appSupportRoot = when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrBlank()) File(appData) else File(home, "AppData/Roaming")
            }
            os.contains("mac") -> File(home, "Library/Application Support")
            else -> File(home, ".local/share")
        }
        return File(appSupportRoot, LEGACY_PACKAGE_DIR)
    }

    companion object {
        private const val LEGACY_PACKAGE_DIR = "com.perol.pixez"
        private const val TEST_DB_ROOT_PROPERTY = "pixez.test.db.root"
    }
}
