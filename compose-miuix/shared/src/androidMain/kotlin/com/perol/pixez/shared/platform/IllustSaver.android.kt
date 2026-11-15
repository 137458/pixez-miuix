package com.perol.pixez.shared.platform

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android 平台实现：将图片保存到公共 Pictures/PixEz 目录（ZIP 保存到公共 Downloads/PixEz 目录），并刷新 MediaStore。
 */
actual class IllustSaver {
    actual suspend fun save(
        fileName: String,
        bytes: ByteArray,
        subDir: String?,
        customBasePath: String?,
    ): String = withContext(Dispatchers.IO) {
        val safeFileName = FileNamePolicy.requireSafeBaseName(fileName)
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 applicationContext")

        val isZip = fileName.endsWith(".zip", ignoreCase = true)
        val mimeType = if (isZip) "application/zip" else getMimeType(fileName)
        val relativeSubDir = if (!subDir.isNullOrBlank()) {
            File.separator + FileNamePolicy.sanitizeSegment(subDir.trim())
        } else ""

        val baseFolderName = customBasePath?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
            FileNamePolicy.sanitizeSegment(File(raw).name).takeIf { it.isNotBlank() }
        } ?: "PixEz"

        // minSdk 31 恒高于 Q（29）：分区存储恒可用，旧的公共目录直写 + MediaStore 刷新回退已删除。
        val targetUri = if (isZip) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val relativePath = if (isZip) {
            Environment.DIRECTORY_DOWNLOADS + File.separator + baseFolderName + relativeSubDir
        } else {
            Environment.DIRECTORY_PICTURES + File.separator + baseFolderName + relativeSubDir
        }

        val resolver = context.contentResolver
        saveToMediaStoreQ(
            resolver = resolver,
            targetUri = targetUri,
            safeFileName = safeFileName,
            displayErrorName = fileName,
            mimeType = mimeType,
            relativePath = relativePath,
            writer = { outputStream -> outputStream.write(bytes) },
        ).toString()
    }

    actual suspend fun saveFromTempFile(
        fileName: String,
        tempFilePath: okio.Path,
        subDir: String?,
        customBasePath: String?,
    ): String = withContext(Dispatchers.IO) {
        val safeFileName = FileNamePolicy.requireSafeBaseName(fileName)
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 applicationContext")

        val isZip = fileName.endsWith(".zip", ignoreCase = true)
        val mimeType = if (isZip) "application/zip" else getMimeType(fileName)
        val relativeSubDir = if (!subDir.isNullOrBlank()) {
            File.separator + FileNamePolicy.sanitizeSegment(subDir.trim())
        } else ""

        val baseFolderName = customBasePath?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
            FileNamePolicy.sanitizeSegment(File(raw).name).takeIf { it.isNotBlank() }
        } ?: "PixEz"

        val tempFile = File(tempFilePath.toString())
        require(tempFile.exists() && tempFile.length() > 0) { "临时文件不存在或为空: $tempFilePath" }

        try {
            // minSdk 31 恒高于 Q（29）：同 save()，分区存储单路径即可。
            val targetUri = if (isZip) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val relativePath = if (isZip) {
                Environment.DIRECTORY_DOWNLOADS + File.separator + baseFolderName + relativeSubDir
            } else {
                Environment.DIRECTORY_PICTURES + File.separator + baseFolderName + relativeSubDir
            }

            saveToMediaStoreQ(
                resolver = context.contentResolver,
                targetUri = targetUri,
                safeFileName = safeFileName,
                displayErrorName = fileName,
                mimeType = mimeType,
                relativePath = relativePath,
                writer = { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                },
            ).toString()
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private inline fun saveToMediaStoreQ(
        resolver: android.content.ContentResolver,
        targetUri: android.net.Uri,
        safeFileName: String,
        displayErrorName: String,
        mimeType: String,
        relativePath: String,
        writer: (java.io.OutputStream) -> Unit,
    ): android.net.Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(targetUri, contentValues)
            ?: throw IllegalStateException("MediaStore 插入失败: $displayErrorName")

        try {
            resolver.openOutputStream(uri)?.use(writer)
                ?: throw IllegalStateException("无法打开输出流: $displayErrorName")

            // 写盘完成后再移除同名旧记录并取消 pending 标记，避免写入失败时误删既有文件。
            runCatching {
                resolver.delete(
                    targetUri,
                    "${MediaStore.MediaColumns._ID} != ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND (${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} = ?)",
                    arrayOf(
                        uri.lastPathSegment.orEmpty(),
                        safeFileName,
                        relativePath,
                        "$relativePath/",
                    ),
                )
            }
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "zip" -> "application/zip"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
