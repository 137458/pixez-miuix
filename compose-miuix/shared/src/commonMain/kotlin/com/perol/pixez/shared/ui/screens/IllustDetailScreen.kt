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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
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
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.buildIllustCopyInfo
import com.perol.pixez.shared.ui.components.buildIllustShareLink
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            result.isSuccess && illust != null -> when {
                isBanned && !isTempView -> BanPage(
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
                                        0 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                                        1 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                                        2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                                        else -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
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

                                PixivAsyncImage(
                                    model = pageUrl,
                                    thumbnailUrl = thumbnailUrl,
                                    contentDescription = "${illust.title} ($pageIndex)",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = pageModifier,
                                )
                                if (pageIndex < illust.metaPages.lastIndex) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        } else {
                            item(key = "single_page", contentType = "single_page") {
                                val effectiveQuality = remember(illust.type, settings?.pictureQuality, settings?.mangaQuality, settings?.changeVersion) {
                                    if (illust.type == "manga") {
                                        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
                                    } else {
                                        settings?.pictureQuality ?: 0
                                    }
                                }
                                val singleUrl = remember(illust, effectiveQuality) {
                                    when (effectiveQuality) {
                                        0 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                                        1 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                                        2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                                        else -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
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
                                                            illust,
                                                            onProgress = { completed, total ->
                                                                toastMessage = "${strings.downloadStatusDownloading} $completed/$total"
                                                            },
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

        // ── 统一锚点单层顶栏（物理坐标绝对恒定，材质与标题平滑演变，彻底根除跳变与闪烁） ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.TopCenter,
        ) {
            // 1. 底层全宽毛玻璃背景（随滚动进度平滑淡入，往回划平滑淡出，始终驻留GPU层）
            BlurredBar(
                backdrop = detailBackdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = collapseProgress },
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp),
                )
            }

            // 2. 顶栏主体内容行（单套恒定按钮与动态平滑标题）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bubbleAlpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
                val dynamicIconTint = androidx.compose.ui.graphics.lerp(
                    Color.White,
                    MiuixTheme.colorScheme.onSurface,
                    collapseProgress,
                )

                // ── 返回按钮（物理锚点固定，微气泡背景平滑消退，往回划平滑复现） ──
                TooltipBox(text = strings.back) {
                    val backInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val isBackPressed by backInteractionSource.collectIsPressedAsState()
                    val backPressScale = remember { androidx.compose.animation.core.Animatable(1f) }
                    LaunchedEffect(isBackPressed) {
                        backPressScale.animateTo(
                            targetValue = if (isBackPressed) 0.92f else 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.7f,
                                stiffness = 500f,
                            ),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = backPressScale.value
                                scaleY = backPressScale.value
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // 独立圆形微气泡背景（始终驻留，仅在 graphicsLayer 驱动透明度，绝不破坏节点树）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = bubbleAlpha }
                                .liquidGlass(
                                    backdrop = detailBackdrop,
                                    shape = CircleShape,
                                    blurRadius = 16.dp,
                                    tintColor = Color.Black,
                                    tintAlpha = 0.38f,
                                ),
                        )

                        // 交互与前景层
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = backInteractionSource,
                                    indication = null,
                                    onClick = onBack,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                                tint = dynamicIconTint,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                // ── 中间标题区（随上滑折叠平滑淡入作品标题） ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .graphicsLayer {
                            alpha = ((collapseProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = illust?.title.orEmpty(),
                        style = MiuixTheme.textStyles.title3,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }

                // ── 右侧操作按钮组（收藏、下载、更多） ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. 收藏按钮
                    TooltipBox(text = if (isBookmarked) strings.bookmarked else strings.bookmark) {
                        val favInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val isFavPressed by favInteractionSource.collectIsPressedAsState()
                        val favPressScale = remember { androidx.compose.animation.core.Animatable(1f) }
                        LaunchedEffect(isFavPressed) {
                            favPressScale.animateTo(
                                targetValue = if (isFavPressed) 0.90f else 1f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.7f,
                                    stiffness = 500f,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = favPressScale.value
                                    scaleY = favPressScale.value
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = bubbleAlpha }
                                    .liquidGlass(
                                        backdrop = detailBackdrop,
                                        shape = CircleShape,
                                        blurRadius = 16.dp,
                                        tintColor = Color.Black,
                                        tintAlpha = 0.38f,
                                    ),
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = favInteractionSource,
                                        indication = null,
                                        enabled = !isBookmarkLoading && illust != null,
                                        onClick = performBookmark,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                                    contentDescription = if (isBookmarked) strings.bookmarked else strings.bookmark,
                                    tint = if (isBookmarked) Color(0xFFFF4D6A) else dynamicIconTint,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            scaleX = bookmarkHeartScale.value
                                            scaleY = bookmarkHeartScale.value
                                        },
                                )
                            }
                        }
                    }

                    // 2. 下载按钮
                    TooltipBox(text = strings.download) {
                        val dlInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val isDlPressed by dlInteractionSource.collectIsPressedAsState()
                        val dlPressScale = remember { androidx.compose.animation.core.Animatable(1f) }
                        LaunchedEffect(isDlPressed) {
                            dlPressScale.animateTo(
                                targetValue = if (isDlPressed) 0.90f else 1f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.7f,
                                    stiffness = 500f,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = dlPressScale.value
                                    scaleY = dlPressScale.value
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = bubbleAlpha }
                                    .liquidGlass(
                                        backdrop = detailBackdrop,
                                        shape = CircleShape,
                                        blurRadius = 16.dp,
                                        tintColor = Color.Black,
                                        tintAlpha = 0.38f,
                                    ),
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = dlInteractionSource,
                                        indication = null,
                                        enabled = !isDownloading && illust != null,
                                        onClick = performDownload,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isDownloading) {
                                    InfiniteProgressIndicator(
                                        color = if (collapseProgress > 0.5f) MiuixTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = MiuixIcons.Download,
                                        contentDescription = strings.download,
                                        tint = dynamicIconTint,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 3. 更多菜单按钮
                    if (illust != null) {
                        TooltipBox(text = strings.menuMoreActions) {
                            val moreInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val isMorePressed by moreInteractionSource.collectIsPressedAsState()
                            val morePressScale = remember { androidx.compose.animation.core.Animatable(1f) }
                            LaunchedEffect(isMorePressed) {
                                morePressScale.animateTo(
                                    targetValue = if (isMorePressed) 0.90f else 1f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = 0.7f,
                                        stiffness = 500f,
                                    ),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        scaleX = morePressScale.value
                                        scaleY = morePressScale.value
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { alpha = bubbleAlpha }
                                        .liquidGlass(
                                            backdrop = detailBackdrop,
                                            shape = CircleShape,
                                            blurRadius = 16.dp,
                                            tintColor = Color.Black,
                                            tintAlpha = 0.38f,
                                        ),
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = moreInteractionSource,
                                            indication = null,
                                            onClick = { showMoreMenu = !showMoreMenu },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.More,
                                        contentDescription = strings.menuMoreActions,
                                        tint = dynamicIconTint,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 更多操作液态玻璃浮动菜单 ──
        if (illust != null) {
            val currentIllust = illust
            PlatformBackHandler(enabled = showMoreMenu) {
                showMoreMenu = false
            }

            AnimatedVisibility(
                visible = showMoreMenu,
                enter = fadeIn(androidx.compose.animation.core.spring(dampingRatio = 0.8f)) +
                    scaleIn(
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.65f, stiffness = 420f),
                        initialScale = 0.80f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.95f, 0f),
                    ),
                exit = fadeOut(androidx.compose.animation.core.spring(dampingRatio = 0.9f)) +
                    scaleOut(
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f),
                        targetScale = 0.85f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.95f, 0f),
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 全屏透明遮罩（点击外部关闭菜单）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = { showMoreMenu = false },
                            ),
                    )

                    // 悬浮液态玻璃菜单卡片（位于右上角更多按钮正下方）
                    val menuCornerRadius = 18.dp
                    val menuShape = remember { RoundedCornerShape(menuCornerRadius) }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 58.dp, end = 16.dp)
                            .widthIn(min = 160.dp, max = 200.dp)
                            .liquidGlass(
                                backdrop = detailBackdrop,
                                shape = menuShape,
                                blurRadius = 18.dp,
                                tintColor = Color.Black,
                                tintAlpha = 0.45f,
                            )
                            .squircleBorder(
                                width = 0.6.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                cornerRadius = menuCornerRadius,
                            )
                            .clip(menuShape)
                            .padding(vertical = 4.dp),
                    ) {
                        LiquidMenuItem(
                            icon = MiuixIcons.Copy,
                            text = strings.menuCopyInfo,
                            onClick = {
                                showMoreMenu = false
                                val text = buildIllustCopyInfo(currentIllust)
                                runCatching { clipboard.copy(text) }.fold(
                                    onSuccess = { toastMessage = strings.copiedToClipboard },
                                    onFailure = { e -> toastMessage = "${strings.copy}${strings.loadFailed}: ${e.message}" },
                                )
                            },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        LiquidMenuItem(
                            icon = MiuixIcons.Show,
                            text = strings.menuCopyImage,
                            onClick = {
                                showMoreMenu = false
                                coroutineScope.launch {
                                    suspendRunCatchingNonCancel {
                                        val imageLoader = coil3.SingletonImageLoader.get(context)
                                        val diskCache = imageLoader.diskCache
                                        val candidateUrls = listOf(
                                            currentIllust.imageUrls.large,
                                            currentIllust.imageUrls.medium,
                                            currentIllust.imageUrls.squareMedium,
                                        )
                                        var bytes: ByteArray? = null
                                        for (candidateUrl in candidateUrls) {
                                            diskCache?.openSnapshot(candidateUrl)?.use { snapshot ->
                                                val file = snapshot.data.toFile()
                                                if (file.exists() && file.length() > 0) {
                                                    bytes = file.readBytes()
                                                }
                                            }
                                            if (bytes != null) break
                                        }
                                        bytes?.let { clipboard.copyImage(it) } ?: throw IllegalStateException("未找到图片缓存")
                                    }.fold(
                                        onSuccess = { toastMessage = strings.imageCopySuccess },
                                        onFailure = { e -> toastMessage = "${strings.menuCopyImage}: ${e.message}" },
                                    )
                                }
                            },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        LiquidMenuItem(
                            icon = MiuixIcons.Link,
                            text = strings.menuCopyLink,
                            onClick = {
                                showMoreMenu = false
                                val link = buildIllustShareLink(currentIllust)
                                runCatching { clipboard.copy(link) }.fold(
                                    onSuccess = { toastMessage = strings.copiedToClipboard },
                                    onFailure = { e -> toastMessage = "${strings.copy}${strings.loadFailed}: ${e.message}" },
                                )
                            },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        LiquidMenuItem(
                            icon = MiuixIcons.Share,
                            text = strings.share,
                            onClick = {
                                showMoreMenu = false
                                val link = buildIllustShareLink(currentIllust)
                                runCatching { share.share(link, currentIllust.title) }.fold(
                                    onSuccess = { toastMessage = strings.share },
                                    onFailure = { e -> toastMessage = "${strings.share}: ${e.message}" },
                                )
                            },
                        )
                        if (!isBanned) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .height(0.5.dp)
                                    .background(Color.White.copy(alpha = 0.10f)),
                            )
                            LiquidMenuItem(
                                icon = MiuixIcons.Blocklist,
                                text = strings.menuBanWork,
                                onClick = {
                                    showMoreMenu = false
                                    coroutineScope.launch {
                                        suspendRunCatchingNonCancel {
                                            banRepository.insertBanIllust(currentIllust.id, currentIllust.title)
                                        }.fold(
                                            onSuccess = {
                                                isBanned = true
                                                toastMessage = strings.menuBanWork
                                            },
                                            onFailure = { e -> toastMessage = "${strings.menuBanWork}: ${e.message}" },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (fullScreenPageIndex != null && illust != null) {
            IllustFullScreenViewer(
                illust = illust,
                initialPage = fullScreenPageIndex!!,
                zoomQuality = settings?.zoomQuality ?: 0,
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
 * 插画全屏/高清缩放预览组件：
 * 支持双指平滑手势缩放 (Pinch-to-zoom)、双击放大/复原 (Double-tap-to-zoom)、拖拽平移 (Pan) 以及多 P 左右切页。
 * 根据 [SettingsRepository.zoomQuality] 加载对应画质大图，优先使用已有内存缓存作为过渡底图。
 */
@Composable
private fun IllustFullScreenViewer(
    illust: Illust,
    initialPage: Int,
    zoomQuality: Int,
    onDismiss: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val pageCount = if (illust.metaPages.isNotEmpty()) illust.metaPages.size else 1
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )
    var currentPageScale by remember { mutableFloatStateOf(1f) }
    var showControls by remember { mutableStateOf(true) }

    PlatformBackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (pageCount > 1) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentPageScale <= 1.05f,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                val page = illust.metaPages[pageIndex]
                val zoomUrl = remember(page, zoomQuality) {
                    when (zoomQuality) {
                        0 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                        1 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
                        2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                        else -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                    }
                }
                val thumbnailUrl = remember(page) {
                    page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
                }

                ZoomableImage(
                    model = zoomUrl,
                    thumbnailUrl = thumbnailUrl,
                    contentDescription = "${illust.title} ($pageIndex)",
                    onTap = { showControls = !showControls },
                    onScaleChanged = { scale ->
                        if (pagerState.currentPage == pageIndex) {
                            currentPageScale = scale
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            val singleZoomUrl = remember(illust, zoomQuality) {
                when (zoomQuality) {
                    0 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                    1 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                    2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                    else -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                }
            }
            val thumbnailUrl = remember(illust) {
                illust.imageUrls.medium.ifBlank { illust.imageUrls.squareMedium }
            }

            ZoomableImage(
                model = singleZoomUrl,
                thumbnailUrl = thumbnailUrl,
                contentDescription = illust.title,
                onTap = { showControls = !showControls },
                onScaleChanged = { scale -> currentPageScale = scale },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 顶部浮层：返回按钮与页码指示器（带淡入淡出动画）
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = strings.back,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                if (pageCount > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount",
                            style = MiuixTheme.textStyles.body2,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 支持双指手势平滑缩放、双击放大/重置与边界限制拖拽平移的图片组件。
 */
@Composable
private fun ZoomableImage(
    model: Any?,
    thumbnailUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onScaleChanged: ((Float) -> Unit)? = null,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                            onScaleChanged?.invoke(1f)
                        } else {
                            scale = 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val targetOffset = (center - tapOffset) * 1.5f
                            val maxOffsetX = (size.width * 1.5f) / 2f
                            val maxOffsetY = (size.height * 1.5f) / 2f
                            offset = Offset(
                                targetOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                targetOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
                            )
                            onScaleChanged?.invoke(2.5f)
                        }
                    },
                    onTap = { onTap() },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    onScaleChanged?.invoke(newScale)
                    if (newScale > 1.05f) {
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        val newOffsetX = (offset.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX)
                        val newOffsetY = (offset.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
                        offset = Offset(newOffsetX, newOffsetY)
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        PixivAsyncImage(
            model = model,
            thumbnailUrl = thumbnailUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
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

/**
 * 液态玻璃菜单单项组件。
 */
@Composable
private fun LiquidMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPressed) Color.White.copy(alpha = 0.12f) else Color.Transparent,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = Color.White,
        )
    }
}

