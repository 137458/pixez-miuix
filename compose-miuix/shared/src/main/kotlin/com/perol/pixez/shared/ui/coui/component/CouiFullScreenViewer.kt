package com.perol.pixez.shared.ui.coui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.platform.isUrlInCoilCache
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.platform.rememberOptimizedImageModel
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ZoomableUgoiraViewer
import com.perol.pixez.shared.ui.components.executeViewerDownload
import com.perol.pixez.shared.ui.components.preloadViewerAdjacentPages
import com.perol.pixez.shared.ui.components.resolveViewerOriginalUrl
import com.perol.pixez.shared.ui.components.resolveViewerThumbnailUrl
import com.perol.pixez.shared.ui.components.resolveViewerZoomUrl
import com.perol.pixez.shared.ui.components.triggerViewerCopyImage
import com.perol.pixez.shared.ui.components.triggerViewerCopyLink
import com.perol.pixez.shared.ui.components.triggerViewerSauceNao
import com.perol.pixez.shared.ui.components.triggerViewerShare
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.accessibleTouchTarget
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.MouseWheelZoom
import net.engawapg.lib.zoomable.ScrollGesturePropagation
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.toggleScale
import net.engawapg.lib.zoomable.zoomable

/** 加载提示胶囊延迟显示时长（毫秒），本地命中缓存时不闪烁。 */
private const val COUI_VIEWER_LOADING_DELAY_MS = 200L

/** 自动重试探测缓存前等待时长（毫秒）。 */
private const val COUI_VIEWER_RETRY_DELAY_MS = 350L

/**
 * COUI 全屏看图器覆盖层入口：完全基于 COUI 原生组件与 [CouiIcons] 重写，
 * 独立承载缩放手势、多 P 分页/垂直卷轴切换、加载/重试反馈胶囊与锚点菜单，
 * 仅复用非 UI 的 URL/缓存/下载纯函数，不委托给 MIUIX 全屏查看器。
 */
@Composable
internal fun CouiIllustFullScreenOverlay(
    illust: Illust,
    pageIndex: Int,
    settings: SettingsRepository?,
    repository: IllustRepository,
    downloadRepository: DownloadRepository,
    onToast: (ToastData) -> Unit,
    onDismiss: () -> Unit,
) {
    val pageIdx = pageIndex.coerceAtLeast(0)
    val effectiveQuality = if (illust.type == IllustType.MANGA) {
        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
    } else {
        settings?.pictureQuality ?: 0
    }
    val currentPreviewUrl = if (illust.metaPages.isNotEmpty()) {
        val page = illust.metaPages.getOrNull(pageIdx)
        val rawTarget = if (page != null) {
            when (effectiveQuality) {
                0 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                else -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
            }
        } else {
            illust.imageUrls.large
        }
        rememberOptimizedImageModel(
            illust = illust,
            pageIndex = pageIdx,
            targetUrl = rawTarget,
            originalUrl = page?.imageUrls?.original,
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

    CouiFullScreenViewer(
        illust = illust,
        initialPage = pageIdx,
        zoomQuality = settings?.zoomQuality ?: 0,
        downloadRepository = downloadRepository,
        illustRepository = repository,
        previewUrl = currentPreviewUrl,
        onToast = onToast,
        onDismiss = onDismiss,
    )
}

@Composable
private fun CouiFullScreenViewer(
    illust: Illust,
    initialPage: Int,
    zoomQuality: Int,
    downloadRepository: DownloadRepository,
    illustRepository: IllustRepository,
    previewUrl: String?,
    onToast: (ToastData) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val pageCount = if (illust.metaPages.isNotEmpty()) illust.metaPages.size else 1
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )
    val verticalListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPage.coerceIn(0, pageCount - 1),
    )
    var isVerticalScrollMode by rememberSaveable(illust.id) { mutableStateOf(false) }
    val currentPageState = remember(isVerticalScrollMode, pageCount) {
        derivedStateOf {
            when {
                pageCount <= 1 -> 0
                isVerticalScrollMode -> verticalListState.firstVisibleItemIndex.coerceIn(0, pageCount - 1)
                else -> pagerState.currentPage
            }
        }
    }
    val currentDisplayPage by currentPageState
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var showControls by remember { mutableStateOf(true) }

    val triggerDownload: (Int) -> Unit = { page ->
        coroutineScope.launch {
            executeViewerDownload(
                illust = illust,
                currentPage = page,
                illustRepository = illustRepository,
                downloadRepository = downloadRepository,
                strings = strings,
                onToast = onToast,
            )
        }
    }

    PlatformBackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Throwable) {
            Napier.w("COUI 全屏查看器请求焦点失败", e, tag = "CouiIllustViewer")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                if ((keyEvent.isCtrlPressed || keyEvent.isMetaPressed) && keyEvent.key == Key.S) {
                    triggerDownload(currentDisplayPage)
                    true
                } else {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionLeft, Key.PageUp, Key.A -> handleViewerPageStep(
                            step = -1,
                            pageCount = pageCount,
                            isVerticalScrollMode = isVerticalScrollMode,
                            pagerState = pagerState,
                            verticalListState = verticalListState,
                            coroutineScope = coroutineScope,
                        )
                        Key.DirectionRight, Key.PageDown, Key.D, Key.Spacebar -> handleViewerPageStep(
                            step = 1,
                            pageCount = pageCount,
                            isVerticalScrollMode = isVerticalScrollMode,
                            pagerState = pagerState,
                            verticalListState = verticalListState,
                            coroutineScope = coroutineScope,
                        )
                        else -> false
                    }
                }
            },
    ) {
        CouiViewerPagesSection(
            illust = illust,
            illustRepository = illustRepository,
            pageCount = pageCount,
            pagerState = pagerState,
            verticalListState = verticalListState,
            isVerticalScrollMode = isVerticalScrollMode,
            zoomQuality = zoomQuality,
            initialPage = initialPage,
            previewUrl = previewUrl,
            onTap = { showControls = !showControls },
            modifier = Modifier.fillMaxSize(),
        )

        val toggleScrollMode: () -> Unit = {
            if (isVerticalScrollMode) {
                val current = verticalListState.firstVisibleItemIndex
                coroutineScope.launch {
                    pagerState.scrollToPage(current.coerceIn(0, pageCount - 1))
                }
                isVerticalScrollMode = false
            } else {
                val current = pagerState.currentPage
                coroutineScope.launch {
                    verticalListState.scrollToItem(current.coerceIn(0, pageCount - 1))
                }
                isVerticalScrollMode = true
            }
        }

        CouiViewerTopOverlay(
            visible = showControls,
            pageCount = pageCount,
            currentPageState = currentPageState,
            isVerticalScrollMode = isVerticalScrollMode,
            illust = illust,
            coroutineScope = coroutineScope,
            onDismiss = onDismiss,
            onToggleScrollMode = toggleScrollMode,
            onDownloadClick = { triggerDownload(currentDisplayPage) },
            onToast = onToast,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private fun handleViewerPageStep(
    step: Int,
    pageCount: Int,
    isVerticalScrollMode: Boolean,
    pagerState: PagerState,
    verticalListState: LazyListState,
    coroutineScope: CoroutineScope,
): Boolean {
    if (pageCount <= 1) return false
    return if (isVerticalScrollMode) {
        val current = verticalListState.firstVisibleItemIndex
        val target = (current + step).coerceIn(0, pageCount - 1)
        if (target == current) false
        else {
            coroutineScope.launch { verticalListState.animateScrollToItem(target) }
            true
        }
    } else {
        val target = pagerState.currentPage + step
        if (target !in 0 until pageCount) false
        else {
            coroutineScope.launch { pagerState.animateScrollToPage(target) }
            true
        }
    }
}

@Composable
private fun CouiViewerPagesSection(
    illust: Illust,
    illustRepository: IllustRepository,
    pageCount: Int,
    pagerState: PagerState,
    verticalListState: LazyListState,
    isVerticalScrollMode: Boolean,
    zoomQuality: Int,
    initialPage: Int,
    previewUrl: String?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current
    Box(modifier = modifier.fillMaxSize()) {
        if (IllustType.isUgoira(illust.type)) {
            ZoomableUgoiraViewer(
                illust = illust,
                illustRepository = illustRepository,
                onTap = onTap,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (pageCount > 1) {
            val activePage = if (isVerticalScrollMode) verticalListState.firstVisibleItemIndex else pagerState.currentPage
            LaunchedEffect(activePage, pageCount, zoomQuality, settings.pictureSource) {
                preloadViewerAdjacentPages(
                    context = context,
                    imageLoader = SingletonImageLoader.get(context),
                    illust = illust,
                    pageCount = pageCount,
                    activePage = activePage,
                    zoomQuality = zoomQuality,
                    settings = settings,
                )
            }

            if (isVerticalScrollMode) {
                LazyColumn(
                    state = verticalListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 64.dp, bottom = 32.dp),
                    overscrollEffect = rememberCouiOverscrollEffect(),
                ) {
                    items(pageCount, key = { it }) { pageIndex ->
                        CouiViewerPageItem(
                            illust = illust,
                            pageIndex = pageIndex,
                            initialPage = initialPage,
                            zoomQuality = zoomQuality,
                            previewUrl = previewUrl,
                            isVerticalMode = true,
                            isCurrentPage = verticalListState.firstVisibleItemIndex == pageIndex,
                            onTap = onTap,
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
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    CouiViewerPageItem(
                        illust = illust,
                        pageIndex = pageIndex,
                        initialPage = initialPage,
                        zoomQuality = zoomQuality,
                        previewUrl = previewUrl,
                        isVerticalMode = false,
                        isCurrentPage = pagerState.currentPage == pageIndex,
                        onTap = onTap,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            CouiViewerPageItem(
                illust = illust,
                pageIndex = 0,
                initialPage = 0,
                zoomQuality = zoomQuality,
                previewUrl = previewUrl,
                isVerticalMode = false,
                isCurrentPage = true,
                onTap = onTap,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CouiViewerPageItem(
    illust: Illust,
    pageIndex: Int,
    initialPage: Int,
    zoomQuality: Int,
    previewUrl: String?,
    isVerticalMode: Boolean,
    isCurrentPage: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettingsRepository.current
    val page = illust.metaPages.getOrNull(pageIndex)
    val rawZoomUrl = remember(illust, pageIndex, zoomQuality) {
        resolveViewerZoomUrl(illust, pageIndex, zoomQuality)
    }
    val zoomUrl = rememberOptimizedImageModel(
        illust = illust,
        pageIndex = pageIndex,
        targetUrl = rawZoomUrl,
        originalUrl = resolveViewerOriginalUrl(illust, pageIndex),
        customBasePath = settings.storePath,
        pictureSource = settings.pictureSource,
    )
    val thumbnailUrl = remember(illust, pageIndex, initialPage, previewUrl) {
        resolveViewerThumbnailUrl(illust, pageIndex, initialPage, previewUrl)
    }
    val contentSize = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            Size(illust.width.toFloat(), illust.height.toFloat())
        } else {
            Size.Zero
        }
    }

    CouiZoomableImage(
        model = zoomUrl,
        thumbnailUrl = thumbnailUrl,
        contentDescription = if (page != null) "${illust.title} ($pageIndex)" else illust.title,
        contentSize = contentSize,
        isVerticalMode = isVerticalMode,
        isCurrentPage = isCurrentPage,
        onTap = onTap,
        modifier = modifier,
    )
}

/**
 * COUI 手势缩放图片组件：双指缩放、双击弹性放缩、拖拽平移及边缘滑动让渡，
 * 加载与重试浮层使用 COUI 胶囊、矢量图标与排版样式。
 */
@Composable
private fun CouiZoomableImage(
    model: Any?,
    thumbnailUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentSize: Size = Size.Zero,
    isVerticalMode: Boolean = false,
    isCurrentPage: Boolean = true,
    onTap: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current
    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberZoomState(
        maxScale = 8f,
        contentSize = contentSize,
    )
    var isLoading by remember(model) { mutableStateOf(true) }
    var isError by remember(model) { mutableStateOf(false) }
    var reloadTrigger by remember(model) { mutableIntStateOf(0) }
    var autoRetryCount by remember(model) { mutableIntStateOf(0) }
    var showLoadingIndicator by remember(model) { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && zoomState.scale > 1f) {
            zoomState.reset()
        }
    }

    LaunchedEffect(isLoading, model) {
        if (isLoading) {
            delay(COUI_VIEWER_LOADING_DELAY_MS)
            showLoadingIndicator = true
        } else {
            showLoadingIndicator = false
        }
    }

    val isZoomedIn by remember(zoomState) {
        derivedStateOf { zoomState.scale > 1.05f }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerHoverIcon(if (isZoomedIn) PointerIcon.Hand else PointerIcon.Default),
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
                        delay(COUI_VIEWER_RETRY_DELAY_MS)
                        val cached = modelStr != null && withContext(Dispatchers.IO) {
                            isUrlInCoilCache(context, modelStr, settings.pictureSource)
                        }
                        if (cached || autoRetryCount == 1) {
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
                .zoomable(
                    zoomState = zoomState,
                    scrollGesturePropagation = ScrollGesturePropagation.ContentEdge,
                    mouseWheelZoom = if (isVerticalMode) {
                        MouseWheelZoom.EnabledWithCtrlKey
                    } else {
                        MouseWheelZoom.Enabled
                    },
                    onTap = { onTap() },
                    onDoubleTap = { position ->
                        performHapticFeedback(HapticType.Tick)
                        zoomState.toggleScale(2.5f, position)
                    },
                ),
        )

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
                    .clip(CouiCapsuleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), CouiCapsuleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CouiViewerSpinner(modifier = Modifier.size(16.dp), color = Color.White)
                    Text(
                        text = strings.loading,
                        kind = CouiTextKind.ItemSummary,
                        color = Color.White,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) {
            val retryInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(CouiCapsuleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.18f), CouiCapsuleShape)
                    .couiButtonFeedback(dark = true, interactionSource = retryInteraction, list = false)
                    .clickable(
                        interactionSource = retryInteraction,
                        indication = null,
                        role = Role.Button,
                    ) {
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
                    Image(
                        painter = rememberVectorPainter(CouiIcons.Refresh),
                        contentDescription = strings.retry,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = strings.retry,
                        kind = CouiTextKind.ItemSummary,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiViewerSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    val transition = rememberInfiniteTransition(label = "couiViewerSpinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "couiViewerSpinnerAngle",
    )
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        drawArc(
            color = color.copy(alpha = 0.25f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 240f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun CouiViewerTopOverlay(
    visible: Boolean,
    pageCount: Int,
    currentPageState: State<Int>,
    isVerticalScrollMode: Boolean,
    illust: Illust,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    onToggleScrollMode: () -> Unit,
    onDownloadClick: () -> Unit,
    onToast: (ToastData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val context = LocalPlatformContext.current
    val currentDisplayPage by currentPageState
    val menuAnchor = rememberCouiMenuAnchor()
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            CouiViewerCircleButton(
                icon = CouiIcons.Back,
                contentDescription = strings.back,
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart),
            )

            if (pageCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CouiCapsuleShape)
                        .background(Color.Black.copy(alpha = 0.56f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), CouiCapsuleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${currentDisplayPage + 1} / $pageCount",
                        kind = CouiTextKind.ItemSummary,
                        color = Color.White,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pageCount > 1) {
                    val switchTooltip = if (isVerticalScrollMode) {
                        strings.viewerModeHorizontal
                    } else {
                        strings.viewerModeVertical
                    }
                    val switchIcon = if (isVerticalScrollMode) {
                        CouiIcons.HorizontalPages
                    } else {
                        CouiIcons.VerticalScroll
                    }
                    CouiViewerCircleButton(
                        icon = switchIcon,
                        contentDescription = switchTooltip,
                        onClick = onToggleScrollMode,
                    )
                }

                CouiViewerCircleButton(
                    icon = CouiIcons.Download,
                    contentDescription = strings.download,
                    onClick = onDownloadClick,
                )

                Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                    CouiViewerCircleButton(
                        icon = CouiIcons.More,
                        contentDescription = strings.menuMoreActions,
                        onClick = { menuAnchor.open { showMoreMenu = true } },
                    )
                    val menuItems = listOf(
                        CouiMenuItem(
                            value = "copy_image",
                            text = strings.menuCopyImage,
                            icon = CouiIcons.Copy,
                        ),
                        CouiMenuItem(
                            value = "copy_link",
                            text = strings.menuCopyLink,
                            icon = CouiIcons.Link,
                        ),
                        CouiMenuItem(
                            value = "share",
                            text = strings.share,
                            icon = CouiIcons.Share,
                            dividerBefore = true,
                        ),
                        CouiMenuItem(
                            value = "saucenao",
                            text = strings.menuSauceNao,
                            icon = CouiIcons.Search,
                        ),
                    )
                    Menu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        anchor = menuAnchor,
                        headerText = strings.menuMoreActions,
                        items = menuItems,
                        onItemClick = { clicked ->
                            showMoreMenu = false
                            when (clicked.value) {
                                "copy_image" -> triggerViewerCopyImage(
                                    illust = illust,
                                    currentDisplayPage = currentDisplayPage,
                                    context = context,
                                    coroutineScope = coroutineScope,
                                    strings = strings,
                                    onToast = onToast,
                                )
                                "copy_link" -> triggerViewerCopyLink(
                                    illust = illust,
                                    pageCount = pageCount,
                                    currentDisplayPage = currentDisplayPage,
                                    strings = strings,
                                    onToast = onToast,
                                )
                                "share" -> triggerViewerShare(
                                    illust = illust,
                                    pageCount = pageCount,
                                    currentDisplayPage = currentDisplayPage,
                                    strings = strings,
                                    onToast = onToast,
                                )
                                "saucenao" -> triggerViewerSauceNao(
                                    illust = illust,
                                    currentDisplayPage = currentDisplayPage,
                                    strings = strings,
                                    onToast = onToast,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiViewerCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .accessibleTouchTarget(44.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.56f))
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .couiButtonFeedback(dark = true, interactionSource = source, list = false)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(20.dp),
        )
    }
}
