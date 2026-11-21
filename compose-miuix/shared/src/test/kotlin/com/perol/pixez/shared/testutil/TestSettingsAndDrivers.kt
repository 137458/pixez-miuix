package com.perol.pixez.shared.testutil

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.LegacyDatabaseMigrations
import com.perol.pixez.shared.data.local.LocalDatabase
import com.perol.pixez.shared.data.local.SqlDriverFactory
import com.perol.pixez.shared.data.settings.SettingsProvider
import com.perol.pixez.shared.platform.FileNamePolicy
import com.perol.pixez.shared.platform.IllustFileSaver
import com.russhwolf.settings.Settings
import java.io.File

/**
 * 纯内存 [Settings] 实现，供 Android JVM 单元测试隔离偏好存储。
 */
class InMemorySettings(
    val backing: MutableMap<String, Any> = linkedMapOf(),
) : Settings {
    override val keys: Set<String> get() = backing.keys.toSet()
    override val size: Int get() = backing.size

    override fun clear() {
        backing.clear()
    }

    override fun remove(key: String) {
        backing.remove(key)
    }

    override fun hasKey(key: String): Boolean = backing.containsKey(key)

    override fun putInt(key: String, value: Int) { backing[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int =
        (backing[key] as? Number)?.toInt() ?: defaultValue
    override fun getIntOrNull(key: String): Int? = (backing[key] as? Number)?.toInt()

    override fun putLong(key: String, value: Long) { backing[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long =
        (backing[key] as? Number)?.toLong() ?: defaultValue
    override fun getLongOrNull(key: String): Long? = (backing[key] as? Number)?.toLong()

    override fun putString(key: String, value: String) { backing[key] = value }
    override fun getString(key: String, defaultValue: String): String =
        backing[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = backing[key] as? String

    override fun putFloat(key: String, value: Float) { backing[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float =
        (backing[key] as? Number)?.toFloat() ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = (backing[key] as? Number)?.toFloat()

    override fun putDouble(key: String, value: Double) { backing[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double =
        (backing[key] as? Number)?.toDouble() ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = (backing[key] as? Number)?.toDouble()

    override fun putBoolean(key: String, value: Boolean) { backing[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        backing[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = backing[key] as? Boolean
}

class TestSettingsProvider(
    private val settings: Settings = InMemorySettings(),
) : SettingsProvider {
    override fun createSettings(): Settings = settings

    override suspend fun migrateIfNeeded() = Unit
}

/**
 * 基于临时目录的 JVM SQLite 测试驱动工厂，复用 [LegacyDatabaseMigrations] 验证迁移与持久化。
 */
class TestSqlDriverFactory(
    private val rootDir: File,
) : SqlDriverFactory {
    private val dbDir: File get() = File(rootDir, "databases").apply { mkdirs() }

    override fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
        val dbFile = File(dbDir, fileName)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        val currentVersion = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor: SqlCursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
            binders = null,
        ).value
        val tableNames = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type='table'",
            mapper = { cursor: SqlCursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) add(cursor.getString(0).orEmpty())
                    },
                )
            },
            parameters = 0,
            binders = null,
        ).value

        val hasLegacy = LegacyDatabaseMigrations.hasLegacyUserTables(tableNames)
        when {
            currentVersion == 0L && !hasLegacy -> {
                schema.create(driver)
                driver.execute(null, "PRAGMA user_version = ${schema.version}", 0, null)
            }
            currentVersion == 0L && hasLegacy -> {
                migrateLegacy(driver, fileName)
                driver.execute(null, "PRAGMA user_version = ${schema.version}", 0, null)
            }
            currentVersion < schema.version -> {
                schema.migrate(driver, currentVersion, schema.version)
                driver.execute(null, "PRAGMA user_version = ${schema.version}", 0, null)
            }
        }
        return driver
    }

    override fun resolveDatabaseFileName(database: LocalDatabase): String =
        database.candidateFileNames.firstOrNull { File(dbDir, it).exists() } ?: database.fileName

    override fun closeDriver(driver: SqlDriver) {
        driver.close()
    }

    private fun migrateLegacy(driver: SqlDriver, fileName: String) {
        LegacyDatabaseMigrations.migrateLegacyDatabase(
            fileName = fileName,
            migrator = LegacyDatabaseMigrations.LegacyColumnMigrator(
                execute = { sql -> driver.execute(null, sql, 0, null) },
                queryStringList = { sql ->
                    driver.executeQuery(
                        identifier = null,
                        sql = sql,
                        mapper = { cursor: SqlCursor ->
                            QueryResult.Value(
                                buildList {
                                    while (cursor.next().value) add(cursor.getString(1).orEmpty())
                                },
                            )
                        },
                        parameters = 0,
                        binders = null,
                    ).value
                },
            ),
        )
    }
}

/**
 * JVM 测试用 [IllustFileSaver]：将字节或临时文件直接写入指定临时目录。
 */
class TempDirIllustSaver(
    private val defaultBaseDir: File,
) : IllustFileSaver {
    override suspend fun save(
        fileName: String,
        bytes: ByteArray,
        subDir: String?,
        customBasePath: String?,
    ): String {
        val targetFile = resolveTargetFile(fileName, subDir, customBasePath)
        targetFile.writeBytes(bytes)
        return targetFile.absolutePath
    }

    override suspend fun saveFromTempFile(
        fileName: String,
        tempFilePath: okio.Path,
        subDir: String?,
        customBasePath: String?,
    ): String {
        val src = File(tempFilePath.toString())
        require(src.exists() && src.length() > 0) { "临时文件不存在或为空: $tempFilePath" }
        val targetFile = resolveTargetFile(fileName, subDir, customBasePath)
        src.inputStream().use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        return targetFile.absolutePath
    }

    private fun resolveTargetFile(fileName: String, subDir: String?, customBasePath: String?): File {
        val baseDir = customBasePath?.takeIf { it.isNotBlank() }?.let(::File) ?: defaultBaseDir
        val dir = if (subDir.isNullOrBlank()) baseDir else File(baseDir, FileNamePolicy.sanitizeSegment(subDir))
        dir.mkdirs()
        return File(dir, FileNamePolicy.requireSafeBaseName(fileName))
    }
}
