package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

        try {
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            Napier.e("保存图片失败 file=$file", e)
            throw e
        }
    }
}
