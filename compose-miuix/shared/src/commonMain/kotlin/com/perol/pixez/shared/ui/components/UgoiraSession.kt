package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UgoiraFrame
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.platform.UgoiraZipExtractor
import com.perol.pixez.shared.platform.getAppCacheDirectory
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Ugoira 动图双缓冲滑动窗口帧解码器。
 *
 * 避免一次性将 100+ 帧全部解码为 ImageBitmap 驻留堆引发 OOM；
 * 仅在内存中维护当前播放窗口附近的 ImageBitmap，并在后台预解码下一帧。
 *
 * 帧文件读写与位图缓存都可能被并发的播放循环访问，因此内部状态以 [synchronized]
 * 保护；解码本身在锁外完成，避免长耗时的解码阻塞其他访问者。
 */
internal class UgoiraFrameProvider(
    val frames: List<UgoiraFrame>,
    private val framesDir: Path,
) {
    private val cache = mutableMapOf<Int, ImageBitmap>()

    fun getFrameBitmap(index: Int): ImageBitmap? {
        synchronized(cache) { cache[index] }?.let { return it }
        val frame = frames.getOrNull(index) ?: return null
        val bitmap = decodeFrame(frame) ?: return null
        synchronized(cache) {
            cache[index] = bitmap
            // 维持最多 8 帧已解码位图窗口，及时回收远离当前播放点的位图
            if (cache.size > MAX_DECODED_FRAMES) {
                val keysToRemove = cache.keys.filter { key ->
                    val diff = kotlin.math.abs(key - index)
                    val cyclicDiff = frames.size - diff
                    minOf(diff, cyclicDiff) > DECODED_WINDOW_RADIUS
                }
                keysToRemove.forEach { cache.remove(it) }
            }
        }
        return bitmap
    }

    fun preloadNext(index: Int) {
        if (frames.isEmpty()) return
        val nextIdx = (index + 1) % frames.size
        if (synchronized(cache) { cache.containsKey(nextIdx) }) return
        val frame = frames.getOrNull(nextIdx) ?: return
        val bitmap = decodeFrame(frame) ?: return
        synchronized(cache) { cache[nextIdx] = bitmap }
    }

    /** 从磁盘读取并解码一帧；文件缺失或解码失败时返回 null。 */
    private fun decodeFrame(frame: UgoiraFrame): ImageBitmap? {
        val framePath = framesDir / frame.file
        val bytes = runCatching {
            if (FileSystem.SYSTEM.exists(framePath)) {
                FileSystem.SYSTEM.read(framePath) { readByteArray() }
            } else {
                null
            }
        }.getOrNull() ?: return null
        return runCatching { bytes.decodeToImageBitmap() }.getOrNull()
    }

    private companion object {
        const val MAX_DECODED_FRAMES = 8
        const val DECODED_WINDOW_RADIUS = 3
    }
}

/**
 * 一次已就绪的 Ugoira 播放会话：解码器 + 其帧来源的临时文件。
 *
 * 该对象由 [UgoiraSessionCache] 按作品 ID 复用，因此**必须跨视图共享**：
 * 详情页内嵌播放器与全屏查看器会同时持有同一实例。
 *
 * 播放进度不存放在这里——活跃帧索引由各播放视图各自持有（见 [UgoiraPlayback]），
 * 跨视图续播所需的起始帧通过 [beginPlayback] 快照传递，避免两个并发循环互相改写同一状态。
 */
internal class UgoiraReadySession(
    val provider: UgoiraFrameProvider,
    val tempZipPath: Path?,
    val framesDir: Path?,
    val zipUrl: String,
) {
    /** 最近一次播放进度，仅在视图进入/离开时读写，作为跨视图续播的交接点。 */
    private var lastFrameIndex: Int = 0

    /**
     * 领取一次播放并取得起始帧索引（上一次会话离开时的进度，首次为 0）。
     *
     * 内嵌播放器与全屏查看器各自调用一次，互不干扰。
     */
    @Synchronized
    fun beginPlayback(): Int = lastFrameIndex.coerceAtLeast(0)

    /** 播报本次播放进度，供后续视图续播。 */
    @Synchronized
    fun reportProgress(index: Int) {
        lastFrameIndex = index
    }
}

/**
 * 进程内轻量级最近动图会话缓存（最多保留 [MAX_ENTRIES] 个作品），
 * 确保从详情页内嵌视图切换到全屏查看器时零延迟无缝续播，不重复请求网络。
 *
 * 缓存被多个组合作用域的协程并发访问，故以 [synchronized] 保护；淘汰出的会话
 * 其临时 zip 与帧目录一并清理。
 */
internal object UgoiraSessionCache {
    private const val MAX_ENTRIES = 2
    private val map = LinkedHashMap<Int, UgoiraReadySession>()

    @Synchronized
    fun get(illustId: Int): UgoiraReadySession? = map[illustId]

    @Synchronized
    fun put(illustId: Int, session: UgoiraReadySession) {
        map[illustId] = session
        while (map.size > MAX_ENTRIES) {
            val eldestKey = map.entries.firstOrNull()?.key ?: break
            map.remove(eldestKey)?.deleteFiles()
        }
    }

    /** 清空全部会话并回收其临时文件。 */
    @Synchronized
    fun clear() {
        map.values.forEach { it.deleteFiles() }
        map.clear()
    }

    private fun UgoiraReadySession.deleteFiles() {
        tempZipPath?.let { runCatching { FileSystem.SYSTEM.delete(it) } }
        framesDir?.let { runCatching { FileSystem.SYSTEM.deleteRecursively(it) } }
    }
}

/**
 * 加载（或复用）某个作品的 Ugoira 播放会话。
 *
 * 内嵌播放器与全屏查看器共用这一条流水线：命中缓存直接返回，
 * 否则依次拉取元数据、下载 zip、落盘、解包写帧、预热首帧，最后入缓存。
 *
 * @param illustId 作品 ID。
 * @param illustRepository 用于获取元数据与 zip。
 * @param onStage 阶段回调，供调用方更新加载文案。
 * @return 就绪会话；元数据无有效帧时返回 null。
 */
internal suspend fun loadUgoiraSession(
    illustId: Int,
    illustRepository: IllustRepository,
    onStage: (ugoiraStage: UgoiraLoadStage) -> Unit,
): UgoiraReadySession? {
    UgoiraSessionCache.get(illustId)?.let { return it }

    onStage(UgoiraLoadStage.LoadingMetadata)
    val metadataResponse = illustRepository.getUgoiraMetadata(illustId)
    val zipUrl = metadataResponse.ugoiraMetadata.zipUrls.medium

    onStage(UgoiraLoadStage.Downloading)
    val zipBytes = illustRepository.downloadUgoiraZip(zipUrl)

    val tempZipPath = withContext(Dispatchers.IO) {
        val path = getAppCacheDirectory() / "ugoira_temp_${illustId}.zip"
        runCatching {
            FileSystem.SYSTEM.write(path) { write(zipBytes) }
            path
        }.getOrNull()
    }

    onStage(UgoiraLoadStage.Extracting)
    val (framesDir, validFrames) = withContext(Dispatchers.IO) {
        val dir = getAppCacheDirectory() / "ugoira_frames_${illustId}"
        FileSystem.SYSTEM.createDirectories(dir)
        val frameMap = UgoiraZipExtractor().extractFrames(zipBytes)
        for ((fileName, bytes) in frameMap) {
            FileSystem.SYSTEM.write(dir / fileName) { write(bytes) }
        }
        val valid = metadataResponse.ugoiraMetadata.frames.filter { frameMap.containsKey(it.file) }
        dir to valid
    }

    if (validFrames.isEmpty()) return null

    val provider = UgoiraFrameProvider(validFrames, framesDir)
    withContext(Dispatchers.Default) {
        provider.getFrameBitmap(0)
        provider.preloadNext(0)
    }
    val session = UgoiraReadySession(
        provider = provider,
        tempZipPath = tempZipPath,
        framesDir = framesDir,
        zipUrl = zipUrl,
    )
    UgoiraSessionCache.put(illustId, session)
    return session
}

/** [loadUgoiraSession] 的加载阶段，用于向用户呈现进度文案。 */
internal enum class UgoiraLoadStage {
    LoadingMetadata,
    Downloading,
    Extracting,
}

/**
 * 驱动 Ugoira 逐帧循环，直到调用方协程被取消。
 *
 * 帧索引由本函数独占维护并作为参数回传给调用方渲染，多个视图各自启动一条循环时
 * 互不干扰；进度同时回写到 [session]，供下一个视图续播。
 *
 * @param session 已就绪的播放会话。
 * @param startFrameIndex 起始帧索引。
 * @param onFrame 每帧回调，参数为应当渲染的帧索引；本视图据此渲染。
 * @param onProgress 进度回调，用于把播放位置回写给会话以支持跨视图续播。
 */
internal suspend fun runUgoiraFrameLoop(
    session: UgoiraReadySession,
    startFrameIndex: Int,
    onFrame: (frameIndex: Int) -> Unit,
    onProgress: (frameIndex: Int) -> Unit,
) {
    val frames = session.provider.frames
    if (frames.isEmpty()) return

    var frameIndex = startFrameIndex.coerceIn(0, frames.lastIndex)
    var nextFrameTargetTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    try {
        while (currentCoroutineContext().isActive) {
            onFrame(frameIndex)
            val expectedDelay = frames.getOrNull(frameIndex)?.delay?.toLong()?.coerceAtLeast(MIN_FRAME_DELAY_MILLIS)
                ?: MIN_FRAME_DELAY_MILLIS
            nextFrameTargetTime += expectedDelay

            session.provider.preloadNext(frameIndex)
            frameIndex = (frameIndex + 1) % frames.size
            onProgress(frameIndex)

            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val waitTime = nextFrameTargetTime - now
            if (waitTime > 0L) {
                delay(waitTime)
            } else if (now - nextFrameTargetTime > expectedDelay * 2) {
                nextFrameTargetTime = now
            }
        }
    } finally {
        session.reportProgress(frameIndex)
    }
    // 循环仅在协程取消时退出，此处不额外处理。
}

/** 单帧最短展示时长，避免异常元数据导致空转。 */
private const val MIN_FRAME_DELAY_MILLIS = 10L

/**
 * 保存一个 Ugoira 作品：拉取元数据与原始 zip 后交给 [DownloadRepository] 落盘并记录下载历史。
 *
 * 详情页顶栏与全屏查看器共用这一条保存路径，避免两处各自维护一份
 * 「取元数据 → 下 zip → 保存」的流程而出现口径漂移。
 *
 * @param illust 目标作品。
 * @param illustRepository 元数据与 zip 数据源。
 * @param downloadRepository 落盘与历史记录。
 * @return 保存结果，失败时携带异常。
 */
internal suspend fun saveUgoiraIllust(
    illust: Illust,
    illustRepository: IllustRepository,
    downloadRepository: DownloadRepository,
): Result<String> = suspendRunCatchingNonCancel {
    val meta = illustRepository.getUgoiraMetadata(illust.id)
    val zipUrl = meta.ugoiraMetadata.zipUrls.medium
    val zipBytes = illustRepository.downloadUgoiraZip(zipUrl)
    downloadRepository.saveUgoiraZip(illust = illust, bytes = zipBytes, zipUrl = zipUrl)
}
