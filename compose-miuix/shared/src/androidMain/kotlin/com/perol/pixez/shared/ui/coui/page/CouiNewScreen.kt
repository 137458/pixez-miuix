package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.actionFailureToast
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiSegmentedTabs
import com.perol.pixez.shared.ui.coui.component.CouiTopBarSegmentedTabs
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiMainScaffold
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.NewFeedRestrict
import com.perol.pixez.shared.ui.state.NewFeedState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState
import com.perol.pixez.shared.ui.state.rememberNewFeedState
import com.perol.pixez.shared.ui.state.shouldPreloadMore
import kotlinx.coroutines.flow.Flow

/**
 * COUI 动态页：复用 [rememberNewFeedState] 的登录/筛选/分页/追更逻辑，仅替换视觉壳。
 *
 * 骨架用一级页 [CouiMainScaffold]；主页签与动态范围两条 chip 栏作为正文头部交给列表，
 * 因此能随内容滚进折叠顶栏的覆盖区。首屏三态用 core 占位组件，护栏只出现在列表
 * `contentPadding` 与非滚动正文的 `.padding()` 上，页面不再自行铺底或扣安全区。
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
    val toast = LocalToastSink.current
    val feed = rememberNewFeedState(reselectFlow = reselectFlow, onLoginClick = onLoginClick)
    val mainTabLabels = remember(strings) {
        listOf(strings.tabNew, strings.watchlistTitle)
    }
    val restrictLabels = remember(strings) {
        NewFeedRestrict.entries.map { it.label(strings) }
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
                toast(actionFailureToast(strings.watchlistTitle, error, strings))
            }
        },
    )

    // 动态范围次级筛选：仅在已登录且处于动态页签时展示在列表顶部。
    val headerContent: (@Composable () -> Unit)? = if (feed.isLoggedIn == true && feed.selectedMainTab == 0) {
        {
            Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                CouiSegmentedTabs(
                    items = restrictLabels,
                    selectedIndex = NewFeedRestrict.entries.indexOf(feed.selectedRestrict).coerceAtLeast(0),
                    onSelect = { idx ->
                        NewFeedRestrict.entries.getOrNull(idx)?.let(feed.onRestrictSelected)
                    },
                    small = true,
                )
            }
        }
    } else {
        null
    }

    CouiMainScaffold(
        title = if (feed.selectedMainTab == 1) strings.watchlistTitle else strings.tabNew,
        actions = {
            CouiTopBarIconButton(
                onClick = feed.refresh,
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
            )
            if (feed.isLoggedIn == false) {
                CouiTopBarIconButton(
                    onClick = onLoginClick,
                    icon = CouiIcons.Contacts,
                    contentDescription = strings.goLogin,
                )
            }
            if (feed.isLoggedIn == true) {
                val account = feed.currentAccount
                val targetUserId = account?.userId?.toLongOrNull()
                if (account != null && targetUserId != null) {
                    val pressInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = pressInteraction, list = false)
                            .clickable(
                                interactionSource = pressInteraction,
                                indication = null,
                            ) { onUserClick(targetUserId) },
                        contentAlignment = Alignment.Center,
                    ) {
                        PixivAsyncImage(
                            model = account.userImage,
                            contentDescription = strings.myProfile,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
        },
        topBarBottom = {
            CouiTopBarSegmentedTabs(
                items = mainTabLabels,
                selectedIndex = if (feed.selectedMainTab == 1) 1 else 0,
                onSelect = feed.onMainTabSelected,
            )
        },
    ) { padding ->
        val safeHeader: @Composable () -> Unit = { headerContent?.invoke() }
        when (feed.isLoggedIn) {
            false -> CouiNewStateBody(
                message = strings.loginNewNeedLogin,
                padding = padding,
                header = safeHeader,
                actionText = strings.btnGoLogin,
                onAction = onLoginClick,
            )

            null -> CouiNewStateBody(
                message = strings.loading,
                padding = padding,
                header = safeHeader,
            )

            true -> if (feed.selectedMainTab == 1) {
                CouiWatchlistContent(
                    feed = feed,
                    onSeriesClick = onSeriesClick,
                    padding = padding,
                    header = safeHeader,
                )
            } else {
                val currentIllusts = feed.listState.filteredIllusts
                when {
                    currentIllusts == null && feed.listState.initialError == null -> CouiNewStateBody(
                        message = strings.loading,
                        padding = padding,
                        header = safeHeader,
                    )

                    currentIllusts == null && feed.listState.initialError != null -> CouiNewErrorBody(
                        error = feed.listState.initialError,
                        onRetry = feed.refresh,
                        padding = padding,
                        header = safeHeader,
                    )

                    currentIllusts != null && currentIllusts.isEmpty() -> CouiNewStateBody(
                        message = strings.loginNewEmpty,
                        padding = padding,
                        header = safeHeader,
                    )

                    currentIllusts != null -> CouiIllustGrid(
                        illusts = currentIllusts,
                        state = feed.gridState,
                        onIllustClick = onIllustClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(padding),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            top = padding.calculateTopPadding() + 4.dp,
                            end = 10.dp,
                            bottom = padding.calculateBottomPadding() + 8.dp,
                        ),
                        header = headerContent,
                        hasMore = feed.listState.nextUrl != null,
                        isLoadingMore = feed.listState.isLoadingMore,
                        loadMoreError = feed.listState.loadMoreError,
                        isRefreshing = feed.listState.isRefreshing,
                        onLoadMore = feed.loadMore,
                    )
                }
            }
        }
    }
}

/** 带头部的居中占位：登录提示 / 加载中 / 空态共用，头部与列表保持同一滚动起点。 */
@Composable
private fun CouiNewStateBody(
    message: String,
    padding: PaddingValues,
    header: @Composable () -> Unit,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),
    ) {
        header()
        CouiPlaceholder(
            message = message,
            modifier = Modifier.weight(1f),
            actionText = actionText,
            onAction = onAction,
        )
    }
}

/** 带头部的错误占位。 */
@Composable
private fun CouiNewErrorBody(
    error: Throwable?,
    onRetry: () -> Unit,
    padding: PaddingValues,
    header: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),
    ) {
        header()
        CouiErrorPlaceholder(
            error = error,
            onRetry = onRetry,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 追更页签：系列卡片列表 + 触底预加载 + 三态 footer。 */
@Composable
private fun CouiWatchlistContent(
    feed: NewFeedState,
    onSeriesClick: (Long) -> Unit,
    padding: PaddingValues,
    header: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val series = feed.watchlistState.items
    val hasMore = feed.watchlistState.nextUrl != null
    val isLoadingMore = feed.watchlistState.isLoadingMore
    val loadMoreError = feed.watchlistState.loadMoreError
    val isRefreshing = feed.watchlistState.isRefreshing

    when {
        series == null && feed.watchlistState.initialError == null -> CouiNewStateBody(
            message = strings.loading,
            padding = padding,
            header = header,
        )

        series == null && feed.watchlistState.initialError != null -> CouiNewErrorBody(
            error = feed.watchlistState.initialError,
            onRetry = feed.refresh,
            padding = padding,
            header = header,
        )

        series != null && series.isEmpty() -> CouiNewStateBody(
            message = strings.watchlistEmpty,
            padding = padding,
            header = header,
        )

        series != null -> {
            val shouldLoadMore by remember(feed.watchlistListState, series.size, hasMore, isLoadingMore, loadMoreError, isRefreshing) {
                derivedStateOf {
                    shouldPreloadMore(
                        itemCount = series.size,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        hasLoadMoreError = loadMoreError != null,
                        lastVisibleItemIndex = feed.watchlistListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                        thresholdItems = AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS,
                        isRefreshing = isRefreshing,
                    )
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) feed.loadMoreWatchlist()
            }

            LazyColumn(
                state = feed.watchlistListState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = padding.calculateTopPadding() + 6.dp,
                    end = 10.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_new_header", contentType = "coui_new_header") { header() }
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
                        CouiLoadMoreFooter(state = footerState, onRetry = feed.loadMoreWatchlist)
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
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = pressInteraction, list = true)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
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
            icon = CouiIcons.Delete,
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
