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
import com.perol.pixez.shared.ui.i18n.LocalStrings
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.decodeToImageBitmap
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.perol.pixez.shared.data.repository.DownloadRepository

private sealed interface UgoiraState {
    data object Idle : UgoiraState
    data class Loading(val stageText: String) : UgoiraState
    data class Ready(val frames: List<Pair<UgoiraFrame, ImageBitmap>>, val rawZipBytes: ByteArray, val zipUrl: String) : UgoiraState
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

                state = UgoiraState.Loading(strings.ugoiraExtracting)
                val frameMap = withContext(Dispatchers.Default) {
                    UgoiraZipExtractor().extractFrames(zipBytes)
                }

                val decodedFrames = withContext(Dispatchers.Default) {
                    metadataResponse.ugoiraMetadata.frames.mapNotNull { frame ->
                        val bytes = frameMap[frame.file] ?: return@mapNotNull null
                        runCatching {
                            val bitmap = bytes.decodeToImageBitmap()
                            frame to bitmap
                        }.getOrNull()
                    }
                }

                if (decodedFrames.isEmpty()) {
                    state = UgoiraState.Error(strings.ugoiraDecodeFailed)
                } else {
                    currentFrameIndex = 0
                    isPlaying = true
                    state = UgoiraState.Ready(decodedFrames, zipBytes, zipUrl)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Napier.e("加载动图失败 illustId=${illust.id}", e, tag = "UgoiraPlayer")
                state = UgoiraState.Error(e.message ?: strings.ugoiraLoadFailed)
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
        val frames = currentState.frames
        if (frames.isEmpty()) return@LaunchedEffect

        var nextFrameTargetTime = Clock.System.now().toEpochMilliseconds()
        while (isActive && isPlaying) {
            val currentPair = frames.getOrNull(currentFrameIndex) ?: frames.first()
            val expectedDelay = currentPair.first.delay.toLong().coerceAtLeast(10L)
            nextFrameTargetTime += expectedDelay
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
                val currentBitmap = st.frames.getOrNull(currentFrameIndex)?.second
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
                                text = "${currentFrameIndex + 1} / ${st.frames.size}",
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
                                        val path = if (downloadRepository != null) {
                                            downloadRepository.saveUgoiraZip(
                                                illust = illust,
                                                bytes = st.rawZipBytes,
                                                zipUrl = st.zipUrl,
                                            )
                                        } else {
                                            illustSaver.save(
                                                fileName = "${illust.id}_ugoira.zip",
                                                bytes = st.rawZipBytes,
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
