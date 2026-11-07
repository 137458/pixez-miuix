package com.perol.pixez.shared.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * 一次可下载的更新包信息。
 */
data class UpdateAsset(
    val downloadUrl: String,
    val fileName: String,
    val size: Long = 0L,
    /** GitHub release 原始 digest（`sha256:<hex>`），为空表示远端未提供摘要。 */
    val sha256Digest: String? = null,
)

/**
 * 应用内更新包下载状态。
 *
 * 用 StateFlow 而不是 Compose Snapshot 状态：下载跑在 IO 线程，StateFlow 本身线程安全，
 * 弹窗与更新页各取一份 collectAsState 即可共享同一份事实源。
 */
sealed interface UpdateDownloadState {

    /** 无下载任务：初始态，也是取消后回到的态。 */
    data object Idle : UpdateDownloadState

    /**
     * 下载中。[total] 为 0 表示服务器未给出长度（分块传输），此时只能展示已下载字节数，
     * 不应把 0% 当成“进度为零”。[speedBytesPerSec] 是 EMA 平滑后的速度。
     */
    data class Downloading(
        val downloaded: Long,
        val total: Long,
        val speedBytesPerSec: Long,
    ) : UpdateDownloadState {
        val progress: Float
            get() = updateProgressFraction(downloaded, total)
    }

    /** 下载完成，[filePath] 可直接交给 [AppInstaller.install]。 */
    data class Done(val filePath: String) : UpdateDownloadState

    /** 下载失败，[message] 为内部排查信息，界面使用本地化固定文案。 */
    data class Failed(val message: String) : UpdateDownloadState
}

/** 下载进度换算：总长未知时返回 0，越界数据夹到 0..1。 */
fun updateProgressFraction(downloaded: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return (downloaded.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

/**
 * 进程级更新下载器。
 *
 * 状态挂在应用级单例（[com.perol.pixez.shared.AppDependencies]）上，协程跑在应用级 scope：
 * 弹窗关闭、页面销毁、退到后台都不取消下载；界面只观察 [state]，动作走 [start] / [cancel]。
 *
 * 代际号 [generation] 防旧协程回写：`Job.cancel()` 是异步的，取消后立刻重新开始时，
 * 旧协程可能仍会在取消生效前写出一帧进度，代际号让过期写入直接丢弃。
 *
 * 取舍：低层传输、文件摘要与删除通过构造参数注入，默认指向各平台 actual 实现；
 * 这样状态机与三态校验能在 commonTest 里用假传输完整覆盖。
 */
class AppUpdateDownloader(
    private val scope: CoroutineScope,
    private val downloadFile: suspend (
        downloadUrl: String,
        fileName: String,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) -> Result<String> = ::downloadUpdateFile,
    private val sha256OfFile: (path: String) -> String? = ::sha256HexOfFile,
    private val deleteFile: (path: String) -> Boolean = ::deleteUpdateFile,
) {

    private val _state = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    /** 当前任务的代际；只增不减，旧任务据此丢弃过期写入。 */
    private var generation = 0L
    private var job: Job? = null

    /** 开始下载 [asset]；已在下载中直接返回，避免连点重复建任务。 */
    fun start(asset: UpdateAsset) {
        if (_state.value is UpdateDownloadState.Downloading) return
        job?.cancel()
        val token = ++generation
        _state.value = UpdateDownloadState.Downloading(
            downloaded = 0L,
            total = asset.size.coerceAtLeast(0L),
            speedBytesPerSec = 0L,
        )
        job = scope.launch {
            var lastReportAt = 0L
            var lastBytes = 0L
            var speedBytesPerSec = 0L
            try {
                val result = downloadFile(asset.downloadUrl, asset.fileName) { downloaded, total ->
                    val now = Clock.System.now().toEpochMilliseconds()
                    if (lastReportAt == 0L) {
                        lastReportAt = now
                        lastBytes = downloaded
                    }
                    val elapsed = now - lastReportAt
                    if (elapsed >= PROGRESS_REPORT_INTERVAL_MS) {
                        val instant = (downloaded - lastBytes) * 1000L / elapsed
                        // 与 jm3 相同的 7:3 EMA：瞬时抖动不至于让速度数字满屏跳。
                        speedBytesPerSec = (speedBytesPerSec * 7L + instant * 3L) / 10L
                        lastReportAt = now
                        lastBytes = downloaded
                        publish(
                            token,
                            UpdateDownloadState.Downloading(
                                downloaded = downloaded,
                                total = total.coerceAtLeast(0L),
                                speedBytesPerSec = speedBytesPerSec,
                            ),
                        )
                    }
                }

                result.onSuccess { path ->
                    val expected = UpdateDigest.expectedSha256(asset.sha256Digest)
                    if (expected == null) {
                        // 远端没给可用摘要：跳过校验，不拦下载。
                        publish(token, UpdateDownloadState.Done(path))
                        return@onSuccess
                    }
                    val actual = sha256OfFile(path)
                    when (val check = UpdateDigest.verify(expected, actual)) {
                        is DigestCheck.Skipped -> publish(token, UpdateDownloadState.Done(path))
                        is DigestCheck.Matched -> publish(token, UpdateDownloadState.Done(path))
                        is DigestCheck.Mismatched -> {
                            // 内容与发布信息不一致：删包，绝不把它交给安装器。
                            deleteFile(path)
                            publish(token, UpdateDownloadState.Failed(UpdateDigest.mismatchMessage(check.expected, check.actual)))
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    publish(token, UpdateDownloadState.Failed(error.message ?: "更新包下载失败"))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                publish(token, UpdateDownloadState.Failed(error.message ?: "更新包下载失败"))
            }
        }
    }

    /** 取消下载并丢弃半成品，状态回到 [UpdateDownloadState.Idle]。 */
    fun cancel() {
        generation++
        job?.cancel()
        job = null
        _state.value = UpdateDownloadState.Idle
    }

    private fun publish(token: Long, state: UpdateDownloadState) {
        if (token == generation) {
            _state.value = state
        }
    }
}

/** 进度回调节流：网络每读一块都更新 StateFlow 会带来大量无效重组。 */
private const val PROGRESS_REPORT_INTERVAL_MS = 500L

/**
 * 平台传输实现：下载安装包到本地缓存目录，返回本地绝对路径。
 *
 * 实际字节数校验（HTTP 总长 / 截断 / 大小上限）在 actual 内完成；摘要校验在公共状态机
 * 下载完成后再做，保证“无摘要跳过、有摘要不一致删包报错”的口径只有一份。
 */
internal expect suspend fun downloadUpdateFile(
    downloadUrl: String,
    fileName: String,
    onProgress: (downloaded: Long, total: Long) -> Unit,
): Result<String>
