package com.perol.pixez.shared.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * 获取当前平台专用的应用私有沙盒缓存根目录。
 *
 * - Android: 优先使用 context.cacheDir
 */
fun getAppCacheDirectory(): Path {
    val context = BrowserLauncherContext.applicationContext
    val cacheDir = context?.cacheDir
    return if (cacheDir != null) {
        cacheDir.absolutePath.toPath()
    } else {
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }
}
