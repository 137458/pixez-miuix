package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.NovelWebResponse
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.core.CouiLoadingPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.core.withExtra
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry
import com.perol.pixez.shared.ui.navigation.animation.TransitionTarget
import com.perol.pixez.shared.ui.navigation.animation.TransitionTargetKind
import com.perol.pixez.shared.ui.navigation.animation.cardExpandDetailChromeAlpha
import com.perol.pixez.shared.ui.navigation.animation.commentsTransitionBounds
import com.perol.pixez.shared.ui.navigation.animation.illustTransitionBounds
import com.perol.pixez.shared.ui.screens.buildNovelViewerItems
import com.perol.pixez.shared.ui.screens.NovelViewerItem
import com.perol.pixez.shared.ui.theme.LocalAppExtraColors
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private sealed interface CouiNovelViewState {
    data object Loading : CouiNovelViewState
    data class Success(val data: NovelWebResponse) : CouiNovelViewState
    data class Error(val message: String) : CouiNovelViewState
}


/**
 * COUI 小说阅读器：库骨架 [CouiSecondaryScaffold] + 整页正文画布。
 *
 * 业务编排与 MIUIX 版一致：正文/内嵌图源（`/webview/v2/novel`）与详情并行加载、收藏（点击常规 / 长按强制私密）、
 * 字号调节持久化、正文复制/分享/浏览器打开、内嵌插画与上传图、系列前后章节跳转。
 * 表现层：骨架负责折叠大标题与返回，动作槽只保留评论/收藏/更多三个图标，统一走 [CouiTopBarIconButton] 凝光视效，
 * 字号微调、复制、分享与浏览器打开收敛在更多菜单；正文为可选中的整页长文列表，带本地阅读进度记忆。
 */
@Composable
internal fun CouiNovelViewerScreen(
    novelId: Long,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit = {},
    onIllustClick: (Long) -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    onCommentsClick: (Long) -> Unit = {},
) {
    val novelRepository = LocalNovelRepository.current
    val bookmarkRepository = LocalBookmarkRepository.current
    val novelHistoryRepository = LocalNovelHistoryRepository.current
    val strings = LocalStrings.current
    val settings = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()
    val toastSink = LocalToastSink.current

    var state by remember(novelId) { mutableStateOf<CouiNovelViewState>(CouiNovelViewState.Loading) }
    // 详情与正文并行加载：仅用于收藏初值与浏览历史，失败不阻塞正文。
    var novelDetail by remember(novelId) { mutableStateOf<Novel?>(null) }
    var isBookmarked by remember(novelId) { mutableStateOf(false) }

    // 阅读进度：位置只记在本地库，导航路由不带任何位置参数；换书即换一套列表状态。
    val listState = remember(novelId) { LazyListState() }
    // 恢复门闩：进度读取完成（命中或确认无记录）前禁止回写，
    // 否则正文首帧的 (0, 0) 会先落库，把上次位置覆盖成开头。
    var progressRestored by remember(novelId) { mutableStateOf(false) }
    val progressRestoredState = rememberUpdatedState(progressRestored)

    var fontSizeSp by remember(settings.novelFontSize) {
        mutableFloatStateOf(
            settings.novelFontSize.coerceIn(
                AppConstants.Novel.MIN_FONT_SIZE_SP,
                AppConstants.Novel.MAX_FONT_SIZE_SP,
            )
        )
    }

    fun loadNovel() {
        scope.launch {
            state = CouiNovelViewState.Loading
            suspendRunCatchingNonCancel { novelRepository.getNovelWebResource(novelId) }
                .onSuccess { data -> state = CouiNovelViewState.Success(data) }
                .onFailure { err ->
                    Napier.e("加载小说正文失败 novelId=$novelId", err, tag = "CouiNovelViewer")
                    state = CouiNovelViewState.Error(PixivApiErrorFormatter.format(err, strings))
                }
        }
        scope.launch {
            suspendRunCatchingNonCancel { novelRepository.getNovelDetail(novelId) }
                .onSuccess { detail ->
                    novelDetail = detail.novel
                    isBookmarked = detail.novel.isBookmarked
                    suspendRunCatchingNonCancel {
                        novelHistoryRepository.insert(detail.novel)
                    }.onFailure { err ->
                        Napier.e("小说浏览历史写入失败 novelId=$novelId", err, tag = "CouiNovelViewer")
                    }
                }
                .onFailure { err ->
                    Napier.e("加载小说详情失败 novelId=$novelId", err, tag = "CouiNovelViewer")
                }
        }
    }

    // 点击普通收藏（私密与否跟随 defaultPrivateLike），长按强制私密；已收藏时均为取消。
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
                    toastSink(ToastData(strings.bookmarked, ToastType.Success))
                }
            }.onFailure { err ->
                performHapticFeedback(HapticType.Reject)
                Napier.e("小说收藏操作失败 novelId=${target.id}", err, tag = "CouiNovelViewer")
                toastSink(ToastData(PixivApiErrorFormatter.format(err, strings), ToastType.Error))
            }
        }
    }

    LaunchedEffect(novelId) {
        loadNovel()
    }

    // 进度恢复：正文（列表数据）到位后读取本地进度并定位；无记录时直接放行回写。
    val loadedData = (state as? CouiNovelViewState.Success)?.data
    LaunchedEffect(novelId, loadedData) {
        if (loadedData == null) return@LaunchedEffect
        val progress = suspendRunCatchingNonCancel {
            novelHistoryRepository.getReadingProgress(novelId)
        }.getOrNull()
        if (progress != null && (progress.blockIndex > 0 || progress.blockOffset > 0)) {
            // 越界索引由 LazyListState 自行收敛到末尾，不会抛异常。
            listState.scrollToItem(progress.blockIndex, progress.blockOffset)
        }
        progressRestored = true
    }

    // 进度回写：恢复完成后监听首可视项，collectLatest 取消未到期的写入即为防抖。
    LaunchedEffect(novelId, listState, progressRestored) {
        if (!progressRestored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collectLatest { (index, offset) ->
                delay(AppConstants.Novel.READING_PROGRESS_SAVE_DEBOUNCE_MILLIS)
                suspendRunCatchingNonCancel {
                    novelHistoryRepository.saveReadingProgress(novelId, index, offset)
                }
            }
    }

    // 离场兜底：rememberCoroutineScope 随组件离场一起取消，这里用 IO 上的一次性作用域写最后一次位置。
    DisposableEffect(novelId, listState) {
        onDispose {
            if (progressRestoredState.value) {
                val index = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset
                CoroutineScope(Dispatchers.IO).launch {
                    suspendRunCatchingNonCancel {
                        novelHistoryRepository.saveReadingProgress(novelId, index, offset)
                    }
                }
            }
        }
    }

    val sharedBoundsRegistry = LocalSharedBoundsRegistry.current
    val transitionTarget = remember(novelId) {
        TransitionTarget(TransitionTargetKind.Novel, novelId)
    }
    DisposableEffect(sharedBoundsRegistry, transitionTarget) {
        sharedBoundsRegistry.updateDisplayedTarget(
            originTarget = transitionTarget,
            displayedTarget = transitionTarget,
        )
        onDispose {
            sharedBoundsRegistry.onDetailTargetDisposed(transitionTarget)
        }
    }
    val chromeAlphaModifier = Modifier.graphicsLayer {
        alpha = if (sharedBoundsRegistry.activeTransitionTarget == transitionTarget) {
            cardExpandDetailChromeAlpha(sharedBoundsRegistry.activeTransitionExpansion)
        } else {
            1f
        }
    }

    val novelText = loadedData?.text
    val webTitle = loadedData?.title

    val menuAnchor = rememberCouiMenuAnchor()
    var menuExpanded by remember { mutableStateOf(false) }

    CouiSecondaryScaffold(
        title = novelDetail?.title?.takeIf { it.isNotBlank() }
            ?: webTitle?.takeIf { it.isNotBlank() }
            ?: strings.novelReaderTitle,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = { onCommentsClick(novelId) },
                icon = CouiIcons.Messages,
                contentDescription = strings.commentsTitle,
                modifier = chromeAlphaModifier.commentsTransitionBounds(
                    targetId = novelId,
                    isNovel = true,
                    cornerRadius = 20.dp,
                ),
            )
            if (novelDetail != null) {
                CouiTopBarIconButton(
                    onClick = { toggleNovelBookmark(forcePrivate = false) },
                    onLongClick = { toggleNovelBookmark(forcePrivate = true) },
                    icon = if (isBookmarked) CouiIcons.HeartFill else CouiIcons.Heart,
                    contentDescription = strings.bookmark,
                    tint = if (isBookmarked) LocalAppExtraColors.current.bookmarkActive else null,
                    modifier = chromeAlphaModifier,
                )
            }
            Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                CouiTopBarIconButton(
                    onClick = { menuAnchor.open { menuExpanded = true } },
                    icon = CouiIcons.More,
                    contentDescription = strings.menuMoreActions,
                    modifier = chromeAlphaModifier,
                )
                Menu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    anchor = menuAnchor,
                    headerText = strings.menuMoreActions,
                    items = listOf(
                        CouiMenuItem(
                            value = "font_smaller",
                            text = strings.novelFontSmaller,
                            enabled = fontSizeSp > AppConstants.Novel.MIN_FONT_SIZE_SP,
                        ),
                        CouiMenuItem(
                            value = "font_larger",
                            text = strings.novelFontLarger,
                            enabled = fontSizeSp < AppConstants.Novel.MAX_FONT_SIZE_SP,
                        ),
                        CouiMenuItem(value = "copy_text", text = strings.copy, dividerBefore = true),
                        CouiMenuItem(value = "share", text = strings.share),
                        CouiMenuItem(value = "browser", text = strings.openInBrowser),
                    ),
                    onItemClick = { item ->
                        menuExpanded = false
                        when (item.value) {
                            "font_smaller" -> {
                                fontSizeSp = (fontSizeSp - 1f).coerceAtLeast(AppConstants.Novel.MIN_FONT_SIZE_SP)
                                settings.novelFontSize = fontSizeSp
                            }
                            "font_larger" -> {
                                fontSizeSp = (fontSizeSp + 1f).coerceAtMost(AppConstants.Novel.MAX_FONT_SIZE_SP)
                                settings.novelFontSize = fontSizeSp
                            }
                            "copy_text" -> if (!novelText.isNullOrBlank()) {
                                IllustClipboard().copy(novelText)
                                toastSink(ToastData(strings.copiedToClipboard, ToastType.Success))
                            }
                            "share" -> IllustShare().share(
                                AppConstants.Urls.pixivNovelUrl(novelId),
                                strings.novelReaderTitle,
                            )
                            "browser" -> openSafeUrl(
                                AppConstants.Urls.pixivNovelUrl(novelId),
                                strings,
                                onError = { message ->
                                    toastSink(ToastData(message, ToastType.Error))
                                },
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        val stateModifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (val st = state) {
            is CouiNovelViewState.Loading -> CouiLoadingPlaceholder(modifier = stateModifier)
            is CouiNovelViewState.Error -> CouiPlaceholder(
                message = st.message,
                modifier = stateModifier,
                actionText = strings.retry,
                onAction = { loadNovel() },
            )
            is CouiNovelViewState.Success -> CouiNovelBody(
                data = st.data,
                novelDetail = novelDetail,
                fontSizeSp = fontSizeSp,
                strings = strings,
                padding = padding,
                listState = listState,
                onNovelClick = onNovelClick,
                onIllustClick = onIllustClick,
                onUserClick = onUserClick,
                onOpenUrl = { url ->
                    openSafeUrl(url, strings, onError = { message ->
                        toastSink(ToastData(message, ToastType.Error))
                    })
                },
            )
        }
    }
}

/**
 * 正文区：整页阅读画布。
 *
 * 正文、章节与图片不再套卡片外框、圆角与外边距，直接以页面底色铺满可视区；
 * 左右只保留阅读边距，上下沿用骨架安全区（已含底部栏预留），列表尾部再留少量呼吸空间。
 * 详情头部固定为列表首项（无详情时为空项），使进度记录的条目下标在详情加载失败时也不偏移。
 */
@Composable
private fun CouiNovelBody(
    data: NovelWebResponse,
    novelDetail: Novel?,
    fontSizeSp: Float,
    strings: AppStrings,
    padding: PaddingValues,
    listState: LazyListState,
    onNovelClick: (Long) -> Unit,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val novelText = data.text
    val seriesPrev = data.seriesNavigation?.prevNovel
    val seriesNext = data.seriesNavigation?.nextNovel
    val viewerItems = remember(novelText) {
        if (novelText.isBlank()) emptyList() else buildNovelViewerItems(novelText)
    }
    val contentPadding = remember(padding) {
        padding.withExtra(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp)
    }
    val pageBackground = CouiTheme.colorScheme.card

    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .consumeWindowInsets(padding),
            contentPadding = contentPadding,
            overscrollEffect = rememberCouiOverscrollEffect(),
        ) {
            item {
                if (novelDetail != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    ) {
                        Text(
                            text = novelDetail.title,
                            kind = CouiTextKind.ItemTitle,
                            color = CouiTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onUserClick(novelDetail.user.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PixivAsyncImage(
                                model = novelDetail.user.profileImageUrls.medium,
                                contentDescription = novelDetail.user.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                            )
                            Column {
                                Text(
                                    text = novelDetail.user.name,
                                    kind = CouiTextKind.ItemTitle,
                                    color = CouiTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = novelDetail.user.account,
                                    kind = CouiTextKind.ItemSummary,
                                    color = CouiTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (viewerItems.isEmpty()) {
                item {
                    BasicText(
                        text = strings.novelTextEmpty,
                        style = TextStyle(
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.65f).sp,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(
                    count = viewerItems.size,
                    // 条目类型不唯一（多个 [newpage] 都是同一个 data object），只能用条目下标做 key。
                    key = { index -> index },
                    contentType = { index -> viewerItems[index]::class },
                ) { index ->
                    CouiNovelViewerItemContent(
                        item = viewerItems[index],
                        webResource = data,
                        fontSizeSp = fontSizeSp,
                        onIllustClick = onIllustClick,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }

            if (seriesPrev != null || seriesNext != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (seriesPrev != null) {
                            CouiSeriesNavButton(
                                text = strings.novelPrevChapter,
                                onClick = { onNovelClick(seriesPrev.id) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        if (seriesNext != null) {
                            CouiSeriesNavButton(
                                text = strings.novelNextChapter,
                                onClick = { onNovelClick(seriesNext.id) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** 系列章节跳转按钮：强调色胶囊。 */
@Composable
private fun CouiSeriesNavButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(CouiTheme.colorScheme.container)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.accentOnSurface,
            maxLines = 1,
        )
    }
}

/** 单条阅读器条目渲染：正文/章节/分页留白/超链接/内嵌图。 */
@Composable
private fun CouiNovelViewerItemContent(
    item: NovelViewerItem,
    webResource: NovelWebResponse?,
    fontSizeSp: Float,
    onIllustClick: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val bodyStyle = TextStyle(
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.65f).sp,
        color = CouiTheme.colorScheme.onSurface,
    )
    when (item) {
        is NovelViewerItem.TextChunk -> BasicText(
            text = item.text,
            style = bodyStyle,
            modifier = Modifier.fillMaxWidth(),
        )

        is NovelViewerItem.Chapter -> BasicText(
            text = item.title,
            style = TextStyle(
                fontSize = (fontSizeSp + AppConstants.Novel.CHAPTER_TITLE_FONT_SIZE_DELTA_SP).sp,
                lineHeight = ((fontSizeSp + AppConstants.Novel.CHAPTER_TITLE_FONT_SIZE_DELTA_SP) * 1.4f).sp,
                fontWeight = FontWeight.Bold,
                color = CouiTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )

        is NovelViewerItem.NewPageSpacer -> Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppConstants.Novel.NEW_PAGE_SPACING_DP.dp),
        )

        is NovelViewerItem.JumpUriLink -> BasicText(
            text = item.title,
            style = bodyStyle.copy(color = CouiTheme.colorScheme.accentOnSurface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenUrl(item.url) },
        )

        // [jump:页码] 当前阅读器无分页概念，按原始文本兜底展示（对齐旧版行为）。
        is NovelViewerItem.JumpRaw -> BasicText(
            text = item.raw,
            style = bodyStyle,
            modifier = Modifier.fillMaxWidth(),
        )

        is NovelViewerItem.EmbeddedIllust -> {
            // 映射键为 "作品ID-页码"；标记未带页码时兜底查 "ID-1"。
            val images = webResource?.illusts?.get(item.key)?.illust?.images
                ?: item.key.takeUnless { it.contains('-') }
                    ?.let { webResource?.illusts?.get("$it-1")?.illust?.images }
            val url = images?.let { it.medium ?: it.original ?: it.small }
            CouiNovelEmbeddedImage(
                url = url,
                placeholderText = item.key,
                contentDescription = item.key,
                illustId = item.illustId,
                onClick = { onIllustClick(item.illustId) },
            )
        }

        is NovelViewerItem.UploadedPicture -> {
            // 取值顺序对齐旧版：128x128 → 1200x1200 → original。
            val urls = webResource?.images?.get(item.imageId)?.urls
            val url = urls?.let { it.size128 ?: it.size1200 ?: it.original }
            CouiNovelEmbeddedImage(
                url = url,
                placeholderText = item.imageId,
                contentDescription = item.imageId,
                onClick = null,
            )
        }
    }
}

/** 正文内嵌图片：映射未命中时渲染带键名的占位框。 */
@Composable
private fun CouiNovelEmbeddedImage(
    url: String?,
    placeholderText: String,
    contentDescription: String,
    illustId: Long? = null,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(AppConstants.Novel.EMBEDDED_IMAGE_HEIGHT_DP.dp)
            .illustTransitionBounds(
                illustId = illustId?.takeIf { it > 0L },
                cornerRadius = 12.dp,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(CouiTheme.colorScheme.container)
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
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
