package com.perol.pixez.shared.platform

import java.io.FileInputStream
import java.util.zip.ZipInputStream
import okio.FileSystem
import okio.Path

actual class UgoiraZipExtractor actual constructor() {
    actual fun extractFrames(zipPath: Path, framesDir: Path): List<Path> {
        FileSystem.SYSTEM.createDirectories(framesDir)
        val framesDirNio = framesDir.toNioPath().normalize()
        val written = mutableListOf<Path>()
        ZipInputStream(FileInputStream(zipPath.toString())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.size <= MAX_ENTRY_BYTES) {
                    // 剥离正斜杠与 Windows 反斜杠两级目录分隔符，防 zip-slip 写出目标目录
                    val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (name.isNotEmpty() && name != "." && name != "..") {
                        val target = framesDir / name
                        require(target.toNioPath().normalize().startsWith(framesDirNio)) {
                            "Zip entry escapes frames directory: ${'$'}{entry.name}"
                        }
                        // 分块写盘并二次限额（防 entry.size 谎报），超限截断即跳过
                        val buffer = ByteArray(BUFFER_SIZE)
                        var total = 0L
                        FileSystem.SYSTEM.write(target) {
                            while (true) {
                                val read = zis.read(buffer)
                                if (read < 0) break
                                total += read
                                if (total > MAX_ENTRY_BYTES) break
                                write(buffer, 0, read)
                            }
                        }
                        if (total > MAX_ENTRY_BYTES) FileSystem.SYSTEM.delete(target)
                        else written.add(target)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return written
    }

    private companion object {
        const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
        const val BUFFER_SIZE = 64 * 1024
    }
}
