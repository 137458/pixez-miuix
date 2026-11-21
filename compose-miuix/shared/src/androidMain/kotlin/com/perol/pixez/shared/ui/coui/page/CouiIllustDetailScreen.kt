package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import com.itos.coui.menu.CouiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.platform.rememberOptimizedImageModel
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalDownloadRepository
import com.perol.pixez.shared.ui.LocalHistoryRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UgoiraPlayer
import com.perol.pixez.shared.ui.components.buildIllustDetailMoreMenuEntries
import com.perol.pixez.shared.ui.components.resolveIllustCoverUrl
import com.perol.pixez.shared.ui.components.toLocalizedDownloadError
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.core.CouiErrorMessage
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry
import com.perol.pixez.shared.ui.screens.IllustDetailActions
import com.perol.pixez.shared.ui.screens.IllustDetailFullScreenOverlay
import com.perol.pixez.shared.ui.screens.IllustDetailViewport
import com.perol.pixez.shared.ui.screens.rememberIllustDetailActions
import com.perol.pixez.shared.ui.screens.resolveIllustDetailImageWidthPx
import com.perol.pixez.shared.ui.utils.accessibleTouchTarget
import com.perol.pixez.shared.ui.utils.formatPixivDate
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Show

/**
 * COUI 插画详情页（第一期）：骨架、顶栏、图片区、作者行、操作区与简介/标签。
 *
 * 业务编排与 MIUIX 详情页一致：收藏/下载动作来自 [rememberIllustDetailActions]，
 * 全屏查看复用 [IllustDetailFullScreenOverlay]，图片 URL 与画质解析沿用同一套规则。
 * 壳层交给 [CouiSecondaryScaffold]：折叠顶栏、返回、护栏与底栏留白都由骨架提供，
 * 收藏/下载/更多收进顶栏 actions 槽；皮肤差异仅在正文卡片的材质与排版。
 */
@Composable
internal fun CouiIllustDetailScreen(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)? = null,
    onNovelClick: ((Long) -> Unit)? = null,
) {
    val repository = LocalIllustRepository.current
    val settings = LocalSettingsRepository.current
    val sharedBoundsRegistry = LocalSharedBoundsRegistry.current

    DisposableEffect(sharedBoundsRegistry, illustId) {
        onDispose {
            sharedBoundsRegistry.onDetailDisposed(originIllustId = illustId)
        }
    }

    if (settings.swipeChangeArtwork) {
        val relatedState = produceState<List<Long>>(initialValue = emptyList(), illustId) {
            val list = suspendRunCatchingNonCancel { repository.getIllustRelated(illustId) }
                .onFailure { Napier.w(tag = "IllustRelated", throwable = it) { "加载相关作品失败 illustId=$illustId" } }
                .getOrNull().orEmpty()
            value = list.map { it.id }.filter { it != illustId }
        }
        val idList = remember(illustId, relatedState.value) {
            listOf(illustId) + relatedState.value
        }
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { idList.size })
        val currentDisplayedId = idList.getOrElse(pagerState.currentPage) { illustId }

        LaunchedEffect(sharedBoundsRegistry, illustId, currentDisplayedId) {
            sharedBoundsRegistry.updateDisplayedIllustId(
                originIllustId = illustId,
                displayedIllustId = currentDisplayedId,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { idList[it] },
        ) { page ->
            CouiIllustDetailSingle(
                illustId = idList[page],
                isCurrentPage = pagerState.currentPage == page,
                onBack = onBack,
                onUserClick = onUserClick,
                onCommentsClick = onCommentsClick,
                onRelatedIllustsClick = onRelatedIllustsClick,
                onIllustSeriesClick = onIllustSeriesClick,
                onTagClick = onTagClick,
                onIllustClick = onIllustClick,
                onNovelClick = onNovelClick,
                settings = settings,
            )
        }
    } else {
        LaunchedEffect(sharedBoundsRegistry, illustId) {
            sharedBoundsRegistry.updateDisplayedIllustId(
                originIllustId = illustId,
                displayedIllustId = illustId,
            )
        }

        CouiIllustDetailSingle(
            illustId = illustId,
            isCurrentPage = true,
            onBack = onBack,
            onUserClick = onUserClick,
            onCommentsClick = onCommentsClick,
            onRelatedIllustsClick = onRelatedIllustsClick,
            onIllustSeriesClick = onIllustSeriesClick,
            onTagClick = onTagClick,
            onIllustClick = onIllustClick,
            onNovelClick = onNovelClick,
            settings = settings,
        )
    }
}

@Composable
private fun CouiIllustDetailSingle(
    illustId: Long,
    isCurrentPage: Boolean,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)?,
    onNovelClick: ((Long) -> Unit)?,
    settings: SettingsRepository,
) {
    val strings = LocalStrings.current
    val repository = LocalIllustRepository.current
    val bookmarkRepository = LocalBookmarkRepository.current
    val downloadRepository = LocalDownloadRepository.current
    val banRepository = LocalBanRepository.current
    val historyRepository = LocalHistoryRepository.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()

    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    val cachedIllust = remember(illustId) { repository.getCachedIllust(illustId) }
    val state = produceState<Result<Illust>?>(
        initialValue = cachedIllust?.let { Result.success(it) },
        illustId,
        repository,
        retryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.getIllustDetail(illustId) }
    }
    val result = state.value
    val illust = result?.getOrNull()

    // 浏览历史只记录当前展示页，避免预加载页污染历史。
    LaunchedEffect(illust, isCurrentPage) {
        if (illust != null && isCurrentPage) {
            suspendRunCatchingNonCancel { historyRepository.insert(illust) }
        }
    }

    var isBookmarked by rememberSaveable(illust) { mutableStateOf(illust?.isBookmarked ?: false) }
    var isBookmarkLoading by remember { mutableStateOf(false) }
    var bookmarkError by rememberSaveable { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var isBanned by rememberSaveable(illustId) { mutableStateOf(false) }
    var isTempView by rememberSaveable(illustId) { mutableStateOf(false) }
    var fullScreenPageIndex by rememberSaveable(illustId) { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(illustId) {
        suspendRunCatchingNonCancel { banRepository.isBanIllust(illustId) }
            .onSuccess { isBanned = it }
    }

    val changeVersion = settings.changeVersion.collectAsState().value
    val isRestricted = remember(illust, isBanned, settings.hIsNotAllow, changeVersion) {
        isBanned || (settings.hIsNotAllow == true && illust?.isR18() == true)
    }

    val actions = rememberIllustDetailActions(
        illust = illust,
        isBookmarked = isBookmarked,
        isDownloading = isDownloading,
        settings = settings,
        strings = strings,
        coroutineScope = coroutineScope,
        repository = repository,
        bookmarkRepository = bookmarkRepository,
        downloadRepository = downloadRepository,
        onBookmarkedChange = { isBookmarked = it },
        onBookmarkLoadingChange = { isBookmarkLoading = it },
        onBookmarkErrorChange = { bookmarkError = it },
        onDownloadingChange = { isDownloading = it },
        onToast = { data -> data?.let(toastSink) },
    )

    // 全屏查看层必须盖住顶栏，而顶栏由骨架在正文之上绘制，因此它留在骨架之外、
    // 与骨架同级后置绘制。外层盒子不画底色、不加安全区，页面底色仍由骨架提供。
    Box(modifier = Modifier.fillMaxSize()) {
        CouiSecondaryScaffold(
            title = illust?.title.orEmpty(),
            onBack = onBack,
            actions = {
                CouiDetailTopBarActions(
                    illust = illust,
                    isBookmarked = isBookmarked,
                    isBookmarkLoading = isBookmarkLoading,
                    isDownloading = isDownloading,
                    isBanned = isBanned,
                    strings = strings,
                    coroutineScope = coroutineScope,
                    banRepository = banRepository,
                    actions = actions,
                    onBanSuccess = { isBanned = true },
                    onToast = toastSink,
                )
            },
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
            ) {
                val viewport = IllustDetailViewport(
                    isWideScreen = maxWidth >= AppConstants.Layout.WIDE_SCREEN_MIN_WIDTH_DP.dp,
                    containerWidthPx = constraints.maxWidth.toFloat(),
                    containerHeightPx = constraints.maxHeight.toFloat(),
                )
                when {
                    result == null -> CouiPlaceholder(
                        message = strings.loading,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )

                    illust != null -> {
                        if (isRestricted && !isTempView) {
                            CouiPlaceholder(
                                message = illust.title,
                                actionText = strings.confirm,
                                onAction = { isTempView = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                            )
                        } else {
                            val illustAspectRatio = remember(illust.width, illust.height) {
                                if (illust.width > 0 && illust.height > 0) {
                                    (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.1f, 10.0f)
                                } else {
                                    null
                                }
                            }
                            CouiIllustDetailList(
                                illust = illust,
                                illustAspectRatio = illustAspectRatio,
                                viewport = viewport,
                                settings = settings,
                                listState = listState,
                                padding = padding,
                                repository = repository,
                                downloadRepository = downloadRepository,
                                isDownloading = isDownloading,
                                coroutineScope = coroutineScope,
                                strings = strings,
                                onToast = { data -> data?.let(toastSink) },
                                onDownloadAll = actions::downloadAll,
                                onFullScreen = { fullScreenPageIndex = it },
                                onUserClick = onUserClick,
                                onCommentsClick = onCommentsClick,
                                onRelatedIllustsClick = onRelatedIllustsClick,
                                onIllustSeriesClick = onIllustSeriesClick,
                                onTagClick = onTagClick,
                                onIllustClick = onIllustClick,
                                onNovelClick = onNovelClick,
                            )
                        }
                    }

                    else -> CouiPlaceholder(
                        message = CouiErrorMessage(result.exceptionOrNull()),
                        actionText = strings.retry,
                        onAction = { retryCount++ },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }

        val fullScreenPage = fullScreenPageIndex
        if (fullScreenPage != null && illust != null) {
            IllustDetailFullScreenOverlay(
                illust = illust,
                pageIndex = fullScreenPage,
                settings = settings,
                repository = repository,
                downloadRepository = downloadRepository,
                detailBackdrop = null,
                onToast = toastSink,
                onDismiss = { fullScreenPageIndex = null },
            )
        }

        LaunchedEffect(bookmarkError) {
            bookmarkError?.let {
                toastSink(ToastData(it, ToastType.Error))
                bookmarkError = null
            }
        }
    }
}

/**
 * COUI 详情页顶栏动作：收藏、下载、更多菜单。
 *
 * 三个按钮都放在骨架的 actions 槽内，材质、按压反馈与图标色由顶栏下发
 * （API 34+ 官方按钮，API 31-33 同尺寸平替）。收藏态用实心图标表达。
 */
@Composable
private fun CouiDetailTopBarActions(
    illust: Illust?,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    isDownloading: Boolean,
    isBanned: Boolean,
    strings: AppStrings,
    coroutineScope: CoroutineScope,
    banRepository: BanRepository,
    actions: IllustDetailActions,
    onBanSuccess: () -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val menuAnchor = rememberCouiMenuAnchor()
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    CouiTopBarIconButton(
        onClick = actions::bookmark,
        icon = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
        contentDescription = if (isBookmarked) strings.bookmarked else strings.bookmark,
        enabled = !isBookmarkLoading && illust != null,
    )
    CouiTopBarIconButton(
        onClick = actions::download,
        icon = MiuixIcons.Download,
        contentDescription = strings.download,
        enabled = !isDownloading && illust != null,
    )
    if (illust != null) {
        // 菜单挂在顶栏按钮的锚点上，位置由 couiMenuAnchor 按可见边界计算。
        Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
            CouiTopBarIconButton(
                onClick = { menuAnchor.open { menuExpanded = true } },
                icon = MiuixIcons.More,
                contentDescription = strings.menuMoreActions,
            )
            CouiDetailMoreMenu(
                illust = illust,
                expanded = menuExpanded,
                anchor = menuAnchor,
                isBanned = isBanned,
                strings = strings,
                coroutineScope = coroutineScope,
                banRepository = banRepository,
                onDismiss = { menuExpanded = false },
                onBanSuccess = onBanSuccess,
                onToast = onToast,
            )
        }
    }
}

/**
 * COUI 更多操作菜单：复制/分享/浏览器/SauceNAO/屏蔽的动作编排统一取自共享构建器
 * buildIllustDetailMoreMenuEntries，这里只保留 COUI 自己的锚点弹层与条目材质。
 * 之前整份复制一份 when 分支，文案、屏蔽流程、异常提示会随 MIUIX 版各自漂移。
 */
@Composable
private fun CouiDetailMoreMenu(
    illust: Illust,
    expanded: Boolean,
    anchor: CouiMenuAnchor,
    isBanned: Boolean,
    strings: AppStrings,
    coroutineScope: CoroutineScope,
    banRepository: BanRepository,
    onDismiss: () -> Unit,
    onBanSuccess: () -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val context = LocalPlatformContext.current
    val entries = buildIllustDetailMoreMenuEntries(
        strings = strings,
        illust = illust,
        isBanned = isBanned,
        banRepository = banRepository,
        onBanSuccess = onBanSuccess,
        onToast = onToast,
        clipboard = clipboard,
        share = share,
        context = context,
        coroutineScope = coroutineScope,
    )
    // CouiMenuItem 只有 value/text/分隔线，没有图标槽，因此把共享条目按组拍平，
    // 用下标回查动作；分隔线按共享分组边界（非首组的第一项）还原。
    val flatItems = entries.flatMap { it.items }
    val items = buildList {
        var index = 0
        entries.forEachIndexed { entryIndex, entry ->
            entry.items.forEachIndexed { itemIndex, item ->
                add(
                    CouiMenuItem(
                        value = index.toString(),
                        text = item.text,
                        dividerBefore = itemIndex == 0 && entryIndex > 0,
                    ),
                )
                index += 1
            }
        }
    }
    Menu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        anchor = anchor,
        headerText = strings.menuMoreActions,
        items = items,
        onItemClick = { item ->
            onDismiss()
            // value 是外部库的不透明类型，这里按值相等回查，不依赖它的具体类型。
            flatItems.getOrNull(items.indexOfFirst { it.value == item.value })?.onClick?.invoke()
        },
    )
}

/** COUI 详情页主列表：图片区、信息卡、简介卡、标签卡与互动卡。 */
@Composable
private fun CouiIllustDetailList(
    illust: Illust,
    illustAspectRatio: Float?,
    viewport: IllustDetailViewport,
    settings: SettingsRepository,
    listState: LazyListState,
    padding: PaddingValues,
    repository: IllustRepository,
    downloadRepository: DownloadRepository,
    isDownloading: Boolean,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (ToastData?) -> Unit,
    onDownloadAll: () -> Unit,
    onFullScreen: (Int) -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)?,
    onNovelClick: ((Long) -> Unit)?,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(padding),
        // 护栏交给列表内层：首图从收起后的顶栏下方开始，滚动时仍能滚进顶栏覆盖区。
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding(),
        ),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        if (illust.metaPages.isNotEmpty()) {
            items(
                count = illust.metaPages.size,
                key = { "coui_page_$it" },
                contentType = { "coui_meta_page" },
            ) { pageIndex ->
                CouiDetailImagePage(
                    illust = illust,
                    pageIndex = pageIndex,
                    illustAspectRatio = illustAspectRatio,
                    viewport = viewport,
                    settings = settings,
                    downloadRepository = downloadRepository,
                    coroutineScope = coroutineScope,
                    strings = strings,
                    onToast = onToast,
                    onPageClick = onFullScreen,
                )
            }
        } else {
            item(key = "coui_single_page", contentType = "coui_single_page") {
                CouiDetailSinglePageImage(
                    illust = illust,
                    illustAspectRatio = illustAspectRatio,
                    viewport = viewport,
                    settings = settings,
                    repository = repository,
                    onPageClick = onFullScreen,
                )
            }
        }

        item(key = "coui_info_card", contentType = "coui_info_card") {
            CouiDetailInfoCard(
                illust = illust,
                isDownloading = isDownloading,
                strings = strings,
                onUserClick = onUserClick,
                onIllustSeriesClick = onIllustSeriesClick,
                onDownloadAll = onDownloadAll,
            )
        }

        if (illust.caption.isNotBlank()) {
            item(key = "coui_caption", contentType = "coui_caption") {
                CouiDetailSectionCard(title = strings.searchTargetTitleCaption) {
                    HtmlCaptionText(
                        html = illust.caption,
                        onUserClick = onUserClick,
                        onIllustClick = onIllustClick,
                        onIllustSeriesClick = onIllustSeriesClick,
                        onNovelClick = onNovelClick,
                        onTagClick = onTagClick,
                        onLinkClick = { url ->
                            openSafeUrl(url, strings, onError = { onToast(ToastData(it, ToastType.Error)) })
                        },
                        style = LocalCouiTextStyle.current,
                    )
                }
            }
        }

        if (illust.tags.isNotEmpty()) {
            item(key = "coui_tags", contentType = "coui_tags") {
                CouiDetailSectionCard(title = strings.tags) {
                    CouiDetailTagFlow(tags = illust.tags, onTagClick = onTagClick)
                }
            }
        }

        item(key = "coui_interaction", contentType = "coui_interaction") {
            Spacer(modifier = Modifier.height(10.dp))
            CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                CouiValueRow(
                    title = strings.commentsTitle,
                    value = "${illust.totalComments ?: 0}",
                    onClick = { onCommentsClick(illust.id) },
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.relatedIllusts,
                    onClick = { onRelatedIllustsClick(illust.id) },
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** COUI 详情页单张图片：按视口约束的宽度 + 右下角下载浮钮。 */
@Composable
private fun CouiDetailImagePage(
    illust: Illust,
    pageIndex: Int,
    illustAspectRatio: Float?,
    viewport: IllustDetailViewport,
    settings: SettingsRepository,
    downloadRepository: DownloadRepository,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (ToastData?) -> Unit,
    onPageClick: (Int) -> Unit,
) {
    val page = illust.metaPages[pageIndex]
    val changeVersion = settings.changeVersion.collectAsState().value
    val effectiveQuality = remember(illust.type, settings.pictureQuality, settings.mangaQuality, changeVersion) {
        resolveCouiIllustQuality(illust, settings)
    }
    val rawPageUrl = remember(page, effectiveQuality) {
        when (effectiveQuality) {
            0 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
            1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
            2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
            else -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
        }
    }
    val pageUrl = rememberOptimizedImageModel(
        illust = illust,
        pageIndex = pageIndex,
        targetUrl = rawPageUrl,
        originalUrl = page.imageUrls?.original,
        customBasePath = settings.storePath,
        pictureSource = settings.pictureSource,
    )
    val thumbnailUrl = remember(page, pageIndex, illust, settings.feedPreviewQuality, changeVersion) {
        if (pageIndex == 0) {
            resolveIllustCoverUrl(illust.imageUrls, settings.feedPreviewQuality, allowSquare = false)
        } else {
            page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
        }
    }
    var pageLoaded by remember(pageIndex) { mutableStateOf(false) }
    val imageWidthDp = rememberCouiDetailImageWidthDp(viewport, illustAspectRatio)
    val imageModifier = Modifier
        .fillMaxWidth()
        .then(
            if ((pageIndex == 0 || !pageLoaded) && illustAspectRatio != null) {
                Modifier.aspectRatio(illustAspectRatio)
            } else {
                Modifier
            },
        )
        .illustDragAndDropSource(illust, pageIndex = pageIndex)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { onPageClick(pageIndex) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = if (viewport.isWideScreen) Modifier.width(imageWidthDp) else Modifier.fillMaxWidth(),
        ) {
            PixivAsyncImage(
                model = pageUrl,
                thumbnailUrl = thumbnailUrl,
                contentDescription = "${illust.title} ($pageIndex)",
                contentScale = ContentScale.FillWidth,
                modifier = imageModifier,
                onSuccess = { pageLoaded = true },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .accessibleTouchTarget(48.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        coroutineScope.launch {
                            val pageNumber = pageIndex + 1
                            onToast(ToastData("${strings.downloadStatusDownloading} P$pageNumber…", ToastType.Normal))
                            val task = downloadRepository.download(illust, pageIndex = pageIndex)
                            onToast(
                                when (task.status) {
                                    DownloadStatus.Success -> ToastData("${strings.downloadStatusSuccess} (P$pageNumber)", ToastType.Success)
                                    DownloadStatus.Failed -> ToastData("${strings.downloadStatusFailed}: ${task.error.toLocalizedDownloadError(strings)}", ToastType.Error)
                                    else -> null
                                },
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = rememberVectorPainter(MiuixIcons.Download),
                    contentDescription = "${strings.download} P${pageIndex + 1}",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    if (pageIndex < illust.metaPages.lastIndex) {
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** COUI 详情页单页图片：Ugoira 播放器或普通单图。 */
@Composable
private fun CouiDetailSinglePageImage(
    illust: Illust,
    illustAspectRatio: Float?,
    viewport: IllustDetailViewport,
    settings: SettingsRepository,
    repository: IllustRepository,
    onPageClick: (Int) -> Unit,
) {
    val imageWidthDp = rememberCouiDetailImageWidthDp(viewport, illustAspectRatio)
    val sizedModifier = if (viewport.isWideScreen) Modifier.width(imageWidthDp) else Modifier.fillMaxWidth()
    if (IllustType.isUgoira(illust.type)) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            UgoiraPlayer(
                illust = illust,
                illustRepository = repository,
                onClick = { onPageClick(0) },
                modifier = sizedModifier,
            )
        }
    } else {
        val changeVersion = settings.changeVersion.collectAsState().value
        val effectiveQuality = remember(illust.type, settings.pictureQuality, settings.mangaQuality, changeVersion) {
            resolveCouiIllustQuality(illust, settings)
        }
        val rawSingleUrl = remember(illust, effectiveQuality) {
            when (effectiveQuality) {
                0 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                else -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
            }
        }
        val singleUrl = rememberOptimizedImageModel(
            illust = illust,
            pageIndex = 0,
            targetUrl = rawSingleUrl,
            originalUrl = illust.metaSinglePage?.originalImageUrl,
            customBasePath = settings.storePath,
            pictureSource = settings.pictureSource,
        )
        val thumbnailUrl = remember(illust, settings.feedPreviewQuality, changeVersion) {
            resolveIllustCoverUrl(illust.imageUrls, settings.feedPreviewQuality, allowSquare = false)
        }
        val imageModifier = Modifier
            .fillMaxWidth()
            .then(
                if (illustAspectRatio != null) {
                    Modifier.aspectRatio(illustAspectRatio)
                } else {
                    Modifier
                },
            )
            .illustDragAndDropSource(illust, pageIndex = 0)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onPageClick(0) }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = sizedModifier) {
                PixivAsyncImage(
                    model = singleUrl,
                    thumbnailUrl = thumbnailUrl,
                    contentDescription = illust.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = imageModifier,
                )
            }
        }
    }
}

/** 信息卡：标题、指标、ID/尺寸、作者行、下载全部与系列入口。 */
@Composable
private fun CouiDetailInfoCard(
    illust: Illust,
    isDownloading: Boolean,
    strings: AppStrings,
    onUserClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onDownloadAll: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = illust.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiDetailMetric(icon = MiuixIcons.Show, text = illust.totalView.toString())
                CouiDetailMetric(icon = MiuixIcons.Favorites, text = illust.totalBookmarks.toString())
                CouiDetailMetric(icon = MiuixIcons.Recent, text = formatPixivDate(illust.createDate))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ID: ${illust.id}   ${illust.width}x${illust.height}",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            CouiDetailArtistRow(illust = illust, strings = strings, onUserClick = onUserClick)
            if (illust.pageCount > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                CouiValueRow(
                    title = strings.downloadTaskFilterAll,
                    value = "${illust.pageCount} P",
                    onClick = {
                        if (!isDownloading) onDownloadAll()
                    },
                )
            }
            illust.series?.let { series ->
                Spacer(modifier = Modifier.height(4.dp))
                CouiValueRow(
                    title = series.title.orEmpty(),
                    onClick = { onIllustSeriesClick(series.id) },
                )
            }
        }
    }
}

@Composable
private fun CouiDetailMetric(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(CouiTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CouiDetailArtistRow(
    illust: Illust,
    strings: AppStrings,
    onUserClick: (Long) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CouiTheme.colorScheme.container)
            .couiPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onUserClick(illust.user.id) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixivAsyncImage(
            model = illust.user.profileImageUrls.medium,
            contentDescription = illust.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = illust.user.name,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "@${illust.user.account}",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        androidx.compose.foundation.Image(
            painter = rememberVectorPainter(MiuixIcons.Search),
            contentDescription = strings.author,
            colorFilter = ColorFilter.tint(CouiTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 通用分区卡片：标题 + 内容。 */
@Composable
private fun CouiDetailSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(modifier = Modifier.height(10.dp))
    CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiDetailTagFlow(
    tags: List<IllustTag>,
    onTagClick: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        tags.forEach { tag ->
            val tagText = buildString {
                append("#")
                append(tag.name)
                if (!tag.translatedName.isNullOrBlank() && tag.translatedName != tag.name) {
                    append(" ")
                    append(tag.translatedName)
                }
            }
            CouiAssistChip(
                text = tagText,
                selected = false,
                onClick = { onTagClick(tag.name) },
            )
        }
    }
}

/** 与 MIUIX 详情页同一套画质档位解析：漫画走独立档，其它走通用图片档。 */
private fun resolveCouiIllustQuality(illust: Illust, settings: SettingsRepository?): Int =
    if (illust.type == IllustType.MANGA) {
        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
    } else {
        settings?.pictureQuality ?: 0
    }

/** 复用详情页视口约束：大屏图片收进内容列并限制高度，窄屏保持全出血。 */
@Composable
private fun rememberCouiDetailImageWidthDp(
    viewport: IllustDetailViewport,
    illustAspectRatio: Float?,
): Dp {
    val density = LocalDensity.current
    return remember(viewport, illustAspectRatio, density) {
        with(density) {
            resolveIllustDetailImageWidthPx(
                containerWidthPx = viewport.containerWidthPx,
                containerHeightPx = viewport.containerHeightPx,
                illustAspectRatio = illustAspectRatio,
                isWideScreen = viewport.isWideScreen,
                contentMaxWidthPx = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp.toPx(),
                imageHorizontalPaddingPx = 12.dp.toPx(),
                maxHeightFraction = AppConstants.Layout.DETAIL_IMAGE_MAX_HEIGHT_FRACTION,
            ).toDp()
        }
    }
}
