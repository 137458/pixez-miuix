package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品详情页：沉浸式大图展示、单一大标题、高对比度 MIUIX Card 容器与胶囊标签（Capsule Chips）。
 *
 * 当开启 [SettingsRepository.swipeChangeArtwork] 时，通过 HorizontalPager 支持左右滑动切换关联作品。
 */
@Composable
fun IllustDetailScreen(
    illustId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
    onTagClick: (String) -> Unit,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    banRepository: BanRepository,
    historyRepository: HistoryRepository? = null,
    onIllustClick: ((Int) -> Unit)? = null,
    onNovelClick: ((Int) -> Unit)? = null,
) {
    val settings = LocalSettingsRepository.current
    val swipeChangeArtwork = settings?.swipeChangeArtwork == true
    val sharedBoundsRegistry = com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry.current

    androidx.compose.runtime.DisposableEffect(sharedBoundsRegistry) {
        onDispose {
            sharedBoundsRegistry.activeDetailIllustId = null
        }
    }

    if (swipeChangeArtwork) {
        val relatedState = produceState<List<Int>>(initialValue = emptyList(), illustId) {
            val list = suspendRunCatchingNonCancel { repository.getIllustRelated(illustId) }.getOrNull().orEmpty()
            value = list.map { it.id }.filter { it != illustId }
        }
        val idList = remember(illustId, relatedState.value) {
            listOf(illustId) + relatedState.value
        }
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { idList.size })
        val currentDisplayedId = idList.getOrElse(pagerState.currentPage) { illustId }

        LaunchedEffect(sharedBoundsRegistry, currentDisplayedId) {
            sharedBoundsRegistry.activeDetailIllustId = currentDisplayedId
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { idList[it] },
        ) { page ->
            IllustDetailSingleContent(
                illustId = idList[page],
                isCurrentPage = (pagerState.currentPage == page),
                onBack = onBack,
                onUserClick = onUserClick,
                onCommentsClick = onCommentsClick,
                onRelatedIllustsClick = onRelatedIllustsClick,
                onIllustSeriesClick = onIllustSeriesClick,
                onTagClick = onTagClick,
                repository = repository,
                bookmarkRepository = bookmarkRepository,
                downloadRepository = downloadRepository,
                banRepository = banRepository,
                historyRepository = historyRepository,
                onIllustClick = onIllustClick,
                onNovelClick = onNovelClick,
            )
        }
    } else {
        LaunchedEffect(sharedBoundsRegistry, illustId) {
            sharedBoundsRegistry.activeDetailIllustId = illustId
        }

        IllustDetailSingleContent(
            illustId = illustId,
            isCurrentPage = true,
            onBack = onBack,
            onUserClick = onUserClick,
            onCommentsClick = onCommentsClick,
            onRelatedIllustsClick = onRelatedIllustsClick,
            onIllustSeriesClick = onIllustSeriesClick,
            onTagClick = onTagClick,
            repository = repository,
            bookmarkRepository = bookmarkRepository,
            downloadRepository = downloadRepository,
            banRepository = banRepository,
            historyRepository = historyRepository,
            onIllustClick = onIllustClick,
            onNovelClick = onNovelClick,
        )
    }
}

@Composable
private fun IllustDetailSingleContent(
    illustId: Int,
    isCurrentPage: Boolean = true,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
    onTagClick: (String) -> Unit,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    banRepository: BanRepository,
    historyRepository: HistoryRepository? = null,
    onIllustClick: ((Int) -> Unit)? = null,
    onNovelClick: ((Int) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val settings = LocalSettingsRepository.current

    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
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

    // 成功加载插画详情时，自动异步写入本地浏览历史（仅当前展示页写入，避免预加载污染历史）
    LaunchedEffect(illust, isCurrentPage) {
        if (illust != null && isCurrentPage && historyRepository != null) {
            suspendRunCatchingNonCancel {
                historyRepository.insert(illust)
            }
        }
    }

    var isBookmarked by rememberSaveable(illust) { mutableStateOf(illust?.isBookmarked ?: false) }
    // 进行中标志使用 remember 而非 rememberSaveable：进程恢复后协程不会恢复，避免按钮被永久禁用。
    var isBookmarkLoading by remember { mutableStateOf(false) }
    var bookmarkError by rememberSaveable { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isBanned by rememberSaveable(illustId) { mutableStateOf(false) }
    var isTempView by rememberSaveable(illustId) { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable(illustId) { mutableStateOf(false) }
    var fullScreenPageIndex by rememberSaveable(illustId) { mutableStateOf<Int?>(null) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // 页面进入或作品 ID 变化时，查询本地屏蔽记录；数据库异常时保持未屏蔽，避免崩溃。
    LaunchedEffect(illustId) {
        suspendRunCatchingNonCancel { banRepository.isBanIllust(illustId) }
            .onSuccess { isBanned = it }
    }

    val detailBackdrop = rememberBlurBackdrop()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val scrollThresholdPx = with(density) { 72.dp.toPx() }
    var scrollOffset by remember { mutableStateOf(0f) }

    val detailNestedScrollConnection = remember(scrollThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                scrollOffset = (scrollOffset - delta).coerceIn(0f, scrollThresholdPx)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            scrollOffset = 0f
        }
    }

    val collapseProgressState = remember(scrollThresholdPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (scrollOffset / scrollThresholdPx).coerceIn(0f, 1f)
            }
        }
    }

    val isRestricted = remember(illust, isBanned, settings?.hIsNotAllow, settings?.changeVersion) {
        isBanned || (settings?.hIsNotAllow == true && illust?.isR18() == true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            result.isSuccess && illust != null -> when {
                isRestricted && !isTempView -> BanPage(
                    name = illust.title,
                    onView = { isTempView = true },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> {
                    val illustAspectRatio = remember(illust.width, illust.height) {
                        if (illust.width > 0 && illust.height > 0) {
                            (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.1f, 10.0f)
                        } else {
                            null
                        }
                    }

                    IllustDetailContentList(
                        illust = illust,
                        illustAspectRatio = illustAspectRatio,
                        settings = settings,
                        listState = listState,
                        nestedScrollConnection = detailNestedScrollConnection,
                        detailBackdrop = detailBackdrop,
                        repository = repository,
                        bookmarkRepository = bookmarkRepository,
                        downloadRepository = downloadRepository,
                        isDownloading = isDownloading,
                        isBookmarked = isBookmarked,
                        coroutineScope = coroutineScope,
                        strings = strings,
                        onToast = { toastMessage = it },
                        onDownloadingChange = { isDownloading = it },
                        onBookmarkedChange = { isBookmarked = it },
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
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── 顶部浮动导航栏（液态玻璃胶囊工具栏）──
        val isDarkTheme = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        val bookmarkHeartScale = remember { androidx.compose.animation.core.Animatable(1f) }
        LaunchedEffect(isBookmarked) {
            if (isBookmarked) {
                bookmarkHeartScale.animateTo(
                    targetValue = 1.32f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.35f,
                        stiffness = 700f,
                    ),
                )
                bookmarkHeartScale.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f,
                        stiffness = 400f,
                    ),
                )
            }
        }

        val fullScreenPage = fullScreenPageIndex
        if (fullScreenPage == null) {
            IllustDetailTopBarSection(
                illust = illust,
                isBookmarked = isBookmarked,
                isBookmarkLoading = isBookmarkLoading,
                isDownloading = isDownloading,
                isBanned = isBanned,
                settings = settings,
                strings = strings,
                detailBackdrop = detailBackdrop,
                collapseProgressProvider = { collapseProgressState.value },
                bookmarkHeartScale = bookmarkHeartScale,
                coroutineScope = coroutineScope,
                repository = repository,
                bookmarkRepository = bookmarkRepository,
                downloadRepository = downloadRepository,
                banRepository = banRepository,
                onBookmarkedChange = { isBookmarked = it },
                onBookmarkLoadingChange = { isBookmarkLoading = it },
                onBookmarkErrorChange = { bookmarkError = it },
                onDownloadingChange = { isDownloading = it },
                onToast = { toastMessage = it },
                onBanSuccess = { isBanned = true },
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else if (illust != null) {
            IllustDetailFullScreenOverlay(
                illust = illust,
                pageIndex = fullScreenPage,
                settings = settings,
                repository = repository,
                downloadRepository = downloadRepository,
                detailBackdrop = detailBackdrop,
                onToast = { toastMessage = it },
                onDismiss = { fullScreenPageIndex = null },
            )
        }

        ToastMessage(
            message = toastMessage ?: bookmarkError,
            type = if (bookmarkError != null) ToastType.Error else null,
            backdrop = detailBackdrop,
            onDismiss = {
                toastMessage = null
                bookmarkError = null
            },
        )
    }
}

/**
 * 作品详情页主列表：沉浸式图片页、作品信息卡、简介卡、标签卡与互动卡。
 *
 * 仅负责 LazyColumn 骨架与子组件编排，所有页面级状态由 [IllustDetailSingleContent] 通过参数下发。
 */
@Composable
private fun IllustDetailContentList(
    illust: Illust,
    illustAspectRatio: Float?,
    settings: SettingsRepository?,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    detailBackdrop: LayerBackdrop?,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    isDownloading: Boolean,
    isBookmarked: Boolean,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (String?) -> Unit,
    onDownloadingChange: (Boolean) -> Unit,
    onBookmarkedChange: (Boolean) -> Unit,
    onFullScreen: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Int) -> Unit)?,
    onNovelClick: ((Int) -> Unit)?,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .blurBackdropSource(detailBackdrop)
            .nestedScroll(nestedScrollConnection),
    ) {
        // 1. 沉浸式顶部大图（从屏幕最顶端开始渲染，消除生硬的一刀切顶栏）
        if (illust.metaPages.isNotEmpty()) {
            items(
                count = illust.metaPages.size,
                key = { "page_$it" },
                contentType = { "meta_page" },
            ) { pageIndex ->
                IllustDetailImagePage(
                    illust = illust,
                    pageIndex = pageIndex,
                    page = illust.metaPages[pageIndex],
                    illustAspectRatio = illustAspectRatio,
                    settings = settings,
                    downloadRepository = downloadRepository,
                    coroutineScope = coroutineScope,
                    strings = strings,
                    onToast = onToast,
                    onPageClick = onFullScreen,
                )
            }
        } else {
            item(key = "single_page", contentType = "single_page") {
                IllustDetailSinglePageImage(
                    illust = illust,
                    illustAspectRatio = illustAspectRatio,
                    settings = settings,
                    repository = repository,
                    downloadRepository = downloadRepository,
                    strings = strings,
                    onToast = onToast,
                    onPageClick = onFullScreen,
                )
            }
        }

        // 2. 作品信息与画师卡片（单一清晰大标题、数据指标、画师头像名称与下载全部/系列入口）
        item(key = "illust_info_card", contentType = "info_card") {
            IllustDetailInfoCard(
                illust = illust,
                isDownloading = isDownloading,
                isBookmarked = isBookmarked,
                settings = settings,
                strings = strings,
                downloadRepository = downloadRepository,
                bookmarkRepository = bookmarkRepository,
                coroutineScope = coroutineScope,
                onToast = onToast,
                onDownloadingChange = onDownloadingChange,
                onBookmarkedChange = onBookmarkedChange,
                onUserClick = onUserClick,
                onIllustSeriesClick = onIllustSeriesClick,
            )
        }

        // 3. 简介与文案卡片
        if (illust.caption.isNotBlank()) {
            item {
                IllustDetailCaptionCard(
                    illust = illust,
                    strings = strings,
                    onUserClick = onUserClick,
                    onIllustClick = onIllustClick,
                    onIllustSeriesClick = onIllustSeriesClick,
                    onNovelClick = onNovelClick,
                    onTagClick = onTagClick,
                )
            }
        }

        // 4. 标签卡片与胶囊包裹（Capsule Chips）
        if (illust.tags.isNotEmpty()) {
            item {
                IllustDetailTagsCard(
                    tags = illust.tags,
                    strings = strings,
                    onTagClick = onTagClick,
                )
            }
        }

        // 5. 互动操作卡片（评论与相关作品）
        item {
            IllustDetailInteractionCard(
                illust = illust,
                strings = strings,
                onCommentsClick = onCommentsClick,
                onRelatedIllustsClick = onRelatedIllustsClick,
            )
        }
    }
}

/**
 * 屏蔽占位页。
 */
@Composable
private fun BanPage(
    name: String,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$name\n",
            style = MiuixTheme.textStyles.title2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onView) {
            Text(text = strings.confirm)
        }
    }
}