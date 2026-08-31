package com.perol.pixez.shared.platform

/**
 * 判断当前是否为 Android 平台。
 *
 * 用于在 UI 层控制 Android 专属设置的显示与隐藏；
 * Desktop/iOS 等其他平台实际返回 false。
 */
expect fun isAndroidPlatform(): Boolean

/**
 * 判断当前是否为 Compose Desktop (JVM) 平台。
 *
 * 用于展示仅依赖桌面窗口/系统托盘能力的设置项。
 */
expect fun isDesktopPlatform(): Boolean

/**
 * 打开系统「默认打开方式」设置页（Android 12+）。
 *
 * 仅 Android 平台有实际实现，其他平台为空操作。
 */
expect fun openDefaultAppSettings()
