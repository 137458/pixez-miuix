package com.perol.pixez.shared.data.local

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 旧库判定：Android framework 在 SQLiteOpenHelper.onCreate 之前就已建好 android_metadata
 * （AUTOINCREMENT 表还会带出 sqlite_sequence），这些内部表不能让全新库被误判为旧 Flutter 库。
 * 误判会让 onCreate 跳过 schema.create，随后执行 CREATE INDEX 时报
 * "no such table: main.account" 闪退。
 */
class LegacyUserTableDetectionTest {

    @Test
    fun `全新库只含 Android 内部表时不算旧库`() {
        assertFalse(
            LegacyDatabaseMigrations.hasLegacyUserTables(listOf("android_metadata")),
            "只有 android_metadata 的全新库必须走 schema.create",
        )
        assertFalse(
            LegacyDatabaseMigrations.hasLegacyUserTables(listOf("android_metadata", "sqlite_sequence")),
            "sqlite_sequence 属于 SQLite 内部表",
        )
        assertFalse(LegacyDatabaseMigrations.hasLegacyUserTables(emptyList()))
    }

    @Test
    fun `存在业务表时判定为旧库`() {
        assertTrue(
            LegacyDatabaseMigrations.hasLegacyUserTables(listOf("android_metadata", "account")),
            "旧 Flutter 库的 account 表必须触发补列迁移",
        )
        assertTrue(LegacyDatabaseMigrations.hasLegacyUserTables(listOf("task")))
    }
}
