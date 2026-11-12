package com.perol.pixez.shared.platform

import okio.Path

/**
 * Android 端动图 Zip 解压实现：委派至 JVM 双端共用实现。
 */
actual class UgoiraZipExtractor actual constructor() {
    actual fun extractFrames(zipPath: Path, framesDir: Path): List<Path> =
        extractUgoiraFramesJvm(zipPath, framesDir)
}
