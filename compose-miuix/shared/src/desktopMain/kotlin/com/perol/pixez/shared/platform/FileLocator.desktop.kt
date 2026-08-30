package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File

/**
 * Desktop(JVM) 平台实现：在资源管理器/Finder 中定位文件。
 */
actual class FileLocator {
    actual fun showInFileManager(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Napier.w("定位文件不存在: $filePath")
                return false
            }

            val os = System.getProperty("os.name")?.lowercase() ?: ""
            when {
                os.contains("win") -> {
                    // Windows: explorer.exe /select,"C:\path\to\file"
                    Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", file.absolutePath))
                    true
                }
                os.contains("mac") -> {
                    // macOS: open -R "/path/to/file"
                    Runtime.getRuntime().exec(arrayOf("open", "-R", file.absolutePath))
                    true
                }
                else -> {
                    // Linux / Other: 使用 Desktop 打开父文件夹
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(file.parentFile ?: file)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("在文件管理器中定位文件失败: $filePath", e)
            false
        }
    }
}
