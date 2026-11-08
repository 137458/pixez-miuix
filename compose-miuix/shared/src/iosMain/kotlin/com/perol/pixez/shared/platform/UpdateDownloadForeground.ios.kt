package com.perol.pixez.shared.platform

internal actual fun updateDownloadForegroundStart() = Unit

internal actual fun updateDownloadForegroundProgress(downloaded: Long, total: Long) = Unit

internal actual fun updateDownloadForegroundStop() = Unit
