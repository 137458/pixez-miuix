package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UgoiraFrame
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.platform.UgoiraZipExtractor
import com.perol.pixez.shared.platform.getAppCacheDirectory
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
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
    private val inFlightPreload = mutableSetOf<Int>()
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 仅读取已解码缓存，不做任何磁盘读/解码操作，可在组合期安全调用。 */
    fun getCachedFrame(index: Int): ImageBitmap? = synchronized(cache) { cache[index] }

    /** 同步取帧：缓存未命中时在调用方调度器上解码，调用方须位于 Default 等后台调度器。 */
    fun getFrameBitmap(index: Int): ImageBitmap? {
        getCachedFrame(index)?.let { return it }
        return decodeAndCache(index)
    }

    /**
     * 提交式预解码（E-2）：在 Default 调度器后台解码下一帧，调用方立即返回不等解码；
     * 已缓存或在途的帧会被忽略，缓存仍维持 MAX_DECODED_FRAMES 滑动窗口。
     */
    fun requestPreload(index: Int) {
        if (frames.isEmpty()) return
        val nextIdx = (index + 1) % frames.size
        val shouldStart = synchronized(cache) {
            // cache 的读必须在 cache 锁内（LinkedHashMap 非线程安全）；判重写回 inFlightPreload 锁
            if (cache.containsKey(nextIdx)) {
                false
            } else {
                synchronized(inFlightPreload) { inFlightPreload.add(nextIdx) }
            }
        }
        if (!shouldStart) return
        preloadScope.launch {
            try {
                decodeAndCache(nextIdx)
            } finally {
                synchronized(inFlightPreload) { inFlightPreload.remove(nextIdx) }
            }
        }
    }

    /** 会话被缓存淘汰后取消在途预解码任务并释放调度资源。 */
    fun release() {
        preloadScope.cancel()
    }

    private fun decodeAndCache(index: Int): ImageBitmap? {
        val frame = frames.getOrNull(index) ?: return null
        val bitmap = decodeFrame(frame) ?: return null
        synchronized(cache) {
            cache[index] = bitmap
            // 维持最多 8 帧已解码位图窗口，及时回收远离当前播放点的位图
            trimWindow(index)
        }
        return bitmap
    }

    private fun trimWindow(index: Int) {
        if (cache.size > MAX_DECODED_FRAMES) {
            val keysToRemove = cache.keys.filter { key ->
                val diff = kotlin.math.abs(key - index)
                val cyclicDiff = frames.size - diff
                minOf(diff, cyclicDiff) > DECODED_WINDOW_RADIUS
            }
            keysToRemove.forEach { cache.remove(it) }
        }
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
    private val map = LinkedHashMap<Long, UgoiraReadySession>()

    @Synchronized
    fun get(illustId: Long): UgoiraReadySession? = map[illustId]

    @Synchronized
    fun put(illustId: Long, session: UgoiraReadySession) {
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
        provider.release()
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
    illustId: Long,
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
    val framesDir = getAppCacheDirectory() / "ugoira_frames_${illustId}"
    try {
        // 流式解压：解压器直接从临时 zip 逐 entry 写帧文件，不再整体驻留内存
        val extractedNames = if (tempZipPath != null) {
            withContext(Dispatchers.IO) { UgoiraZipExtractor().extractFrames(tempZipPath, framesDir) }
        } else {
            emptyList()
        }
        val extractedNameSet = extractedNames.map { it.name }.toSet()
        val validFrames = metadataResponse.ugoiraMetadata.frames.filter { it.file in extractedNameSet }

        if (validFrames.isEmpty()) {
            // 无有效帧：清理临时 zip 与空帧目录后返回，避免残留累积。
            cleanupUgoiraLoadFailure(illustId, tempZipPath, framesDir)
            return null
        }

        val provider = UgoiraFrameProvider(validFrames, framesDir)
        withContext(Dispatchers.Default) {
            provider.getFrameBitmap(0)
            provider.requestPreload(0)
        }
        val session = UgoiraReadySession(
            provider = provider,
            tempZipPath = tempZipPath,
            framesDir = framesDir,
            zipUrl = zipUrl,
        )
        UgoiraSessionCache.put(illustId, session)
        return session
    } catch (e: Throwable) {
        // 失败路径同样清理临时文件：反复失败时缓存目录可能累积数百 MB 残留。
        cleanupUgoiraLoadFailure(illustId, tempZipPath, framesDir)
        throw e
    }
}

/**
 * 删除一次失败的 ugoira 加载留下的临时 zip 与帧目录；路径可能不存在，尽力清理即可。
 * 同作品的并发加载可能已成功入缓存并开始播放，此时帧目录仍在被读取，须保留。
 */
private fun cleanupUgoiraLoadFailure(illustId: Long, tempZipPath: Path?, framesDir: Path) {
    tempZipPath?.let { runCatching { FileSystem.SYSTEM.delete(it) } }
    if (UgoiraSessionCache.get(illustId) == null) {
        runCatching { FileSystem.SYSTEM.deleteRecursively(framesDir) }
    }
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

            session.provider.requestPreload(frameIndex)
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
    val cachedSession = UgoiraSessionCache.get(illust.id)
    val cachedZipPath = cachedSession?.tempZipPath
    val cachedBytes = if (cachedZipPath != null) {
        withContext(Dispatchers.IO) {
            runCatching {
                if (FileSystem.SYSTEM.exists(cachedZipPath)) {
                    FileSystem.SYSTEM.read(cachedZipPath) { readByteArray() }.takeIf { it.isNotEmpty() }
                } else null
            }.getOrNull()
        }
    } else null

    if (cachedSession != null && cachedBytes != null && cachedSession.zipUrl.isNotBlank()) {
        return@suspendRunCatchingNonCancel downloadRepository.saveUgoiraZip(
            illust = illust,
            bytes = cachedBytes,
            zipUrl = cachedSession.zipUrl,
        )
    }

    val meta = illustRepository.getUgoiraMetadata(illust.id)
    val zipUrl = meta.ugoiraMetadata.zipUrls.medium
    val zipBytes = illustRepository.downloadUgoiraZip(zipUrl)
    downloadRepository.saveUgoiraZip(illust = illust, bytes = zipBytes, zipUrl = zipUrl)
}
