package com.perol.pixez.shared.platform

/**
 * 跨平台打开外部浏览器访问指定 URL。
 *
 * Android 使用 Intent；Desktop 使用 java.awt.Desktop；iOS/macOS 使用系统打开 URL API。
 */
expect fun openBrowser(url: String)
