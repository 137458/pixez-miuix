package com.perol.pixez.shared.platform

import android.content.Context
import android.content.Intent

/**
 * Android 平台实现：使用系统 `Intent.ACTION_SEND` 启动分享选择器。
 *
 * 复用 [BrowserLauncherContext] 获取应用 Context，与 [IllustClipboard] 保持一致。
 */
actual class IllustShare {
    actual fun share(text: String, subject: String?) {
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 Context")

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }
}
