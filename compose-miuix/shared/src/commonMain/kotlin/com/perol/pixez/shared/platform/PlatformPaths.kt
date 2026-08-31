package com.perol.pixez.shared.platform

import okio.Path

/**
 * 获取当前平台专用的应用私有沙盒缓存根目录。
 *
 * - Android: 优先使用 context.cacheDir
 * - Desktop: 优先使用 ~/.pixez/cache
 * - iOS / macOS: 优先使用沙盒缓存目录
 */
expect fun getAppCacheDirectory(): Path

/** Default platform directory used for saved pictures when no custom path is configured. */
expect fun getDefaultPictureDirectory(): String
