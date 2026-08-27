package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
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
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
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
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
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
        val detailResult = suspendRunCatchingNonCancel { repository.getIllustDetail(illustId) }
        // 关键容灾：后台刷新若遇网络抖动或异常，保留已有有效作品数据展示，绝不覆盖为 Failure 造成全屏灰色占位
        if (detailResult.isFailure && (value?.getOrNull() != null || cachedIllust != null)) {
            detailResult.exceptionOrNull()?.let { e ->
                io.github.aakira.napier.Napier.w("后台更新作品详情失败: ${e.message}", e, tag = "IllustDetail")
            }
        } else {
            value = detailResult
        }
    }

    val result = state.value
    val illust = result?.getOrNull() ?: cachedIllust

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
    var showActionMenu by rememberSaveable { mutableStateOf(false) }
    var isBanned by rememberSaveable(illustId) { mutableStateOf(false) }
    var isTempView by rememberSaveable(illustId) { mutableStateOf(false) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    // 页面进入或作品 ID 变化时，查询本地屏蔽记录；数据库异常时保持未屏蔽，避免崩溃。
    LaunchedEffect(illustId) {
        suspendRunCatchingNonCancel { banRepository.isBanIllust(illustId) }
            .onSuccess { isBanned = it }
    }

    val detailBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        when {
            illust != null -> when {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(detailBackdrop),
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
                                        1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                                        2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                                        else -> page.imageUrls?.large.orEmpty()
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
                                        1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                                        2 -> illust.imageUrls.medium
                                        else -> illust.imageUrls.large
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
                                        Text(
                                            text = illust.caption,
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
            result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            result?.isFailure == true -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier.fillMaxSize(),
            )
            else -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        }

        // 浮动悬浮顶部操作栏（半透明暗色胶囊/圆形按钮，平滑渐变，不遮挡大图，在任何明暗底色下清晰可见）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 返回按钮
            TooltipBox(
                text = strings.back,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            backdrop = detailBackdrop,
                            shape = CircleShape,
                            blurRadius = 16.dp,
                            tintColor = Color.Black,
                            tintAlpha = 0.35f,
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = strings.back,
                        tint = Color.White,
                    )
                }
            }

            // 右侧操作按钮组（收藏、下载、更多）
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 收藏按钮
                TooltipBox(text = if (isBookmarked) strings.bookmarked else strings.bookmark) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .liquidGlass(
                                backdrop = detailBackdrop,
                                shape = CircleShape,
                                blurRadius = 16.dp,
                                tintColor = Color.Black,
                                tintAlpha = 0.35f,
                            )
                            .clip(CircleShape)
                            .clickable(
                                enabled = !isBookmarkLoading && illust != null,
                                onClick = {
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
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                            contentDescription = if (isBookmarked) strings.bookmarked else strings.bookmark,
                            tint = if (isBookmarked) Color(0xFFFF4D6A) else Color.White,
                        )
                    }
                }

                // 下载按钮
                TooltipBox(text = strings.download) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .liquidGlass(
                                backdrop = detailBackdrop,
                                shape = CircleShape,
                                blurRadius = 16.dp,
                                tintColor = Color.Black,
                                tintAlpha = 0.35f,
                            )
                            .clip(CircleShape)
                            .clickable(
                                enabled = !isDownloading && illust != null,
                                onClick = {
                                    if (isDownloading || illust == null) return@clickable
                                    coroutineScope.launch {
                                        try {
                                            isDownloading = true
                                            toastMessage = "${strings.downloadStatusDownloading}…"
                                            val task = downloadRepository.download(illust, pageIndex = 0)
                                            toastMessage = when (task.status) {
                                                DownloadStatus.Success -> {
                                                    if (settings?.starAfterSave == true && !isBookmarked) {
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
                                                    strings.downloadStatusSuccess
                                                }
                                                DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                                else -> null
                                            }
                                        } finally {
                                            isDownloading = false
                                        }
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = strings.download,
                            tint = Color.White,
                        )
                    }
                }

                // 更多菜单按钮
                TooltipBox(text = strings.menuMoreActions) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .liquidGlass(
                                backdrop = detailBackdrop,
                                shape = CircleShape,
                                blurRadius = 16.dp,
                                tintColor = Color.Black,
                                tintAlpha = 0.35f,
                            )
                            .clip(CircleShape)
                            .clickable(
                                enabled = illust != null,
                                onClick = { showActionMenu = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = strings.menuMoreActions,
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        ToastMessage(
            message = toastMessage ?: bookmarkError,
            onDismiss = {
                toastMessage = null
                bookmarkError = null
            },
        )

        illust?.let {
            IllustActionMenu(
                show = showActionMenu,
                showBan = !isBanned,
                onDismissRequest = { showActionMenu = false },
                onCopyInfo = {
                    showActionMenu = false
                    val text = buildIllustCopyInfo(it)
                    runCatching { clipboard.copy(text) }.fold(
                        onSuccess = { toastMessage = strings.copiedToClipboard },
                        onFailure = { e -> toastMessage = "${strings.copy}: ${e.message}" },
                    )
                },
                onCopyLink = {
                    showActionMenu = false
                    val link = buildIllustShareLink(it)
                    runCatching { clipboard.copy(link) }.fold(
                        onSuccess = { toastMessage = strings.copiedToClipboard },
                        onFailure = { e -> toastMessage = "${strings.copy}: ${e.message}" },
                    )
                },
                onShareLink = {
                    showActionMenu = false
                    val link = buildIllustShareLink(it)
                    runCatching { share.share(link, it.title) }.fold(
                        onSuccess = { toastMessage = strings.share },
                        onFailure = { e -> toastMessage = "${strings.share}: ${e.message}" },
                    )
                },
                onBan = {
                    showActionMenu = false
                    coroutineScope.launch {
                        suspendRunCatchingNonCancel {
                            banRepository.insertBanIllust(it.id, it.title)
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
