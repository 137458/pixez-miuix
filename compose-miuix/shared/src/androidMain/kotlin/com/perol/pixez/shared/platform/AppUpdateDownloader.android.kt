package com.perol.pixez.shared.platform

import android.os.Build
import java.io.File
import java.io.FileOutputStream

/**
 * Android 传输实现：委派至 JVM 共用下载实现，并保留 API 24-25 原子落盘兼容回退。
 */
internal actual suspend fun downloadUpdateFile(
    downloadUrl: String,
    fileName: String,
    onProgress: (downloaded: Long, total: Long) -> Unit,
): Result<String> = downloadUpdateFileJvm(
    downloadUrl = downloadUrl,
    fileName = fileName,
    onProgress = onProgress,
    moveAtomically = { tempFile, targetFile ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.nio.file.Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
        } else {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.inputStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
            }
        }
    },
)

internal actual suspend fun scanUpdateDirectory(): List<UpdateFileEntry> = scanUpdateDirectoryJvm()
