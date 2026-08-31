package com.perol.pixez.shared.platform

import com.perol.pixez.shared.network.TrustedUrlPolicy
import com.perol.pixez.shared.ui.AppInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Android update downloader with trusted redirects and atomic file replacement. */
actual class AppUpdateDownloader actual constructor() {
    actual suspend fun download(
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var tempFile: File? = null
        try {
            val safeFileName = FileNamePolicy.requireSafeBaseName(fileName)
            var currentUrl = TrustedUrlPolicy.releaseAssetUrl(downloadUrl)
            val saveDir = File(AppInstaller().getUpdateSaveDir()).canonicalFile
            require(saveDir.exists() || saveDir.mkdirs()) { "无法创建更新目录" }
            val targetFile = File(saveDir, safeFileName).canonicalFile
            require(targetFile.parentFile == saveDir) { "更新文件路径越界" }
            tempFile = File(saveDir, ".${safeFileName}.${System.nanoTime()}.part")

            var redirectCount = 0
            while (true) {
                connection?.disconnect()
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "PixEz-MIUIX/${AppInfo.VERSION_NAME}")
                }
                val code = connection.responseCode
                if (code in REDIRECT_CODES) {
                    require(redirectCount++ < MAX_REDIRECTS) { "更新下载重定向次数过多" }
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("更新下载缺少重定向地址")
                    currentUrl = TrustedUrlPolicy.releaseAssetUrl(
                        URI(currentUrl).resolve(location).toString(),
                    )
                    continue
                }
                require(code in 200..299) { "HTTP 错误代码: $code" }
                break
            }

            val totalBytes = connection.contentLengthLong
            require(totalBytes <= MAX_UPDATE_BYTES || totalBytes < 0) { "更新包超过大小限制" }
            var downloadedBytes = 0L
            val buffer = ByteArray(BUFFER_SIZE)
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead < 0) break
                        downloadedBytes += bytesRead
                        require(downloadedBytes <= MAX_UPDATE_BYTES) { "更新包超过大小限制" }
                        output.write(buffer, 0, bytesRead)
                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else -1f
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.fd.sync()
                }
            }
            require(tempFile.length() > 0L) { "更新包为空" }
            require(tempFile.renameTo(targetFile)) { "无法提交更新包" }
            tempFile = null
            Result.success(targetFile.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("下载更新包失败", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
            tempFile?.delete()
        }
    }

    private companion object {
        const val BUFFER_SIZE = 32 * 1024
        const val MAX_REDIRECTS = 5
        const val MAX_UPDATE_BYTES = 256L * 1024L * 1024L
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
