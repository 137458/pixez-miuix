package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import top.yukonga.miuix.kmp.squircle.squircleClip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.derivedStateOf
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PagingLoadMoreFooter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import androidx.compose.ui.text.style.TextOverflow
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.LiquidFilterBar
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.NewFeedRestrict
import com.perol.pixez.shared.ui.state.rememberNewFeedState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import com.perol.pixez.shared.ui.components.blurBackdropSource

/**
 * 最新/关注页：「动态 / 追更」双页签。
 *
 * 动态页签展示已登录用户关注画师的最新插画（未登录时显示登录入口，支持 all/public/private 筛选）；
 * 追更页签展示漫画系列追更列表，支持分页、点击进入系列页与取消追更。
 *
 * 业务与竞态逻辑见 [rememberNewFeedState]，本函数只负责 MIUIX 视觉壳。
 */
@Composable
fun NewScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onLoginClick: () -> Unit,
    onSeriesClick: (Long) -> Unit = {},
    reselectFlow: kotlinx.coroutines.flow.Flow<Unit>? = null,
) {
    val strings = LocalStrings.current
    val watchlistToast = LocalToastSink.current
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val feed = rememberNewFeedState(reselectFlow = reselectFlow, onLoginClick = onLoginClick)
    val currentRestrict = feed.selectedRestrict

    // 未登录提示对话框：位于 Scaffold 外层，确保能覆盖整个页面。
    OverlayDialog(
        title = strings.dialogNeedLogin,
        summary = strings.dialogNeedLoginSummary,
        show = feed.showLoginDialog,
        onDismissRequest = feed.dismissLoginDialog,
    ) {
        TextButton(
            text = strings.btnCancelLogin,
            onClick = feed.dismissLoginDialog,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = strings.btnGoLogin,
            onClick = feed.goLogin,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = if (feed.selectedMainTab == 1) strings.watchlistTitle else strings.tabNew,
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                        actions = {
                            IconButton(
                                onClick = feed.refresh,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Refresh,
                                    contentDescription = strings.refresh,
                                )
                            }
                            if (feed.isLoggedIn == false) {
                                Button(
                                    onClick = onLoginClick,
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.padding(end = 12.dp),
                                ) {
                                    Text(strings.btnGoLogin)
                                }
                            }
                            if (feed.isLoggedIn == true) {
                                val account = feed.currentAccount
                                val targetUserId = account?.userId?.toLongOrNull()
                                if (account != null && targetUserId != null) {
                                    IconButton(
                                        onClick = { onUserClick(targetUserId) },
                                    ) {
                                        PixivAsyncImage(
                                            model = account.userImage,
                                            contentDescription = strings.myProfile,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape),
                                        )
                                    }
                                }
                            }
                        },
                    )
                    // 「动态 / 追更」主页签（参照 HistoryScreen 的 TabRow 用法）。
                    val mainTabs = remember(strings) {
                        listOf(strings.tabNew, strings.watchlistTitle)
                    }
                    TabRow(
                        tabs = mainTabs,
                        selectedTabIndex = feed.selectedMainTab,
                        onTabSelected = feed.onMainTabSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (feed.isLoggedIn == true && feed.selectedMainTab == 0) {
                        LiquidFilterBar(
                            items = NewFeedRestrict.entries,
                            selectedItem = currentRestrict,
                            onItemSelected = feed.onRestrictSelected,
                            labelProvider = { it.label(strings) },
                            backdrop = backdrop,
                            isScrollable = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            when (feed.isLoggedIn) {
                false -> {
                    EmptyPlaceholder(
                        message = strings.loginNewNeedLogin,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                }

                null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )

                true -> {
                    if (feed.selectedMainTab == 1) {
                        // 追更页签：漫画系列追更列表。
                        val currentSeries = feed.watchlistState.items
                        when {
                            currentSeries == null && feed.watchlistState.initialError == null -> LoadingPlaceholder(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                            currentSeries == null && feed.watchlistState.initialError != null -> ErrorPlaceholder(
                                error = feed.watchlistState.initialError,
                                onRetry = feed.refresh,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                            currentSeries != null -> {
                                if (currentSeries.isEmpty()) {
                                    EmptyPlaceholder(
                                        message = strings.watchlistEmpty,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(paddingValues),
                                    )
                                } else {
                                    PullToRefresh(
                                        isRefreshing = feed.watchlistRefreshing,
                                        onRefresh = feed.refresh,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                        topAppBarScrollBehavior = scrollBehavior,
                                    ) {
                                        WatchlistSeriesList(
                                            series = currentSeries,
                                            state = feed.watchlistListState,
                                            onSeriesClick = onSeriesClick,
                                            onRemoveClick = feed.requestRemoveSeries,
                                            onLoadMore = feed.loadMoreWatchlist,
                                            isLoadingMore = feed.watchlistState.isLoadingMore,
                                            loadMoreError = feed.watchlistState.loadMoreError,
                                            hasMore = feed.watchlistState.nextUrl != null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                                            contentPadding = PaddingValues(
                                                start = 16.dp,
                                                top = paddingValues.calculateTopPadding() + 8.dp,
                                                end = 16.dp,
                                                bottom = LocalBottomBarContentPadding.current,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val currentIllusts = feed.listState.filteredIllusts
                        when {
                            currentIllusts == null && feed.listState.initialError == null -> LoadingPlaceholder(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                            currentIllusts == null && feed.listState.initialError != null -> ErrorPlaceholder(
                                error = feed.listState.initialError,
                                onRetry = feed.refresh,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                            currentIllusts != null -> {
                                if (currentIllusts.isEmpty()) {
                                    EmptyPlaceholder(
                                        message = strings.loginNewEmpty,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(paddingValues),
                                    )
                                } else {
                                    PullToRefresh(
                                        isRefreshing = feed.isManualRefreshing,
                                        onRefresh = feed.refresh,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                        topAppBarScrollBehavior = scrollBehavior,
                                    ) {
                                        IllustStaggeredGrid(
                                            illusts = currentIllusts,
                                            state = feed.gridState,
                                            onIllustClick = onIllustClick,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                                            contentPadding = PaddingValues(
                                                start = 8.dp,
                                                top = paddingValues.calculateTopPadding() + 8.dp,
                                                end = 8.dp,
                                                bottom = LocalBottomBarContentPadding.current,
                                            ),
                                            hasMore = feed.listState.nextUrl != null,
                                            isLoadingMore = feed.listState.isLoadingMore,
                                            loadMoreError = feed.listState.loadMoreError,
                                            onLoadMore = feed.loadMore,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 取消追更二次确认（参照 HistoryScreen 的 OverlayDialog 确认模式）。
            OverlayDialog(
                title = strings.watchlistTitle,
                summary = strings.watchlistRemoveConfirm,
                show = feed.seriesToRemove != null,
                onDismissRequest = feed.dismissRemoveSeries,
            ) {
                TextButton(
                    text = strings.cancel,
                    onClick = feed.dismissRemoveSeries,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = strings.confirm,
                    enabled = !feed.isRemovingWatchlist,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        feed.confirmRemoveSeries { error ->
                            watchlistToast(
                                ToastData(
                                    "${strings.watchlistTitle}${strings.failedSuffix}: " +
                                        PixivApiErrorFormatter.format(error, strings),
                                    ToastType.Error,
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * 追更列表：系列卡片纵向排布 + 触底分页尾部指示。
 */
@Composable
private fun WatchlistSeriesList(
    series: List<MangaSeries>,
    state: androidx.compose.foundation.lazy.LazyListState,
    onSeriesClick: (Long) -> Unit,
    onRemoveClick: (MangaSeries) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    hasMore: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val shouldLoadMore by remember(state, series.size, hasMore, isLoadingMore, loadMoreError) {
        derivedStateOf {
            if (!hasMore || isLoadingMore || loadMoreError != null || series.isEmpty()) {
                false
            } else {
                val lastVisibleIndex = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex >= series.size - AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS
            }
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(
            items = series,
            key = { it.id },
            contentType = { "watchlist_series" },
        ) { item ->
            WatchlistSeriesRow(
                item = item,
                onClick = { onSeriesClick(item.id) },
                onRemoveClick = { onRemoveClick(item) },
            )
        }

        // 分页 footer 三态与其余列表页共用同一组件（UI-3）：Hidden 时不挂 item。
        val footerState = PagingFooterState(
            hasMore = hasMore,
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
            hasItems = series.isNotEmpty(),
        )
        if (footerState.phase != PagingFooterPhase.Hidden) {
            item(key = "watchlist_pagination_footer", contentType = "watchlist_pagination_footer") {
                PagingLoadMoreFooter(
                    state = footerState,
                    onRetry = onLoadMore,
                )
            }
        }
    }
}

/**
 * 追更系列条目：封面（叠加已发布话数徽章）+ 标题/作者/最新发布时间 + 取消追更操作。
 * 卡片布局对齐旧 Flutter watchlist.dart 的 MangaSeriesItem。
 */
@Composable
private fun WatchlistSeriesRow(
    item: MangaSeries,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .squircleClip(12.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            PixivAsyncImage(
                model = item.url,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(96.dp)
                    .height(128.dp)
                    .squircleClip(8.dp),
            )
            Text(
                text = item.publishedContentCount.toString(),
                style = MiuixTheme.textStyles.footnote1,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .squircleClip(8.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MiuixTheme.textStyles.body2,
                maxLines = 2,
            )
            Text(
                text = item.user?.name ?: "",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = item.lastPublishedContentDatetime?.take(16)?.replace('T', ' ') ?: "",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = MiuixIcons.Delete,
                contentDescription = strings.watchlistRemoveConfirm,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
