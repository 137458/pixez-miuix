package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.NovelTextResponse
import com.perol.pixez.shared.data.model.NovelWebResponse
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.theme.LocalAppExtraColors
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.NovelContentParser
import com.perol.pixez.shared.utils.NovelSpan
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
 * 阅读器渲染条目：标记解析结果按展示形态展开，
 * Text 段沿用按行 chunked(10) 切分以保持 LazyColumn 长文渲染性能。
 */
private sealed interface NovelViewerItem {
    data class TextChunk(val text: String) : NovelViewerItem
    data class Chapter(val title: String) : NovelViewerItem
    data object NewPageSpacer : NovelViewerItem
    data class JumpUriLink(val title: String, val url: String) : NovelViewerItem
    data class EmbeddedIllust(val illustId: Long, val key: String) : NovelViewerItem
    data class UploadedPicture(val imageId: String) : NovelViewerItem
    data class JumpRaw(val raw: String) : NovelViewerItem
}

private fun buildNovelViewerItems(novelText: String): List<NovelViewerItem> = buildList {
    for (span in NovelContentParser.parse(novelText)) {
        when (span) {
            is NovelSpan.Text ->
                span.text
                    .split("\n")
                    .chunked(10) { it.joinToString("\n") }
                    .forEach { chunk -> add(NovelViewerItem.TextChunk(chunk)) }
            is NovelSpan.Chapter -> add(NovelViewerItem.Chapter(span.title))
            NovelSpan.NewPage -> add(NovelViewerItem.NewPageSpacer)
            is NovelSpan.JumpUri -> add(NovelViewerItem.JumpUriLink(span.title, span.url))
            is NovelSpan.PixivImage -> add(NovelViewerItem.EmbeddedIllust(span.illustId, span.key))
            is NovelSpan.UploadedImage -> add(NovelViewerItem.UploadedPicture(span.imageId))
            is NovelSpan.Jump -> add(NovelViewerItem.JumpRaw(span.raw))
        }
    }
}

/**
 * 小说阅读器沉浸式界面。
 *
 * 遵循 Xiaomi HyperOS / MIUIX 视觉范式，支持正文富文本标记渲染（章节标题、分页符、
 * 内嵌插画、用户上传图、超链接）、动态字号缩放调节与系列章节前后跳转。
 */
@Composable
fun NovelViewerScreen(
    novelId: Long,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit = {},
    onIllustClick: (Long) -> Unit = {},
    onCommentsClick: (Long) -> Unit = {},
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
    // webview 内嵌图源映射并行加载：失败仅记日志并置 null，
    // 富文本渲染降级为无图纯文本（chapter/newpage/jumpuri 不依赖映射，仍然生效）。
    var webResource by remember(novelId) { mutableStateOf<NovelWebResponse?>(null) }

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
        // 内嵌图源映射并行加载：仅服务于 pixivimage/uploadedimage 渲染，失败降级为占位框。
        scope.launch {
            suspendRunCatchingNonCancel {
                novelRepository.getNovelWebResource(novelId)
            }.onSuccess { resource ->
                webResource = resource
            }.onFailure { err ->
                Napier.w("加载小说内嵌资源失败，降级为无图纯文本 novelId=$novelId", err, tag = "NovelViewer")
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
                        // 评论：跳转小说评论页。
                        IconButton(onClick = { onCommentsClick(novelId) }) {
                            Icon(
                                imageVector = MiuixIcons.Messages,
                                contentDescription = strings.commentsTitle,
                            )
                        }
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

                    // 标记解析 → 展示条目：Text 段沿用 chunked(10) 切分保持长文渲染性能。
                    val viewerItems = remember(novelText) {
                        if (novelText.isBlank()) emptyList() else buildNovelViewerItems(novelText)
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

                                    if (viewerItems.isEmpty()) {
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
                                            count = viewerItems.size,
                                            key = { it },
                                            contentType = { index -> viewerItems[index]::class },
                                        ) { index ->
                                            val item = viewerItems[index]
                                            NovelViewerItemContent(
                                                item = item,
                                                webResource = webResource,
                                                fontSizeSp = fontSizeSp,
                                                onIllustClick = onIllustClick,
                                                onOpenUrl = { url ->
                                                    openSafeUrl(url, strings, onError = { message ->
                                                        toastMessage = ToastData(message, ToastType.Error)
                                                    })
                                                },
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

/**
 * 单条阅读器条目渲染：按条目类型选择正文 / 标题 / 分页留白 / 超链接 / 内嵌图样式。
 */
@Composable
private fun NovelViewerItemContent(
    item: NovelViewerItem,
    webResource: NovelWebResponse?,
    fontSizeSp: Float,
    onIllustClick: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    when (item) {
        is NovelViewerItem.TextChunk -> Text(
            text = item.text,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.65f).sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        )

        is NovelViewerItem.Chapter -> Text(
            text = item.title,
            fontSize = (fontSizeSp + AppConstants.Novel.CHAPTER_TITLE_FONT_SIZE_DELTA_SP).sp,
            lineHeight = ((fontSizeSp + AppConstants.Novel.CHAPTER_TITLE_FONT_SIZE_DELTA_SP) * 1.4f).sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )

        is NovelViewerItem.NewPageSpacer -> Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppConstants.Novel.NEW_PAGE_SPACING_DP.dp),
        )

        is NovelViewerItem.JumpUriLink -> Text(
            text = item.title,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.65f).sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clickable { onOpenUrl(item.url) },
        )

        // [jump:页码] 当前阅读器无分页概念，按原始文本兜底展示（对齐旧版行为）。
        is NovelViewerItem.JumpRaw -> Text(
            text = item.raw,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.65f).sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        )

        is NovelViewerItem.EmbeddedIllust -> {
            // 映射键为 "作品ID-页码"；标记未带页码（如 "[pixivimage:12345]"）时兜底查 "12345-1"。
            val images = webResource?.illusts?.get(item.key)?.illust?.images
                ?: item.key.takeUnless { it.contains('-') }
                    ?.let { webResource?.illusts?.get("$it-1")?.illust?.images }
            val url = images?.let { it.medium ?: it.original ?: it.small }
            NovelEmbeddedImage(
                url = url,
                placeholderText = item.key,
                contentDescription = item.key,
                onClick = { onIllustClick(item.illustId) },
            )
        }

        is NovelViewerItem.UploadedPicture -> {
            // 取值顺序对齐旧版：128x128 → 1200x1200 → original。
            val urls = webResource?.images?.get(item.imageId)?.urls
            val url = urls?.let { it.size128 ?: it.size1200 ?: it.original }
            NovelEmbeddedImage(
                url = url,
                placeholderText = item.imageId,
                contentDescription = item.imageId,
                onClick = null,
            )
        }
    }
}

/**
 * 正文内嵌图片：映射未命中时渲染带键名的占位框，命中时经 PixivAsyncImage 渲染（自动补 Referer）。
 */
@Composable
private fun NovelEmbeddedImage(
    url: String?,
    placeholderText: String,
    contentDescription: String,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .height(AppConstants.Novel.EMBEDDED_IMAGE_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            PixivAsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = placeholderText,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

