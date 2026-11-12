package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicLong

/**
 * Desktop 平台实现：将图片保存到用户主目录下的 Pictures/PixEz。
 */
actual class IllustSaver {
    actual suspend fun save(
        fileName: String,
        bytes: ByteArray,
        subDir: String?,
        customBasePath: String?,
    ): String = withContext(Dispatchers.IO) {
        val safeFileName = FileNamePolicy.requireSafeBaseName(fileName)
        val baseDir = if (!customBasePath.isNullOrBlank()) {
            val custom = File(customBasePath.trim())
            if (!custom.exists()) custom.mkdirs()
            custom
        } else {
            val userHome = System.getProperty("user.home")
                ?: throw IllegalStateException("无法获取用户主目录")
            val picturesDir = File(userHome, "Pictures").apply { mkdirs() }
            File(picturesDir, "PixEz").apply { mkdirs() }
        }

        val targetDir = if (!subDir.isNullOrBlank()) {
            val safeSubDir = FileNamePolicy.sanitizeSegment(subDir.trim())
            File(baseDir, safeSubDir).apply { mkdirs() }
        } else {
            baseDir
        }

        val file = File(targetDir, safeFileName).canonicalFile
        require(file.toPath().startsWith(baseDir.canonicalFile.toPath())) { "保存路径越界" }

        writeAtomically(file) { tempOut ->
            tempOut.write(bytes)
        }
    }

    actual suspend fun saveFromTempFile(
        fileName: String,
        tempFilePath: okio.Path,
        subDir: String?,
        customBasePath: String?,
    ): String = withContext(Dispatchers.IO) {
        val safeFileName = FileNamePolicy.requireSafeBaseName(fileName)
        val baseDir = if (!customBasePath.isNullOrBlank()) {
            val custom = File(customBasePath.trim())
            if (!custom.exists()) custom.mkdirs()
            custom
        } else {
            val userHome = System.getProperty("user.home")
                ?: throw IllegalStateException("无法获取用户主目录")
            val picturesDir = File(userHome, "Pictures").apply { mkdirs() }
            File(picturesDir, "PixEz").apply { mkdirs() }
        }

        val targetDir = if (!subDir.isNullOrBlank()) {
            val safeSubDir = FileNamePolicy.sanitizeSegment(subDir.trim())
            File(baseDir, safeSubDir).apply { mkdirs() }
        } else {
            baseDir
        }

        val file = File(targetDir, safeFileName).canonicalFile
        require(file.toPath().startsWith(baseDir.canonicalFile.toPath())) { "保存路径越界" }

        val tempFile = File(tempFilePath.toString())
        try {
            writeAtomically(file) { tempOut ->
                tempFile.inputStream().use { input ->
                    input.copyTo(tempOut, bufferSize = 64 * 1024)
                }
            }
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    /** 先写入同目录隐藏临时文件并刷盘，再原子替换目标文件，防止半写入破坏既有图片。 */
    private fun writeAtomically(target: File, writer: (FileOutputStream) -> Unit): String {
        val staging = File(
            target.parentFile,
            ".${target.name}.${stagingSeq.incrementAndGet()}.tmp",
        )
        try {
            FileOutputStream(staging).use { out ->
                writer(out)
                out.fd.sync()
            }
            try {
                Files.move(
                    staging.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: Exception) {
                Files.move(
                    staging.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            return target.absolutePath
        } catch (e: Exception) {
            Napier.e("保存图片失败 file=$target", e)
            runCatching { staging.delete() }
            throw e
        }
    }

    companion object {
        private val stagingSeq = AtomicLong(0L)
    }
}
