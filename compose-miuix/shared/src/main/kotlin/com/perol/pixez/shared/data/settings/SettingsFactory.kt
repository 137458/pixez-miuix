package com.perol.pixez.shared.data.settings

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * [Settings] 工厂接口，供生产平台实现与 JVM 测试替身共用。
 */
interface SettingsProvider {
    fun createSettings(): Settings

    suspend fun migrateIfNeeded()
}

/**
 * Android 平台的 [Settings] 工厂，用于创建与旧 SharedPreferences 兼容的存储。
 */
class SettingsFactory(private val context: Context) : SettingsProvider {
    /**
     * 创建当前平台的 [Settings] 实例。
     * 该方法应尽可能轻量，不执行文件 I/O 等可能阻塞调用线程的操作。
     */
    override fun createSettings(): Settings {
        // 使用与旧 Flutter 应用相同的 SharedPreferences 文件名 "FlutterSharedPreferences"
        val sharedPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(sharedPrefs)
    }

    /**
     * 在后台协程中执行一次性旧设置迁移。
     * Android 直接使用旧 SharedPreferences，无需额外迁移。
     */
    override suspend fun migrateIfNeeded() {
        // Android 直接使用旧 SharedPreferences 文件，无需一次性导入。
    }
}
