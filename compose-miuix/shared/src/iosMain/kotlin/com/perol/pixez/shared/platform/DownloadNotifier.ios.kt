package com.perol.pixez.shared.platform

actual class DownloadNotifier {
    actual fun notifyProgress(id: Long, title: String, current: Int, total: Int) {}
    actual fun notifyFinished(id: Long, title: String, successCount: Int, failedCount: Int) {}
    actual fun cancel(id: Long) {}
}
