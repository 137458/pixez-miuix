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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share
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
) {
    val strings = LocalStrings.current
    val pageCount = if (illust.metaPages.isNotEmpty()) illust.metaPages.size else 1
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var currentPageScale by remember { mutableFloatStateOf(1f) }
    var showControls by remember { mutableStateOf(true) }

    PlatformBackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Throwable) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionLeft, Key.PageUp, Key.A -> {
                            if (pageCount > 1 && pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                                true
                            } else false
                        }
                        Key.DirectionRight, Key.PageDown, Key.D, Key.Spacebar -> {
                            if (pageCount > 1 && pagerState.currentPage < pageCount - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            },
    ) {
        if (pageCount > 1) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentPageScale <= 1.05f,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                val page = illust.metaPages[pageIndex]
                val zoomUrl = remember(page, zoomQuality) {
                    when (zoomQuality) {
                        0 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                        1 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                        2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                        else -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                    }
                }
                val thumbnailUrl = remember(page) {
                    page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
                }

                ZoomableImage(
                    model = zoomUrl,
                    thumbnailUrl = thumbnailUrl,
                    contentDescription = "${illust.title} ($pageIndex)",
                    onTap = { showControls = !showControls },
                    onScaleChanged = { scale ->
                        if (pagerState.currentPage == pageIndex) {
                            currentPageScale = scale
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            val singleZoomUrl = remember(illust, zoomQuality) {
                when (zoomQuality) {
                    0 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                    1 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                    2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                    else -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                }
            }
            val thumbnailUrl = remember(illust) {
                illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium }
            }

            ZoomableImage(
                model = singleZoomUrl,
                thumbnailUrl = thumbnailUrl,
                contentDescription = illust.title,
                onTap = { showControls = !showControls },
                onScaleChanged = { scale -> currentPageScale = scale },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 顶部浮层：返回按钮与页码指示器（带淡入淡出动画）
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
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = strings.back,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 中间：页码指示器
                if (pageCount > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount",
                            style = MiuixTheme.textStyles.body2,
                            color = Color.White,
                        )
                    }
                }

                // 右侧：快捷操作组（单页下载、复制链接、分享、SauceNAO 搜图）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 下载当前展示页
                    LiquidCircleActionButton(
                        tooltip = strings.download,
                        onClick = {
                            val currentPage = pagerState.currentPage
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
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = strings.download,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // 复制单页作品链接
                    LiquidCircleActionButton(
                        tooltip = strings.menuCopyLink,
                        onClick = {
                            val currentPage = pagerState.currentPage
                            val pageAnchor = if (pageCount > 1) "#page=${currentPage + 1}" else ""
                            val link = "${buildIllustShareLink(illust)}$pageAnchor"
                            runCatching {
                                IllustClipboard().copy(link)
                                onToast(strings.copiedToClipboard)
                            }.onFailure {
                                onToast("${strings.copy}${strings.loadFailed}: ${it.message}")
                            }
                        },
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
                            val currentPage = pagerState.currentPage
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
                            val currentPage = pagerState.currentPage
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

/**
 * 支持双指手势平滑缩放、鼠标滚轮定点缩放、双击放大/重置与边界限制拖拽平移的图片组件。
 */
@Composable
private fun ZoomableImage(
    model: Any?,
    thumbnailUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onScaleChanged: ((Float) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember(model) { mutableStateOf(true) }
    var isError by remember(model) { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val change = event.changes.firstOrNull() ?: continue
                            val scrollDelta = change.scrollDelta.y
                            if (scrollDelta != 0f) {
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
                isLoading = false
                isError = true
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

        // 高清原图加载中指示器（轻量悬浮暗色胶囊）
        AnimatedVisibility(
            visible = isLoading,
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
