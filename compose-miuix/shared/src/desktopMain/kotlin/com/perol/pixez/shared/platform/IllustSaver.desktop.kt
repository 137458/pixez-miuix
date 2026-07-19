package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop 平台实现：将图片保存到用户主目录下的 Pictures/PixEz。
 */
actual class IllustSaver {
    actual suspend fun save(fileName: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val userHome = System.getProperty("user.home")
            ?: throw IllegalStateException("无法获取用户主目录")
        val picturesDir = File(userHome, "Pictures").apply { mkdirs() }
        val pixezDir = File(picturesDir, "PixEz").apply { mkdirs() }
        val file = File(pixezDir, fileName)

        try {
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            Napier.e("保存图片失败 file=$file", e)
            throw e
        }
    }
}
