package com.perol.pixez.shared.platform

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.github.aakira.napier.Napier

/**
 * Android 平台返回 true。
 */
actual fun isAndroidPlatform(): Boolean = true

actual fun isDesktopPlatform(): Boolean = false

/**
 * Android 12+ 跳转系统「默认打开方式」设置页。
 *
 * 使用 ACTION_APP_OPEN_BY_DEFAULT_SETTINGS（API 31+），
 * 低于 Android 12 时降级到应用详情页。
 */
actual fun openDefaultAppSettings() {
    val context = BrowserLauncherContext.applicationContext
        ?: throw IllegalStateException("BrowserLauncherContext 未初始化")
    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Napier.e("打开默认打开方式设置页失败", e)
    }
}
