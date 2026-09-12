package com.perol.pixez.shared.platform

actual object PlatformDownloadKeeper {
    actual fun acquire(taskId: Int) {}
    actual fun release(taskId: Int) {}
}
