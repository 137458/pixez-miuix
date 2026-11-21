package com.perol.pixez.shared.platform

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.github.aakira.napier.Napier

/**
 * 跳转系统「默认打开方式」设置页。
 *
 * 使用 ACTION_APP_OPEN_BY_DEFAULT_SETTINGS（API 31+）。minSdk 31 下该 Action 恒可用，
 * 早期降级到应用详情页的分支已无意义，故只保留单一路径。
 */
fun openDefaultAppSettings() {
    val context = BrowserLauncherContext.applicationContext
        ?: throw IllegalStateException("BrowserLauncherContext 未初始化")
    try {
        val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Napier.e("打开默认打开方式设置页失败", e)
    }
}
