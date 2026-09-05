package com.perol.pixez.shared.platform

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val targetUri = if (isZip) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val relativePath = if (isZip) {
                Environment.DIRECTORY_DOWNLOADS + File.separator + "PixEz" + relativeSubDir
            } else {
                Environment.DIRECTORY_PICTURES + File.separator + "PixEz" + relativeSubDir
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(targetUri, contentValues)
                ?: throw IllegalStateException("MediaStore 插入失败: $fileName")

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                } ?: throw IllegalStateException("无法打开输出流: $fileName")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

            uri.toString()
        } else {
            val baseDir = if (isZip) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            val targetBaseDir = if (!customBasePath.isNullOrBlank()) {
                File(customBasePath.trim()).apply { mkdirs() }
            } else {
                File(baseDir, "PixEz").apply { mkdirs() }
            }
            val targetDir = if (!subDir.isNullOrBlank()) {
                File(targetBaseDir, FileNamePolicy.sanitizeSegment(subDir.trim())).apply { mkdirs() }
            } else {
                targetBaseDir
            }

            val file = File(targetDir, safeFileName).canonicalFile
            require(file.toPath().startsWith(targetBaseDir.canonicalFile.toPath())) { "保存路径越界" }
            FileOutputStream(file).use { outputStream ->
                outputStream.write(bytes)
            }

            // 刷新图库，使非 zip 图片立即在相册应用中可见。
            if (!isZip) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: Napier.w("MediaStore 刷新失败: $fileName")
            }

            file.absolutePath
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
