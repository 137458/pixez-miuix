package com.perol.pixez.shared.platform

import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.network.TrustedUrlPolicy
import com.perol.pixez.shared.network.createPlatformHttpClient
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/**
 * JVM 双端（Android / Desktop）共用的更新包流式下载实现：
 * 统一复用 Ktor `updateDownloadClient`（DoH / 代理 / 连接池），并执行可信重定向校验、
 * 协程取消检查、落盘 `fd.sync()`、截断校验与原子替换提交。
 */
internal suspend fun downloadUpdateFileJvm(
    downloadUrl: String,
    fileName: String,
    onProgress: (downloaded: Long, total: Long) -> Unit,
    moveAtomically: (tempFile: File, targetFile: File) -> Unit,
): Result<String> {
    var committedTarget: File? = null
    return try {
        withContext(Dispatchers.IO) {
            var tempFile: File? = null
            var fallbackClient: HttpClient? = null
            try {
                val client = AppDependencies.orNull()?.httpClient?.updateDownloadClient
                    ?: createPlatformHttpClient(followRedirects = false).also { fallbackClient = it }

                val safeFileName = FileNamePolicy.requireSafeBaseName(fileName)
                var currentUrl = TrustedUrlPolicy.releaseAssetUrl(downloadUrl)
                val saveDir = File(AppInstaller().getUpdateSaveDir()).canonicalFile
                require(saveDir.exists() || saveDir.mkdirs()) { "无法创建更新目录" }
                val targetFile = File(saveDir, safeFileName).canonicalFile
                require(targetFile.parentFile == saveDir) { "更新文件路径越界" }
                val workFile = File(saveDir, ".${safeFileName}.${System.nanoTime()}.part")
                tempFile = workFile

                var redirectCount = 0
                while (true) {
                    val requestUrl = currentUrl
                    val outcome = client.prepareGet(requestUrl) {
                        header(HttpHeaders.UserAgent, "PixEz-MIUIX/${AppInfo.VERSION_NAME}")
                    }.execute { response ->
                        if (response.status.value in UPDATE_REDIRECT_CODES) {
                            UpdateDownloadOutcome.Redirect(response.headers[HttpHeaders.Location])
                        } else {
                            require(response.status.value in 200..299) {
                                "HTTP 错误代码: ${response.status.value}"
                            }
                            val totalBytes = response.contentLength() ?: -1L
                            require(totalBytes <= MAX_UPDATE_BYTES || totalBytes < 0) { "更新包超过大小限制" }
                            var downloadedBytes = 0L
                            val buffer = ByteArray(UPDATE_BUFFER_SIZE)
                            val channel = response.bodyAsChannel()
                            FileOutputStream(workFile).use { output ->
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                                    if (bytesRead < 0) break
                                    downloadedBytes += bytesRead
                                    require(downloadedBytes <= MAX_UPDATE_BYTES) { "更新包超过大小限制" }
                                    output.write(buffer, 0, bytesRead)
                                    onProgress(downloadedBytes, totalBytes)
                                }
                                output.fd.sync()
                            }
                            require(workFile.length() > 0L) { "更新包为空" }
                            require(totalBytes <= 0L || downloadedBytes == totalBytes) {
                                "更新包下载不完整: $downloadedBytes / $totalBytes"
                            }
                            UpdateDownloadOutcome.Completed
                        }
                    }

                    val redirect = outcome as? UpdateDownloadOutcome.Redirect ?: break
                    require(redirectCount++ < UPDATE_MAX_REDIRECTS) { "更新下载重定向次数过多" }
                    val location = redirect.location ?: throw IllegalStateException("更新下载缺少重定向地址")
                    currentUrl = TrustedUrlPolicy.releaseAssetUrl(
                        URI(requestUrl).resolve(location).toString(),
                    )
                }

                currentCoroutineContext().ensureActive()
                moveAtomically(workFile, targetFile)
                committedTarget = targetFile
                tempFile = null
                Result.success(targetFile.absolutePath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("下载更新包失败", e)
                Result.failure(e)
            } finally {
                tempFile?.delete()
                runCatching { fallbackClient?.close() }
            }
        }
    } catch (e: CancellationException) {
        committedTarget?.delete()
        throw e
    }
}

internal suspend fun scanUpdateDirectoryJvm(): List<UpdateFileEntry> = withContext(Dispatchers.IO) {
    val dir = runCatching { File(AppInstaller().getUpdateSaveDir()) }.getOrNull()
        ?: return@withContext emptyList()
    dir.listFiles()
        ?.filter { it.isFile }
        ?.map { UpdateFileEntry(it.absolutePath, it.name, it.length()) }
        ?: emptyList()
}

private sealed interface UpdateDownloadOutcome {
    data class Redirect(val location: String?) : UpdateDownloadOutcome
    data object Completed : UpdateDownloadOutcome
}

private const val UPDATE_BUFFER_SIZE = 32 * 1024
private const val UPDATE_MAX_REDIRECTS = 5
private const val MAX_UPDATE_BYTES = 256L * 1024L * 1024L
private val UPDATE_REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
