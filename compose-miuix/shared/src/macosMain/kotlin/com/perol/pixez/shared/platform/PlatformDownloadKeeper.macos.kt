package com.perol.pixez.shared.platform

actual object PlatformDownloadKeeper {
    actual fun acquire(taskId: Int, title: String) {}
    actual fun release(taskId: Int) {}
}
