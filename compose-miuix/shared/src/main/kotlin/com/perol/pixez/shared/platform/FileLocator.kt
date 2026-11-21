package com.perol.pixez.shared.platform

import android.app.DownloadManager
import android.content.Intent
import io.github.aakira.napier.Napier

/**
 * Android 平台文件定位与管理器打开工具：打开系统下载管理器或图库。
 */
class FileLocator {
    /**
     * 在系统文件管理器中打开或定位 [filePath] 文件。
     *
     * @param filePath 文件的绝对路径
     * @return 是否成功唤起系统文件管理器
     */
    fun showInFileManager(filePath: String): Boolean {
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
