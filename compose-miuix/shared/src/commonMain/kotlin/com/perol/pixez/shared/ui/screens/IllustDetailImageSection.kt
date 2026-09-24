package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.MetaPage
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.platform.rememberOptimizedImageModel
import com.perol.pixez.shared.ui.components.IllustFullScreenViewer
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.UgoiraPlayer
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.utils.accessibleTouchTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 作品详情页的图片展示区块：多页图片项、单页/Ugoira 图片项与全屏查看器。
 *
 * 每个图片项内部仅保留与其自身渲染相关的 remember（URL 记忆化、加载完成标志），
 * 页面级状态（全屏索引、Toast）通过回调上传至 [IllustDetailSingleContent]。
 */

/**
 * 多页作品中的单个图片项：铺满宽度的图片 + 右下角下载按钮。
 */
@Composable
internal fun IllustDetailImagePage(
    illust: Illust,
    pageIndex: Int,
    page: MetaPage,
    illustAspectRatio: Float?,
    settings: SettingsRepository?,
    downloadRepository: DownloadRepository,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (String?) -> Unit,
    onPageClick: (Int) -> Unit,
) {
    val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
        if (illust.type == "manga") {
            settings?.mangaQuality ?: settings?.pictureQuality ?: 0
        } else {
            settings?.pictureQuality ?: 0
        }
    }
    val rawPageUrl = remember(page, effectiveQuality) {
        when (effectiveQuality) {
            0 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
            1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
            2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
            else -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
        }
    }
    val pageUrl = rememberOptimizedImageModel(
        illust = illust,
        pageIndex = pageIndex,
        targetUrl = rawPageUrl,
        originalUrl = page.imageUrls?.original,
        customBasePath = settings?.storePath,
        pictureSource = settings?.pictureSource,
    )
    val thumbnailUrl = remember(page) {
        page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
    }
    var pageLoaded by remember(pageIndex) { mutableStateOf(false) }
    val pageModifier = Modifier
        .fillMaxWidth()
        .then(
            if (!pageLoaded && illustAspectRatio != null) {
                Modifier.aspectRatio(illustAspectRatio)
            } else {
                Modifier
            },
        )
        .illustDragAndDropSource(illust, pageIndex = pageIndex)
        .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
        ) { onPageClick(pageIndex) }

    Box(modifier = Modifier.fillMaxWidth()) {
        PixivAsyncImage(
            model = pageUrl,
            thumbnailUrl = thumbnailUrl,
            contentDescription = "${illust.title} ($pageIndex)",
            contentScale = ContentScale.FillWidth,
            modifier = pageModifier,
            onSuccess = { pageLoaded = true },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .accessibleTouchTarget(48.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable {
                    coroutineScope.launch {
                        val pageNumber = pageIndex + 1
                        onToast("${strings.downloadStatusDownloading} P$pageNumber…")
                        val task = downloadRepository.download(illust, pageIndex = pageIndex)
                        onToast(
                            when (task.status) {
                                DownloadStatus.Success -> "${strings.downloadStatusSuccess} (P$pageNumber)"
                                DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                else -> null
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Download,
                contentDescription = "${strings.download} P${pageIndex + 1}",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    if (pageIndex < illust.metaPages.lastIndex) {
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * 单页作品的图片项：Ugoira 动图播放器或普通单图。
 */
@Composable
internal fun IllustDetailSinglePageImage(
    illust: Illust,
    illustAspectRatio: Float?,
    settings: SettingsRepository?,
    repository: IllustRepository,
    downloadRepository: DownloadRepository,
    strings: AppStrings,
    onToast: (String?) -> Unit,
    onPageClick: (Int) -> Unit,
) {
    if (illust.type == "ugoira") {
        UgoiraPlayer(
            illust = illust,
            illustRepository = repository,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
            if (illust.type == "manga") {
                settings?.mangaQuality ?: settings?.pictureQuality ?: 0
            } else {
                settings?.pictureQuality ?: 0
            }
        }
        val rawSingleUrl = remember(illust, effectiveQuality) {
            when (effectiveQuality) {
                0 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                else -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
            }
        }
        val singleUrl = rememberOptimizedImageModel(
            illust = illust,
            pageIndex = 0,
            targetUrl = rawSingleUrl,
            originalUrl = illust.metaSinglePage?.originalImageUrl,
            customBasePath = settings?.storePath,
            pictureSource = settings?.pictureSource,
        )
        val thumbnailUrl = remember(illust) {
            illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium }
        }
        val singleModifier = Modifier
            .fillMaxWidth()
            .then(
                if (illustAspectRatio != null) {
                    Modifier.aspectRatio(illustAspectRatio)
                } else {
                    Modifier
                },
            )
            .illustDragAndDropSource(illust, pageIndex = 0)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) { onPageClick(0) }

        PixivAsyncImage(
            model = singleUrl,
            thumbnailUrl = thumbnailUrl,
            contentDescription = illust.title,
            contentScale = ContentScale.FillWidth,
            modifier = singleModifier,
        )
    }
}

/**
 * 全屏查看器浮层：根据当前页索引计算预览图 URL 并唤起 [IllustFullScreenViewer]。
 */
@Composable
internal fun IllustDetailFullScreenOverlay(
    illust: Illust,
    pageIndex: Int,
    settings: SettingsRepository?,
    downloadRepository: DownloadRepository,
    detailBackdrop: LayerBackdrop?,
    onToast: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val pageIdx = pageIndex.coerceAtLeast(0)
    val effectiveQuality = if (illust.type == "manga") {
        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
    } else {
        settings?.pictureQuality ?: 0
    }
    val currentPreviewUrl = if (illust.metaPages.isNotEmpty()) {
        val p = illust.metaPages.getOrNull(pageIdx)
        val rawTarget = if (p != null) {
            when (effectiveQuality) {
                0 -> p.imageUrls?.large.orEmpty().ifEmpty { p.imageUrls?.original.orEmpty() }
                1 -> p.imageUrls?.original ?: p.imageUrls?.large.orEmpty()
                2 -> p.imageUrls?.medium ?: p.imageUrls?.large.orEmpty()
                else -> p.imageUrls?.large.orEmpty().ifEmpty { p.imageUrls?.original.orEmpty() }
            }
        } else illust.imageUrls.large
        rememberOptimizedImageModel(
            illust = illust,
            pageIndex = pageIdx,
            targetUrl = rawTarget,
            originalUrl = p?.imageUrls?.original,
            customBasePath = settings?.storePath,
            pictureSource = settings?.pictureSource,
        )
    } else {
        val rawTarget = when (effectiveQuality) {
            0 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
            1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
            2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
            else -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
        }
        rememberOptimizedImageModel(
            illust = illust,
            pageIndex = 0,
            targetUrl = rawTarget,
            originalUrl = illust.metaSinglePage?.originalImageUrl,
            customBasePath = settings?.storePath,
            pictureSource = settings?.pictureSource,
        )
    }

    IllustFullScreenViewer(
        illust = illust,
        initialPage = pageIdx,
        zoomQuality = settings?.zoomQuality ?: 0,
        downloadRepository = downloadRepository,
        previewUrl = currentPreviewUrl,
        onToast = onToast,
        onDismiss = onDismiss,
        detailBackdrop = detailBackdrop,
    )
}