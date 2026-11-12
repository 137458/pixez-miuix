package com.perol.pixez.shared.platform

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Desktop 传输实现：委派至 JVM 共用下载实现（Ktor DoH / 代理 + 可信重定向 + fd.sync）。
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
        Files.move(
            tempFile.toPath(),
            targetFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    },
)

internal actual suspend fun scanUpdateDirectory(): List<UpdateFileEntry> = scanUpdateDirectoryJvm()
