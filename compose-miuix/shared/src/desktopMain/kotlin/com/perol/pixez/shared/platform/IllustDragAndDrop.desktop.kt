package com.perol.pixez.shared.platform

import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.perol.pixez.shared.data.model.Illust
import io.github.aakira.napier.Napier
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

/**
 * Desktop(JVM) 平台跨应用内容拖拽源实现：
 * 支持将插画直接拖拽至桌面、文件管理器、浏览器上传区、Photoshop 或社交聊天软件。
 */
@Composable
actual fun Modifier.illustDragAndDropSource(illust: Illust, pageIndex: Int): Modifier {
    return this.dragAndDropSource(
        transferData = {
            val url = "https://www.pixiv.net/artworks/${illust.id}"
            val originalUrl = resolveOriginalUrl(illust, pageIndex)
            val extension = extractExtension(originalUrl)

            val userHome = System.getProperty("user.home") ?: "."
            val dragDir = File(userHome, ".pixez/cache/drag_drops").apply { mkdirs() }
            val imageFile = File(dragDir, "${illust.id}_p${pageIndex}.${extension}")

            var readyFile: File? = null

            try {
                // 1. 优先检索 Pictures/PixEz 目录中已下载的真实原图
                val localFile = findLocalDownloadedOriginal(illust, pageIndex, extension)
                if (localFile != null && localFile.exists() && localFile.length() > 0) {
                    readyFile = localFile
                }

                // 2. 检索 Coil 磁盘缓存并提取
                if (readyFile == null) {
                    val imageLoader = SingletonImageLoader.get(PlatformContext.INSTANCE)
                    val diskCache = imageLoader.diskCache
                    val candidateUrls = buildCandidateUrls(illust, pageIndex, originalUrl)

                    for (candidateUrl in candidateUrls) {
                        diskCache?.openSnapshot(candidateUrl)?.use { snapshot ->
                            val sourceFile = snapshot.data.toFile()
                            if (sourceFile.exists() && sourceFile.length() > 0) {
                                sourceFile.copyTo(imageFile, overwrite = true)
                                readyFile = imageFile
                            }
                        }
                        if (readyFile != null) break
                    }
                }
            } catch (e: Exception) {
                Napier.w("Desktop 拖拽源准备文件异常", e)
            }

            val files = if (readyFile != null && readyFile.exists()) listOf(readyFile) else emptyList()
            @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
            DragAndDropTransferData(
                transferable = androidx.compose.ui.draganddrop.DragAndDropTransferable(
                    transferable = DesktopFileAndTextTransferable(
                        files = files,
                        text = url,
                    ),
                ),
                supportedActions = listOf(androidx.compose.ui.draganddrop.DragAndDropTransferAction.Copy),
            )
        },
    )
}

private fun resolveOriginalUrl(illust: Illust, pageIndex: Int): String {
    if (illust.pageCount > 1) {
        val pageOriginal = illust.metaPages.getOrNull(pageIndex)?.imageUrls?.original
        if (!pageOriginal.isNullOrBlank()) return pageOriginal
    }
    val singleOriginal = illust.metaSinglePage?.originalImageUrl
    if (!singleOriginal.isNullOrBlank()) return singleOriginal

    if (illust.pageCount > 1) {
        val pageLarge = illust.metaPages.getOrNull(pageIndex)?.imageUrls?.large
        if (!pageLarge.isNullOrBlank()) return pageLarge
    }
    return illust.imageUrls.large.ifBlank { illust.imageUrls.medium }
}

private fun extractExtension(url: String): String {
    val cleanUrl = url.substringBefore('?')
    val ext = cleanUrl.substringAfterLast('.', "jpg").lowercase()
    return when (ext) {
        "png", "jpg", "jpeg", "gif", "webp" -> ext
        else -> "jpg"
    }
}

private fun findLocalDownloadedOriginal(illust: Illust, pageIndex: Int, ext: String): File? {
    return runCatching {
        val userHome = System.getProperty("user.home") ?: return@runCatching null
        val pixezDir = File(File(userHome, "Pictures"), "PixEz")
        if (!pixezDir.exists() || !pixezDir.isDirectory) return@runCatching null

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
        }

        for (name in candidateNames) {
            val file = File(pixezDir, name)
            if (file.exists() && file.length() > 0) return@runCatching file
        }

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

private fun buildCandidateUrls(illust: Illust, pageIndex: Int, originalUrl: String): List<String> {
    val urls = mutableListOf<String>()
    if (originalUrl.isNotBlank()) urls.add(originalUrl)

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

private class DesktopFileAndTextTransferable(
    private val files: List<File>,
    private val text: String,
) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return if (files.isNotEmpty()) {
            arrayOf(DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor)
        } else {
            arrayOf(DataFlavor.stringFlavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return (flavor == DataFlavor.javaFileListFlavor && files.isNotEmpty()) || flavor == DataFlavor.stringFlavor
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor == DataFlavor.javaFileListFlavor && files.isNotEmpty()) {
            return files
        }
        if (flavor == DataFlavor.stringFlavor) {
            return text
        }
        throw UnsupportedFlavorException(flavor)
    }
}


