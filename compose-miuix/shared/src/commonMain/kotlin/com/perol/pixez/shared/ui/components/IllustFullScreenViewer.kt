package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed as pointerIsCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed as pointerIsMetaPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Precision
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.platform.rememberOptimizedImageModel
import com.perol.pixez.shared.platform.resolveOptimizedImageModel
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.ktor.http.URLBuilder
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 插画全屏/高清缩放预览组件：
 * 支持双指平滑手势缩放 (Pinch-to-zoom)、鼠标滚轮定点缩放、鼠标拖拽平移 (Pan)、双击放大/复原 (Double-tap-to-zoom)、
 * 键盘左右方向键/翻页键切页、ESC 快速退出以及多 P 左右切页。
 * 根据 [SettingsRepository.zoomQuality] 加载对应画质大图，优先使用已有内存缓存作为过渡底图。
 */
@Composable
fun IllustFullScreenViewer(
    illust: Illust,
    initialPage: Int,
    zoomQuality: Int,
    downloadRepository: DownloadRepository,
    onToast: (String) -> Unit,
    onDismiss: () -> Unit,
    detailBackdrop: Backdrop? = null,
    previewUrl: String? = null,
) {
    val strings = LocalStrings.current
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current
    val pageCount = if (illust.metaPages.isNotEmpty()) illust.metaPages.size else 1
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )
    val verticalListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPage.coerceIn(0, pageCount - 1),
    )
    var isVerticalScrollMode by rememberSaveable(illust.id) { mutableStateOf(false) }
    val currentDisplayPage by remember(isVerticalScrollMode, pageCount) {
        derivedStateOf {
            if (pageCount <= 1) 0
            else if (isVerticalScrollMode) {
                verticalListState.firstVisibleItemIndex.coerceIn(0, pageCount - 1)
            } else {
                pagerState.currentPage
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var currentPageScale by remember { mutableFloatStateOf(1f) }
    var showControls by remember { mutableStateOf(true) }

    val internalBackdrop = if (isRuntimeShaderSupported()) {
        rememberLayerBackdrop {
            drawRect(Color.Black)
            drawContent()
        }
    } else null
    val effectiveBackdrop = internalBackdrop ?: detailBackdrop
    val effectiveLayerBackdrop = internalBackdrop ?: (detailBackdrop as? LayerBackdrop)

    PlatformBackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Throwable) {
            // 窗口尚未就绪时可能无法获取焦点，不影响查看器主体功能，仅记录日志
            Napier.w("全屏查看器请求焦点失败", e, tag = "IllustViewer")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if ((keyEvent.isCtrlPressed || keyEvent.isMetaPressed) && keyEvent.key == Key.S) {
                        val currentPage = currentDisplayPage
                        val pageNumber = currentPage + 1
                        coroutineScope.launch {
                            onToast("${strings.downloadStatusDownloading} P$pageNumber…")
                            val task = downloadRepository.download(illust, pageIndex = currentPage)
                            val msg = when (task.status) {
                                DownloadStatus.Success -> "${strings.downloadStatusSuccess} (P$pageNumber)"
                                DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                else -> null
                            }
                            if (msg != null) onToast(msg)
                        }
                        true
                    } else when (keyEvent.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionLeft, Key.PageUp, Key.A -> {
                            if (pageCount > 1) {
                                if (isVerticalScrollMode) {
                                    val target = (verticalListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                    coroutineScope.launch {
                                        verticalListState.animateScrollToItem(target)
                                    }
                                    true
                                } else if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                    true
                                } else false
                            } else false
                        }
                        Key.DirectionRight, Key.PageDown, Key.D, Key.Spacebar -> {
                            if (pageCount > 1) {
                                if (isVerticalScrollMode) {
                                    val target = (verticalListState.firstVisibleItemIndex + 1).coerceAtMost(pageCount - 1)
                                    coroutineScope.launch {
                                        verticalListState.animateScrollToItem(target)
                                    }
                                    true
                                } else if (pagerState.currentPage < pageCount - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                    true
                                } else false
                            } else false
                        }
                        else -> false
                    }
                } else false
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(internalBackdrop),
        ) {
            if (pageCount > 1) {
                // 多 P 相邻页静默预加载（前后各 1 页）
                LaunchedEffect(pagerState.currentPage, pageCount, zoomQuality, settings?.pictureSource) {
                    val imageLoader = SingletonImageLoader.get(context)
                    val adjacentPages = listOf(pagerState.currentPage + 1, pagerState.currentPage - 1)
                        .filter { it in 0 until pageCount }
                    // 解析过程涉及目录遍历与磁盘缓存查询，统一切到 IO 线程，避免阻塞主线程
                    withContext(Dispatchers.IO) {
                        for (pIndex in adjacentPages) {
                            val p = illust.metaPages.getOrNull(pIndex) ?: continue
                            val rawTarget = when (zoomQuality) {
                                0 -> p.imageUrls?.original ?: p.imageUrls?.large.orEmpty()
                                1 -> p.imageUrls?.large.orEmpty().ifEmpty { p.imageUrls?.original.orEmpty() }
                                2 -> p.imageUrls?.medium ?: p.imageUrls?.large.orEmpty()
                                else -> p.imageUrls?.original ?: p.imageUrls?.large.orEmpty()
                            }
                            val optModel = resolveOptimizedImageModel(
                                context = context,
                                illust = illust,
                                pageIndex = pIndex,
                                targetUrl = rawTarget,
                                originalUrl = p.imageUrls?.original,
                                customBasePath = settings?.storePath,
                                pictureSource = settings?.pictureSource,
                            )
                            if (optModel.isNotBlank() && !optModel.startsWith("file:")) {
                                val transformed = if (settings?.pictureSource != null && settings.pictureSource != AppConstants.Network.HOST_PXIMG) {
                                    optModel.replace("://${AppConstants.Network.HOST_PXIMG}", "://${settings.pictureSource}")
                                } else optModel
                                // 预加载只用于写入磁盘缓存（Coil 在解码前落盘），解码结果随即丢弃，
                                // 因此禁用内存缓存并把解码尺寸压到最小，避免原图全尺寸位图短暂驻留堆内存引发 OOM
                                val req = ImageRequest.Builder(context)
                                    .data(transformed)
                                    .diskCacheKey(transformed)
                                    .size(
                                        Dimension(AppConstants.Network.IMAGE_PRELOAD_DECODE_DIMENSION),
                                        Dimension(AppConstants.Network.IMAGE_PRELOAD_DECODE_DIMENSION),
                                    )
                                    .precision(Precision.INEXACT)
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                                imageLoader.enqueue(req)
                            }
                        }
                    }
                }

                if (isVerticalScrollMode) {
                    LazyColumn(
                        state = verticalListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 64.dp, bottom = 32.dp),
                    ) {
                        items(pageCount, key = { it }) { pageIndex ->
                            ViewerPageItem(
                                illust = illust,
                                pageIndex = pageIndex,
                                initialPage = initialPage,
                                zoomQuality = zoomQuality,
                                previewUrl = previewUrl,
                                isVerticalMode = true,
                                onTap = { showControls = !showControls },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (pageIndex < pageCount - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = currentPageScale <= 1.05f,
                        modifier = Modifier.fillMaxSize(),
                    ) { pageIndex ->
                        ViewerPageItem(
                            illust = illust,
                            pageIndex = pageIndex,
                            initialPage = initialPage,
                            zoomQuality = zoomQuality,
                            previewUrl = previewUrl,
                            isVerticalMode = false,
                            onTap = { showControls = !showControls },
                            onScaleChanged = { scale ->
                                if (pagerState.currentPage == pageIndex) {
                                    currentPageScale = scale
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                ViewerPageItem(
                    illust = illust,
                    pageIndex = 0,
                    initialPage = 0,
                    zoomQuality = zoomQuality,
                    previewUrl = previewUrl,
                    isVerticalMode = false,
                    onTap = { showControls = !showControls },
                    onScaleChanged = { scale -> currentPageScale = scale },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // 顶部浮层：返回按钮与页码指示器（带淡入淡出动画与液态玻璃效果）
        CompositionLocalProvider(LocalBackdrop provides effectiveLayerBackdrop) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 左侧：返回按钮
                    LiquidCircleActionButton(
                        tooltip = strings.back,
                        onClick = onDismiss,
                        detailBackdrop = effectiveBackdrop,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // 中间：页码指示器（液态玻璃胶囊）
                    if (pageCount > 1) {
                        val indicatorShape = remember { RoundedCornerShape(16.dp) }
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    backdrop = effectiveBackdrop,
                                    shape = indicatorShape,
                                    blurRadius = 16.dp,
                                    tintColor = Color.Black,
                                    tintAlpha = 0.45f,
                                )
                                .squircleBorder(
                                    width = 0.6.dp,
                                    color = Color.White.copy(alpha = 0.18f),
                                    cornerRadius = 16.dp,
                                )
                                .clip(indicatorShape)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${currentDisplayPage + 1} / $pageCount",
                                style = MiuixTheme.textStyles.body2,
                                color = Color.White,
                            )
                        }
                    }

                    // 右侧：快捷操作组（阅读模式切换、单页下载、复制链接、分享、SauceNAO 搜图）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 切换阅读模式（垂直连续卷轴 vs 水平分页）
                        if (pageCount > 1) {
                            LiquidCircleActionButton(
                                tooltip = if (isVerticalScrollMode) strings.viewerModeHorizontal else strings.viewerModeVertical,
                                onClick = {
                                    if (isVerticalScrollMode) {
                                        val cur = verticalListState.firstVisibleItemIndex
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(cur.coerceIn(0, pageCount - 1))
                                        }
                                        isVerticalScrollMode = false
                                    } else {
                                        val cur = pagerState.currentPage
                                        coroutineScope.launch {
                                            verticalListState.scrollToItem(cur.coerceIn(0, pageCount - 1))
                                        }
                                        isVerticalScrollMode = true
                                    }
                                },
                                detailBackdrop = effectiveBackdrop,
                            ) {
                                Icon(
                                    imageVector = if (isVerticalScrollMode) MiuixIcons.ExpandMore else MiuixIcons.More,
                                    contentDescription = if (isVerticalScrollMode) strings.viewerModeHorizontal else strings.viewerModeVertical,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // 下载当前展示页
                        LiquidCircleActionButton(
                            tooltip = strings.download,
                            onClick = {
                                val currentPage = currentDisplayPage
                                val pageNumber = currentPage + 1
                                coroutineScope.launch {
                                    onToast("${strings.downloadStatusDownloading} P$pageNumber…")
                                    val task = downloadRepository.download(illust, pageIndex = currentPage)
                                    val msg = when (task.status) {
                                        DownloadStatus.Success -> "${strings.downloadStatusSuccess} (P$pageNumber)"
                                        DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                        else -> null
                                    }
                                    if (msg != null) onToast(msg)
                                }
                            },
                            detailBackdrop = effectiveBackdrop,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Download,
                                contentDescription = strings.download,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // 复制单页位图到系统剪贴板
                        LiquidCircleActionButton(
                            tooltip = strings.menuCopyImage,
                            onClick = {
                                val currentPage = currentDisplayPage
                                val targetUrl = if (illust.metaPages.isNotEmpty() && currentPage in illust.metaPages.indices) {
                                    illust.metaPages[currentPage].imageUrls?.large
                                        ?: illust.metaPages[currentPage].imageUrls?.medium
                                        ?: illust.imageUrls.large
                                } else {
                                    illust.imageUrls.large.ifEmpty { illust.imageUrls.medium }
                                }
                                coroutineScope.launch {
                                    suspendRunCatchingNonCancel {
                                        val candidateUrls = listOfNotNull(
                                            targetUrl,
                                            illust.imageUrls.large,
                                            illust.imageUrls.medium,
                                        )
                                        val bytes = extractCachedImageBytes(context, candidateUrls)
                                        bytes?.let { IllustClipboard().copyImage(it) }
                                            ?: throw IllegalStateException(strings.imageNoCacheFound)
                                    }.fold(
                                        onSuccess = { onToast(strings.imageCopySuccess) },
                                        onFailure = { e -> onToast("${strings.menuCopyImage}: ${e.message}") },
                                    )
                                }
                            },
                            detailBackdrop = effectiveBackdrop,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Copy,
                                contentDescription = strings.menuCopyImage,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // 复制单页作品链接
                        LiquidCircleActionButton(
                            tooltip = strings.menuCopyLink,
                            onClick = {
                                val currentPage = currentDisplayPage
                                val pageAnchor = if (pageCount > 1) "#page=${currentPage + 1}" else ""
                                val link = "${buildIllustShareLink(illust)}$pageAnchor"
                                runCatching {
                                    IllustClipboard().copy(link)
                                    onToast(strings.copiedToClipboard)
                                }.onFailure {
                                    onToast("${strings.copy}${strings.loadFailed}: ${it.message}")
                                }
                            },
                            detailBackdrop = effectiveBackdrop,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Link,
                                contentDescription = strings.menuCopyLink,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // 分享单页作品
                        LiquidCircleActionButton(
                            tooltip = strings.share,
                            onClick = {
                                val currentPage = currentDisplayPage
                                val pageAnchor = if (pageCount > 1) "#page=${currentPage + 1}" else ""
                                val link = "${buildIllustShareLink(illust)}$pageAnchor"
                                val shareTitle = if (pageCount > 1) "${illust.title} (P${currentPage + 1})" else illust.title
                                runCatching {
                                    IllustShare().share(link, shareTitle)
                                    onToast(strings.share)
                                }.onFailure {
                                    onToast("${strings.share}: ${it.message}")
                                }
                            },
                            detailBackdrop = effectiveBackdrop,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Share,
                                contentDescription = strings.share,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // SauceNAO 搜图
                        LiquidCircleActionButton(
                            tooltip = strings.menuSauceNao,
                            onClick = {
                                val currentPage = currentDisplayPage
                                val imgUrl = if (illust.metaPages.isNotEmpty() && currentPage in illust.metaPages.indices) {
                                    illust.metaPages[currentPage].imageUrls?.medium
                                        ?: illust.metaPages[currentPage].imageUrls?.squareMedium
                                        ?: illust.imageUrls.medium
                                } else {
                                    illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                                }
                                val sauceUrl = buildSauceNaoUrl(imgUrl)
                                openSafeUrl(sauceUrl, strings, onError = { onToast(it) })
                            },
                            detailBackdrop = effectiveBackdrop,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = strings.menuSauceNao,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 提取全屏查看器单页图片统一渲染逻辑，消除分页与垂直卷轴模式的重复代码。
 */
@Composable
private fun ViewerPageItem(
    illust: Illust,
    pageIndex: Int,
    initialPage: Int,
    zoomQuality: Int,
    previewUrl: String?,
    isVerticalMode: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onScaleChanged: ((Float) -> Unit)? = null,
) {
    val settings = LocalSettingsRepository.current
    val page = illust.metaPages.getOrNull(pageIndex)

    val rawZoomUrl = remember(page, zoomQuality) {
        if (page != null) {
            when (zoomQuality) {
                0 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                1 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                else -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
            }
        } else {
            when (zoomQuality) {
                0 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                1 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                else -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
            }
        }
    }
    val zoomUrl = rememberOptimizedImageModel(
        illust = illust,
        pageIndex = pageIndex,
        targetUrl = rawZoomUrl,
        originalUrl = page?.imageUrls?.original ?: illust.metaSinglePage?.originalImageUrl,
        customBasePath = settings?.storePath,
        pictureSource = settings?.pictureSource,
    )
    val thumbnailUrl = remember(page, pageIndex, initialPage, previewUrl) {
        if (pageIndex == initialPage && !previewUrl.isNullOrBlank()) {
            previewUrl
        } else if (page != null) {
            page.imageUrls?.let { it.large.ifEmpty { it.medium } }
                ?: illust.imageUrls.large.ifEmpty { illust.imageUrls.medium }
        } else {
            illust.imageUrls.large.ifEmpty { illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium } }
        }
    }

    ZoomableImage(
        model = zoomUrl,
        thumbnailUrl = thumbnailUrl,
        contentDescription = if (page != null) "${illust.title} ($pageIndex)" else illust.title,
        onTap = onTap,
        onScaleChanged = onScaleChanged,
        isVerticalMode = isVerticalMode,
        modifier = modifier,
    )
}

/**
 * 支持双指手势平滑缩放、鼠标滚轮定点缩放、双击放大/重置与边界限制拖拽平移的图片组件。
 */
@Composable
private fun ZoomableImage(
    model: Any?,
    thumbnailUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isVerticalMode: Boolean = false,
    onTap: () -> Unit = {},
    onScaleChanged: ((Float) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current
    val coroutineScope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember(model) { mutableStateOf(true) }
    var isError by remember(model) { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var autoRetryCount by remember(model) { mutableIntStateOf(0) }
    var showLoadingIndicator by remember(model) { mutableStateOf(false) }

    LaunchedEffect(isLoading, model) {
        if (isLoading) {
            delay(200)
            showLoadingIndicator = true
        } else {
            showLoadingIndicator = false
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(isVerticalMode) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val change = event.changes.firstOrNull() ?: continue
                            val scrollDelta = change.scrollDelta.y
                            if (scrollDelta != 0f) {
                                val isCtrlDown = event.keyboardModifiers.pointerIsCtrlPressed || event.keyboardModifiers.pointerIsMetaPressed
                                if (isVerticalMode && scale <= 1.05f && !isCtrlDown) {
                                    continue
                                }
                                val zoomFactor = if (scrollDelta < 0f) 1.15f else 0.8695f
                                val newScale = (scale * zoomFactor).coerceIn(1f, 8f)
                                val mousePos = change.position
                                val center = Offset(size.width / 2f, size.height / 2f)

                                if (newScale > 1.05f) {
                                    val scaleRatio = newScale / scale
                                    val newOffset = (offset + (center - mousePos)) * scaleRatio - (center - mousePos)
                                    val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                    offset = Offset(
                                        newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                        newOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                                scale = newScale
                                onScaleChanged?.invoke(newScale)
                                change.consume()
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        com.perol.pixez.shared.platform.performHapticFeedback(com.perol.pixez.shared.platform.HapticType.Tick)
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                            onScaleChanged?.invoke(1f)
                        } else {
                            scale = 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val targetOffset = (center - tapOffset) * 1.5f
                            val maxOffsetX = (size.width * 1.5f) / 2f
                            val maxOffsetY = (size.height * 1.5f) / 2f
                            offset = Offset(
                                targetOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                targetOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
                            )
                            onScaleChanged?.invoke(2.5f)
                        }
                    },
                    onTap = { onTap() },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 8f)
                    scale = newScale
                    onScaleChanged?.invoke(newScale)
                    if (newScale > 1.05f) {
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        val newOffsetX = (offset.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX)
                        val newOffsetY = (offset.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
                        offset = Offset(newOffsetX, newOffsetY)
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            .pointerHoverIcon(if (scale > 1.05f) PointerIcon.Hand else PointerIcon.Default),
        contentAlignment = Alignment.Center,
    ) {
        val effectiveModel = remember(model, reloadTrigger) {
            if (reloadTrigger > 0 && model is String) {
                if (model.contains("?")) "$model&_t=$reloadTrigger" else "$model?_t=$reloadTrigger"
            } else {
                model
            }
        }

        PixivAsyncImage(
            model = effectiveModel,
            thumbnailUrl = thumbnailUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            loadOriginalSize = true,
            filterQuality = FilterQuality.High,
            onLoading = {
                isLoading = true
                isError = false
            },
            onSuccess = {
                isLoading = false
                isError = false
            },
            onError = {
                val modelStr = model?.toString()
                if (autoRetryCount < 2) {
                    autoRetryCount++
                    coroutineScope.launch {
                        delay(350)
                        // 磁盘缓存探测属于阻塞 IO，切到 IO 线程执行
                        val cached = modelStr != null && withContext(Dispatchers.IO) {
                            com.perol.pixez.shared.platform.isUrlInCoilCache(context, modelStr, settings?.pictureSource)
                        }
                        if (cached) {
                            reloadTrigger++
                            isLoading = true
                            isError = false
                        } else if (autoRetryCount == 1) {
                            reloadTrigger++
                            isLoading = true
                            isError = false
                        } else {
                            isLoading = false
                            isError = true
                        }
                    }
                } else {
                    isLoading = false
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )

        // 高清原图加载中指示器（轻量悬浮暗色胶囊，防抖避免闪烁）
        AnimatedVisibility(
            visible = showLoadingIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfiniteProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = strings.loading,
                        style = MiuixTheme.textStyles.footnote1,
                        color = Color.White,
                    )
                }
            }
        }

        // 加载失败重试按钮
        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable {
                        isError = false
                        isLoading = true
                        autoRetryCount = 0
                        reloadTrigger++
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = strings.retry,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = strings.retry,
                        style = MiuixTheme.textStyles.footnote1,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
