package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.Settings

/**
 * 平台相关的 [Settings] 工厂，用于创建与旧 SharedPreferences / NSUserDefaults 兼容的存储。
 */
expect class SettingsFactory {
    fun createSettings(): Settings
}
