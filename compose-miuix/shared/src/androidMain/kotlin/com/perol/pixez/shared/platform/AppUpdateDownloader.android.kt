package com.perol.pixez.shared.platform

import com.perol.pixez.shared.ui.AppInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android 平台更新包下载器实现：支持断点覆写、流式进度回调并写入 updates 缓存目录。
 */
actual class AppUpdateDownloader actual constructor() {
    actual suspend fun download(
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val saveDir = File(AppInstaller().getUpdateSaveDir())
            if (!saveDir.exists()) {
                saveDir.mkdirs()
            }
            val targetFile = File(saveDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var currentUrl = downloadUrl
            var redirectCount = 0
            while (redirectCount < 5) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "PixEz-MIUIX/${AppInfo.VERSION_NAME}")
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val responseCode = connection?.responseCode ?: -1
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP 错误代码: $responseCode")
            }

            val totalBytes = connection?.contentLengthLong ?: -1L
            val inputStream = connection?.inputStream
                ?: throw IllegalStateException("无法获取网络输入流")

            var downloadedBytes = 0L
            val buffer = ByteArray(32768)

            FileOutputStream(targetFile).use { output ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    } else {
                        -1f
                    }
                    onProgress(progress, downloadedBytes, totalBytes)
                }
                output.flush()
            }

            Result.success(targetFile.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("下载更新包失败: $downloadUrl", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
