package com.perol.pixez.shared.platform

import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * JVM 双端（Android / Desktop）共用的动图 Zip 流式解压实现。
 *
 * 防护策略（Zip Bomb / 路径穿越）：
 * - 剥离目录层级，仅保留安全文件名并校验规范路径不越出 [framesDir]；
 * - 单条目超过 [MAX_ENTRY_BYTES]（64MB）跳过不落盘；
 * - 累计非目录条目数超过 [MAX_ENTRY_COUNT]（512）或累计解压字节超过 [MAX_TOTAL_BYTES]（256MB）立即终止并清理已写入帧。
 */
internal fun extractUgoiraFramesJvm(zipPath: Path, framesDir: Path): List<Path> {
    val zipFile = File(zipPath.toString())
    if (!zipFile.exists()) return emptyList()

    val targetDir = File(framesDir.toString()).canonicalFile
    targetDir.mkdirs()

    val extracted = mutableListOf<Path>()
    val buffer = ByteArray(UGOIRA_BUFFER_SIZE)
    var entryCount = 0
    var totalBytesExtracted = 0L

    try {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.isDirectory) {
                    zis.closeEntry()
                    continue
                }
                entryCount++
                check(entryCount <= MAX_ENTRY_COUNT) {
                    "Ugoira zip entry count exceeded limit ($MAX_ENTRY_COUNT)"
                }

                if (entry.size > MAX_ENTRY_BYTES) {
                    zis.closeEntry()
                    continue
                }

                val safeName = entry.name.substringAfterLast('/').substringAfterLast('\\').trim()
                if (safeName.isEmpty() || safeName == "." || safeName == "..") {
                    zis.closeEntry()
                    continue
                }

                val outFile = File(targetDir, safeName).canonicalFile
                if (outFile.parentFile != targetDir) {
                    zis.closeEntry()
                    continue
                }

                var entryBytesWritten = 0L
                var exceededSingleLimit = false
                FileOutputStream(outFile).use { fos ->
                    while (true) {
                        val read = zis.read(buffer)
                        if (read <= 0) break
                        entryBytesWritten += read
                        if (entryBytesWritten > MAX_ENTRY_BYTES) {
                            exceededSingleLimit = true
                            break
                        }
                        totalBytesExtracted += read
                        check(totalBytesExtracted <= MAX_TOTAL_BYTES) {
                            "Ugoira zip total extracted size exceeded limit ($MAX_TOTAL_BYTES bytes)"
                        }
                        fos.write(buffer, 0, read)
                    }
                }

                if (exceededSingleLimit) {
                    outFile.delete()
                } else {
                    extracted.add(outFile.absolutePath.toPath())
                }
                zis.closeEntry()
            }
        }
    } catch (t: Throwable) {
        extracted.forEach { runCatching { File(it.toString()).delete() } }
        throw t
    }

    return extracted
}

private const val UGOIRA_BUFFER_SIZE = 16 * 1024
private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
private const val MAX_TOTAL_BYTES = 256L * 1024 * 1024
private const val MAX_ENTRY_COUNT = 512
