package com.perol.pixez.shared.platform

import android.content.Intent
import androidx.core.content.FileProvider
import io.github.aakira.napier.Napier
import java.io.File

/**
 * Android 平台应用安装器实现：通过 FileProvider 获取 APK content Uri 并唤起系统 PackageInstaller。
 */
actual class AppInstaller actual constructor() {
    actual fun install(filePath: String): Boolean {
        return try {
            val context = BrowserLauncherContext.applicationContext
                ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 applicationContext")
            val file = File(filePath)
            if (!file.exists()) {
                Napier.e("安装包文件不存在: $filePath")
                return false
            }
            val apkUri = FileProvider.getUriForFile(
                context,
                "com.perol.pixez.miuix.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Napier.e("调起系统应用安装器失败: $filePath", e)
            false
        }
    }

    actual fun getUpdateSaveDir(): String {
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化")
        val dir = File(context.externalCacheDir ?: context.cacheDir, "updates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }
}
