package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import java.awt.SystemTray
import java.awt.TrayIcon

/**
 * Desktop(JVM) 平台下载通知实现：通过系统托盘气泡弹出下载完成通知。
 */
actual class DownloadNotifier {
    actual fun notifyProgress(id: Int, title: String, current: Int, total: Int) {
        // Desktop 进度通过任务页面实时展示，不频繁触发系统气泡打扰用户
    }

    actual fun notifyFinished(id: Int, title: String, successCount: Int, failedCount: Int) {
        try {
            if (SystemTray.isSupported()) {
                val systemTray = SystemTray.getSystemTray()
                val trayIcon = systemTray.trayIcons.firstOrNull()
                val message = if (failedCount > 0) {
                    "下载完成：$successCount 项成功，$failedCount 项失败"
                } else {
                    "下载完成：共 $successCount 项"
                }
                val msgType = if (failedCount > 0) TrayIcon.MessageType.WARNING else TrayIcon.MessageType.INFO
                trayIcon?.displayMessage(title.ifBlank { "PixEz 下载管理器" }, message, msgType)
            }
        } catch (e: Exception) {
            Napier.w("Desktop 弹出下载通知失败", e)
        }
    }

    actual fun cancel(id: Int) {}
}

