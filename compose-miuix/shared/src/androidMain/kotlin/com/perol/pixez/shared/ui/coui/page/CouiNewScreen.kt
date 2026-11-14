package com.perol.pixez.shared.ui.coui.page

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiPageScaffold
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.NewFeedRestrict
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState
import com.perol.pixez.shared.ui.state.rememberNewFeedState
import com.perol.pixez.shared.ui.state.shouldPreloadMore
import kotlinx.coroutines.flow.Flow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 动态页：复用 [rememberNewFeedState] 的登录/筛选/分页/追更逻辑，仅替换视觉壳。
 */
@Composable
internal fun CouiNewScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onLoginClick: () -> Unit,
    onSeriesClick: (Long) -> Unit,
    reselectFlow: Flow<Unit>?,
) {
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
    val toast = LocalToastSink.current
    val feed = rememberNewFeedState(reselectFlow = reselectFlow, onLoginClick = onLoginClick)
    val mainTabs = remember(strings) {
        listOf(
            CouiFilterChip(key = "feed", label = strings.tabNew),
            CouiFilterChip(key = "watchlist", label = strings.watchlistTitle),
        )
    }
    val restrictChips = remember(strings) {
        NewFeedRestrict.entries.map { CouiFilterChip(key = it.code, label = it.label(strings)) }
    }

    CouiLoginPromptDialog(
        show = feed.showLoginDialog,
        onDismiss = feed.dismissLoginDialog,
        onGoLogin = feed.goLogin,
    )
    CouiRemoveWatchlistDialog(
        series = feed.seriesToRemove,
        isRemoving = feed.isRemovingWatchlist,
        onDismiss = feed.dismissRemoveSeries,
        onConfirm = {
            feed.confirmRemoveSeries { error ->
                toast(
                    ToastData(
                        "${strings.watchlistTitle}${strings.failedSuffix}: " +
                            PixivApiErrorFormatter.format(error, strings),
                        ToastType.Error,
                    ),
                )
            }
        },
    )

    CouiPageScaffold(
        title = if (feed.selectedMainTab == 1) strings.watchlistTitle else strings.tabNew,
        actions = {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = feed.refresh,
            )
            if (feed.isLoggedIn == false) {
                CouiActionButton(
                    icon = MiuixIcons.Contacts,
                    contentDescription = strings.goLogin,
                    onClick = onLoginClick,
                )
            }
            if (feed.isLoggedIn == true) {
                val account = feed.currentAccount
                val targetUserId = account?.userId?.toLongOrNull()
                if (account != null && targetUserId != null) {
                    PixivAsyncImage(
                        model = account.userImage,
                        contentDescription = strings.myProfile,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onUserClick(targetUserId) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CouiTheme.colorScheme.background)
                .padding(padding),
        ) {
            CouiFilterChipBar(
                chips = mainTabs,
                selectedKey = if (feed.selectedMainTab == 1) "watchlist" else "feed",
                onSelect = { key -> feed.onMainTabSelected(if (key == "watchlist") 1 else 0) },
            )
            if (feed.isLoggedIn == true && feed.selectedMainTab == 0) {
                CouiFilterChipBar(
                    chips = restrictChips,
                    selectedKey = feed.selectedRestrict.code,
                    onSelect = { key ->
                        NewFeedRestrict.entries.firstOrNull { it.code == key }?.let(feed.onRestrictSelected)
                    },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (feed.isLoggedIn) {
                    false -> CouiStatePlaceholder(
                        message = strings.loginNewNeedLogin,
                        actionText = strings.btnGoLogin,
                        onAction = onLoginClick,
                    )

                    null -> CouiStatePlaceholder(message = strings.loading)

                    true -> if (feed.selectedMainTab == 1) {
                        CouiWatchlistContent(
                            feed = feed,
                            onSeriesClick = onSeriesClick,
                            bottomBarPadding = bottomBarPadding,
                        )
                    } else {
                        val currentIllusts = feed.listState.filteredIllusts
                        when {
                            currentIllusts == null && feed.listState.initialError == null -> CouiStatePlaceholder(
                                message = strings.loading,
                            )
                            currentIllusts == null && feed.listState.initialError != null -> CouiStatePlaceholder(
                                message = feed.listState.initialError?.message ?: strings.retry,
                                actionText = strings.retry,
                                onAction = feed.refresh,
                            )
                            currentIllusts != null && currentIllusts.isEmpty() -> CouiStatePlaceholder(
                                message = strings.loginNewEmpty,
                            )
                            currentIllusts != null -> CouiIllustGrid(
                                illusts = currentIllusts,
                                state = feed.gridState,
                                onIllustClick = onIllustClick,
                                contentPadding = PaddingValues(
                                    start = 10.dp,
                                    top = 4.dp,
                                    end = 10.dp,
                                    bottom = bottomBarPadding + 8.dp,
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

/** 追更页签：系列卡片列表 + 触底预加载 + 三态 footer。 */
@Composable
private fun CouiWatchlistContent(
    feed: com.perol.pixez.shared.ui.state.NewFeedState,
    onSeriesClick: (Long) -> Unit,
    bottomBarPadding: androidx.compose.ui.unit.Dp,
) {
    val strings = LocalStrings.current
    val series = feed.watchlistState.items
    val hasMore = feed.watchlistState.nextUrl != null
    val isLoadingMore = feed.watchlistState.isLoadingMore
    val loadMoreError = feed.watchlistState.loadMoreError

    when {
        series == null && feed.watchlistState.initialError == null -> CouiStatePlaceholder(
            message = strings.loading,
        )
        series == null && feed.watchlistState.initialError != null -> CouiStatePlaceholder(
            message = feed.watchlistState.initialError?.message ?: strings.retry,
            actionText = strings.retry,
            onAction = feed.refresh,
        )
        series != null && series.isEmpty() -> CouiStatePlaceholder(
            message = strings.watchlistEmpty,
        )
        series != null -> {
            val shouldLoadMore by remember(feed.watchlistListState, series.size, hasMore, isLoadingMore, loadMoreError) {
                derivedStateOf {
                    shouldPreloadMore(
                        itemCount = series.size,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        hasLoadMoreError = loadMoreError != null,
                        lastVisibleItemIndex = feed.watchlistListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                        thresholdItems = AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS,
                    )
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) feed.loadMoreWatchlist()
            }

            LazyColumn(
                state = feed.watchlistListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = 6.dp,
                    end = 10.dp,
                    bottom = bottomBarPadding + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = series, key = { it.id }, contentType = { "coui_watchlist" }) { item ->
                    CouiWatchlistRow(
                        item = item,
                        onClick = { onSeriesClick(item.id) },
                        onRemoveClick = { feed.requestRemoveSeries(item) },
                    )
                }
                val footerState = PagingFooterState(
                    hasMore = hasMore,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    hasItems = series.isNotEmpty(),
                )
                if (footerState.phase != PagingFooterPhase.Hidden) {
                    item(key = "coui_watchlist_footer", contentType = "coui_watchlist_footer") {
                        CouiPagingFooter(state = footerState, onRetry = feed.loadMoreWatchlist)
                    }
                }
            }
        }
    }
}

/** COUI 追更条目：封面 + 标题/作者/更新时间 + 取消追更。 */
@Composable
private fun CouiWatchlistRow(
    item: MangaSeries,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
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
                    .width(84.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = item.publishedContentCount.toString(),
                    kind = CouiTextKind.ItemSummary,
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 1,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Text(
                text = item.user?.name ?: "",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = item.lastPublishedContentDatetime?.take(16)?.replace('T', ' ') ?: "",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        CouiActionButton(
            icon = MiuixIcons.Delete,
            contentDescription = strings.watchlistRemoveConfirm,
            onClick = onRemoveClick,
        )
    }
}

/** 未登录提示：使用 COUI 官方 Dialog（材质不可用时自动退回不透明弹窗）。 */
@Composable
private fun CouiLoginPromptDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onGoLogin: () -> Unit,
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        visible = show,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogNeedLogin,
        message = strings.dialogNeedLoginSummary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.btnGoLogin,
            onClick = onGoLogin,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
        ),
        dismissAction = CouiDialogAction(text = strings.btnCancelLogin, onClick = onDismiss),
    ) {}
}

/** 取消追更确认：破坏性主操作。 */
@Composable
private fun CouiRemoveWatchlistDialog(
    series: MangaSeries?,
    isRemoving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        visible = series != null,
        material = couiDialogMaterialOrNull(),
        title = strings.watchlistTitle,
        message = strings.watchlistRemoveConfirm,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.confirm,
            onClick = onConfirm,
            enabled = !isRemoving,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(danger = true),
        ),
        dismissAction = CouiDialogAction(text = strings.cancel, onClick = onDismiss),
    ) {}
}

