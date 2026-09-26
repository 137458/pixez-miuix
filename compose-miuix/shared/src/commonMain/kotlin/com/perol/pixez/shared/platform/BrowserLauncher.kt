package com.perol.pixez.shared.platform

/**
 * 跨平台打开外部浏览器访问指定 URL。
 *
 * Android 使用 Intent；Desktop 使用 java.awt.Desktop；iOS/macOS 使用系统打开 URL API。
 */
expect fun openBrowser(url: String)

internal fun isBrowserUrlAllowed(url: String): Boolean {
    val scheme = url.substringBefore(':', "").lowercase()
    return scheme in BROWSER_URL_SCHEMES
}

// 与 ui/utils/UrlLauncher 的 DEFAULT_ALLOWED_URL_SCHEMES 保持一致
private val BROWSER_URL_SCHEMES = setOf("http", "https", "mailto")
