package com.perol.pixez.shared.platform

actual object PlatformAutoStart {
    actual fun setEnabled(enabled: Boolean) {
        // 非 Windows 平台无注册表自启机制
    }
}
