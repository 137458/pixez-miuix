package com.perol.pixez.shared.platform

import android.content.Intent
import androidx.core.content.FileProvider
import io.github.aakira.napier.Napier
import java.io.File

/**
 * Android 平台应用安装器实现：通过 FileProvider 获取 APK content Uri 并唤起系统 PackageInstaller。
 */
class AppInstaller {
    /**
     * 打开并安装指定路径的应用安装包。
     *
     * @param filePath 安装包本地绝对路径。
     * @return 是否成功调起系统安装器或打开文件。
     */
    fun install(filePath: String): Boolean {
        return try {
            val context = BrowserLauncherContext.applicationContext
                ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 applicationContext")
            val file = File(filePath)
            if (!file.exists() || file.length() <= 0L) {
                Napier.e("安装包文件不存在或为空: $filePath")
                return false
            }
            val apkUri = FileProvider.getUriForFile(
                context,
                // 与 manifest 的 ${applicationId}.fileprovider 一致；硬编码包名在改 applicationId
                // 或加 flavor 后会直接抛 IllegalArgumentException，安装入口静默失效。
                "${context.packageName}.fileprovider",
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

    /**
     * 获取用于存放下载更新安装包的本地缓存目录路径。
     */
    fun getUpdateSaveDir(): String {
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化")
        val dir = File(context.externalCacheDir ?: context.cacheDir, "updates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }
}
