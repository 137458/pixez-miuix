package com.perol.pixez.shared.platform

/**
 * 跨平台下载状态与系统通知抽象。
 *
 * 在 Android 16 (API 36) / 15 上接入 Rich Ongoing Notifications，
 * 状态栏自动呈现实时动态微胶囊。
 */
expect class DownloadNotifier() {
    fun notifyProgress(id: Int, title: String, current: Int, total: Int)
    fun notifyFinished(id: Int, title: String, successCount: Int, failedCount: Int)
    fun cancel(id: Int)
}
