package com.perol.pixez.shared.platform

import android.os.Build
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.network.TrustedUrlPolicy
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Android 传输实现：可信重定向、原子替换、字节数截断校验。
 *
 * 摘要校验不在这里做。下载完成后的 SHA-256 统一由公共状态机调用，保证“无摘要跳过、
 * 有摘要不一致删包”只有一份判定。
 */
internal actual suspend fun downloadUpdateFile(
    downloadUrl: String,
    fileName: String,
    onProgress: (downloaded: Long, total: Long) -> Unit,
): Result<String> {
    // rename 之后才观察到取消时，withContext 会在返回边界抛 CancellationException，
    // 此时 lambda 的 finally 已经执行完（tempFile 已置空），只能在这一层删除刚转正的安装包。
    var committedTarget: File? = null
    return try {
        withContext(Dispatchers.IO) {
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
                    val urlObj = URI(currentUrl).toURL()
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
                            // 阻塞 read 不响应协程取消，只能逐块检查；否则取消后仍会把整包下完。
                            currentCoroutineContext().ensureActive()
                            val bytesRead = input.read(buffer)
                            if (bytesRead < 0) break
                            downloadedBytes += bytesRead
                            require(downloadedBytes <= MAX_UPDATE_BYTES) { "更新包超过大小限制" }
                            output.write(buffer, 0, bytesRead)
                            onProgress(downloadedBytes, totalBytes)
                        }
                        output.fd.sync()
                    }
                }
                require(tempFile.length() > 0L) { "更新包为空" }
                // 连接提前 EOF 时 totalBytes 已知却不等：截断的安装包不得转正
                require(totalBytes <= 0L || downloadedBytes == totalBytes) {
                    "更新包下载不完整: $downloadedBytes / $totalBytes"
                }
                currentCoroutineContext().ensureActive()
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
                committedTarget = targetFile
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
    } catch (e: CancellationException) {
        committedTarget?.delete()
        throw e
    }
}

private const val BUFFER_SIZE = 32 * 1024
private const val MAX_REDIRECTS = 5
private const val MAX_UPDATE_BYTES = 256L * 1024L * 1024L
private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

internal actual suspend fun scanUpdateDirectory(): List<UpdateFileEntry> = withContext(Dispatchers.IO) {
    val dir = runCatching { File(AppInstaller().getUpdateSaveDir()) }.getOrNull()
        ?: return@withContext emptyList()
    dir.listFiles()
        ?.filter { it.isFile }
        ?.map { UpdateFileEntry(it.absolutePath, it.name, it.length()) }
        ?: emptyList()
}
