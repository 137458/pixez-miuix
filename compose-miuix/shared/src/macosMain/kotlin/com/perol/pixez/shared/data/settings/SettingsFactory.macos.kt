package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun createSettings(): Settings {
        // 旧 Flutter 应用在 macOS 上默认使用 NSUserDefaults.standardUserDefaults
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
