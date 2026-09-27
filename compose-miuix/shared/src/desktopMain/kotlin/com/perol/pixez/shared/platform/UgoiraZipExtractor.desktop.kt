package com.perol.pixez.shared.platform

import java.io.FileInputStream
import java.util.zip.ZipInputStream
import okio.FileSystem
import okio.Path

actual class UgoiraZipExtractor actual constructor() {
    actual fun extractFrames(zipPath: Path, framesDir: Path): List<Path> {
        FileSystem.SYSTEM.createDirectories(framesDir)
        val written = mutableListOf<Path>()
        ZipInputStream(FileInputStream(zipPath.toString())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // 剥离目录层级；峰值内存为单帧字节，读后立即落盘
                    val name = entry.name.substringAfterLast('/')
                    val target = framesDir / name
                    FileSystem.SYSTEM.write(target) { write(zis.readBytes()) }
                    written.add(target)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return written
    }
}
