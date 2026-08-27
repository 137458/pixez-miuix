package com.perol.pixez.shared.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun getAppCacheDirectory(): Path {
    val userHome = System.getProperty("user.home")
    return if (!userHome.isNullOrBlank()) {
        userHome.toPath() / ".pixez" / "cache"
    } else {
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_cache"
    }
}
