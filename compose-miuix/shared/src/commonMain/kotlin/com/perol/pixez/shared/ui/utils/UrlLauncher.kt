package com.perol.pixez.shared.ui.utils

import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.i18n.AppStrings
import io.github.aakira.napier.Napier

/**
 * 默认允许通过浏览器打开的 URL Scheme 白名单
 */
val DEFAULT_ALLOWED_URL_SCHEMES: Set<String> = setOf("http", "https", "mailto")

/**
 * 安全打开外部链接，严格校验 URL Scheme，并通过回调优雅返回本地化错误提示。
 */
fun openSafeUrl(
    url: String,
    strings: AppStrings,
    allowedSchemes: Set<String> = DEFAULT_ALLOWED_URL_SCHEMES,
    onError: (String) -> Unit,
) {
    try {
        val scheme = url.substringBefore(":", "").lowercase()
        require(scheme in allowedSchemes) { "Invalid scheme: $scheme" }
        openBrowser(url)
    } catch (e: Exception) {
        Napier.w(tag = "UrlLauncher", throwable = e) { "Failed to open url: $url" }
        onError("${strings.loadFailed}: ${e.message ?: strings.loadFailed}")
    }
}
