package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.skin.LocalUiSkin
import com.perol.pixez.shared.ui.skin.UiSkin
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.MouseWheelZoom
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.toggleScale
import net.engawapg.lib.zoomable.zoomable
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator

/**
 * Ugoira 播放视图状态。
 *
 * 仅描述「加载到哪一步」，播放进度不在此状态内——活跃帧索引由各视图的
 * [mutableIntStateOf] 独占持有，避免内嵌播放器与全屏查看器并发改写同一份进度。
 */
private sealed interface UgoiraState {
    data object Idle : UgoiraState
    data class Loading(val stageText: String) : UgoiraState
    data class Ready(val session: UgoiraReadySession) : UgoiraState
    data class Error(val message: String) : UgoiraState
}

/** 把加载阶段映射为本地化文案。 */
private fun UgoiraLoadStage.toText(strings: AppStrings): String = when (this) {
    UgoiraLoadStage.LoadingMetadata -> strings.ugoiraLoadingMetadata
    UgoiraLoadStage.Downloading -> strings.ugoiraDownloading
    UgoiraLoadStage.Extracting -> strings.ugoiraExtracting
}

/**
 * 加载 Ugoira 会话并驱动逐帧循环的公共逻辑，供内嵌播放器与全屏查看器复用。
 *
 * @param illustId 作品 ID。
 * @param illustRepository 元数据与 zip 数据源。
 * @param autoLoad 是否在进入组合时立即加载（全屏查看器始终加载）。
 * @param onError 加载失败回调。
 * @return 当前状态、当前帧索引与帧解码器。
 */
@Composable
private fun rememberUgoiraPlayback(
    illustId: Long,
    illustRepository: IllustRepository,
    autoLoad: Boolean,
    onError: (Throwable) -> Unit,
): UgoiraPlayback {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val cached = remember(illustId) { UgoiraSessionCache.get(illustId) }
    var state by remember(illustId) {
        mutableStateOf<UgoiraState>(
            if (cached != null) UgoiraState.Ready(cached) else UgoiraState.Idle,
        )
    }
    var frameIndex by remember(illustId) { mutableIntStateOf(cached?.beginPlayback() ?: 0) }

    fun load() {
        val existing = UgoiraSessionCache.get(illustId)
        if (existing != null) {
            frameIndex = existing.beginPlayback()
            state = UgoiraState.Ready(existing)
            return
        }
        scope.launch {
            state = UgoiraState.Loading(strings.ugoiraLoadingMetadata)
            try {
                val session = loadUgoiraSession(
                    illustId = illustId,
                    illustRepository = illustRepository,
                    onStage = { stage -> state = UgoiraState.Loading(stage.toText(strings)) },
                )
                if (session == null) {
                    state = UgoiraState.Error(strings.ugoiraDecodeFailed)
                } else {
                    frameIndex = session.beginPlayback()
                    state = UgoiraState.Ready(session)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Napier.e("加载动图失败 illustId=$illustId", e, tag = "UgoiraPlayer")
                state = UgoiraState.Error(PixivApiErrorFormatter.format(e, strings))
                onError(e)
            }
        }
    }

    val readyState = state as? UgoiraState.Ready
    LaunchedEffect(readyState) {
        val session = readyState?.session ?: return@LaunchedEffect
        runUgoiraFrameLoop(
            session = session,
            startFrameIndex = frameIndex,
            onFrame = { frameIndex = it },
            onProgress = { frameIndex = it },
        )
    }

    return UgoiraPlayback(
        state = state,
        frameIndex = frameIndex,
        load = ::load,
        shouldAutoLoad = autoLoad && state is UgoiraState.Idle,
    )
}

/** [rememberUgoiraPlayback] 暴露给渲染层的只读快照与操作。 */
private class UgoiraPlayback(
    val state: UgoiraState,
    val frameIndex: Int,
    val load: () -> Unit,
    val shouldAutoLoad: Boolean,
)

/**
 * Pixiv Ugoira 动图渲染组件：
 * 默认与普通插画图片保持一致的排版比例与铺满宽度显示，进入页面自动后台拉取帧并无缝切换为循环动态画面，
 * 点击时直接触发详情页统一的 [IllustFullScreenViewer] 全屏查看器。
 */
@Composable
fun UgoiraPlayer(
    illust: Illust,
    illustRepository: IllustRepository,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val playback = rememberUgoiraPlayback(
        illustId = illust.id,
        illustRepository = illustRepository,
        autoLoad = autoPlay,
        onError = {},
    )
    val state = playback.state

    LaunchedEffect(playback.shouldAutoLoad) {
        if (playback.shouldAutoLoad) playback.load()
    }

    val aspectRatio = if (illust.width > 0 && illust.height > 0) {
        illust.width.toFloat() / illust.height.toFloat()
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                when (state) {
                    is UgoiraState.Error, is UgoiraState.Idle -> playback.load()
                    else -> onClick?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val readyState = state as? UgoiraState.Ready
        val provider = readyState?.session?.provider
        // E-2：组合期仅读已解码缓存，未命中时后台解码后置状态，主线程不做磁盘读与解码
        val currentBitmap by produceState(
            initialValue = provider?.getCachedFrame(playback.frameIndex),
            key1 = provider,
            key2 = playback.frameIndex,
        ) {
            if (provider != null) {
                value = withContext(Dispatchers.Default) { provider.getFrameBitmap(playback.frameIndex) }
            }
        }

        val bitmap = currentBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = illust.title,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PixivAsyncImage(
                model = illust.imageUrls.large.ifEmpty { illust.imageUrls.medium },
                thumbnailUrl = illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium },
                contentDescription = illust.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth,
            )
        }

        // 后台加载动图帧时仅在右下角展示轻量级指示器，不遮挡画作主体
        if (state is UgoiraState.Loading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                if (LocalUiSkin.current == UiSkin.COLOROS) {
                    UgoiraMinimalSpinner(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                    )
                }
            }
        } else if (state is UgoiraState.Error) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { playback.load() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = strings.retry,
                    style = TextStyle(color = Color.White, fontSize = 12.sp),
                )
            }
        }
    }
}

/**
 * 供 [IllustFullScreenViewer] 调用的全屏可手势缩放动图渲染组件：
 * 复用 [UgoiraSessionCache] 已解码的动图帧，并与普通图片共用同一个全屏查看器顶栏与液态玻璃材质按钮。
 */
@Composable
internal fun ZoomableUgoiraViewer(
    illust: Illust,
    illustRepository: IllustRepository,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val playback = rememberUgoiraPlayback(
        illustId = illust.id,
        illustRepository = illustRepository,
        autoLoad = true,
        onError = {},
    )
    val state = playback.state

    LaunchedEffect(playback.shouldAutoLoad) {
        if (playback.shouldAutoLoad) playback.load()
    }

    val ugoiraContentSize = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            Size(illust.width.toFloat(), illust.height.toFloat())
        } else {
            Size.Zero
        }
    }
    val fullZoomState = rememberZoomState(
        maxScale = 8f,
        contentSize = ugoiraContentSize,
    )
    val viewerZoomedState = LocalFullScreenViewerZoomedState.current
    LaunchedEffect(fullZoomState.scale, viewerZoomedState) {
        viewerZoomedState?.value = fullZoomState.scale > 1.03f
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val readyState = state as? UgoiraState.Ready
        val provider = readyState?.session?.provider
        // E-2：同内嵌播放器，组合期只读缓存、后台解码置状态
        val fullBitmap by produceState(
            initialValue = provider?.getCachedFrame(playback.frameIndex),
            key1 = provider,
            key2 = playback.frameIndex,
        ) {
            if (provider != null) {
                value = withContext(Dispatchers.Default) { provider.getFrameBitmap(playback.frameIndex) }
            }
        }
        val full = fullBitmap
        if (full != null) {
            Image(
                bitmap = full,
                contentDescription = illust.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        zoomState = fullZoomState,
                        mouseWheelZoom = MouseWheelZoom.Enabled,
                        onTap = { onTap() },
                        onDoubleTap = { position ->
                            performHapticFeedback(HapticType.Tick)
                            fullZoomState.toggleScale(2.5f, position)
                        },
                    ),
            )
        } else {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                PixivAsyncImage(
                    model = illust.imageUrls.large.ifEmpty { illust.imageUrls.medium },
                    thumbnailUrl = illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium },
                    contentDescription = illust.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(
                            zoomState = fullZoomState,
                            mouseWheelZoom = MouseWheelZoom.Enabled,
                            onTap = { onTap() },
                            onDoubleTap = { position ->
                                performHapticFeedback(HapticType.Tick)
                                fullZoomState.toggleScale(2.5f, position)
                            },
                        ),
                )
            }
            if (state is UgoiraState.Loading) {
                BasicText(
                    text = state.stageText,
                    style = TextStyle(color = Color.White, fontSize = 13.sp),
                )
            } else if (state is UgoiraState.Error) {
                BasicText(
                    text = state.message,
                    style = TextStyle(color = Color.White, fontSize = 13.sp),
                )
            }
        }
    }
}

/** 皮肤无关的轻量圆弧加载指示器，供 COUI 路径使用以避免依赖 MIUIX 进度控件。 */
@Composable
private fun UgoiraMinimalSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    val transition = rememberInfiniteTransition(label = "ugoiraSpinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ugoiraSpinnerAngle",
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
