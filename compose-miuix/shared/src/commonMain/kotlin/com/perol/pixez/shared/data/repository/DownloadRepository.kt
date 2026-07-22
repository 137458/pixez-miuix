package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTask
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustSeries
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaPage
import com.perol.pixez.shared.data.model.MetaPageImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.platform.IllustSaver
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException

/**
 * 插画下载仓库：负责解析原图 URL、下载图片字节并调用平台保存，
 * 同时通过 [DownloadHistoryRepository] 将任务状态写入本地历史。
 *
 * 提供单页 [download] 与多页 [downloadAllPages] 下载入口，并将取消异常重新抛出以保留协程取消语义。
 */
class DownloadRepository(
    private val httpClient: HttpClient,
    private val saver: IllustSaver,
    private val historyRepository: DownloadHistoryRepository,
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

        // 历史记录 ID；初始写入失败时保持 0，用于判断是否能回写状态。
        var historyId = 0L
        return try {
            // 先写入下载历史，获取数据库 ID 以便后续更新同一行。
            historyId = historyRepository.saveTask(pendingTask, illust).id
            val bytes = downloadBytes(remoteUrl)
            val savedPath = saver.save(fileName, bytes)
            Napier.d("下载完成 path=$savedPath")
            val successTask = pendingTask.copy(status = DownloadStatus.Success)
            historyRepository.saveTask(successTask, illust, historyId)
            successTask
        } catch (e: CancellationException) {
            // 协程取消时直接抛出，避免被转为 Failed 状态而破坏取消语义。
            throw e
        } catch (e: Exception) {
            Napier.e("下载失败 illustId=${illust.id} page=$pageIndex", e)
            val failedTask = pendingTask.copy(
                status = DownloadStatus.Failed,
                error = e.message ?: "下载失败",
            )
            if (historyId > 0) {
                // 历史记录已创建时尽力回写失败状态；不因为回写失败而覆盖原始错误。
                runCatching { historyRepository.saveTask(failedTask, illust, historyId) }
            }
            failedTask
        }
    }

    /**
     * 下载作品全部页原图并保存到本地。
     *
     * 按页码顺序逐页下载，返回每一页的下载任务结果。
     *
     * @param illust 目标作品
     * @param onProgress 进度回调，参数为 (已完成数量, 总页数)
     * @return 每页对应的下载任务列表
     */
    suspend fun downloadAllPages(
        illust: Illust,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
    ): List<DownloadTask> {
        return (0 until illust.pageCount).mapIndexed { index, pageIndex ->
            onProgress?.invoke(index, illust.pageCount)
            download(illust, pageIndex)
        }.also { tasks ->
            onProgress?.invoke(tasks.size, illust.pageCount)
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
        // 构造待重试任务对象，状态先置为下载中。
        val pendingTask = DownloadTask(
            illustId = history.illustId,
            pageIndex = history.pageIndex,
            remoteUrl = history.remoteUrl,
            fileName = history.fileName,
            status = DownloadStatus.Downloading,
        )

        // 先将历史记录更新为下载中，让用户能在「运行中」标签页看到重试任务。
        if (history.id > 0) {
            runCatching { historyRepository.saveTask(pendingTask, history.toMinimalIllust(), history.id) }
        }

        return try {
            // 复用已有 HTTP 下载与平台保存逻辑。
            val bytes = downloadBytes(history.remoteUrl)
            val savedPath = saver.save(history.fileName, bytes)
            Napier.d("重试下载完成 path=$savedPath")
            val successTask = pendingTask.copy(status = DownloadStatus.Success)
            historyRepository.saveTask(successTask, history.toMinimalIllust(), history.id)
            successTask
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("重试下载失败 illustId=${history.illustId}", e)
            val failedTask = pendingTask.copy(
                status = DownloadStatus.Failed,
                error = e.message ?: "下载失败",
            )
            if (history.id > 0) {
                // 历史记录已存在时尽力回写失败状态。
                runCatching { historyRepository.saveTask(failedTask, history.toMinimalIllust(), history.id) }
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
     * 构建保存文件名：`{title}_p{index}.{ext}`。
     *
     * 扩展名优先从原图 URL 路径中提取；无法提取时默认 jpg。
     * 标题中的文件系统非法字符会被替换为下划线，避免保存失败。
     */
    private fun buildFileName(illust: Illust, pageIndex: Int, remoteUrl: String): String {
        val safeTitle = sanitizeFileName(illust.title)
        val ext = extractExtension(remoteUrl)
        return "${safeTitle}_p${pageIndex}.${ext}"
    }

    /**
     * 下载图片字节。
     *
     * 显式附加 Referer，满足 Pixiv 图片防盗链要求（客户端 defaultRequest 已配置，此处作为防御性补充）。
     */
    private suspend fun downloadBytes(url: String): ByteArray {
        val response = httpClient.get(url) {
            header("Referer", "https://app-api.pixiv.net/")
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

    /**
     * 清理文件名中的非法字符，避免跨平台保存失败。
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(INVALID_FILE_NAME_CHARS, "_")
    }

    /**
     * 将下载历史记录转换为可供 [saveTask] 使用的最小化 [Illust] 对象。
     *
     * 重试时仅需要作品元信息（标题、画师、页码、原图 URL），其余字段使用默认值填充。
     */
    private fun DownloadTaskHistory.toMinimalIllust(): Illust {
        // 根据页码构造单页或多页结构，确保 resolveOriginalUrl 能正确取到 remoteUrl。
        val singlePage = if (pageIndex == 0) MetaSinglePage(originalImageUrl = remoteUrl) else null
        val metaPages = if (pageIndex > 0) {
            listOf(
                MetaPage(
                    imageUrls = MetaPageImageUrls(
                        squareMedium = medium ?: "",
                        medium = medium ?: "",
                        large = "",
                        original = remoteUrl,
                    ),
                ),
            )
        } else {
            emptyList()
        }

        return Illust(
            id = illustId,
            title = title,
            type = "illust",
            imageUrls = ImageUrls(
                squareMedium = medium ?: "",
                medium = medium ?: "",
                large = "",
            ),
            caption = "",
            restrict = 0,
            user = IllustUser(
                id = userId,
                name = userName,
                account = "",
                profileImageUrls = IllustProfileImageUrls(medium = ""),
                comment = "",
                isFollowed = false,
            ),
            tags = emptyList(),
            tools = emptyList(),
            createDate = "",
            pageCount = maxOf(pageIndex + 1, 1),
            width = 0,
            height = 0,
            sanityLevel = sanityLevel ?: 0,
            xRestrict = 0,
            metaSinglePage = singlePage,
            metaPages = metaPages,
            totalView = 0,
            totalBookmarks = 0,
            isBookmarked = false,
            visible = false,
            isMuted = false,
            illustAIType = 1,
            series = null,
        )
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
        private val INVALID_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|]")
    }
}
