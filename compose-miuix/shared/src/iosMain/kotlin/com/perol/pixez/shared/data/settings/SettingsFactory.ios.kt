package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun createSettings(): Settings {
        // 旧 Flutter 应用在 iOS 上默认使用 NSUserDefaults.standardUserDefaults
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }

    actual suspend fun migrateIfNeeded() {
        // iOS 直接使用旧 NSUserDefaults，无需一次性导入。
    }
}
