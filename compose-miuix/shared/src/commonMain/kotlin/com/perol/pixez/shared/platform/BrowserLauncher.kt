package com.perol.pixez.shared.platform

/**
 * 跨平台打开外部浏览器访问指定 URL。
 *
 * Android 使用 Intent；Desktop 使用 java.awt.Desktop。
 */
expect fun openBrowser(url: String)

internal fun isBrowserUrlAllowed(url: String): Boolean {
    val scheme = url.substringBefore(':', "").lowercase()
    return scheme in DEFAULT_ALLOWED_EXTERNAL_URL_SCHEMES
}

/** 外部浏览器默认允许打开的安全 URL Scheme 白名单，与 `UrlLauncher` 共用单点定义。 */
internal val DEFAULT_ALLOWED_EXTERNAL_URL_SCHEMES: Set<String> = setOf("http", "https", "mailto")
