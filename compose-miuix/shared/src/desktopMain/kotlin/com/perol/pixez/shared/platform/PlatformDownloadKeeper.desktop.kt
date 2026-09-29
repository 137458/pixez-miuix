package com.perol.pixez.shared.platform

actual object PlatformDownloadKeeper {
    actual fun acquire(taskId: Long) {}
    actual fun release(taskId: Long) {}
}
