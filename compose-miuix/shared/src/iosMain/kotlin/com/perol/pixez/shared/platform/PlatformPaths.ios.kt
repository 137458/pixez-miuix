package com.perol.pixez.shared.platform

import okio.FileSystem
import okio.Path

actual fun getAppCacheDirectory(): Path {
    return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_cache"
}
