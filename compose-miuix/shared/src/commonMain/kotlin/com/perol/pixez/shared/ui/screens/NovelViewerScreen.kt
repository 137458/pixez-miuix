package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.NovelTextResponse
import com.perol.pixez.shared.data.repository.NovelRepository
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private sealed interface NovelViewState {
    data object Loading : NovelViewState
    data class Success(val data: NovelTextResponse) : NovelViewState
    data class Error(val message: String) : NovelViewState
}

/**
 * 小说阅读器沉浸式界面。
 *
 * 遵循 Xiaomi HyperOS / MIUIX 视觉范式，支持正文阅读、动态字号缩放调节与系列章节前后跳转。
 */
@Composable
fun NovelViewerScreen(
    novelId: Int,
    novelRepository: NovelRepository,
    onBack: () -> Unit,
    onNovelClick: (Int) -> Unit = {},
) {
    val strings = LocalStrings.current
    val settings = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()
    var state by remember(novelId) { mutableStateOf<NovelViewState>(NovelViewState.Loading) }

    // 字号持久化与微调（基准 AppConstants.Novel.DEFAULT_FONT_SIZE_SP，范围 MIN_FONT_SIZE_SP ~ MAX_FONT_SIZE_SP）
    var fontSizeSp by remember(settings?.novelFontSize) {
        mutableFloatStateOf(
            settings?.novelFontSize?.coerceIn(
                AppConstants.Novel.MIN_FONT_SIZE_SP,
                AppConstants.Novel.MAX_FONT_SIZE_SP,
            ) ?: AppConstants.Novel.DEFAULT_FONT_SIZE_SP
        )
    }

    fun loadNovel() {
        scope.launch {
            state = NovelViewState.Loading
            val result = suspendRunCatchingNonCancel {
                novelRepository.getNovelText(novelId)
            }
            result.onSuccess { data ->
                state = NovelViewState.Success(data)
            }.onFailure { err ->
                Napier.e("加载小说正文失败 novelId=$novelId", err, tag = "NovelViewer")
                state = NovelViewState.Error(err.message ?: strings.loadFailed)
            }
        }
    }

    LaunchedEffect(novelId) {
        loadNovel()
    }

    val backdrop = rememberBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = strings.novelReaderTitle,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                    actions = {
                        // 缩小字号
                        IconButton(
                            onClick = {
                                if (fontSizeSp > AppConstants.Novel.MIN_FONT_SIZE_SP) {
                                    fontSizeSp -= 1f
                                    settings?.novelFontSize = fontSizeSp
                                }
                            },
                        ) {
                            Text(
                                text = "A-",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                            )
                        }
                        // 放大字号
                        IconButton(
                            onClick = {
                                if (fontSizeSp < AppConstants.Novel.MAX_FONT_SIZE_SP) {
                                    fontSizeSp += 1f
                                    settings?.novelFontSize = fontSizeSp
                                }
                            },
                        ) {
                            Text(
                                text = "A+",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .blurBackdropSource(backdrop),
        ) {
            when (val st = state) {
                is NovelViewState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        InfiniteProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                }
                is NovelViewState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(modifier = Modifier.padding(24.dp)) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = st.message,
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { loadNovel() }) {
                                    Text(strings.retry)
                                }
                            }
                        }
                    }
                }
                is NovelViewState.Success -> {
                    val novelText = st.data.novelText
                    val seriesPrev = st.data.seriesPrev
                    val seriesNext = st.data.seriesNext

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(12.dp)) }

                        // 小说正文卡片
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 20.dp),
                                ) {
                                    if (novelText.isBlank()) {
                                        Text(
                                            text = strings.novelTextEmpty,
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        )
                                    } else {
                                        Text(
                                            text = novelText,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp * 1.65f).sp,
                                            color = MiuixTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        // 系列上下章节导航
                        if (seriesPrev?.id != null || seriesNext?.id != null) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (seriesPrev?.id != null) {
                                        Button(
                                            onClick = { onNovelClick(seriesPrev.id) },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                        ) {
                                            Text(
                                                text = "◀ ${strings.novelPrevChapter}",
                                                fontSize = 13.sp,
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    if (seriesNext?.id != null) {
                                        Button(
                                            onClick = { onNovelClick(seriesNext.id) },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                        ) {
                                            Text(
                                                text = "${strings.novelNextChapter} ▶",
                                                fontSize = 13.sp,
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}
