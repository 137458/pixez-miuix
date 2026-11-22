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
import androidx.compose.runtime.setValue
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
import com.perol.pixez.shared.ui.screens.buildNovelViewerItems
import com.perol.pixez.shared.ui.screens.NovelViewerItem
import com.perol.pixez.shared.ui.theme.LocalAppExtraColors
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

private sealed interface CouiNovelViewState {
    data object Loading : CouiNovelViewState
    data class Success(val data: NovelWebResponse) : CouiNovelViewState
    data class Error(val message: String) : CouiNovelViewState
}

/**
 * COUI 小说阅读器：库骨架 [CouiSecondaryScaffold] + COUI 卡片正文。
 *
 * 业务编排与 MIUIX 版一致：正文/内嵌图源（`/webview/v2/novel`）与详情并行加载、收藏（点击常规 / 长按强制私密）、
 * 字号调节持久化、正文复制/分享/浏览器打开、内嵌插画与上传图、系列前后章节跳转。
 * 表现层：骨架负责折叠大标题与返回，动作（评论/收藏/更多）进 actions 槽，统一走 [CouiTopBarIconButton] 凝光视效，
 * 字号微调、复制、分享与浏览器打开收敛在更多菜单；正文为可选中的 COUI 卡片列表。
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

    val novelText = (state as? CouiNovelViewState.Success)?.data?.text
    val webTitle = (state as? CouiNovelViewState.Success)?.data?.title

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
                modifier = chromeAlphaModifier,
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
                            text = "${strings.novelFontSize} -",
                            enabled = fontSizeSp > AppConstants.Novel.MIN_FONT_SIZE_SP,
                        ),
                        CouiMenuItem(
                            value = "font_larger",
                            text = "${strings.novelFontSize} +",
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

/** 正文区：COUI 卡片承载可选中的长文列表与系列章节导航。 */
@Composable
private fun CouiNovelBody(
    data: NovelWebResponse,
    novelDetail: Novel?,
    fontSizeSp: Float,
    strings: AppStrings,
    padding: PaddingValues,
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
        padding.withExtra(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp)
    }
    val cardBackground = CouiTheme.colorScheme.card

    SelectionContainer {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            contentPadding = contentPadding,
            overscrollEffect = rememberCouiOverscrollEffect(),
        ) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(cardBackground),
                )
            }

            novelDetail?.let { detail ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBackground)
                            .padding(horizontal = 18.dp),
                    ) {
                        Text(
                            text = detail.title,
                            kind = CouiTextKind.ItemTitle,
                            color = CouiTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onUserClick(detail.user.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PixivAsyncImage(
                                model = detail.user.profileImageUrls.medium,
                                contentDescription = detail.user.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                            )
                            Column {
                                Text(
                                    text = detail.user.name,
                                    kind = CouiTextKind.ItemTitle,
                                    color = CouiTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = detail.user.account,
                                    kind = CouiTextKind.ItemSummary,
                                    color = CouiTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (viewerItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBackground)
                            .padding(horizontal = 18.dp),
                    ) {
                        BasicText(
                            text = strings.novelTextEmpty,
                            style = TextStyle(
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.65f).sp,
                                color = CouiTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            } else {
                items(
                    count = viewerItems.size,
                    key = { it },
                    contentType = { index -> viewerItems[index]::class },
                ) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBackground),
                    ) {
                        CouiNovelViewerItemContent(
                            item = viewerItems[index],
                            webResource = data,
                            fontSizeSp = fontSizeSp,
                            onIllustClick = onIllustClick,
                            onOpenUrl = onOpenUrl,
                        )
                    }
                }
            }

            if (seriesPrev != null || seriesNext != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBackground)
                            .padding(horizontal = 18.dp),
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
            }

            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(cardBackground),
                )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
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
                .padding(horizontal = 18.dp)
                .clickable { onOpenUrl(item.url) },
        )

        // [jump:页码] 当前阅读器无分页概念，按原始文本兜底展示（对齐旧版行为）。
        is NovelViewerItem.JumpRaw -> BasicText(
            text = item.raw,
            style = bodyStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
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
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .height(AppConstants.Novel.EMBEDDED_IMAGE_HEIGHT_DP.dp)
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
