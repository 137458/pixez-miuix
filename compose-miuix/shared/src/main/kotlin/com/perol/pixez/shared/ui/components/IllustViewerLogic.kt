package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Precision
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.screens.starAfterSaveIfNeeded
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.mapToPictureSource
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.platform.resolveOptimizedImageModel
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share

/**
 * 全屏查看器的纯逻辑与菜单装配：不持有 Compose 状态，可单独阅读与复用。
 * 页面渲染与手势留在 [IllustFullScreenViewer] 与 [ZoomableImage]。
 */

/**
 * 解析当前全屏查看页的目标大图 URL，用于复制位图及以图搜图等次级操作。
 */
internal fun resolveViewerTargetImageUrl(illust: Illust, pageIndex: Int): String {
    return if (illust.metaPages.isNotEmpty() && pageIndex in illust.metaPages.indices) {
        illust.metaPages[pageIndex].imageUrls?.large
            ?: illust.metaPages[pageIndex].imageUrls?.medium
            ?: illust.imageUrls.large
    } else {
        illust.imageUrls.large.ifEmpty { illust.imageUrls.medium }
    }
}

/**
 * 解析全屏查看器目标页按 [zoomQuality] 选取的原始待加载 URL（优先 original / large / medium）。
 */
internal fun resolveViewerZoomUrl(illust: Illust, pageIndex: Int, zoomQuality: Int): String {
    val page = illust.metaPages.getOrNull(pageIndex)
    return if (page != null) {
        val urls = page.imageUrls
        when (zoomQuality) {
            0 -> urls?.original?.ifEmpty { urls.large } ?: urls?.large.orEmpty()
            1 -> urls?.large.orEmpty().ifEmpty { urls?.original.orEmpty() }
            2 -> urls?.medium?.ifEmpty { urls.large } ?: urls?.large.orEmpty()
            else -> urls?.original?.ifEmpty { urls.large } ?: urls?.large.orEmpty()
        }
    } else {
        when (zoomQuality) {
            0 -> illust.metaSinglePage?.originalImageUrl?.ifEmpty { illust.imageUrls.large } ?: illust.imageUrls.large
            1 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
            2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
            else -> illust.metaSinglePage?.originalImageUrl?.ifEmpty { illust.imageUrls.large } ?: illust.imageUrls.large
        }
    }
}

/**
 * 解析全屏查看器目标页对应的原图 URL（供本地已下载文件直读命中优化）。
 */
internal fun resolveViewerOriginalUrl(illust: Illust, pageIndex: Int): String? =
    illust.metaPages.getOrNull(pageIndex)?.imageUrls?.original
        ?: illust.metaSinglePage?.originalImageUrl

/**
 * 解析全屏查看器目标页过渡缩略图 URL：仅首进页优先复用详情页已缓存的 [previewUrl]。
 */
internal fun resolveViewerThumbnailUrl(
    illust: Illust,
    pageIndex: Int,
    initialPage: Int,
    previewUrl: String?,
): String {
    val page = illust.metaPages.getOrNull(pageIndex)
    return if (pageIndex == initialPage && !previewUrl.isNullOrBlank()) {
        previewUrl
    } else if (page != null) {
        page.imageUrls?.let { it.large.ifEmpty { it.medium } }
            ?: illust.imageUrls.large.ifEmpty { illust.imageUrls.medium }
    } else {
        illust.imageUrls.large.ifEmpty { illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium } }
    }
}

/**
 * 执行全屏查看器单页或动图下载，并通过 [onToast] 反馈结果。
 */
internal suspend fun executeViewerDownload(
    illust: Illust,
    currentPage: Int,
    illustRepository: IllustRepository?,
    downloadRepository: DownloadRepository,
    strings: AppStrings,
    onToast: (ToastData) -> Unit,
    settings: SettingsRepository? = null,
    bookmarkRepository: BookmarkRepository? = null,
    isBookmarked: Boolean = illust.isBookmarked,
    onBookmarked: (() -> Unit)? = null,
) {
    val pageNumber = currentPage + 1
    if (IllustType.isUgoira(illust.type) && illustRepository != null) {
        onToast(ToastData("${strings.downloadStatusDownloading}…", ToastType.Normal))
        saveUgoiraIllust(
            illust = illust,
            illustRepository = illustRepository,
            downloadRepository = downloadRepository,
        ).fold(
            onSuccess = {
                performHapticFeedback(HapticType.Confirm)
                starAfterSaveIfNeeded(
                    illustId = illust.id,
                    isBookmarked = isBookmarked,
                    settings = settings,
                    bookmarkRepository = bookmarkRepository,
                    onBookmarked = { onBookmarked?.invoke() },
                )
                onToast(ToastData(strings.downloadStatusSuccess, ToastType.Success))
            },
            onFailure = { e ->
                performHapticFeedback(HapticType.Reject)
                onToast(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
            },
        )
    } else {
        onToast(ToastData("${strings.downloadStatusDownloading} P$pageNumber…", ToastType.Normal))
        val task = downloadRepository.download(illust, pageIndex = currentPage)
        if (task.status == DownloadStatus.Success) {
            starAfterSaveIfNeeded(
                illustId = illust.id,
                isBookmarked = isBookmarked,
                settings = settings,
                bookmarkRepository = bookmarkRepository,
                onBookmarked = { onBookmarked?.invoke() },
            )
        }
        val toast = when (task.status) {
            DownloadStatus.Success -> ToastData("${strings.downloadStatusSuccess} (P$pageNumber)", ToastType.Success)
            DownloadStatus.Failed -> ToastData("${strings.downloadStatusFailed}: ${task.error.toLocalizedDownloadError(strings)}", ToastType.Error)
            else -> null
        }
        if (toast != null) onToast(toast)
    }
}

/**
 * 预加载 [activePage] 前后各一页到磁盘缓存（跳过内存缓存并缩减解码尺寸以控制堆内存）。
 */
internal suspend fun preloadViewerAdjacentPages(
    context: PlatformContext,
    imageLoader: ImageLoader,
    illust: Illust,
    pageCount: Int,
    activePage: Int,
    zoomQuality: Int,
    settings: SettingsRepository,
) {
    val adjacentPages = listOf(activePage + 1, activePage - 1).filter { it in 0 until pageCount }
    withContext(Dispatchers.IO) {
        for (pIndex in adjacentPages) {
            val rawTarget = resolveViewerZoomUrl(illust, pIndex, zoomQuality)
            val optModel = resolveOptimizedImageModel(
                context = context,
                illust = illust,
                pageIndex = pIndex,
                targetUrl = rawTarget,
                originalUrl = resolveViewerOriginalUrl(illust, pIndex),
                customBasePath = settings.storePath,
                pictureSource = settings.pictureSource,
            )
            if (optModel.isBlank() || optModel.startsWith("file:")) continue
            val transformed = optModel.mapToPictureSource(settings.pictureSource)
            val request = ImageRequest.Builder(context)
                .data(transformed)
                .diskCacheKey(transformed)
                .size(
                    Dimension(AppConstants.Image.PRELOAD_DECODE_DIMENSION),
                    Dimension(AppConstants.Image.PRELOAD_DECODE_DIMENSION),
                )
                .precision(Precision.INEXACT)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.execute(request)
        }
    }
}

/**
 * 复制当前全屏展示页的缓存图片到系统剪贴板。
 */
internal fun triggerViewerCopyImage(
    illust: Illust,
    currentDisplayPage: Int,
    context: PlatformContext,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (ToastData) -> Unit,
) {
    val targetUrl = resolveViewerTargetImageUrl(illust, currentDisplayPage)
    coroutineScope.launch {
        suspendRunClipboardShare(
            success = ToastData(strings.imageCopySuccess, ToastType.Success),
            failurePrefix = strings.menuCopyImage,
            onToast = onToast,
        ) {
            withContext(Dispatchers.IO) {
                val candidateUrls = listOfNotNull(
                    targetUrl,
                    illust.imageUrls.large,
                    illust.imageUrls.medium,
                )
                val bytes = extractCachedImageBytes(context, candidateUrls)
                bytes?.let { IllustClipboard().copyImage(it) }
                    ?: throw IllegalStateException(strings.imageNoCacheFound)
            }
        }
    }
}

/**
 * 复制当前全屏展示页的 Pixiv 分享链接到剪贴板。
 */
internal fun triggerViewerCopyLink(
    illust: Illust,
    pageCount: Int,
    currentDisplayPage: Int,
    strings: AppStrings,
    onToast: (ToastData) -> Unit,
) {
    val (link, _) = buildViewerShareInfo(illust, pageCount, currentDisplayPage)
    runClipboardShare(
        success = ToastData(strings.copiedToClipboard, ToastType.Success),
        failurePrefix = "${strings.copy}${strings.failedSuffix}",
        onToast = onToast,
    ) { IllustClipboard().copy(link) }
}

/**
 * 调起系统分享当前全屏展示页。
 */
internal fun triggerViewerShare(
    illust: Illust,
    pageCount: Int,
    currentDisplayPage: Int,
    strings: AppStrings,
    onToast: (ToastData) -> Unit,
) {
    val (link, shareTitle) = buildViewerShareInfo(illust, pageCount, currentDisplayPage)
    runClipboardShare(
        success = ToastData(strings.share, ToastType.Success),
        failurePrefix = strings.share,
        onToast = onToast,
    ) { IllustShare().share(link, shareTitle) }
}

/**
 * 使用浏览器打开 SauceNAO 搜索当前全屏展示页。
 */
internal fun triggerViewerSauceNao(
    illust: Illust,
    currentDisplayPage: Int,
    strings: AppStrings,
    onToast: (ToastData) -> Unit,
) {
    val imgUrl = if (illust.metaPages.isNotEmpty() && currentDisplayPage in illust.metaPages.indices) {
        illust.metaPages[currentDisplayPage].imageUrls?.medium
            ?: illust.metaPages[currentDisplayPage].imageUrls?.squareMedium
            ?: illust.imageUrls.medium
    } else {
        illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
    }
    val sauceUrl = buildSauceNaoUrl(imgUrl)
    openSafeUrl(sauceUrl, strings, onError = { onToast(ToastData(it, ToastType.Error)) })
}

/**
 * 构造全屏查看器单页分享信息（链接与标题）。多图时携带 `#page=X` 锚点与 P 数标题。
 */
internal fun buildViewerShareInfo(
    illust: Illust,
    pageCount: Int,
    currentPage: Int,
): Pair<String, String> {
    val pageAnchor = if (pageCount > 1) "#page=${currentPage + 1}" else ""
    val link = "${buildIllustShareLink(illust)}$pageAnchor"
    val shareTitle = if (pageCount > 1) "${illust.title} (P${currentPage + 1})" else illust.title
    return link to shareTitle
}

/**
 * 计算阅读模式切换按钮的目标 Tooltip 与对应展示图标。
 */
internal fun resolveViewerModeSwitchState(
    isVerticalScrollMode: Boolean,
    strings: AppStrings,
): Pair<String, ImageVector> {
    return if (isVerticalScrollMode) {
        strings.viewerModeHorizontal to MiuixIcons.HorizontalSplit
    } else {
        strings.viewerModeVertical to MiuixIcons.ListView
    }
}

/**
 * 构建全屏查看器「更多操作」下拉菜单条目：
 * 第一组：复制图片、复制作品链接；
 * 第二组：分享作品、SauceNAO 搜图。
 */
internal fun buildViewerMoreMenuEntries(
    strings: AppStrings,
    illust: Illust,
    pageCount: Int,
    currentDisplayPage: Int,
    context: PlatformContext,
    coroutineScope: CoroutineScope,
    onToast: (ToastData) -> Unit,
): List<DropdownEntry> = listOf(
    DropdownEntry(
        items = listOf(
            DropdownItem(
                text = strings.menuCopyImage,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Copy, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    triggerViewerCopyImage(
                        illust = illust,
                        currentDisplayPage = currentDisplayPage,
                        context = context,
                        coroutineScope = coroutineScope,
                        strings = strings,
                        onToast = onToast,
                    )
                },
            ),
            DropdownItem(
                text = strings.menuCopyLink,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Link, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    triggerViewerCopyLink(
                        illust = illust,
                        pageCount = pageCount,
                        currentDisplayPage = currentDisplayPage,
                        strings = strings,
                        onToast = onToast,
                    )
                },
            ),
        ),
    ),
    DropdownEntry(
        items = listOf(
            DropdownItem(
                text = strings.share,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Share, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    triggerViewerShare(
                        illust = illust,
                        pageCount = pageCount,
                        currentDisplayPage = currentDisplayPage,
                        strings = strings,
                        onToast = onToast,
                    )
                },
            ),
            DropdownItem(
                text = strings.menuSauceNao,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Search, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    triggerViewerSauceNao(
                        illust = illust,
                        currentDisplayPage = currentDisplayPage,
                        strings = strings,
                        onToast = onToast,
                    )
                },
            ),
        ),
    ),
)
