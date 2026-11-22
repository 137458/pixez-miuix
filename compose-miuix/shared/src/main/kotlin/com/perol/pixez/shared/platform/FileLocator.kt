package com.perol.pixez.shared.platform

import android.app.DownloadManager
import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import io.github.aakira.napier.Napier

/**
 * Android 平台文件定位与管理器打开工具：优先打开目标存储目录（SAF Document URI），失败时回退系统下载管理器。
 */
class FileLocator {
    /**
     * 在系统文件管理器中打开或定位 [filePath] 路径。
     *
     * @param filePath 文件或目录的绝对路径
     * @return 是否成功唤起系统文件管理器
     */
    fun showInFileManager(filePath: String): Boolean {
        val context = BrowserLauncherContext.applicationContext ?: return false
        val rootPath = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrNull()
        if (rootPath != null && filePath.startsWith(rootPath)) {
            val relative = filePath.removePrefix(rootPath).trimStart('/')
            val docId = if (relative.isEmpty()) "primary:" else "primary:$relative"
            val docUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                docId,
            )
            val folderIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(docUri, "vnd.android.document/directory")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            if (folderIntent.resolveActivity(context.packageManager) != null) {
                val opened = runCatching { context.startActivity(folderIntent) }.isSuccess
                if (opened) return true
            }
        }
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
