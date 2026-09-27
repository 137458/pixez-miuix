package com.perol.pixez.shared.platform

/**
 * 当前构建是否为可调试构建（debug）。
 *
 * Android 以 ApplicationInfo.FLAG_DEBUGGABLE 判定（等价 debug/release 构建类型）；
 * 桌面 JVM 与 iOS 无构建类型区分，一律返回 true（错误响应体已在 PixivHttpClient 统一截断脱敏）。
 */
expect val isDebugBuild: Boolean
