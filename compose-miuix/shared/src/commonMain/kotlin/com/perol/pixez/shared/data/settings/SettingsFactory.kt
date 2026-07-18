package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.Settings

/**
 * 平台相关的 [Settings] 工厂，用于创建与旧 SharedPreferences / NSUserDefaults 兼容的存储。
 */
expect class SettingsFactory {
    /**
     * 创建当前平台的 [Settings] 实例。
     * 该方法应尽可能轻量，不执行文件 I/O 等可能阻塞调用线程的操作。
     */
    fun createSettings(): Settings

    /**
     * 在后台协程中执行一次性旧设置迁移（如 Desktop 从 shared_preferences.json 导入）。
     * Android / iOS / macOS 因直接使用旧 SharedPreferences / NSUserDefaults，无需额外迁移。
     */
    suspend fun migrateIfNeeded()
}
