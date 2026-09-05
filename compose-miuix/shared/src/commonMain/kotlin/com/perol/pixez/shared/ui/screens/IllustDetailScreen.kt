package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.perol.pixez.shared.ui.AppConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.ui.components.liquidGlass
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.IllustActionMenu
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.UgoiraPlayer
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.IllustDetailTopBar
import com.perol.pixez.shared.ui.components.IllustFullScreenViewer
import com.perol.pixez.shared.ui.components.buildIllustCopyInfo
import com.perol.pixez.shared.ui.components.buildIllustShareLink
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.data.repository.HistoryRepository
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

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

    if (swipeChangeArtwork) {
        val relatedState = produceState<List<Int>>(initialValue = emptyList(), illustId) {
            val list = suspendRunCatchingNonCancel { repository.getIllustRelated(illustId) }.getOrNull().orEmpty()
            value = list.map { it.id }.filter { it != illustId }
        }
        val idList = remember(illustId, relatedState.value) {
            listOf(illustId) + relatedState.value
        }
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { idList.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { idList[it] },
        ) { page ->
            IllustDetailSingleContent(
                illustId = idList[page],
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
        IllustDetailSingleContent(
            illustId = illustId,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IllustDetailSingleContent(
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
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

    // 成功加载插画详情时，自动异步写入本地浏览历史
    LaunchedEffect(illust) {
        if (illust != null && historyRepository != null) {
            suspendRunCatchingNonCancel {
                historyRepository.insert(illust)
            }
        }
    }

    var isBookmarked by rememberSaveable(illust) { mutableStateOf(illust?.isBookmarked ?: false) }
    var isBookmarkLoading by rememberSaveable { mutableStateOf(false) }
    var bookmarkError by rememberSaveable { mutableStateOf<String?>(null) }
    var isDownloading by rememberSaveable { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isBanned by rememberSaveable(illustId) { mutableStateOf(false) }
    var isTempView by rememberSaveable(illustId) { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable(illustId) { mutableStateOf(false) }
    var fullScreenPageIndex by rememberSaveable(illustId) { mutableStateOf<Int?>(null) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()
    val context = coil3.compose.LocalPlatformContext.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    // 页面进入或作品 ID 变化时，查询本地屏蔽记录；数据库异常时保持未屏蔽，避免崩溃。
    LaunchedEffect(illustId) {
        suspendRunCatchingNonCancel { banRepository.isBanIllust(illustId) }
            .onSuccess { isBanned = it }
    }

    val detailBackdrop = rememberBlurBackdrop()
    val listState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
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

    val collapseProgress by remember(scrollThresholdPx) {
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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .blurBackdropSource(detailBackdrop)
                            .nestedScroll(detailNestedScrollConnection),
                    ) {
                        // 1. 沉浸式顶部大图（从屏幕最顶端开始渲染，消除生硬的一刀切顶栏）
                        if (illust.metaPages.isNotEmpty()) {
                            items(
                                count = illust.metaPages.size,
                                key = { "page_$it" },
                                contentType = { "meta_page" },
                            ) { pageIndex ->
                                val page = illust.metaPages[pageIndex]
                                val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
                                    if (illust.type == "manga") {
                                        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
                                    } else {
                                        settings?.pictureQuality ?: 0
                                    }
                                }
                                val pageUrl = remember(page, effectiveQuality) {
                                    when (effectiveQuality) {
                                        0 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                                        1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                                        2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                                        else -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                                    }
                                }
                                val thumbnailUrl = remember(page) {
                                    page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
                                }
                                val pageModifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (pageIndex == 0 && illustAspectRatio != null) {
                                            Modifier.aspectRatio(illustAspectRatio)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .illustDragAndDropSource(illust, pageIndex = pageIndex)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                    ) { fullScreenPageIndex = pageIndex }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    PixivAsyncImage(
                                        model = pageUrl,
                                        thumbnailUrl = thumbnailUrl,
                                        contentDescription = "${illust.title} ($pageIndex)",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = pageModifier,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .clickable {
                                                coroutineScope.launch {
                                                    val pageNumber = pageIndex + 1
                                                    toastMessage = "${strings.downloadStatusDownloading} P$pageNumber…"
                                                    val task = downloadRepository.download(illust, pageIndex = pageIndex)
                                                    toastMessage = when (task.status) {
                                                        DownloadStatus.Success -> "${strings.downloadStatusSuccess} (P$pageNumber)"
                                                        DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                                        else -> null
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Download,
                                            contentDescription = "${strings.download} P${pageIndex + 1}",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                if (pageIndex < illust.metaPages.lastIndex) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        } else {
                            item(key = "single_page", contentType = "single_page") {
                                if (illust.type == "ugoira") {
                                    UgoiraPlayer(
                                        illust = illust,
                                        illustRepository = repository,
                                        downloadRepository = downloadRepository,
                                        modifier = Modifier.fillMaxWidth(),
                                        onSavedZip = { path ->
                                            toastMessage = "${strings.ugoiraSaveZipSuccess}: $path"
                                        },
                                    )
                                } else {
                                    val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
                                        if (illust.type == "manga") {
                                            settings?.mangaQuality ?: settings?.pictureQuality ?: 0
                                        } else {
                                            settings?.pictureQuality ?: 0
                                        }
                                    }
                                    val singleUrl = remember(illust, effectiveQuality) {
                                        when (effectiveQuality) {
                                            0 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                                            1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                                            2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                                            else -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                                        }
                                    }
                                    val thumbnailUrl = remember(illust) {
                                        illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium }
                                    }
                                    val singleModifier = Modifier
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
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null,
                                        ) { fullScreenPageIndex = 0 }

                                    PixivAsyncImage(
                                        model = singleUrl,
                                        thumbnailUrl = thumbnailUrl,
                                        contentDescription = illust.title,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = singleModifier,
                                    )
                                }
                            }
                        }

                        // 2. 作品信息与画师卡片（单一清晰大标题、数据指标、画师头像名称与下载全部/系列入口）
                        item(key = "illust_info_card", contentType = "info_card") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                    .padding(horizontal = 12.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    // 唯一大标题
                                    Text(
                                        text = illust.title,
                                        style = MiuixTheme.textStyles.title2,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 浏览、收藏、日期指标
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Show,
                                                contentDescription = strings.views,
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = illust.totalView.toString(),
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Favorites,
                                                contentDescription = strings.bookmarks,
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = illust.totalBookmarks.toString(),
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Recent,
                                                contentDescription = strings.publishDate,
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = illust.createDate.take(10),
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ID: ${illust.id}   ${illust.width}x${illust.height}",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 画师信息栏
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                            .clickable { onUserClick(illust.user.id) }
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
                                                style = MiuixTheme.textStyles.title4,
                                            )
                                            Text(
                                                text = "@${illust.user.account}",
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                        Icon(
                                            imageVector = MiuixIcons.Search,
                                            contentDescription = strings.author,
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    // 多页作品下载全部入口
                                    if (illust.pageCount > 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        BasicComponent(
                                            title = strings.downloadTaskFilterAll,
                                            summary = "${illust.pageCount} P",
                                            onClick = {
                                                if (isDownloading) return@BasicComponent
                                                coroutineScope.launch {
                                                    try {
                                                        isDownloading = true
                                                        val tasks = downloadRepository.downloadAllPages(
                                                            illust = illust,
                                                            onProgress = { completed, total ->
                                                                toastMessage = "${strings.downloadStatusDownloading} $completed/$total"
                                                            },
                                                            maxConcurrency = settings?.maxRunningTask ?: 3,
                                                        )
                                                        val successCount = tasks.count { it.status == DownloadStatus.Success }
                                                        val failedCount = tasks.count { it.status == DownloadStatus.Failed }
                                                        if (successCount > 0 && settings?.starAfterSave == true && !isBookmarked) {
                                                            coroutineScope.launch {
                                                                suspendRunCatchingNonCancel {
                                                                    bookmarkRepository.addBookmark(
                                                                        illustId = illust.id,
                                                                        isPrivate = settings.defaultPrivateLike,
                                                                    )
                                                                }.onSuccess {
                                                                    isBookmarked = true
                                                                }
                                                            }
                                                        }
                                                        toastMessage = when {
                                                            failedCount == 0 -> "${strings.downloadStatusSuccess}: $successCount/${tasks.size}"
                                                            successCount == 0 -> strings.downloadStatusFailed
                                                            else -> "${strings.downloadStatusSuccess}: $successCount, ${strings.downloadStatusFailed} $failedCount"
                                                        }
                                                    } finally {
                                                        isDownloading = false
                                                    }
                                                }
                                            },
                                        )
                                    }

                                    // 系列入口
                                    illust.series?.let { series ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        BasicComponent(
                                            title = series.title.orEmpty(),
                                            summary = "",
                                            onClick = { onIllustSeriesClick(series.id) },
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 简介与文案卡片
                        if (illust.caption.isNotBlank()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                        .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                        .padding(horizontal = 12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Text(
                                            text = strings.searchTargetTitleCaption,
                                            style = MiuixTheme.textStyles.title4,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HtmlCaptionText(
                                            html = illust.caption,
                                            onUserClick = onUserClick,
                                            onIllustClick = onIllustClick,
                                            onIllustSeriesClick = onIllustSeriesClick,
                                            onNovelClick = onNovelClick,
                                            onTagClick = onTagClick,
                                            style = MiuixTheme.textStyles.body2,
                                        )
                                    }
                                }
                            }
                        }

                        // 4. 标签卡片与胶囊包裹（Capsule Chips）
                        if (illust.tags.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                        .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                        .padding(horizontal = 12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Text(
                                            text = strings.tags,
                                            style = MiuixTheme.textStyles.title4,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            illust.tags.forEach { tag ->
                                                TagCapsuleChip(
                                                    tag = tag,
                                                    onClick = { onTagClick(tag.name) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. 互动操作卡片（评论与相关作品）
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                    .padding(horizontal = 12.dp),
                            ) {
                                BasicComponent(
                                    title = strings.commentsTitle,
                                    summary = "${illust.totalComments ?: 0}",
                                    onClick = { onCommentsClick(illust.id) },
                                )
                                BasicComponent(
                                    title = strings.relatedIllusts,
                                    summary = "",
                                    onClick = { onRelatedIllustsClick(illust.id) },
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                    }
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

        // 提取通用业务操作逻辑
        val performBookmark: () -> Unit = {
            illust?.let { targetIllust ->
                coroutineScope.launch {
                    try {
                        isBookmarkLoading = true
                        bookmarkError = null
                        val wasBookmarked = isBookmarked
                        suspendRunCatchingNonCancel {
                            if (wasBookmarked) {
                                bookmarkRepository.deleteBookmark(targetIllust.id)
                            } else {
                                val autoTags = if (settings?.autoTagWhenStar == true) {
                                    targetIllust.tags.map { tag -> tag.name }.take(10).joinToString(" ").ifBlank { null }
                                } else null
                                bookmarkRepository.addBookmark(
                                    illustId = targetIllust.id,
                                    isPrivate = settings?.defaultPrivateLike ?: false,
                                    tags = autoTags,
                                )
                            }
                        }.onSuccess {
                            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isBookmarked = !wasBookmarked
                            if (!wasBookmarked) {
                                if (settings?.saveAfterStar == true) {
                                    coroutineScope.launch {
                                        toastMessage = "${strings.downloadStatusDownloading}…"
                                        val task = downloadRepository.download(targetIllust, pageIndex = 0)
                                        toastMessage = when (task.status) {
                                            DownloadStatus.Success -> strings.downloadStatusSuccess
                                            DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                            else -> null
                                        }
                                    }
                                }
                                if (settings?.followAfterStar == true) {
                                    coroutineScope.launch {
                                        suspendRunCatchingNonCancel {
                                            bookmarkRepository.followUser(targetIllust.user.id)
                                        }
                                    }
                                }
                            }
                        }.onFailure { e ->
                            bookmarkError = e.message ?: strings.loadFailed
                        }
                    } finally {
                        isBookmarkLoading = false
                    }
                }
            }
        }

        val performDownload: () -> Unit = {
            if (!isDownloading && illust != null) {
                val targetIllust = illust
                coroutineScope.launch {
                    try {
                        isDownloading = true
                        toastMessage = "${strings.downloadStatusDownloading}…"
                        val task = downloadRepository.download(targetIllust, pageIndex = 0)
                        toastMessage = when (task.status) {
                            DownloadStatus.Success -> {
                                if (settings?.starAfterSave == true && !isBookmarked) {
                                    coroutineScope.launch {
                                        suspendRunCatchingNonCancel {
                                            bookmarkRepository.addBookmark(
                                                illustId = targetIllust.id,
                                                isPrivate = settings.defaultPrivateLike,
                                            )
                                        }.onSuccess {
                                            isBookmarked = true
                                        }
                                    }
                                }
                                strings.downloadStatusSuccess
                            }
                            DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                            else -> null
                        }
                    } finally {
                        isDownloading = false
                    }
                }
            }
        }

        // ── 统一锚点单层顶栏与液态玻璃操作菜单 ──
        IllustDetailTopBar(
            illust = illust,
            collapseProgress = collapseProgress,
            detailBackdrop = detailBackdrop,
            isBookmarked = isBookmarked,
            isBookmarkLoading = isBookmarkLoading,
            bookmarkHeartScale = bookmarkHeartScale,
            onBookmarkClick = performBookmark,
            isDownloading = isDownloading,
            onDownloadClick = performDownload,
            onBack = onBack,
            isBanned = isBanned,
            banRepository = banRepository,
            onBanSuccess = { isBanned = true },
            onToast = { toastMessage = it },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (fullScreenPageIndex != null && illust != null) {
            IllustFullScreenViewer(
                illust = illust,
                initialPage = fullScreenPageIndex!!,
                zoomQuality = settings?.zoomQuality ?: 0,
                downloadRepository = downloadRepository,
                onToast = { toastMessage = it },
                onDismiss = { fullScreenPageIndex = null },
            )
        }

        ToastMessage(
            message = toastMessage ?: bookmarkError,
            backdrop = detailBackdrop,
            onDismiss = {
                toastMessage = null
                bookmarkError = null
            },
        )
    }
}



/**
 * 胶囊标签 Chip：圆角胶囊背景包裹，清晰展示标签与翻译名称。
 */
@Composable
private fun TagCapsuleChip(
    tag: IllustTag,
    onClick: () -> Unit,
) {
    val tagText = buildString {
        append("#")
        append(tag.name)
        if (!tag.translatedName.isNullOrBlank() && tag.translatedName != tag.name) {
            append(" ")
            append(tag.translatedName)
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tagText,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
        )
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
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


