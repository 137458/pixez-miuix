package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Precision
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.mapToPictureSource
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.platform.rememberOptimizedImageModel
import com.perol.pixez.shared.platform.resolveOptimizedImageModel
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository

/**
 * 插画全屏/高清缩放预览组件：
 * 支持双指平滑手势缩放 (Pinch-to-zoom)、鼠标滚轮定点缩放、鼠标拖拽平移 (Pan)、双击放大/复原 (Double-tap-to-zoom)、
 * 键盘左右方向键/翻页键切页、ESC 快速退出以及多 P 左右切页。
 * 根据 [SettingsRepository.zoomQuality] 加载对应画质大图，优先使用已有内存缓存作为过渡底图；
 * 当作品为 Ugoira 动图时，直接渲染无缝续播的可手势缩放动态画面，并统一使用液态玻璃顶栏。
 */
@Composable
fun IllustFullScreenViewer(
    illust: Illust,
    initialPage: Int,
    zoomQuality: Int,
    downloadRepository: DownloadRepository,
    onToast: (ToastData) -> Unit,
    onDismiss: () -> Unit,
    detailBackdrop: Backdrop? = null,
    previewUrl: String? = null,
    illustRepository: IllustRepository? = null,
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
    // 页码/控件显隐为跨区块共享状态：以 State 引用传入子 Section，保持原有重组作用域粒度。
    val currentPageState = remember(isVerticalScrollMode, pageCount) {
        derivedStateOf {
            if (pageCount <= 1) 0
            else if (isVerticalScrollMode) {
                verticalListState.firstVisibleItemIndex.coerceIn(0, pageCount - 1)
            } else {
                pagerState.currentPage
            }
        }
    }
    val currentDisplayPage by currentPageState
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val showControlsState = remember { mutableStateOf(true) }
    var showControls by showControlsState

    val internalBackdrop = if (isRuntimeShaderSupported()) {
        rememberLayerBackdrop {
            drawRect(Color.Black)
            drawContent()
        }
    } else null
    val effectiveBackdrop = internalBackdrop ?: detailBackdrop
    val effectiveLayerBackdrop = internalBackdrop ?: (detailBackdrop as? LayerBackdrop)

    val triggerDownload: (Int) -> Unit = { currentPage ->
        val pageNumber = currentPage + 1
        coroutineScope.launch {
            if (IllustType.isUgoira(illust.type) && illustRepository != null) {
                // 动图保存复用与详情页顶栏同一条路径，避免两处各自维护下载流程。
                onToast(ToastData("${strings.downloadStatusDownloading}…", ToastType.Normal))
                saveUgoiraIllust(
                    illust = illust,
                    illustRepository = illustRepository,
                    downloadRepository = downloadRepository,
                ).fold(
                    onSuccess = {
                        performHapticFeedback(HapticType.Confirm)
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
                val toast = when (task.status) {
                    DownloadStatus.Success -> ToastData("${strings.downloadStatusSuccess} (P$pageNumber)", ToastType.Success)
                    DownloadStatus.Failed -> ToastData("${strings.downloadStatusFailed}: ${task.error.toLocalizedDownloadError(strings)}", ToastType.Error)
                    else -> null
                }
                if (toast != null) onToast(toast)
            }
        }
    }

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
                        triggerDownload(currentDisplayPage)
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
        ViewerPagesSection(
            illust = illust,
            illustRepository = illustRepository,
            pageCount = pageCount,
            pagerState = pagerState,
            verticalListState = verticalListState,
            isVerticalScrollMode = isVerticalScrollMode,
            zoomQuality = zoomQuality,
            initialPage = initialPage,
            previewUrl = previewUrl,
            internalBackdrop = internalBackdrop,
            onTap = { showControls = !showControls },
            modifier = Modifier.fillMaxSize(),
        )

        // 顶部浮层：返回按钮与页码指示器（带淡入淡出动画与液态玻璃效果）
        ViewerTopOverlaySection(
            visibleState = showControlsState,
            strings = strings,
            pageCount = pageCount,
            currentPageState = currentPageState,
            isVerticalScrollMode = isVerticalScrollMode,
            pagerState = pagerState,
            verticalListState = verticalListState,
            coroutineScope = coroutineScope,
            illust = illust,
            effectiveBackdrop = effectiveBackdrop,
            effectiveLayerBackdrop = effectiveLayerBackdrop,
            onDismiss = onDismiss,
            onVerticalScrollModeChange = { isVerticalScrollMode = it },
            onDownloadClick = { triggerDownload(currentDisplayPage) },
            onToast = onToast,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * 全屏查看器图片内容区：Ugoira 动图 / 多 P（水平分页或垂直卷轴，含相邻页预加载）/ 单页三种渲染分支。
 */
@Composable
private fun ViewerPagesSection(
    illust: Illust,
    illustRepository: IllustRepository?,
    pageCount: Int,
    pagerState: PagerState,
    verticalListState: LazyListState,
    isVerticalScrollMode: Boolean,
    zoomQuality: Int,
    initialPage: Int,
    previewUrl: String?,
    internalBackdrop: LayerBackdrop?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .blurBackdropSource(internalBackdrop),
    ) {
        if (IllustType.isUgoira(illust.type) && illustRepository != null) {
            // 动图为单页作品：其画面由帧序列驱动，不存在多 P 翻页与相邻页预加载，
            // 因此这里优先于 pageCount 分支；页码指示与翻页让渡对其不适用。
            ZoomableUgoiraViewer(
                illust = illust,
                illustRepository = illustRepository,
                onTap = onTap,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (pageCount > 1) {
            // 多 P 相邻页静默预加载（前后各 1 页，同时覆盖横向 Pager 与纵向滚动模式）。
            // 当前页在此处按渲染模式就地取一次，避免把父层的派生状态再复制一份进来。
            val activePage = if (isVerticalScrollMode) verticalListState.firstVisibleItemIndex else pagerState.currentPage
            LaunchedEffect(activePage, pageCount, zoomQuality, settings.pictureSource) {
                preloadAdjacentPages(
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
                ) {
                    items(pageCount, key = { it }) { pageIndex ->
                        ViewerPageItem(
                            illust = illust,
                            pageIndex = pageIndex,
                            initialPage = initialPage,
                            zoomQuality = zoomQuality,
                            previewUrl = previewUrl,
                            isVerticalMode = true,
                            // 纵向模式同样只在页首可见时保留缩放，滚走后由 ZoomableImage 复位。
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
                    ViewerPageItem(
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
            ViewerPageItem(
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

/**
 * 全屏查看器顶部浮层：返回按钮、页码指示器与快捷操作组，带淡入淡出与液态玻璃效果。
 * visibleState/currentPageState 以 State 引用传入，保持控件显隐与页码更新的原重组粒度。
 */
@Composable
private fun ViewerTopOverlaySection(
    visibleState: State<Boolean>,
    strings: AppStrings,
    pageCount: Int,
    currentPageState: State<Int>,
    isVerticalScrollMode: Boolean,
    pagerState: PagerState,
    verticalListState: LazyListState,
    coroutineScope: CoroutineScope,
    illust: Illust,
    effectiveBackdrop: Backdrop?,
    effectiveLayerBackdrop: LayerBackdrop?,
    onDismiss: () -> Unit,
    onVerticalScrollModeChange: (Boolean) -> Unit,
    onDownloadClick: () -> Unit,
    onToast: (ToastData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible by visibleState
    val currentDisplayPage by currentPageState
    CompositionLocalProvider(LocalBackdrop provides effectiveLayerBackdrop) {
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
                // 左侧：返回按钮
                Box(
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
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
                }

                // 中间：页码指示器（真·水平绝对居中，仅多 P 时展示）
                ViewerPageIndicatorSection(
                    pageCount = pageCount,
                    currentDisplayPage = currentDisplayPage,
                    effectiveBackdrop = effectiveBackdrop,
                    modifier = Modifier.align(Alignment.Center),
                )

                // 右侧：快捷操作组（阅读模式切换、单页下载、更多操作下拉菜单）
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    ViewerQuickActionsSection(
                        strings = strings,
                        pageCount = pageCount,
                        currentDisplayPage = currentDisplayPage,
                        isVerticalScrollMode = isVerticalScrollMode,
                        pagerState = pagerState,
                        verticalListState = verticalListState,
                        coroutineScope = coroutineScope,
                        illust = illust,
                        effectiveBackdrop = effectiveBackdrop,
                        onVerticalScrollModeChange = onVerticalScrollModeChange,
                        onDownloadClick = onDownloadClick,
                        onToast = onToast,
                    )
                }
            }
        }
    }
}

/**
 * 页码指示器（液态玻璃胶囊），仅多 P 时展示。
 */
@Composable
private fun ViewerPageIndicatorSection(
    pageCount: Int,
    currentDisplayPage: Int,
    effectiveBackdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    if (pageCount > 1) {
        val indicatorShape = remember { RoundedCornerShape(16.dp) }
        Box(
            modifier = modifier
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
}

/**
 * 顶部快捷操作组：多 P 阅读模式切换、单页下载、更多操作下拉菜单（收敛复制图片/链接、分享与 SauceNAO 搜图）。
 */
@Composable
private fun ViewerQuickActionsSection(
    strings: AppStrings,
    pageCount: Int,
    currentDisplayPage: Int,
    isVerticalScrollMode: Boolean,
    pagerState: PagerState,
    verticalListState: LazyListState,
    coroutineScope: CoroutineScope,
    illust: Illust,
    effectiveBackdrop: Backdrop?,
    onVerticalScrollModeChange: (Boolean) -> Unit,
    onDownloadClick: () -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val context = LocalPlatformContext.current
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 切换阅读模式（垂直连续卷轴 vs 水平切页，仅多 P 时展示）
        if (pageCount > 1) {
            val (switchTooltip, switchIcon) = resolveViewerModeSwitchState(
                isVerticalScrollMode = isVerticalScrollMode,
                strings = strings,
            )
            LiquidCircleActionButton(
                tooltip = switchTooltip,
                onClick = {
                    if (isVerticalScrollMode) {
                        val cur = verticalListState.firstVisibleItemIndex
                        coroutineScope.launch {
                            pagerState.scrollToPage(cur.coerceIn(0, pageCount - 1))
                        }
                        onVerticalScrollModeChange(false)
                    } else {
                        val cur = pagerState.currentPage
                        coroutineScope.launch {
                            verticalListState.scrollToItem(cur.coerceIn(0, pageCount - 1))
                        }
                        onVerticalScrollModeChange(true)
                    }
                },
                detailBackdrop = effectiveBackdrop,
            ) {
                Icon(
                    imageVector = switchIcon,
                    contentDescription = switchTooltip,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 2. 下载当前展示页
        LiquidCircleActionButton(
            tooltip = strings.download,
            onClick = { onDownloadClick() },
            detailBackdrop = effectiveBackdrop,
        ) {
            Icon(
                imageVector = MiuixIcons.Download,
                contentDescription = strings.download,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // 3. 更多操作（点击弹出下拉菜单，优雅收敛低频操作）
        Box {
            LiquidCircleActionButton(
                tooltip = strings.menuMoreActions,
                onClick = { showMoreMenu = !showMoreMenu },
                detailBackdrop = effectiveBackdrop,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = strings.menuMoreActions,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            OverlayDropdownPopup(
                entries = buildViewerMoreMenuEntries(
                    strings = strings,
                    illust = illust,
                    pageCount = pageCount,
                    currentDisplayPage = currentDisplayPage,
                    context = context,
                    coroutineScope = coroutineScope,
                    onToast = onToast,
                ),
                show = showMoreMenu,
                onDismiss = { showMoreMenu = false },
                onDismissFinished = {},
                maxHeight = null,
                dropdownColors = DropdownDefaults.dropdownColors(),
                renderInRootScaffold = true,
                collapseOnSelection = true,
            )
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
    isCurrentPage: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
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
        customBasePath = settings.storePath,
        pictureSource = settings.pictureSource,
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
    val contentSize = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            Size(illust.width.toFloat(), illust.height.toFloat())
        } else {
            Size.Zero
        }
    }

    ZoomableImage(
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
 * 预加载 [activePage] 前后各一页到磁盘缓存。
 *
 * 只写磁盘（Coil 在解码前落盘），解码结果随即丢弃；路径解析涉及目录遍历与缓存查询，
 * 因此整体切到 IO 线程并顺序执行，避免同时申请多份解码缓冲。
 */
private suspend fun preloadAdjacentPages(
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
                customBasePath = settings.storePath,
                pictureSource = settings.pictureSource,
            )
            if (optModel.isBlank() || optModel.startsWith("file:")) continue
            // 图源改写与图片组件走同一条 platform 接缝，避免预加载与最终加载指向不同 URL。
            val transformed = optModel.mapToPictureSource(settings.pictureSource)
            // 预加载只用于写入磁盘缓存，解码结果随即丢弃，因此禁用内存缓存并把解码尺寸压到最小，
            // 避免原图全尺寸位图短暂驻留堆内存引发 OOM。
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
            // 不用 enqueue：其内部固定经 Dispatchers.Main 启动协程，而桌面 JVM 没有
            // kotlinx-coroutines 的 Main dispatcher 实现，会反复抛「Module with the Main
            // dispatcher is missing」且未捕获异常直接打在 AWT 事件线程上；execute 在
            // 调用者上下文挂起（此处已在 IO 线程），预加载仅为写磁盘缓存，顺序执行无感。
            imageLoader.execute(request)
        }
    }
}
