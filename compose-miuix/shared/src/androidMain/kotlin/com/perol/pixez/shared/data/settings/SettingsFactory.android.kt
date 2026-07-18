package com.perol.pixez.shared.data.settings

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SettingsFactory(private val context: Context) {
    actual fun createSettings(): Settings {
        // 使用与旧 Flutter 应用相同的 SharedPreferences 文件名 "FlutterSharedPreferences"
        val sharedPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(sharedPrefs)
    }

    actual suspend fun migrateIfNeeded() {
        // Android 直接使用旧 SharedPreferences 文件，无需一次性导入。
    }
}
