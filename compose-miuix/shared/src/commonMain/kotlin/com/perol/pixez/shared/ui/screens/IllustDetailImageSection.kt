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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UgoiraPlayer
import com.perol.pixez.shared.ui.components.resolveIllustCoverUrl
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppConstants.IllustType
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
 * 按作品类型解析生效的图片画质档位：漫画走独立画质设置，插画走通用图片画质。
 */
private fun resolveIllustQuality(illust: Illust, settings: SettingsRepository?): Int =
    if (illust.type == IllustType.MANGA) {
        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
    } else {
        settings?.pictureQuality ?: 0
    }

/**
 * 详情页视口约束快照：大屏判定与容器物理像素尺寸，作为图片显示尺寸限制的依据。
 * 由页面外层容器（BoxWithConstraints）测得后下发给各图片项。
 */
internal data class IllustDetailViewport(
    val isWideScreen: Boolean,
    val containerWidthPx: Float,
    val containerHeightPx: Float,
)

/**
 * 计算详情页单张图片的显示宽度。
 *
 * 窄屏保持既有全出血行为（宽度即容器宽）；大屏（宽屏容器）下图片收进内容列，
 * 与底部卡片同列对齐，且当按列宽渲染的高度超过视口高度上限时（典型为竖屏图片），
 * 按高度上限收缩宽度，避免图片纵向占满整屏。所有入参单位一致（物理像素或任意统一单位）。
 */
internal fun resolveIllustDetailImageWidthPx(
    containerWidthPx: Float,
    containerHeightPx: Float,
    illustAspectRatio: Float?,
    isWideScreen: Boolean,
    contentMaxWidthPx: Float,
    imageHorizontalPaddingPx: Float,
    maxHeightFraction: Float,
): Float {
    if (!isWideScreen) return containerWidthPx
    val columnWidthPx = (minOf(containerWidthPx, contentMaxWidthPx) - imageHorizontalPaddingPx * 2).coerceAtLeast(0f)
    val aspectRatio = illustAspectRatio?.takeIf { it > 0f } ?: return columnWidthPx
    val maxHeightPx = (containerHeightPx * maxHeightFraction).takeIf { it > 0f } ?: return columnWidthPx
    val heightAtColumnWidth = columnWidthPx / aspectRatio
    return if (heightAtColumnWidth > maxHeightPx) maxHeightPx * aspectRatio else columnWidthPx
}

/**
 * 按视口约束与转场落定比例计算详情页大图的显示宽度。
 *
 * 窄屏保持既有全出血行为；宽屏下大图在转场全程保持全幅（与列表卡片图像逐像素对齐，
 * 消除收回终点的横向错位），随展开落定渐变收进内容列（[heroSettleFraction] 0→1）。
 */
@Composable
private fun rememberDetailImageWidthDp(
    viewport: IllustDetailViewport,
    illustAspectRatio: Float?,
    heroSettleFraction: Float,
): Dp {
    val density = LocalDensity.current
    return remember(viewport, illustAspectRatio, heroSettleFraction, density) {
        with(density) {
            val columnWidthPx = resolveIllustDetailImageWidthPx(
                containerWidthPx = viewport.containerWidthPx,
                containerHeightPx = viewport.containerHeightPx,
                illustAspectRatio = illustAspectRatio,
                isWideScreen = viewport.isWideScreen,
                contentMaxWidthPx = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp.toPx(),
                imageHorizontalPaddingPx = 12.dp.toPx(),
                maxHeightFraction = AppConstants.Layout.DETAIL_IMAGE_MAX_HEIGHT_FRACTION,
            )
            if (!viewport.isWideScreen) {
                columnWidthPx.toDp()
            } else {
                // 全幅（转场收缩态，与卡片图像对齐）与内容列（落定态）之间的容器变换式过渡。
                lerp(viewport.containerWidthPx, columnWidthPx, heroSettleFraction).toDp()
            }
        }
    }
}

/**
 * 多页作品中的单个图片项：铺满宽度的图片 + 右下角下载按钮。
 */
@Composable
internal fun IllustDetailImagePage(
    illust: Illust,
    pageIndex: Int,
    page: MetaPage,
    illustAspectRatio: Float?,
    viewport: IllustDetailViewport,
    heroSettleFraction: Float,
    settings: SettingsRepository?,
    downloadRepository: DownloadRepository,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (ToastData?) -> Unit,
    onPageClick: (Int) -> Unit,
) {
    val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
        resolveIllustQuality(illust, settings)
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
    val thumbnailUrl = remember(page, pageIndex, illust, settings?.feedPreviewQuality, settings?.changeVersion) {
        if (pageIndex == 0) {
            resolveIllustCoverUrl(illust.imageUrls, settings?.feedPreviewQuality)
        } else {
            page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
        }
    }
    var pageLoaded by remember(pageIndex) { mutableStateOf(false) }
    val pageModifier = Modifier
        .fillMaxWidth()
        .then(
            if ((pageIndex == 0 || !pageLoaded) && illustAspectRatio != null) {
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

    // 大屏下转场全程全幅贴合卡片、落定后收进内容列；下载按钮贴图片边缘而非容器边缘。
    val imageWidthDp = rememberDetailImageWidthDp(viewport, illustAspectRatio, heroSettleFraction)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = if (viewport.isWideScreen) Modifier.width(imageWidthDp) else Modifier.fillMaxWidth(),
        ) {
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
                            onToast(ToastData("${strings.downloadStatusDownloading} P$pageNumber…", ToastType.Normal))
                            val task = downloadRepository.download(illust, pageIndex = pageIndex)
                            onToast(
                                when (task.status) {
                                    DownloadStatus.Success -> ToastData("${strings.downloadStatusSuccess} (P$pageNumber)", ToastType.Success)
                                    DownloadStatus.Failed -> ToastData("${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}", ToastType.Error)
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
    viewport: IllustDetailViewport,
    heroSettleFraction: Float,
    settings: SettingsRepository?,
    repository: IllustRepository,
    downloadRepository: DownloadRepository,
    strings: AppStrings,
    onToast: (ToastData?) -> Unit,
    onPageClick: (Int) -> Unit,
) {
    val imageWidthDp = rememberDetailImageWidthDp(viewport, illustAspectRatio, heroSettleFraction)
    val sizedModifier = if (viewport.isWideScreen) Modifier.width(imageWidthDp) else Modifier.fillMaxWidth()
    if (IllustType.isUgoira(illust.type)) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            UgoiraPlayer(
                illust = illust,
                illustRepository = repository,
                onClick = { onPageClick(0) },
                modifier = sizedModifier,
            )
        }
    } else {
        val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
            resolveIllustQuality(illust, settings)
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
        val thumbnailUrl = remember(illust, settings?.feedPreviewQuality, settings?.changeVersion) {
            resolveIllustCoverUrl(illust.imageUrls, settings?.feedPreviewQuality)
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

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = sizedModifier,
            ) {
                PixivAsyncImage(
                    model = singleUrl,
                    thumbnailUrl = thumbnailUrl,
                    contentDescription = illust.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = singleModifier,
                )
            }
        }
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
    repository: IllustRepository,
    downloadRepository: DownloadRepository,
    detailBackdrop: LayerBackdrop?,
    onToast: (ToastData) -> Unit,
    onDismiss: () -> Unit,
) {
    val pageIdx = pageIndex.coerceAtLeast(0)
    val effectiveQuality = resolveIllustQuality(illust, settings)
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
        illustRepository = repository,
        previewUrl = currentPreviewUrl,
        onToast = onToast,
        onDismiss = onDismiss,
        detailBackdrop = detailBackdrop,
    )
}
/** 线性插值。 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
