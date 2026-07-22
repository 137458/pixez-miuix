package com.perol.pixez.shared.platform

/**
 * Desktop(JVM) 平台返回 false，不展示 Android 专属设置项。
 */
actual fun isAndroidPlatform(): Boolean = false

/**
 * Desktop 平台无需打开 Android 系统设置页，空实现。
 */
actual fun openDefaultAppSettings() {
    // Desktop 平台无对应系统设置页，不执行任何操作。
}
