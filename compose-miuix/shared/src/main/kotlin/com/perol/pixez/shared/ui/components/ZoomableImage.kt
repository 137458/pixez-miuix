package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.isUrlInCoilCache
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.MouseWheelZoom
import net.engawapg.lib.zoomable.ScrollGesturePropagation
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.toggleScale
import net.engawapg.lib.zoomable.zoomable
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 加载指示器延迟显示时长（毫秒），快速加载完成时不闪指示器。 */
private const val LOADING_INDICATOR_DELAY_MILLIS = 200L

/** 加载失败自动重试前探测磁盘缓存的等待时长（毫秒）。 */
private const val RETRY_CACHE_PROBE_DELAY_MILLIS = 350L

/**
 * 基于 `net.engawapg.lib.zoomable` 的手势缩放图片组件：
 * 支持双指焦点中心缩放 (Pinch-to-zoom)、双击弹性动画缩放、单指拖拽惯性滑动 (Fling)、
 * 精准 ContentScale.Fit 边界约束以及与 HorizontalPager / LazyColumn 的边缘嵌套滑动让渡。
 *
 * 命中失败先铺 [thumbnailUrl] 作为过渡底图，再按 [model] 拉取目标画质大图；
 * 大图加载失败时先探测磁盘缓存，未命中才用随参数变化的 reload 标记绕过缓存重试。
 *
 * @param isCurrentPage 该页是否为当前展示页：翻走时自动复位缩放，避免页码回退后仍停在放大态。
 * @param isVerticalMode 纵向卷轴模式下滚轮缩放要求 Ctrl 修饰键，横向分页模式直接滚轮缩放。
 */
@Composable
internal fun ZoomableImage(
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

    val viewerZoomedState = LocalFullScreenViewerZoomedState.current
    LaunchedEffect(zoomState, isCurrentPage, viewerZoomedState) {
        if (isCurrentPage && viewerZoomedState != null) {
            androidx.compose.runtime.snapshotFlow { zoomState.scale > 1.03f }
                .collect { viewerZoomedState.value = it }
        }
    }

    LaunchedEffect(isLoading, model) {
        if (isLoading) {
            delay(LOADING_INDICATOR_DELAY_MILLIS)
            showLoadingIndicator = true
        } else {
            showLoadingIndicator = false
        }
    }

    // 缩放动画每帧都会改 scale：派生布尔值让 hover 图标只在跨越阈值时变更，
    // 避免整棵图片子树（含 Coil 请求持有者）随动画帧反复重组。
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
                        delay(RETRY_CACHE_PROBE_DELAY_MILLIS)
                        // 磁盘缓存探测属于阻塞 IO，切到 IO 线程执行
                        val cached = modelStr != null && withContext(Dispatchers.IO) {
                            isUrlInCoilCache(context, modelStr, settings.pictureSource)
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
