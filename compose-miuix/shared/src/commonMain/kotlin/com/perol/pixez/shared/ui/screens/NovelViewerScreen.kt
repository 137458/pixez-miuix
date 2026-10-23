package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.NovelTextResponse
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.theme.LocalAppExtraColors
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
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
    novelId: Long,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit = {},
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val novelRepository = LocalNovelRepository.current
    val bookmarkRepository = LocalBookmarkRepository.current
    val novelHistoryRepository = LocalNovelHistoryRepository.current
    val strings = LocalStrings.current
    val settings = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()
    var state by remember(novelId) { mutableStateOf<NovelViewState>(NovelViewState.Loading) }
    // 详情接口与正文并行加载：仅用于收藏状态初值与浏览历史落库，
    // 加载失败不阻塞正文阅读（此时隐藏收藏按钮、不落历史），错误仅记日志。
    var novelDetail by remember(novelId) { mutableStateOf<Novel?>(null) }
    // 收藏状态为 UI 本地可变状态，详情接口返回只做初值。
    var isBookmarked by remember(novelId) { mutableStateOf(false) }

    // 字号持久化与微调（基准 AppConstants.Novel.DEFAULT_FONT_SIZE_SP，范围 MIN_FONT_SIZE_SP ~ MAX_FONT_SIZE_SP）
    var fontSizeSp by remember(settings.novelFontSize) {
        mutableFloatStateOf(
            settings.novelFontSize.coerceIn(
                AppConstants.Novel.MIN_FONT_SIZE_SP,
                AppConstants.Novel.MAX_FONT_SIZE_SP,
            )
        )
    }

    var toastMessage by remember { mutableStateOf<ToastData?>(null) }

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
                state = NovelViewState.Error(PixivApiErrorFormatter.format(err, strings))
            }
        }
        // 详情并行加载：成功后同步收藏初值并写入浏览历史；失败仅记日志，不影响正文展示。
        scope.launch {
            val detailResult = suspendRunCatchingNonCancel {
                novelRepository.getNovelDetail(novelId)
            }
            detailResult.onSuccess { detail ->
                novelDetail = detail.novel
                isBookmarked = detail.novel.isBookmarked
                suspendRunCatchingNonCancel {
                    novelHistoryRepository.insert(detail.novel)
                }.onFailure { err ->
                    Napier.e("小说浏览历史写入失败 novelId=$novelId", err, tag = "NovelViewer")
                }
            }.onFailure { err ->
                Napier.e("加载小说详情失败 novelId=$novelId", err, tag = "NovelViewer")
            }
        }
    }

    // 收藏/取消收藏：点击普通收藏（私密与否跟随 defaultPrivateLike），
    // 长按强制私密收藏；已收藏时点击与长按均为取消收藏（对齐旧版 novel_bookmark_button）。
    fun toggleNovelBookmark(forcePrivate: Boolean) {
        val target = novelDetail ?: return
        scope.launch {
            val wasBookmarked = isBookmarked
            suspendRunCatchingNonCancel {
                if (wasBookmarked) {
                    bookmarkRepository.deleteNovelBookmark(target.id)
                } else {
                    bookmarkRepository.addNovelBookmark(
                        novelId = target.id,
                        isPrivate = if (forcePrivate) true else settings.defaultPrivateLike,
                    )
                }
            }.onSuccess {
                performHapticFeedback(HapticType.Confirm)
                isBookmarked = !wasBookmarked
                if (!wasBookmarked) {
                    toastMessage = ToastData(strings.bookmarked, ToastType.Success)
                }
            }.onFailure { err ->
                performHapticFeedback(HapticType.Reject)
                Napier.e("小说收藏操作失败 novelId=${target.id}", err, tag = "NovelViewer")
                toastMessage = ToastData(PixivApiErrorFormatter.format(err, strings), ToastType.Error)
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
                        // 收藏：详情加载成功后显示；点击普通收藏，长按强制私密收藏。
                        if (novelDetail != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .combinedClickable(
                                        onClick = { toggleNovelBookmark(forcePrivate = false) },
                                        onLongClick = { toggleNovelBookmark(forcePrivate = true) },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                                    contentDescription = strings.bookmark,
                                    tint = if (isBookmarked) {
                                        LocalAppExtraColors.current.bookmarkActive
                                    } else {
                                        MiuixTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                        // 缩小字号
                        IconButton(
                            onClick = {
                                if (fontSizeSp > AppConstants.Novel.MIN_FONT_SIZE_SP) {
                                    fontSizeSp -= 1f
                                    settings.novelFontSize = fontSizeSp
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
                                    settings.novelFontSize = fontSizeSp
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
                        // 复制正文
                        IconButton(
                            onClick = {
                                val currentText = (state as? NovelViewState.Success)?.data?.novelText
                                if (!currentText.isNullOrBlank()) {
                                    com.perol.pixez.shared.platform.IllustClipboard().copy(currentText)
                                    toastMessage = ToastData(strings.copiedToClipboard, ToastType.Success)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Copy,
                                contentDescription = strings.copy,
                            )
                        }
                        // 分享
                        IconButton(
                            onClick = {
                                val novelUrl = AppConstants.Urls.pixivNovelUrl(novelId)
                                com.perol.pixez.shared.platform.IllustShare().share(novelUrl, strings.novelReaderTitle)
                            },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Share,
                                contentDescription = strings.share,
                            )
                        }
                        // 在浏览器中打开
                        IconButton(
                            onClick = {
                                openSafeUrl(AppConstants.Urls.pixivNovelUrl(novelId), strings, onError = { message ->
                                    toastMessage = ToastData(message, ToastType.Error)
                                })
                            },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Link,
                                contentDescription = strings.openInBrowser,
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

                    val paragraphChunks = remember(novelText) {
                        if (novelText.isBlank()) emptyList()
                        else {
                            novelText.split("\n").chunked(10) { it.joinToString("\n") }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item { Spacer(modifier = Modifier.height(16.dp)) }

                                    if (paragraphChunks.isEmpty()) {
                                        item {
                                            Text(
                                                text = strings.novelTextEmpty,
                                                fontSize = 14.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                modifier = Modifier.padding(horizontal = 18.dp),
                                            )
                                        }
                                    } else {
                                        items(
                                            count = paragraphChunks.size,
                                            key = { it },
                                            contentType = { "novel_chunk" },
                                        ) { index ->
                                            val chunk = paragraphChunks[index]
                                            Text(
                                                text = chunk,
                                                fontSize = fontSizeSp.sp,
                                                lineHeight = (fontSizeSp * 1.65f).sp,
                                                color = MiuixTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 18.dp),
                                            )
                                        }
                                    }

                                    // 系列上下章节导航
                                    if (seriesPrev?.id != null || seriesNext?.id != null) {
                                        item {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 18.dp),
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
            ToastMessage(
                toast = toastMessage,
                onDismiss = { toastMessage = null },
            )
        }
    }
}

