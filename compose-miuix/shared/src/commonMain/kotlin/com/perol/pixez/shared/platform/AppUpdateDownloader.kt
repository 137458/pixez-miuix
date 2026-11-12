package com.perol.pixez.shared.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.concurrent.Volatile

/**
 * 一次可下载更新包的版本归属。
 *
 * 下载结果必须绑定到具体 release 与资产，否则切换版本后旧包会被当成新版本安装。
 */
data class UpdateAssetIdentity(
    val releaseTag: String,
    val fileName: String,
    val sha256Digest: String?,
)

/**
 * 一次可下载的更新包信息。
 */
data class UpdateAsset(
    val downloadUrl: String,
    val fileName: String,
    val size: Long = 0L,
    /** GitHub release 原始 digest（`sha256:<hex>`），为空表示远端未提供摘要。 */
    val sha256Digest: String? = null,
    /** 资产所属 release 标识（`tagName`），用于进度与安装目标校验。 */
    val releaseTag: String = "",
) {
    val identity: UpdateAssetIdentity
        get() = UpdateAssetIdentity(releaseTag, fileName, sha256Digest)
}

/**
 * 应用内更新包下载状态。
 *
 * 用 StateFlow 而不是 Compose Snapshot 状态：下载跑在 IO 线程，StateFlow 本身线程安全，
 * 弹窗与更新页各取一份 collectAsState 即可共享同一份事实源。
 *
 * 非 Idle 态一律携带 [UpdateAssetIdentity]：界面渲染前与当前 release 比对，
 * 标识不一致时按 Idle 处理并删除旧包，安装入口也只认当前版本的文件。
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
        val target: UpdateAssetIdentity,
    ) : UpdateDownloadState {
        val progress: Float
            get() = updateProgressFraction(downloaded, total)
    }

    /** 下载完成，[filePath] 可直接交给 [AppInstaller.install]。 */
    data class Done(val filePath: String, val target: UpdateAssetIdentity) : UpdateDownloadState

    /** 下载失败，[message] 为内部排查信息，界面使用本地化固定文案。 */
    data class Failed(val message: String, val target: UpdateAssetIdentity) : UpdateDownloadState
}

/** 下载进度换算：总长未知时返回 0，越界数据夹到 0..1。 */
fun updateProgressFraction(downloaded: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return (downloaded.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

/** 当前状态所归属的资产标识；Idle 没有归属。 */
fun UpdateDownloadState.identityOrNull(): UpdateAssetIdentity? = when (this) {
    UpdateDownloadState.Idle -> null
    is UpdateDownloadState.Downloading -> target
    is UpdateDownloadState.Done -> target
    is UpdateDownloadState.Failed -> target
}

/**
 * 将状态对齐到当前 release 的资产：标识不一致一律按 Idle 处理。
 *
 * 纯函数，供弹窗与更新页在渲染前统一调用，避免两处各自判断导致口径漂移。
 */
fun UpdateDownloadState.forAsset(asset: UpdateAsset?): UpdateDownloadState {
    val identity = asset?.identity ?: return UpdateDownloadState.Idle
    return if (identityOrNull() == identity) this else UpdateDownloadState.Idle
}

/** 仅当前版本的完成态才返回可安装路径；其它情况一律 null。 */
fun UpdateDownloadState.installPathFor(asset: UpdateAsset?): String? {
    val done = this as? UpdateDownloadState.Done ?: return null
    if (asset == null || done.target != asset.identity) return null
    return done.filePath
}

/**
 * 更新包目录中的一个文件条目。[name] 为不含目录的文件名。
 *
 * 作为 [AppUpdateDownloader] 构造参数的可见类型，需与类本身同为 public。
 */
data class UpdateFileEntry(val path: String, val name: String, val size: Long)

/**
 * 更新包保存目录中的残留文件（进程被杀后遗留的完成包与 `.part` 半成品）。
 *
 * 只在恢复 [UpdateDownloadState.Done] 时扫描一次。
 */
internal expect suspend fun scanUpdateDirectory(): List<UpdateFileEntry>

/**
 * 进程级更新下载器。
 *
 * 状态挂在应用级单例（[com.perol.pixez.shared.AppDependencies]）上，协程跑在应用级 scope：
 * 弹窗关闭、页面销毁、退到后台都不取消下载；界面只观察 [state]，动作走 [start] / [cancel]。
 *
 * 代际号 [generation] 防旧协程回写：`Job.cancel()` 是异步的，取消后立刻重新开始时，
 * 旧协程可能仍会在取消生效前写出一帧进度，代际号让过期写入直接丢弃。
 * 该代际号会被下载协程（IO 线程）读取，必须用 `@Volatile` 保证跨线程可见性。
 *
 * 取舍：低层传输、文件摘要、删除与目录扫描通过构造参数注入，默认指向各平台 actual 实现；
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
    private val scanUpdateDir: suspend () -> List<UpdateFileEntry> = ::scanUpdateDirectory,
) {

    private val _state = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    /** 当前任务的代际；只增不减，旧任务据此丢弃过期写入。跨线程读取需要可见性保证。 */
    @Volatile
    private var generation = 0L
    private var job: Job? = null

    /** 开始下载 [asset]；同一资产已在下载中直接返回，避免连点重复建任务。 */
    fun start(asset: UpdateAsset) {
        val current = _state.value
        if (current is UpdateDownloadState.Downloading && current.target == asset.identity) return
        job?.cancel()
        // 切换到另一个版本：旧版本的完成包不能继续留在缓存里等待被误安装。
        if (current is UpdateDownloadState.Done && current.target != asset.identity) {
            deleteFile(current.filePath)
        }
        val token = ++generation
        _state.value = UpdateDownloadState.Downloading(
            downloaded = 0L,
            total = asset.size.coerceAtLeast(0L),
            speedBytesPerSec = 0L,
            target = asset.identity,
        )
        updateDownloadForegroundStart()
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
                                target = asset.identity,
                            ),
                        )
                    }
                }

                result.onSuccess { path ->
                    val expected = UpdateDigest.expectedSha256(asset.sha256Digest)
                    if (expected == null) {
                        // 远端没给可用摘要：跳过校验，不拦下载。
                        publish(token, UpdateDownloadState.Done(path, asset.identity))
                        return@onSuccess
                    }
                    val check = UpdateDigest.verify(expected, sha256OfFile(path))
                    // 只有 Mismatched 需要拦；Matched 放行，Skipped 已在上面提前返回。
                    if (check is DigestCheck.Mismatched) {
                        // 内容与发布信息不一致：删包，绝不把它交给安装器。
                        deleteFile(path)
                        publish(
                            token,
                            UpdateDownloadState.Failed(
                                UpdateDigest.mismatchMessage(check.expected, check.actual),
                                asset.identity,
                            ),
                        )
                    } else {
                        publish(token, UpdateDownloadState.Done(path, asset.identity))
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    publish(
                        token,
                        UpdateDownloadState.Failed(error.message ?: "更新包下载失败", asset.identity),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                publish(
                    token,
                    UpdateDownloadState.Failed(error.message ?: "更新包下载失败", asset.identity),
                )
            }
        }
    }

    /**
     * 将下载状态对齐到当前 release。
     *
     * [asset] 为空或与状态归属不一致时：取消在途任务、删除旧完成包、状态回 Idle。
     * 界面在 release 变化后调用一次，保证渲染前旧的进度/完成态已被清掉。
     *
     * 对齐后若处于 Idle，顺带尝试从磁盘恢复上一个进程留下的完成包（见 [restoreCompletedFile]）。
     */
    fun syncTarget(asset: UpdateAsset?) {
        val identity = asset?.identity
        when (val current = _state.value) {
            UpdateDownloadState.Idle -> Unit
            is UpdateDownloadState.Downloading -> {
                if (current.target != identity) {
                    job?.cancel()
                    job = null
                    generation++
                    updateDownloadForegroundStop()
                    _state.value = UpdateDownloadState.Idle
                }
            }
            is UpdateDownloadState.Done -> {
                if (current.target != identity) {
                    deleteFile(current.filePath)
                    generation++
                    _state.value = UpdateDownloadState.Idle
                }
            }
            is UpdateDownloadState.Failed -> {
                if (current.target != identity) {
                    generation++
                    _state.value = UpdateDownloadState.Idle
                }
            }
        }
        if (asset != null && _state.value == UpdateDownloadState.Idle) {
            val token = generation
            scope.launch { restoreCompletedFile(asset, token) }
        }
    }

    /**
     * 进程重启后的恢复：把磁盘上已下载完成、且与 [asset] 完全匹配的安装包接回 [UpdateDownloadState.Done]，
     * 并清理中断下载遗留的 `.part` 半成品。
     *
     * 只删除本应用命名的 `.part` 临时文件：桌面端保存目录就是用户的 Downloads 目录，
     * 扩大删除范围会误伤用户文件，因此不在这里清理其它版本的完成包。
     */
    private suspend fun restoreCompletedFile(asset: UpdateAsset, token: Long) {
        if (token != generation || _state.value != UpdateDownloadState.Idle) return
        val safeName = runCatching { FileNamePolicy.requireSafeBaseName(asset.fileName) }.getOrNull() ?: return
        val files = runCatching { scanUpdateDir() }.getOrDefault(emptyList())
        // 扫描是挂起点：期间用户可能已重新开始下载，过期恢复必须丢弃。
        if (token != generation || _state.value != UpdateDownloadState.Idle) return

        files.filter { it.name.startsWith(".") && it.name.endsWith(PART_FILE_SUFFIX) }
            .forEach { deleteFile(it.path) }

        val candidate = files.firstOrNull { it.name == safeName && it.size > 0L } ?: return
        if (asset.size > 0L && candidate.size != asset.size) return
        val expected = UpdateDigest.expectedSha256(asset.sha256Digest)
        if (expected != null &&
            UpdateDigest.verify(expected, sha256OfFile(candidate.path)) is DigestCheck.Mismatched
        ) {
            deleteFile(candidate.path)
            return
        }
        if (token != generation) return
        _state.value = UpdateDownloadState.Done(candidate.path, asset.identity)
    }

    /** 仅当前版本已下载完成时返回可安装路径。 */
    fun installPathFor(asset: UpdateAsset?): String? = _state.value.installPathFor(asset)

    /** 安装失败后清掉不可用包并回到 Idle，让用户可以直接重新下载。 */
    fun discardCompletedAfterInstallFailure(asset: UpdateAsset?) {
        val done = _state.value as? UpdateDownloadState.Done ?: return
        if (asset == null || done.target != asset.identity) return
        deleteFile(done.filePath)
        generation++
        _state.value = UpdateDownloadState.Idle
    }

    /** 取消下载并丢弃半成品，状态回到 [UpdateDownloadState.Idle]。 */
    fun cancel() {
        generation++
        job?.cancel()
        job = null
        updateDownloadForegroundStop()
        _state.value = UpdateDownloadState.Idle
    }

    private fun publish(token: Long, state: UpdateDownloadState) {
        if (token != generation) return
        _state.value = state
        when (state) {
            is UpdateDownloadState.Downloading -> {
                updateDownloadForegroundProgress(state.downloaded, state.total)
            }
            is UpdateDownloadState.Done,
            is UpdateDownloadState.Failed,
            UpdateDownloadState.Idle,
            -> updateDownloadForegroundStop()
        }
    }
}

/** 进度回调节流：网络每读一块都更新 StateFlow 会带来大量无效重组。 */
private const val PROGRESS_REPORT_INTERVAL_MS = 500L

/** 传输中断后遗留的半成品后缀（与各平台 actual 的临时文件命名一致）。 */
private const val PART_FILE_SUFFIX = ".part"

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

/** 前台保活：Android 以前台服务 + 进度通知呈现；桌面端为空实现。 */
internal expect fun updateDownloadForegroundStart()

internal expect fun updateDownloadForegroundProgress(downloaded: Long, total: Long)

internal expect fun updateDownloadForegroundStop()
