package com.perol.pixez.shared.platform

import android.app.DownloadManager
import android.content.Intent
import io.github.aakira.napier.Napier

/**
 * Android 平台实现：打开系统下载管理器或图库。
 */
actual class FileLocator {
    actual fun showInFileManager(filePath: String): Boolean {
        val context = BrowserLauncherContext.applicationContext ?: return false
        return try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Napier.w("打开系统下载管理器失败: $filePath", e)
            false
        }
    }
}
