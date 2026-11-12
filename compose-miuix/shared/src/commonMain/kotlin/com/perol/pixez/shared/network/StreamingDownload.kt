package com.perol.pixez.shared.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/**
 * 跨平台 HTTP 流式下载工具：
 * 统一收敛大文件（原图与动图 Zip）的唯一临时文件路径生成、响应状态校验、
 * Content-Length 预检、64KB 缓冲分块写入与流中上限控制，并在失败或取消时清理残留。
 */
internal object StreamingDownload {
    private val seqMutex = Mutex()
    private var seqCounter = 0L

    /** 生成同一毫秒内也不冲突的唯一临时文件路径，并确保父目录存在。 */
    suspend fun createUniqueTempFile(directory: Path, prefix: String, suffix: String): Path {
        val seq = seqMutex.withLock { ++seqCounter }
        val now = Clock.System.now().toEpochMilliseconds()
        withContext(Dispatchers.IO) {
            FileSystem.SYSTEM.createDirectories(directory)
        }
        return directory / "${prefix}${now}_$seq$suffix"
    }

    /**
     * 将 [response] 响应体流式写入 [targetFile]。
     *
     * @throws IllegalStateException 状态码非 2xx、声明/实收字节超过 [maxBytes] 或与 Content-Length 不匹配时抛出。
     */
    suspend fun writeResponseToFile(
        response: HttpResponse,
        targetFile: Path,
        maxBytes: Long,
        failureLabel: String,
    ): Path {
        val fileSystem = FileSystem.SYSTEM
        try {
            if (!response.status.isSuccess()) {
                throw IllegalStateException("$failureLabel: HTTP ${response.status.value}")
            }
            val declaredLength = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull()
            require(declaredLength == null || declaredLength <= maxBytes) {
                "下载内容声明大小超过上限: $declaredLength"
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            withContext(Dispatchers.IO) {
                fileSystem.write(targetFile) {
                    val buffer = ByteArray(64 * 1024)
                    var totalBytes = 0L
                    while (!channel.isClosedForRead) {
                        currentCoroutineContext().ensureActive()
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read < 0) break
                        if (read == 0) continue
                        totalBytes += read
                        require(totalBytes <= maxBytes) {
                            "下载内容超过大小上限: $totalBytes"
                        }
                        write(buffer, 0, read)
                    }
                    require(declaredLength == null || totalBytes == declaredLength) {
                        "下载内容不完整: 实收 $totalBytes, 声明 $declaredLength"
                    }
                }
            }
            return targetFile
        } catch (t: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                fileSystem.delete(targetFile, mustExist = false)
            }
            throw t
        }
    }
}
