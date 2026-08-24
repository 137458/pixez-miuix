package com.perol.pixez.shared.platform

actual class DownloadNotifier {
    actual fun notifyProgress(id: Int, title: String, current: Int, total: Int) {}
    actual fun notifyFinished(id: Int, title: String, successCount: Int, failedCount: Int) {}
    actual fun cancel(id: Int) {}
}
