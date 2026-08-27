package com.perol.pixez.shared.platform

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import com.perol.pixez.shared.data.model.Illust
import java.io.File

/**
 * Android 平台跨应用内容拖拽源实现：
 * 支持在分屏、小窗及侧边栏场景下全局拖拽插画，深度对接小米传送门/超级岛、OPPO 智慧中转站/流体云、
 * vivo 原子中转站/智慧识屏、荣耀任意门、华为超级中转站及主流第三方应用（微信、备忘录、图库等）。
 *
 * 原图提取策略（四级渐进降级）：
 * 1. 检索公共存储 `Pictures/PixEz` 本地已下载原图文件（0 网络开销、100% 无损分辨率与完整 EXIF）；
 * 2. 检索 Coil 磁盘缓存中的原图 URL（`metaSinglePage.originalImageUrl` / `metaPages[pageIndex].imageUrls.original`）；
 * 3. 检索 Coil 磁盘缓存中的高清/标清显示缓存（`large` -> `medium` -> `squareMedium`）；
 * 4. 离线/无缓存时安全降级为作品 Web URL 文本。
 */
@Composable
actual fun Modifier.illustDragAndDropSource(illust: Illust, pageIndex: Int): Modifier {
    val context = LocalContext.current.applicationContext

    return this.dragAndDropSource(
        transferData = {
            val url = "https://www.pixiv.net/artworks/${illust.id}"
            val originalUrl = resolveOriginalUrl(illust, pageIndex)
            val extension = extractExtension(originalUrl)
            val dragDir = File(context.cacheDir, "drag_shares").apply { mkdirs() }
            val imageFile = File(dragDir, "${illust.id}_p${pageIndex}.${extension}")

            var hasValidImage = false

            try {
                // Tier 1: 优先检索本地 Pictures/PixEz 目录中已下载的真实原图
                val localOriginalFile = findLocalDownloadedOriginal(illust, pageIndex, extension)
                if (localOriginalFile != null && localOriginalFile.exists() && localOriginalFile.length() > 0) {
                    localOriginalFile.copyTo(imageFile, overwrite = true)
                    hasValidImage = true
                }

                // Tier 2 & Tier 3: 检索 Coil 磁盘缓存（原图 URL 优先，随后依次降级为 large / medium / squareMedium）
                if (!hasValidImage) {
                    val imageLoader = SingletonImageLoader.get(context)
                    val diskCache = imageLoader.diskCache
                    val candidateUrls = buildCandidateUrls(illust, pageIndex, originalUrl)

                    for (candidateUrl in candidateUrls) {
                        diskCache?.openSnapshot(candidateUrl)?.use { snapshot ->
                            val sourceFile = snapshot.data.toFile()
                            if (sourceFile.exists() && sourceFile.length() > 0) {
                                sourceFile.copyTo(imageFile, overwrite = true)
                                hasValidImage = true
                            }
                        }
                        if (hasValidImage) break
                    }
                }

                // 若之前暂存过有效图片且文件仍存在，作为最后文件兜底
                if (!hasValidImage && imageFile.exists() && imageFile.length() > 0) {
                    hasValidImage = true
                }
            } catch (_: Throwable) {
                hasValidImage = imageFile.exists() && imageFile.length() > 0
            }

            val clipData: ClipData = if (hasValidImage && imageFile.exists() && imageFile.length() > 0) {
                try {
                    val contentUri: Uri = FileProvider.getUriForFile(
                        context,
                        "com.perol.pixez.miuix.fileprovider",
                        imageFile,
                    )
                    val uriClip = ClipData.newUri(context.contentResolver, illust.title, contentUri)
                    // 补充作品直达链接，便于纯文本/网页意图的接收端解析
                    uriClip.addItem(ClipData.Item(Uri.parse(url)))
                    uriClip
                } catch (_: Throwable) {
                    ClipData.newPlainText(illust.title, url)
                }
            } else {
                ClipData.newPlainText(illust.title, url)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            } else {
                0
            }

            DragAndDropTransferData(
                clipData = clipData,
                flags = flags,
            )
        }
    )
}

/**
 * 解析作品目标页的原图 URL。
 */
private fun resolveOriginalUrl(illust: Illust, pageIndex: Int): String {
    if (illust.pageCount <= 1) {
        val single = illust.metaSinglePage?.originalImageUrl
        if (!single.isNullOrBlank()) return single
    }

    val page = illust.metaPages.getOrNull(pageIndex)
    val pageOriginal = page?.imageUrls?.original
    if (!pageOriginal.isNullOrBlank()) return pageOriginal

    return page?.imageUrls?.large ?: illust.imageUrls.large
}

/**
 * 从 URL 中提取真实图片扩展名（支持 png/jpg/jpeg/gif/webp）。
 */
private fun extractExtension(url: String): String {
    val cleanUrl = url.substringBefore('?')
    val ext = cleanUrl.substringAfterLast('.', "jpg").lowercase()
    return when (ext) {
        "png", "jpg", "jpeg", "gif", "webp" -> ext
        else -> "jpg"
    }
}

/**
 * 检索公共存储 Pictures/PixEz 目录中是否已下载该插画原图。
 */
private fun findLocalDownloadedOriginal(illust: Illust, pageIndex: Int, ext: String): File? {
    return runCatching {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val pixezDir = File(picturesDir, "PixEz")
        if (!pixezDir.exists() || !pixezDir.isDirectory) return@runCatching null

        // 1. 精确名称匹配
        val candidateNames = mutableListOf(
            "${illust.id}_p${pageIndex}.${ext}",
            "${illust.id}_p${pageIndex}.png",
            "${illust.id}_p${pageIndex}.jpg",
            "${illust.id}_p${pageIndex}.gif",
            "${illust.id}_p${pageIndex}.webp",
        )
        if (pageIndex == 0) {
            candidateNames.add("${illust.id}.${ext}")
            candidateNames.add("${illust.id}.png")
            candidateNames.add("${illust.id}.jpg")
            candidateNames.add("${illust.id}.gif")
            candidateNames.add("${illust.id}.webp")
        }

        for (name in candidateNames) {
            val file = File(pixezDir, name)
            if (file.exists() && file.length() > 0) {
                return@runCatching file
            }
        }

        // 2. 标题包含格式匹配（处理 `{title}_p{index}.{ext}` 命名模式）
        val files = pixezDir.listFiles() ?: return@runCatching null
        val idStr = illust.id.toString()
        val pageSuffix = "_p${pageIndex}."

        for (file in files) {
            val name = file.name
            if (file.length() > 0 && name.contains(idStr)) {
                if (pageIndex == 0 || name.contains(pageSuffix)) {
                    return@runCatching file
                }
            }
        }
        null
    }.getOrNull()
}

/**
 * 构建渐进式候选 URL 列表（原图 -> 大图 -> 中图 -> 缩略图）。
 */
private fun buildCandidateUrls(illust: Illust, pageIndex: Int, originalUrl: String): List<String> {
    val urls = mutableListOf<String>()

    if (originalUrl.isNotBlank()) {
        urls.add(originalUrl)
    }

    if (illust.pageCount > 1) {
        val page = illust.metaPages.getOrNull(pageIndex)
        page?.imageUrls?.large?.takeIf { it.isNotBlank() }?.let { urls.add(it) }
        page?.imageUrls?.medium?.takeIf { it.isNotBlank() }?.let { urls.add(it) }
        page?.imageUrls?.squareMedium?.takeIf { it.isNotBlank() }?.let { urls.add(it) }
    }

    illust.imageUrls.large.takeIf { it.isNotBlank() }?.let { urls.add(it) }
    illust.imageUrls.medium.takeIf { it.isNotBlank() }?.let { urls.add(it) }
    illust.imageUrls.squareMedium.takeIf { it.isNotBlank() }?.let { urls.add(it) }

    return urls.distinct()
}


