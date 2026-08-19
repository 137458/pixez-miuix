package com.perol.pixez.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.stringByAppendingPathComponent
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

/**
 * macOS 平台实现：将图片保存到应用沙盒 Documents/PixEz。
 *
 * 当前先保存到应用沙盒文件系统，后续可扩展为保存到系统相册（Photos 框架）。
 */
@OptIn(ExperimentalForeignApi::class)
actual class IllustSaver {
    actual suspend fun save(fileName: String, bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val documentsDir = (NSHomeDirectory() as NSString).stringByAppendingPathComponent("Documents")
        val pixezDir = (documentsDir as NSString).stringByAppendingPathComponent("PixEz")

        // 创建目标目录，允许中间目录不存在时自动创建。
        NSFileManager.defaultManager.createDirectoryAtPath(
            pixezDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        val filePath = (pixezDir as NSString).stringByAppendingPathComponent(fileName)
        val file = fopen(filePath, "wb")
            ?: throw IllegalStateException("无法打开文件写入: $filePath")

        try {
            val written = bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
            }
            if (written != bytes.size.toULong()) {
                throw IllegalStateException("文件写入不完整: expected=${bytes.size}, written=$written")
            }
        } finally {
            fclose(file)
        }

        filePath
    }
}
