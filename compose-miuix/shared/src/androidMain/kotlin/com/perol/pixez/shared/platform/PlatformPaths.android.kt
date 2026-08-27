package com.perol.pixez.shared.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun getAppCacheDirectory(): Path {
    val context = BrowserLauncherContext.applicationContext
    val cacheDir = context?.cacheDir
    return if (cacheDir != null) {
        cacheDir.absolutePath.toPath()
    } else {
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }
}
