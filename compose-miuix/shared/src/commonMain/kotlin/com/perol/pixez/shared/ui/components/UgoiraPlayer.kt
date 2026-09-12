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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
 * Pixiv Ugoira 动图多端播放器与解压渲染组件。
 *
 * 严格遵循 MIUIX 视觉范式，集成跨平台 Zip 解压与 Compose 实时逐帧渲染。
 */
@Composable
fun UgoiraPlayer(
    illust: Illust,
    illustRepository: IllustRepository,
    modifier: Modifier = Modifier,
    downloadRepository: DownloadRepository? = null,
    illustSaver: IllustSaver = remember { IllustSaver() },
    autoPlay: Boolean = false,
    onSavedZip: ((String) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var state by remember(illust.id) { mutableStateOf<UgoiraState>(UgoiraState.Idle) }
    var isPlaying by remember(illust.id) { mutableStateOf(true) }
    var currentFrameIndex by remember(illust.id) { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isSavingZip by remember { mutableStateOf(false) }

    fun loadUgoira() {
        scope.launch {
            state = UgoiraState.Loading(strings.ugoiraLoadingMetadata)
            try {
                val metadataResponse = illustRepository.getUgoiraMetadata(illust.id)
                val zipUrl = metadataResponse.ugoiraMetadata.zipUrls.medium

                state = UgoiraState.Loading(strings.ugoiraDownloading)
                val zipBytes = illustRepository.downloadUgoiraZip(zipUrl)

                // 将 Zip 流式持久化至应用缓存目录，避免在 JVM 堆内存中长期持有数十兆未压缩原始字节
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
                    // 预解码首帧与后续帧
                    withContext(Dispatchers.Default) {
                        provider.getFrameBitmap(0)
                        provider.preloadNext(0)
                    }
                    currentFrameIndex = 0
                    isPlaying = true
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

    // 动图逐帧动画驱动协程
    val currentState = state
    LaunchedEffect(currentState, isPlaying) {
        if (currentState !is UgoiraState.Ready || !isPlaying) return@LaunchedEffect
        val frames = currentState.provider.frames
        if (frames.isEmpty()) return@LaunchedEffect

        var nextFrameTargetTime = Clock.System.now().toEpochMilliseconds()
        while (isActive && isPlaying) {
            val currentFrame = frames.getOrNull(currentFrameIndex) ?: frames.first()
            val expectedDelay = currentFrame.delay.toLong().coerceAtLeast(10L)
            nextFrameTargetTime += expectedDelay

            // 预解码下一帧，平滑帧率
            currentState.provider.preloadNext(currentFrameIndex)

            currentFrameIndex = (currentFrameIndex + 1) % frames.size

            val now = Clock.System.now().toEpochMilliseconds()
            val waitTime = nextFrameTargetTime - now
            if (waitTime > 0L) {
                delay(waitTime)
            } else if (now - nextFrameTargetTime > expectedDelay * 2) {
                // System stutter or window sleep, resync target time
                nextFrameTargetTime = now
            }
        }
    }

    val aspectRatio = if (illust.height > 0) illust.width.toFloat() / illust.height.toFloat() else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.5f, 2.5f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (state is UgoiraState.Ready) {
                    showControls = !showControls
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state !is UgoiraState.Ready) {
            PixivAsyncImage(
                model = illust.imageUrls.large,
                contentDescription = illust.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        when (val st = state) {
            is UgoiraState.Ready -> {
                val currentBitmap = st.provider.getFrameBitmap(currentFrameIndex)
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap,
                        contentDescription = illust.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 悬浮播放控制面板
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { isPlaying = !isPlaying },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isPlaying) "❚❚" else "▶",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${currentFrameIndex + 1} / ${st.provider.frames.size}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                            )
                        }

                        // 保存 Zip 按钮
                        Button(
                            onClick = {
                                if (isSavingZip) return@Button
                                isSavingZip = true
                                scope.launch {
                                    try {
                                        val bytes = withContext(Dispatchers.IO) {
                                            val path = st.tempZipPath
                                            if (path != null && FileSystem.SYSTEM.exists(path)) {
                                                FileSystem.SYSTEM.read(path) { readByteArray() }
                                            } else {
                                                illustRepository.downloadUgoiraZip(st.zipUrl)
                                            }
                                        }
                                        val path = if (downloadRepository != null) {
                                            downloadRepository.saveUgoiraZip(
                                                illust = illust,
                                                bytes = bytes,
                                                zipUrl = st.zipUrl,
                                            )
                                        } else {
                                            illustSaver.save(
                                                fileName = "${illust.id}_ugoira.zip",
                                                bytes = bytes,
                                            )
                                        }
                                        onSavedZip?.invoke(path)
                                    } catch (e: Throwable) {
                                        Napier.e("保存动图 Zip 失败", e, tag = "UgoiraPlayer")
                                    } finally {
                                        isSavingZip = false
                                    }
                                }
                            },
                            modifier = Modifier.height(30.dp),
                        ) {
                            Text(
                                text = if (isSavingZip) "..." else strings.ugoiraSaveZip,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
            is UgoiraState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            InfiniteProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = st.stageText,
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            is UgoiraState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = st.message,
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { loadUgoira() }) {
                                Text(strings.retry, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            is UgoiraState.Idle -> {
                // 播放引导悬浮按钮
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { loadUgoira() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 22.sp,
                    )
                }
            }
        }
    }
}
