package com.perol.pixez.shared.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * 桌面缓存目录按平台惯例落位：Windows 用 %LOCALAPPDATA%，
 * macOS 用 ~/Library/Caches，Linux 用 XDG 缓存目录；不再污染用户主目录点目录。
 *
 * 一次性迁移：旧 `~/.pixez/cache` 存在且新目录未建立时整体 move（缓存可再生，
 * move 失败则回退继续使用旧目录，不阻塞启动）。
 */
actual fun getAppCacheDirectory(): Path {
    val os = System.getProperty("os.name")?.lowercase().orEmpty()
    val userHome = System.getProperty("user.home")
    val base = when {
        os.contains("win") -> {
            val localAppData = System.getenv("LOCALAPPDATA")
            if (!localAppData.isNullOrBlank()) "${localAppData}\\PixEz\\cache" else "${userHome}\\AppData\\Local\\PixEz\\cache"
        }
        os.contains("mac") -> "$userHome/Library/Caches/PixEz"
        !userHome.isNullOrBlank() -> "$userHome/.cache/pixez"
        else -> return (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_cache")
    }.toPath()

    migrateLegacyCache(base)
    return base
}

/** 旧 `~/.pixez/cache` 整体迁移至新平台目录；失败静默回退旧目录（缓存可再生）。 */
private fun migrateLegacyCache(target: Path) {
    if (FileSystem.SYSTEM.exists(target)) return
    val userHome = System.getProperty("user.home") ?: return
    val legacy = (userHome.toPath() / ".pixez" / "cache")
    if (!FileSystem.SYSTEM.exists(legacy)) return
    runCatching {
        FileSystem.SYSTEM.createDirectories(target.parent!!)
        java.nio.file.Files.move(
            legacy.toNioPath(),
            target.toNioPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        )
    }
}
