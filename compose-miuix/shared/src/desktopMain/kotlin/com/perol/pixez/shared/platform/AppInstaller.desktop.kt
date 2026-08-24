package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File

/**
 * Desktop 平台应用安装/打开实现。
 */
actual class AppInstaller actual constructor() {
    actual fun install(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Napier.e("打开安装包文件失败: $filePath", e)
            false
        }
    }

    actual fun getUpdateSaveDir(): String {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, "Downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }
}
