package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File

/**
 * Desktop(JVM) 平台实现：在资源管理器/Finder 中定位文件或打开目录。
 */
actual class FileLocator {
    actual fun showInFileManager(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Napier.w("定位文件不存在: $filePath")
                return false
            }

            if (file.isDirectory) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file)
                    return true
                }
            }

            val os = System.getProperty("os.name")?.lowercase() ?: ""
            when {
                os.contains("win") -> {
                    // Windows explorer.exe 要求 /select, 与路径作为同一个参数传入，否则会忽略路径打开默认文档目录
                    if (file.isDirectory) {
                        ProcessBuilder("explorer.exe", file.absolutePath).start()
                    } else {
                        ProcessBuilder("explorer.exe", "/select,${file.absolutePath}").start()
                    }
                    true
                }
                os.contains("mac") -> {
                    if (file.isDirectory) {
                        ProcessBuilder("open", file.absolutePath).start()
                    } else {
                        ProcessBuilder("open", "-R", file.absolutePath).start()
                    }
                    true
                }
                else -> {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(if (file.isDirectory) file else (file.parentFile ?: file))
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
