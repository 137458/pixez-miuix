package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTask
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.AppErrorKind
import com.perol.pixez.shared.network.PixivApiException
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.platform.DownloadNotifier
import com.perol.pixez.shared.platform.FileNamePolicy
import com.perol.pixez.shared.platform.IllustSaver
import com.perol.pixez.shared.platform.mapToPictureSource
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import okio.FileSystem
import okio.Path
import com.perol.pixez.shared.platform.getAppCacheDirectory
import kotlinx.datetime.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** 用户未自定义命名模板时的默认文件名格式。 */
private const val DEFAULT_NAME_FORMAT = "{illust_id}_p{part}"

/**
 * 插画下载仓库：负责解析原图 URL、下载图片字节并调用平台保存，
 * 同时通过 [DownloadHistoryRepository] 将任务状态写入本地历史。
 *
 * 提供单页 [download] 与多页 [downloadAllPages] 下载入口，支持 Android 16 实时动态胶囊通知。
 */
class DownloadRepository(
    private val httpClient: HttpClient = HttpClient(),
    private val saver: IllustSaver = IllustSaver(),
    private val historyRepository: DownloadHistoryRepository? = null,
    private val notifier: DownloadNotifier = DownloadNotifier(),
    private val settingsRepository: SettingsRepository,
    /** 流式下载大小上限（字节），防恶意/异常服务器无限写盘；默认与更新器上限对齐。 */
    private val maxDownloadBytes: Long = DEFAULT_MAX_DOWNLOAD_BYTES,
) {
    // P-5：任务状态变更事件（下载中/成功/失败写入历史后发出）。
    // UI 层收集后触发列表刷新，替代秒级全表轮询；tryEmit 无订阅者时丢弃（刷新非关键路径）。
    private val _taskEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val taskEventFlow: SharedFlow<Unit> = _taskEventFlow.asSharedFlow()

    private fun notifyTaskChanged() {
        _taskEventFlow.tryEmit(Unit)
    }

    /**
     * 下载指定作品的指定页原图并保存到本地。
     *
     * 下载前先将任务以 [Downloading][DownloadStatus.Downloading] 状态写入历史，
     * 完成后更新为 [Success][DownloadStatus.Success] 或 [Failed][DownloadStatus.Failed]。
     *
     * @param illust 目标作品
     * @param pageIndex 页码，单页作品传 0
     * @return 包含最终状态与保存路径/错误信息的下载任务
     */
    suspend fun download(illust: Illust, pageIndex: Int): DownloadTask =
        downloadPage(illust, pageIndex).task

    /**
     * 单页下载的实现（不放保活、不发通知之外的差异）。
     *
     * 多页作品逐页执行时由 [downloadAllPages] 统一占用保活与进度通知，
     * 因此这里的 keepAlive / notify 只在单页作品上开启，与历史行为一致。
     */
    private suspend fun downloadPage(illust: Illust, pageIndex: Int): TaskOutcome {
        val remoteUrl = resolveOriginalUrl(illust, pageIndex)
        val fileName = buildFileName(illust, pageIndex, remoteUrl)
        val (subDir, customBasePath) = resolveSubDirAndBasePath(illust)
        val singlePageTask = illust.pageCount <= 1
        val pendingTask = DownloadTask(
            illustId = illust.id,
            pageIndex = pageIndex,
            remoteUrl = remoteUrl,
            fileName = fileName,
            status = DownloadStatus.Downloading,
        )

        return runTask(
            history = downloadTaskHistoryOf(pendingTask, illust),
            errorCode = AppErrorKind.DOWNLOAD_FAILED.code,
            keepAlive = singlePageTask,
            notifyProgress = singlePageTask,
            notifyFinishedResult = singlePageTask,
            throwOnFailure = false,
            startWriteTolerant = false,
        ) {
            val tempFileName = "dl_temp_${illust.id}_${pageIndex}_${Clock.System.now().toEpochMilliseconds()}.tmp"
            val tempPath = downloadToTempFile(remoteUrl, tempFileName)
            saver.saveFromTempFile(fileName, tempPath, subDir = subDir, customBasePath = customBasePath)
                .also { Napier.d("下载完成 path=$it") }
        }
    }

    /**
     * 下载作品全部页原图并保存到本地。
     *
     * 按页码顺序受控并发下载（支持根据设置限制并发度），实时向系统状态栏发送 Android 16 动态胶囊进度通知。
     *
     * @param illust 目标作品
     * @param onProgress 进度回调，参数为 (已完成数量, 总页数)
     * @param maxConcurrency 最大并发下载数，限制在 1..6 之间，默认 3
     * @return 按原页码顺序排列的下载任务列表
     */
    suspend fun downloadAllPages(
        illust: Illust,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        maxConcurrency: Int = settingsRepository.maxRunningTask,
    ): List<DownloadTask> = coroutineScope {
        val total = illust.pageCount
        if (total <= 0) return@coroutineScope emptyList()
        // 多页才由聚合层统一占用保活并只发一次结束通知；
        // 单页作品交给 downloadPage 走单任务路径，避免同一作品被通知两次。
        val aggregate = total > 1
        if (aggregate) {
            com.perol.pixez.shared.platform.PlatformDownloadKeeper.acquire(illust.id)
            notifier.notifyProgress(illust.id, illust.title, 0, total)
        }
        val semaphore = Semaphore(coerceParallelTasks(maxConcurrency))
        val progressMutex = Mutex()
        var completedCount = 0

        try {
            val deferredTasks = (0 until total).map { pageIndex ->
                async {
                    val outcome = semaphore.withPermit {
                        downloadPage(illust, pageIndex)
                    }
                    progressMutex.withLock {
                        completedCount++
                        val current = completedCount
                        onProgress?.invoke(current, total)
                        if (aggregate) notifier.notifyProgress(illust.id, illust.title, current, total)
                    }
                    outcome.task
                }
            }
            val results = deferredTasks.awaitAll()
            if (aggregate) {
                val successCount = results.count { it.status == DownloadStatus.Success }
                val failedCount = results.count { it.status == DownloadStatus.Failed }
                notifier.notifyFinished(illust.id, illust.title, successCount, failedCount)
            }
            results
        } catch (e: CancellationException) {
            if (aggregate) notifier.cancel(illust.id)
            throw e
        } finally {
            if (aggregate) com.perol.pixez.shared.platform.PlatformDownloadKeeper.release(illust.id)
        }
    }

    /**
     * 根据已有历史记录重试下载。
     *
     * 直接使用历史记录中保存的远程 URL 与文件名，避免调用方必须持有完整的 [Illust] 对象。
     * 重试会覆盖原历史记录的状态（成功或失败）。
     *
     * @param history 待重试的下载历史记录
     * @return 包含最终状态的下载任务
     */
    suspend fun retry(history: DownloadTaskHistory): DownloadTask {
        // 重试必须对应已持久化的历史记录；ID 不合法时 saveTask 会插入新行，导致重复历史。
        require(history.id > 0) { "重试必须基于已持久化的历史记录" }
        historyRepository ?: throw IllegalStateException("重试功能需要数据库依赖")

        val (subDir, customBasePath) = resolveSubDirAndBasePath(
            userName = history.userName,
            userId = history.userId,
            sanityLevel = history.sanityLevel,
        )

        return runTask(
            history = history,
            errorCode = AppErrorKind.DOWNLOAD_FAILED.code,
            // 重试与首次下载同样占用保活（大文件重试不再裸跑），但不额外发通知，保持原有静默语义。
            keepAlive = true,
            notifyProgress = false,
            notifyFinishedResult = false,
            throwOnFailure = true,
            startWriteTolerant = true,
        ) {
            val tempFileName =
                "dl_retry_${history.illustId}_${history.pageIndex}_${Clock.System.now().toEpochMilliseconds()}.tmp"
            val tempPath = downloadToTempFile(history.remoteUrl, tempFileName)
            saver.saveFromTempFile(
                FileNamePolicy.requireSafeBaseName(history.fileName),
                tempPath,
                subDir = subDir,
                customBasePath = customBasePath,
            ).also { Napier.d("重试下载完成 path=$it") }
        }.task
    }

    /**
     * 流式下载图片至应用缓存临时文件，配合 64KB 缓冲区边拉取边落盘，避免大图或动图占用 JVM 堆内存。
     *
     * 大小防护三重校验：写盘前按声明 Content-Length 预检、流中按累计字节上限拦截、
     * 结束后核验实收字节与声明长度一致（拒绝被截断的响应）；
     * 下载失败或取消时清理临时文件，避免缓存目录残留垃圾。
     */
    private suspend fun downloadToTempFile(url: String, tempFileName: String): Path {
        val mirrorHost = settingsRepository.pictureSource
        val mappedUrl = url.mapToPictureSource(mirrorHost)
        val trustedUrl = com.perol.pixez.shared.network.TrustedUrlPolicy.imageUrl(mappedUrl, mirrorHost)
        val cacheDir = getAppCacheDirectory()
        val tempPath = cacheDir / tempFileName

        try {
            httpClient.prepareGet(trustedUrl) {
                header("Referer", PixivHosts.PIXIV_APP_API)
            }.execute { response ->
                // 写盘前先按声明长度预检，恶意/异常的超大响应不落盘。
                val declaredLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                require(declaredLength == null || declaredLength <= maxDownloadBytes) {
                    "下载内容声明大小超过上限: $declaredLength"
                }
                val channel: ByteReadChannel = response.bodyAsChannel()
                // okio 落盘为阻塞 I/O，切至 IO 调度器，避免占用调用方（UI 主协程）线程。
                withContext(Dispatchers.IO) {
                    FileSystem.SYSTEM.write(tempPath) {
                        val buffer = ByteArray(64 * 1024)
                        var totalBytes = 0L
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read <= 0) break
                            totalBytes += read
                            // 写入前校验累计字节，超限立即中止，防止无限流撑爆磁盘。
                            require(totalBytes <= maxDownloadBytes) {
                                "下载内容超过大小上限: $totalBytes"
                            }
                            write(buffer, 0, read)
                        }
                        // 声明长度已知时核验实收字节，拒绝被截断的损坏响应。
                        require(declaredLength == null || totalBytes == declaredLength) {
                            "下载内容不完整: 实收 $totalBytes, 声明 $declaredLength"
                        }
                    }
                }
            }
            return tempPath
        } catch (e: Throwable) {
            // 下载失败/取消时清理临时文件；清理本身不可再被取消，异常原样上抛。
            withContext(NonCancellable + Dispatchers.IO) {
                FileSystem.SYSTEM.delete(tempPath, mustExist = false)
            }
            throw e
        }
    }

    /**
     * 解析作品指定页的原图 URL。
     *
     * 单页作品优先使用 [Illust.metaSinglePage]；多页作品使用 [Illust.metaPages]。
     */
    private fun resolveOriginalUrl(illust: Illust, pageIndex: Int): String {
        if (pageIndex < 0 || pageIndex >= illust.pageCount) {
            throw IllegalArgumentException("页码越界: pageCount=${illust.pageCount}, pageIndex=$pageIndex")
        }

        if (illust.pageCount == 1) {
            val single = illust.metaSinglePage?.originalImageUrl
            if (!single.isNullOrBlank()) return single
        }

        val page = illust.metaPages.getOrNull(pageIndex)
            ?: throw IllegalStateException("无法获取作品页信息: index=$pageIndex")
        val original = page.imageUrls?.original
            ?: throw IllegalStateException("无法获取原图 URL: index=$pageIndex")
        return original
    }

    /**
     * 构建保存文件名：支持用户自定义模板。
     * 默认格式：`{illust_id}_p{part}.{ext}`。
     * 支持占位符：`{illust_id}`, `{title}`, `{user_id}`, `{user_name}`, `{author}`, `{part}`, `{create_date}`, `{width}x{height}`, `{width}`, `{height}`。
     */
    fun buildFileName(illust: Illust, pageIndex: Int, remoteUrl: String): String {
        val ext = extractExtension(remoteUrl)
        val template = settingsRepository.format.trim().takeUnless { it.isBlank() } ?: DEFAULT_NAME_FORMAT
        var name = template
            .replace("{illust_id}", illust.id.toString())
            .replace("{title}", FileNamePolicy.sanitizeSegment(illust.title))
            .replace("{user_id}", illust.user.id.toString())
            .replace("{user_name}", FileNamePolicy.sanitizeSegment(illust.user.name))
            .replace("{author}", FileNamePolicy.sanitizeSegment(illust.user.name))
            .replace("{part}", pageIndex.toString())
            .replace("{create_date}", FileNamePolicy.sanitizeSegment(illust.createDate.take(10)))
            .replace("{width}x{height}", "${illust.width}x${illust.height}")
            .replace("{width}", illust.width.toString())
            .replace("{height}", illust.height.toString())

        // 如果多页作品且用户自定义格式未包含 {part}，智能追加 _p{index} 避免多图同名相互覆盖
        if (illust.pageCount > 1 && !template.contains("{part}")) {
            name = "${name}_p$pageIndex"
        }
        val safeName = FileNamePolicy.sanitizeSegment(name)
        return "${safeName}.${ext}"
    }

    private fun resolveSubDirAndBasePath(
        userName: String,
        userId: Long,
        sanityLevel: Int?,
        xRestrict: Int = 0,
    ): Pair<String?, String?> {
        val authorSegment = if (settingsRepository.singleFolder == false && userName.isNotBlank() && userId > 0) {
            "${FileNamePolicy.sanitizeSegment(userName)}_${userId}"
        } else null
        val isNsfw = settingsRepository.overSanityLevelFolder == true &&
            ((sanityLevel ?: 0) > 4 || xRestrict > 0)
        val subDir = when {
            isNsfw && authorSegment != null -> "NSFW_${authorSegment}"
            isNsfw -> "NSFW"
            else -> authorSegment
        }
        val customBasePath = settingsRepository.storePath?.takeUnless { it.isBlank() }
        return subDir to customBasePath
    }

    private fun resolveSubDirAndBasePath(illust: Illust): Pair<String?, String?> =
        resolveSubDirAndBasePath(
            userName = illust.user.name,
            userId = illust.user.id,
            sanityLevel = illust.sanityLevel,
            xRestrict = illust.xRestrict,
        )

    /**
     * 保存动图原始 ZIP 压缩包，写入下载历史并发送系统通知。
     */
    suspend fun saveUgoiraZip(illust: Illust, bytes: ByteArray, zipUrl: String): String =
        runUgoiraSaveTask(illust, zipUrl) { fileName, subDir, customBasePath ->
            saver.save(fileName, bytes, subDir = subDir, customBasePath = customBasePath)
        }

    /**
     * 从已下载的临时 zip 文件保存动图作品（架构审查 中-15）：
     * 经 [IllustSaver.saveFromTempFile] 流式复制落盘，zip 不整体读入堆内存；
     * 来源临时文件在保存成功后被平台实现消费（删除）。
     */
    suspend fun saveUgoiraZipFromFile(illust: Illust, zipPath: Path, zipUrl: String): String =
        runUgoiraSaveTask(illust, zipUrl) { fileName, subDir, customBasePath ->
            saver.saveFromTempFile(fileName, zipPath, subDir = subDir, customBasePath = customBasePath)
        }

    /**
     * 动图保存任务公共生命周期：写历史 → 执行保存 → 回写成功/失败状态与通知。
     * [saveAction] 收到已解析的文件名与目标子目录/自定义基准路径，返回保存后的可读路径。
     */
    private suspend fun runUgoiraSaveTask(
        illust: Illust,
        zipUrl: String,
        saveAction: suspend (fileName: String, subDir: String?, customBasePath: String?) -> String,
    ): String {
        val fileName = "${illust.id}_ugoira.zip"
        val (subDir, customBasePath) = resolveSubDirAndBasePath(illust)
        val pendingTask = DownloadTask(
            illustId = illust.id,
            pageIndex = 0,
            remoteUrl = zipUrl,
            fileName = fileName,
            status = DownloadStatus.Downloading,
        )

        return runTask(
            history = downloadTaskHistoryOf(pendingTask, illust),
            errorCode = AppErrorKind.UGOIRA_SAVE_FAILED.code,
            keepAlive = true,
            notifyProgress = true,
            notifyFinishedResult = true,
            throwOnFailure = true,
            startWriteTolerant = false,
        ) {
            saveAction(fileName, subDir, customBasePath)
                .also { Napier.d("动图 Zip 保存完成 path=$it") }
        }.savedPath
    }

    /**
     * 下载/保存任务的唯一执行体：占用保活 → 写历史 → 落盘 → 回写终态 → 发通知。
     *
     * 四类入口只在参数与「保存动作」上不同：
     * - [keepAlive]：是否占用前台保活（多页批量由聚合层统一占用，逐页不重复占用）
     * - [notifyProgress]：是否发送进度/取消通知（重试保持静默）
     * - [notifyFinishedResult]：是否由本次执行发送结束通知（批量只在聚合层发一次）
     * - [throwOnFailure]：失败向上抛出（重试/动图）还是返回 Failed 任务（信息流下载）
     * - [startWriteTolerant]：起始历史写入失败是否可容忍（重试基于已有行，写失败不中止下载）
     */
    private suspend fun runTask(
        history: DownloadTaskHistory,
        errorCode: String,
        keepAlive: Boolean,
        notifyProgress: Boolean,
        notifyFinishedResult: Boolean,
        throwOnFailure: Boolean,
        startWriteTolerant: Boolean,
        saveAction: suspend () -> String,
    ): TaskOutcome {
        val illustId = history.illustId
        val pendingTask = DownloadTask(
            illustId = illustId,
            pageIndex = history.pageIndex,
            remoteUrl = history.remoteUrl,
            fileName = history.fileName,
            status = DownloadStatus.Downloading,
        )

        if (keepAlive) com.perol.pixez.shared.platform.PlatformDownloadKeeper.acquire(illustId)
        if (notifyProgress) notifier.notifyProgress(illustId, history.title, 0, 1)

        // 历史记录 ID；初始写入失败时保持原值（0 表示无法回写状态）。
        var historyId = history.id
        return try {
            val startedHistory = history.copy(status = DownloadStatus.Downloading)
            if (startWriteTolerant) {
                suspendRunCatchingNonCancel { historyRepository?.saveTask(startedHistory)?.id }
                    .onSuccess { writtenId -> if (writtenId != null && writtenId > 0) historyId = writtenId }
                    .onFailure { Napier.e("回写下载历史失败 historyId=${history.id}", it) }
            } else {
                historyRepository?.saveTask(startedHistory)?.let { historyId = it.id }
            }
            notifyTaskChanged()

            val savedPath = saveAction()
            val successTask = pendingTask.copy(status = DownloadStatus.Success)
            if (historyId > 0) {
                historyRepository?.saveTask(history.copy(id = historyId, status = DownloadStatus.Success))
            }
            notifyTaskChanged()
            if (notifyFinishedResult) notifier.notifyFinished(illustId, history.title, 1, 0)
            TaskOutcome(successTask, savedPath)
        } catch (e: CancellationException) {
            if (notifyProgress) notifier.cancel(illustId)
            // 协程取消时直接抛出，避免被转为 Failed 状态而破坏取消语义。
            throw e
        } catch (e: Exception) {
            Napier.e("下载任务失败 illustId=$illustId page=${history.pageIndex}", e)
            val failedTask = pendingTask.copy(
                status = DownloadStatus.Failed,
                // 高-6b：task.error 只存稳定错误码（原始异常细节仅供上方 Napier 日志）。
                error = (e as? PixivApiException)?.errorKind?.code ?: errorCode,
            )
            if (notifyFinishedResult) notifier.notifyFinished(illustId, history.title, 0, 1)
            if (historyId > 0) {
                // 回写失败需记录并附加到原始异常，避免失败状态静默丢失。
                suspendRunCatchingNonCancel {
                    historyRepository?.saveTask(history.copy(id = historyId, status = DownloadStatus.Failed))
                }.onFailure { saveError ->
                    Napier.e("下载失败时回写历史失败 historyId=$historyId", saveError)
                    e.addSuppressed(saveError)
                }
            }
            notifyTaskChanged()
            if (throwOnFailure) throw e
            TaskOutcome(failedTask, savedPath = "")
        } finally {
            if (keepAlive) com.perol.pixez.shared.platform.PlatformDownloadKeeper.release(illustId)
        }
    }

    /** 单次任务执行的产物：最终状态 + 落盘路径（失败时为占位空串）。 */
    private data class TaskOutcome(val task: DownloadTask, val savedPath: String)

    /**
     * 从 URL 路径中提取扩展名；提取失败或不在白名单时返回 jpg。
     */
    private fun extractExtension(url: String): String {
        val path = runCatching { Url(url).encodedPath }.getOrDefault("")
        val ext = path.substringAfterLast('.', "").lowercase()
        return if (ext in SUPPORTED_EXTENSIONS) ext else "jpg"
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")

        /** 单次流式下载的最大字节数（256MB），与更新器 MAX_UPDATE_BYTES 对齐。 */
        const val DEFAULT_MAX_DOWNLOAD_BYTES: Long = 256L * 1024 * 1024

        /**
         * 并发下载上限（含）：设置项档位与运行时夹取的唯一权威（架构审查 R5）。
         *
         * 历史上该上限有三份互不一致的声明（AppConstants.MAX_TASK_OPTIONS=1/2/3/5/8/10、
         * 设置页本地列表 1..6、仓库 coerceIn(1,6)），此处收敛为单一来源。
         */
        const val MAX_PARALLEL_TASKS: Int = 6

        /** 并发下载可选档位，设置页应从本列表派生选项。 */
        val PARALLEL_TASK_OPTIONS: List<Int> = (1..MAX_PARALLEL_TASKS).toList()

        /** 把任意并发设置值夹取到合法区间 [1, MAX_PARALLEL_TASKS]。 */
        fun coerceParallelTasks(value: Int): Int = value.coerceIn(1, MAX_PARALLEL_TASKS)
    }
}
