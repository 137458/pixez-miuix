package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTask
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.platform.IllustSaver
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Url

/**
 * 插画下载仓库：负责解析原图 URL、下载图片字节并调用平台保存。
 *
 * 当前切片仅提供单页下载入口 [download]，多页作品会在后续切片中扩展为批量下载。
 */
class DownloadRepository(
    private val httpClient: HttpClient,
    private val saver: IllustSaver,
) {

    /**
     * 下载指定作品的指定页原图并保存到本地。
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

        return try {
            val bytes = downloadBytes(remoteUrl)
            val savedPath = saver.save(fileName, bytes)
            Napier.d("下载完成 path=$savedPath")
            pendingTask.copy(status = DownloadStatus.Success)
        } catch (e: Exception) {
            Napier.e("下载失败 illustId=${illust.id} page=$pageIndex", e)
            pendingTask.copy(
                status = DownloadStatus.Failed,
                error = e.message ?: "下载失败",
            )
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

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
        private val INVALID_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|]")
    }
}
