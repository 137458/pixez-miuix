package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UgoiraFrame
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.platform.IllustSaver
import com.perol.pixez.shared.platform.UgoiraZipExtractor
import com.perol.pixez.shared.platform.getAppCacheDirectory
import com.perol.pixez.shared.ui.i18n.LocalStrings
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.AppConstants
import net.engawapg.lib.zoomable.MouseWheelZoom
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.toggleScale
import net.engawapg.lib.zoomable.zoomable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

import com.perol.pixez.shared.data.repository.DownloadRepository

/**
 * Ugoira 动图轻量级双缓冲滑动窗口帧解码器。
 *
 * 避免一次性将 100+ 帧全部解码为 ImageBitmap 驻留 JVM 堆引发 OOM；
 * 仅在内存中维护当前播放窗口附近的 ImageBitmap，并在后台预解码下一帧。
 */
private class UgoiraFrameProvider(
    val frames: List<UgoiraFrame>,
    private val framesDir: Path,
) {
    private val cache = mutableMapOf<Int, ImageBitmap>()

    fun getFrameBitmap(index: Int): ImageBitmap? {
        val cached = cache[index]
        if (cached != null) return cached
        val frame = frames.getOrNull(index) ?: return null
        val framePath = framesDir / frame.file
        val bytes = runCatching {
            if (FileSystem.SYSTEM.exists(framePath)) {
                FileSystem.SYSTEM.read(framePath) { readByteArray() }
            } else null
        }.getOrNull() ?: return null
        val bitmap = runCatching { bytes.decodeToImageBitmap() }.getOrNull() ?: return null
        cache[index] = bitmap
        // 维持最多 8 帧已解码位图窗口，及时回收远离当前播放点的位图
        if (cache.size > 8) {
            val keysToRemove = cache.keys.filter { key ->
                val diff = kotlin.math.abs(key - index)
                val cyclicDiff = frames.size - diff
                minOf(diff, cyclicDiff) > 3
            }
            keysToRemove.forEach { cache.remove(it) }
        }
        return bitmap
    }

    fun preloadNext(index: Int) {
        val nextIdx = (index + 1) % frames.size
        if (!cache.containsKey(nextIdx)) {
            val nextFrame = frames.getOrNull(nextIdx) ?: return
            val framePath = framesDir / nextFrame.file
            val bytes = runCatching {
                if (FileSystem.SYSTEM.exists(framePath)) {
                    FileSystem.SYSTEM.read(framePath) { readByteArray() }
                } else null
            }.getOrNull() ?: return
            runCatching {
                val bitmap = bytes.decodeToImageBitmap()
                cache[nextIdx] = bitmap
            }
        }
    }
}

private sealed interface UgoiraState {
    data object Idle : UgoiraState
    data class Loading(val stageText: String) : UgoiraState
    data class Ready(
        val provider: UgoiraFrameProvider,
        val tempZipPath: Path?,
        val framesDir: Path?,
        val zipUrl: String,
    ) : UgoiraState
    data class Error(val message: String) : UgoiraState
}


/**
 * Pixiv Ugoira 动图渲染组件：
 * 默认与普通插画图片保持一致的排版比例与铺满宽度显示，进入页面自动后台拉取帧并无缝切换为循环动态画面，
 * 点击时支持全屏手势缩放预览，不展示冗余播放器控制条与重复保存按钮（保存统一由详情页顶栏下载按钮处理）。
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
    val scope = rememberCoroutineScope()
    var state by remember(illust.id) { mutableStateOf<UgoiraState>(UgoiraState.Idle) }
    var currentFrameIndex by remember(illust.id) { mutableIntStateOf(0) }
    var isFullScreen by remember(illust.id) { mutableStateOf(false) }

    fun loadUgoira() {
        scope.launch {
            state = UgoiraState.Loading(strings.ugoiraLoadingMetadata)
            try {
                val metadataResponse = illustRepository.getUgoiraMetadata(illust.id)
                val zipUrl = metadataResponse.ugoiraMetadata.zipUrls.medium

                state = UgoiraState.Loading(strings.ugoiraDownloading)
                val zipBytes = illustRepository.downloadUgoiraZip(zipUrl)

                val tempZipPath = withContext(Dispatchers.IO) {
                    val cacheDir = getAppCacheDirectory()
                    val path = cacheDir / "ugoira_temp_${illust.id}.zip"
                    runCatching {
                        FileSystem.SYSTEM.write(path) {
                            write(zipBytes)
                        }
                        path
                    }.getOrNull()
                }

                state = UgoiraState.Loading(strings.ugoiraExtracting)
                val (framesDir, validFrames) = withContext(Dispatchers.IO) {
                    val cacheDir = getAppCacheDirectory()
                    val dir = cacheDir / "ugoira_frames_${illust.id}"
                    FileSystem.SYSTEM.createDirectories(dir)
                    val frameMap = UgoiraZipExtractor().extractFrames(zipBytes)
                    for ((fileName, bytes) in frameMap) {
                        FileSystem.SYSTEM.write(dir / fileName) {
                            write(bytes)
                        }
                    }
                    val valid = metadataResponse.ugoiraMetadata.frames.filter { frameMap.containsKey(it.file) }
                    dir to valid
                }

                if (validFrames.isEmpty()) {
                    state = UgoiraState.Error(strings.ugoiraDecodeFailed)
                } else {
                    val provider = UgoiraFrameProvider(validFrames, framesDir)
                    withContext(Dispatchers.Default) {
                        provider.getFrameBitmap(0)
                        provider.preloadNext(0)
                    }
                    currentFrameIndex = 0
                    state = UgoiraState.Ready(provider, tempZipPath, framesDir, zipUrl)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Napier.e("加载动图失败 illustId=${illust.id}", e, tag = "UgoiraPlayer")
                state = UgoiraState.Error(e.message ?: strings.ugoiraLoadFailed)
            }
        }
    }

    DisposableEffect(illust.id) {
        onDispose {
            val ready = state as? UgoiraState.Ready
            val tempPath = ready?.tempZipPath
            if (tempPath != null) {
                runCatching { FileSystem.SYSTEM.delete(tempPath) }
            }
            val framesDir = ready?.framesDir
            if (framesDir != null) {
                runCatching { FileSystem.SYSTEM.deleteRecursively(framesDir) }
            }
        }
    }

    LaunchedEffect(illust.id, autoPlay) {
        if (autoPlay && state is UgoiraState.Idle) {
            loadUgoira()
        }
    }

    // 动图逐帧循环驱动协程
    val currentState = state
    LaunchedEffect(currentState) {
        if (currentState !is UgoiraState.Ready) return@LaunchedEffect
        val frames = currentState.provider.frames
        if (frames.isEmpty()) return@LaunchedEffect

        var nextFrameTargetTime = Clock.System.now().toEpochMilliseconds()
        while (isActive) {
            val currentFrame = frames.getOrNull(currentFrameIndex) ?: frames.first()
            val expectedDelay = currentFrame.delay.toLong().coerceAtLeast(10L)
            nextFrameTargetTime += expectedDelay

            currentState.provider.preloadNext(currentFrameIndex)
            currentFrameIndex = (currentFrameIndex + 1) % frames.size

            val now = Clock.System.now().toEpochMilliseconds()
            val waitTime = nextFrameTargetTime - now
            if (waitTime > 0L) {
                delay(waitTime)
            } else if (now - nextFrameTargetTime > expectedDelay * 2) {
                nextFrameTargetTime = now
            }
        }
    }

    val aspectRatio = if (illust.width > 0 && illust.height > 0) {
        illust.width.toFloat() / illust.height.toFloat()
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                when (state) {
                    is UgoiraState.Error -> loadUgoira()
                    is UgoiraState.Idle -> loadUgoira()
                    else -> {
                        if (onClick != null) {
                            onClick()
                        } else {
                            isFullScreen = true
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val readyState = state as? UgoiraState.Ready
        val currentBitmap = readyState?.provider?.getFrameBitmap(currentFrameIndex)

        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
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
                InfiniteProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                )
            }
        } else if (state is UgoiraState.Error) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { loadUgoira() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.retry,
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
        }
    }

    if (isFullScreen) {
        Popup(
            onDismissRequest = { isFullScreen = false },
            properties = PopupProperties(focusable = true),
        ) {
            PlatformBackHandler(onBack = { isFullScreen = false })

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
            var showFullControls by remember { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                val ready = state as? UgoiraState.Ready
                val fullBitmap = ready?.provider?.getFrameBitmap(currentFrameIndex)
                if (fullBitmap != null) {
                    Image(
                        bitmap = fullBitmap,
                        contentDescription = illust.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(
                                zoomState = fullZoomState,
                                mouseWheelZoom = MouseWheelZoom.Enabled,
                                onTap = { showFullControls = !showFullControls },
                                onDoubleTap = { position ->
                                    performHapticFeedback(HapticType.Tick)
                                    fullZoomState.toggleScale(2.5f, position)
                                },
                            ),
                    )
                } else {
                    PixivAsyncImage(
                        model = illust.imageUrls.large.ifEmpty { illust.imageUrls.medium },
                        contentDescription = illust.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(
                                zoomState = fullZoomState,
                                mouseWheelZoom = MouseWheelZoom.Enabled,
                                onTap = { showFullControls = !showFullControls },
                                onDoubleTap = { position ->
                                    performHapticFeedback(HapticType.Tick)
                                    fullZoomState.toggleScale(2.5f, position)
                                },
                            ),
                    )
                }

                // 顶部返回按钮
                AnimatedVisibility(
                    visible = showFullControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { isFullScreen = false }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.ugoiraExitFullScreen,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}
