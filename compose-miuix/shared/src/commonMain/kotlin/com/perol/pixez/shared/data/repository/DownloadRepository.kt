package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTask
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.DownloadNotifier
import com.perol.pixez.shared.platform.FileNamePolicy
import com.perol.pixez.shared.platform.IllustSaver
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * 插画下载仓库：负责解析原图 URL、下载图片字节并调用平台保存，
 * 同时通过 [DownloadHistoryRepository] 将任务状态写入本地历史。
 *
 * 提供单页 [download] 与多页 [downloadAllPages] 下载入口，支持 Android 16 实时动态胶囊通知。
 */
class DownloadRepository(
    private val httpClient: HttpClient,
    private val saver: IllustSaver,
    private val historyRepository: DownloadHistoryRepository,
    private val notifier: DownloadNotifier = DownloadNotifier(),
    private val settingsRepository: SettingsRepository? = null,
) {

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
    suspend fun download(illust: Illust, pageIndex: Int): DownloadTask {
        val remoteUrl = resolveOriginalUrl(illust, pageIndex)
        val fileName = buildFileName(illust, pageIndex, remoteUrl)
        val pendingTask = DownloadTask(
            illustId = illust.id,
            pageIndex = pageIndex,
            remoteUrl = remoteUrl,
            fileName = fileName,
            status = DownloadStatus.Downloading,
        )

        val (subDir, customBasePath) = resolveSubDirAndBasePath(illust)

        // 历史记录 ID；初始写入失败时保持 0，用于判断是否能回写状态。
        var historyId = 0L
        if (illust.pageCount <= 1) {
            notifier.notifyProgress(illust.id, illust.title, 0, 1)
        }
        return try {
            // 先写入下载历史，获取数据库 ID 以便后续更新同一行。
            historyId = historyRepository.saveTask(pendingTask, illust).id
            val bytes = downloadBytes(remoteUrl)
            val savedPath = saver.save(fileName, bytes, subDir = subDir, customBasePath = customBasePath)
            Napier.d("下载完成 path=$savedPath")
            val successTask = pendingTask.copy(status = DownloadStatus.Success)
            historyRepository.saveTask(successTask, illust, historyId)
            if (illust.pageCount <= 1) {
                notifier.notifyFinished(illust.id, illust.title, 1, 0)
            }
            successTask
        } catch (e: CancellationException) {
            if (illust.pageCount <= 1) {
                notifier.cancel(illust.id)
            }
            // 协程取消时直接抛出，避免被转为 Failed 状态而破坏取消语义。
            throw e
        } catch (e: Exception) {
            Napier.e("下载失败 illustId=${illust.id} page=$pageIndex", e)
            val failedTask = pendingTask.copy(
                status = DownloadStatus.Failed,
                error = e.message ?: "下载失败",
            )
            if (illust.pageCount <= 1) {
                notifier.notifyFinished(illust.id, illust.title, 0, 1)
            }
            if (historyId > 0) {
                // 历史记录已创建时尽力回写失败状态；不因为回写失败而覆盖原始错误。
                // 回写失败至少记录日志，便于排查 DB 状态与真实结果不一致的问题。
                suspendRunCatchingNonCancel { historyRepository.saveTask(failedTask, illust, historyId) }
                    .onFailure { saveError ->
                        Napier.e("回写下载历史失败 historyId=$historyId", saveError)
                        e.addSuppressed(saveError)
                    }
            }
            failedTask
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
        maxConcurrency: Int = 3,
    ): List<DownloadTask> = coroutineScope {
        val total = illust.pageCount
        if (total <= 0) return@coroutineScope emptyList()
        notifier.notifyProgress(illust.id, illust.title, 0, total)
        val semaphore = Semaphore(maxConcurrency.coerceIn(1, 6))
        val progressMutex = Mutex()
        var completedCount = 0

        try {
            val deferredTasks = (0 until total).map { pageIndex ->
                async {
                    val task = semaphore.withPermit {
                        download(illust, pageIndex)
                    }
                    progressMutex.withLock {
                        completedCount++
                        val current = completedCount
                        onProgress?.invoke(current, total)
                        notifier.notifyProgress(illust.id, illust.title, current, total)
                    }
                    task
                }
            }
            val results = deferredTasks.awaitAll()
            val successCount = results.count { it.status == DownloadStatus.Success }
            val failedCount = results.count { it.status == DownloadStatus.Failed }
            notifier.notifyFinished(illust.id, illust.title, successCount, failedCount)
            results
        } catch (e: CancellationException) {
            notifier.cancel(illust.id)
            throw e
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

        // 构造待重试任务对象，状态先置为下载中。
        val pendingTask = DownloadTask(
            illustId = history.illustId,
            pageIndex = history.pageIndex,
            remoteUrl = history.remoteUrl,
            fileName = history.fileName,
            status = DownloadStatus.Downloading,
        )

        // 先将历史记录更新为下载中，让用户能在「运行中」标签页看到重试任务。
        suspendRunCatchingNonCancel { historyRepository.saveTask(history.copy(status = DownloadStatus.Downloading)) }
            .onFailure { Napier.e("重试时回写下载历史失败 historyId=${history.id}", it) }

        return try {
            // 复用已有 HTTP 下载与平台保存逻辑。
            val bytes = downloadBytes(history.remoteUrl)
            val customBasePath = settingsRepository?.storePath?.takeUnless { it.isBlank() }
            val subDir = if (settingsRepository?.singleFolder == false && history.userName.isNotBlank() && history.userId > 0) {
                "${FileNamePolicy.sanitizeSegment(history.userName)}_${history.userId}"
            } else null
            val savedPath = saver.save(
                FileNamePolicy.requireSafeBaseName(history.fileName),
                bytes,
                subDir = subDir,
                customBasePath = customBasePath,
            )
            Napier.d("重试下载完成 path=$savedPath")
            val successTask = pendingTask.copy(status = DownloadStatus.Success)
            historyRepository.saveTask(history.copy(status = DownloadStatus.Success))
            successTask
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("重试下载失败 illustId=${history.illustId}", e)
            val failedTask = pendingTask.copy(
                status = DownloadStatus.Failed,
                error = e.message ?: "下载失败",
            )
            // 历史记录已存在时尽力回写失败状态；回写失败则记录日志并将异常附加到原始异常，避免状态不一致被静默吞掉。
            suspendRunCatchingNonCancel { historyRepository.saveTask(history.copy(status = DownloadStatus.Failed)) }
                .onFailure { saveError ->
                    Napier.e("重试失败时回写下载历史失败 historyId=${history.id}", saveError)
                    e.addSuppressed(saveError)
                }
            // 抛出异常，让调用方感知重试失败，避免 UI 错误地提示「成功」。
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
     * 支持占位符：`{illust_id}`, `{title}`, `{user_id}`, `{user_name}`, `{part}`。
     */
    fun buildFileName(illust: Illust, pageIndex: Int, remoteUrl: String): String {
        val ext = extractExtension(remoteUrl)
        val template = settingsRepository?.format?.trim().takeUnless { it.isNullOrBlank() } ?: AppConstants.Download.DEFAULT_NAME_FORMAT
        var name = template
            .replace("{illust_id}", illust.id.toString())
            .replace("{title}", FileNamePolicy.sanitizeSegment(illust.title))
            .replace("{user_id}", illust.user.id.toString())
            .replace("{user_name}", FileNamePolicy.sanitizeSegment(illust.user.name))
            .replace("{part}", pageIndex.toString())

        // 如果多页作品且用户自定义格式未包含 {part}，智能追加 _p{index} 避免多图同名相互覆盖
        if (illust.pageCount > 1 && !template.contains("{part}")) {
            name = "${name}_p$pageIndex"
        }
        val safeName = FileNamePolicy.sanitizeSegment(name)
        return "${safeName}.${ext}"
    }

    private fun resolveSubDirAndBasePath(illust: Illust): Pair<String?, String?> {
        val subDir = if (settingsRepository?.singleFolder == false) {
            "${FileNamePolicy.sanitizeSegment(illust.user.name)}_${illust.user.id}"
        } else null
        val customBasePath = settingsRepository?.storePath?.takeUnless { it.isBlank() }
        return subDir to customBasePath
    }

    /**
     * 保存动图原始 ZIP 压缩包，写入下载历史并发送系统通知。
     */
    suspend fun saveUgoiraZip(illust: Illust, bytes: ByteArray, zipUrl: String): String {
        val fileName = "${illust.id}_ugoira.zip"
        val pendingTask = DownloadTask(
            illustId = illust.id,
            pageIndex = 0,
            remoteUrl = zipUrl,
            fileName = fileName,
            status = DownloadStatus.Downloading,
        )

        val (subDir, customBasePath) = resolveSubDirAndBasePath(illust)

        var historyId = 0L
        notifier.notifyProgress(illust.id, illust.title, 0, 1)
        return try {
            historyId = historyRepository.saveTask(pendingTask, illust).id
            val savedPath = saver.save(fileName, bytes, subDir = subDir, customBasePath = customBasePath)
            Napier.d("动图 Zip 保存完成 path=$savedPath")
            val successTask = pendingTask.copy(status = DownloadStatus.Success)
            historyRepository.saveTask(successTask, illust, historyId)
            notifier.notifyFinished(illust.id, illust.title, 1, 0)
            savedPath
        } catch (e: CancellationException) {
            notifier.cancel(illust.id)
            throw e
        } catch (e: Exception) {
            Napier.e("动图 Zip 保存失败 illustId=${illust.id}", e)
            val failedTask = pendingTask.copy(
                status = DownloadStatus.Failed,
                error = e.message ?: "动图保存失败",
            )
            notifier.notifyFinished(illust.id, illust.title, 0, 1)
            if (historyId > 0) {
                runCatching { historyRepository.saveTask(failedTask, illust, historyId) }
            }
            throw e
        }
    }

    /**
     * 下载图片字节。
     *
     * 显式附加 Referer，满足 Pixiv 图片防盗链要求（客户端 defaultRequest 已配置，此处作为防御性补充）。
     */
    private suspend fun downloadBytes(url: String): ByteArray {
        val trustedUrl = com.perol.pixez.shared.network.TrustedUrlPolicy.imageUrl(url)
        val response = httpClient.get(trustedUrl) {
            header("Referer", com.perol.pixez.shared.ui.AppConstants.Urls.PIXIV_APP_API)
        }
        return response.readRawBytes()
    }

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
    }
}
