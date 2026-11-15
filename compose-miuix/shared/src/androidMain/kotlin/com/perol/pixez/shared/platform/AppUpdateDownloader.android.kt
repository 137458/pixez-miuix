package com.perol.pixez.shared.platform

/**
 * Android 传输实现：委派至 JVM 共用下载实现。
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
        // minSdk 31 恒高于 O（26），原子覆盖统一走 java.nio，不再保留 renameTo 回退。
        java.nio.file.Files.move(
            tempFile.toPath(),
            targetFile.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        )
    },
)

internal actual suspend fun scanUpdateDirectory(): List<UpdateFileEntry> = scanUpdateDirectoryJvm()
