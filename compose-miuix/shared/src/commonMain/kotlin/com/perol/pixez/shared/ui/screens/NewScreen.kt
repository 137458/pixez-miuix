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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.derivedStateOf
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.data.model.appendDistinct as appendDistinctSeries
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PagingLoadMoreFooter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalWatchlistRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.LiquidFilterBar
import top.yukonga.miuix.kmp.blur.Backdrop
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
 */
@Composable
fun NewScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onLoginClick: () -> Unit,
    onSeriesClick: (Long) -> Unit = {},
    reselectFlow: kotlinx.coroutines.flow.Flow<Unit>? = null,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalIllustRepository.current
    val accountRepository = LocalAccountRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val watchlistRepository = LocalWatchlistRepository.current
    val strings = LocalStrings.current
    // 顶部主页签：0 = 动态，1 = 追更。
    var selectedMainTab by rememberSaveable { mutableIntStateOf(0) }
    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settingsRepository.hasUnreadFeedBadge = false
        currentAccount = suspendRunCatchingNonCancel { accountRepository.currentAccount() }
            .onFailure { Napier.w(tag = "CurrentAccount", throwable = it) { "加载当前账号失败(NewScreen)" } }
            .getOrNull()
        isLoggedIn = currentAccount != null
        accountRepository.loginEventFlow.collect {
            currentAccount = suspendRunCatchingNonCancel { accountRepository.currentAccount() }
                .onFailure { Napier.w(tag = "CurrentAccount", throwable = it) { "登录事件后加载当前账号失败(NewScreen)" } }
                .getOrNull()
            isLoggedIn = currentAccount != null
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
    }

    // 可见性筛选：0=all, 1=public, 2=private。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val restrictOptions: List<Pair<String, String>> = listOf(
        strings.searchUgoiraAll to "all",
        strings.userPublicRestrict to "public",
        strings.userPrivateRestrict to "private",
    )
    val currentRestrict = restrictOptions[selectedRestrictIndex].second

    // retryCount 作为 produceState 的 key，点击重试或切换筛选时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链为高-3「原始/过滤」双列表本地重过滤。状态机以可见性筛选为 remember key：
    // all/public/private 切换时整体重建（等价原实现的清空重拉），retryCount 重试复用同一实例。
    val listState = remember(currentRestrict) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getFollowIllustsResponse(restrict = currentRestrict, nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value

    // ---------- 追更页签：漫画系列追更列表 ----------
    var watchlistRefreshing by rememberSaveable { mutableStateOf(false) }
    var watchlistRetryCount by rememberSaveable { mutableIntStateOf(0) }
    // 待取消追更的系列，非 null 时显示确认框。
    var seriesToRemove by remember { mutableStateOf<MangaSeries?>(null) }
    // 取消追更进行中，禁用确认按钮防重复提交。
    var isRemovingWatchlist by remember { mutableStateOf(false) }
    var watchlistToast by remember { mutableStateOf<ToastData?>(null) }

    // 追更列表分页状态机（generation 竞态守卫），按系列 ID 幂等合并去重。
    val watchlistState = remember {
        PagedListState(
            fetch = { nextUrl ->
                val response = watchlistRepository.getMangaWatchlist(nextUrl = nextUrl)
                PagedListPage(response.series, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinctSeries(new) },
        )
    }
    val watchlistListState = rememberLazyListState()

    // 进入追更页签 / 重试 / 手动刷新时加载首屏；已持有数据且非强制时状态机内部早退。
    LaunchedEffect(isLoggedIn, selectedMainTab, watchlistRetryCount) {
        if (isLoggedIn != true || selectedMainTab != 1) return@LaunchedEffect
        val force = watchlistRefreshing
        watchlistState.loadFirst(force = force)
        watchlistRefreshing = false
    }

    fun loadMoreWatchlist() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            watchlistState.loadMore()
        }
    }

    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 currentAccount/isLoggedIn）时作废旧账号列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(isLoggedIn, currentAccount, currentRestrict, retryCount, filterChangeVersion) {
        // 登录态未检测完成（null）时不发起加载；登出/切号先作废旧账号列表（动态与追更一并作废）。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != currentAccount
        if (accountSwitched) listState.reset()
        if (accountSwitched) watchlistState.reset()
        loadedForAccount = currentAccount
        // 未登录（false）时 UI 层展示登录占位，状态机保持空。
        if (isLoggedIn != true) return@LaunchedEffect
        val force = isManualRefreshing
        // 高-3：过滤条件变化且已持有原始列表时，仅对原始列表本地重过滤（零网络），不重新请求。
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val previous = listState.filteredIllusts
        listState.loadFirst(force = force)
        isManualRefreshing = false
        // 引用变化 = 本轮成功替换了列表（失败与竞态失配时状态机不写入）；force 刷新成功后滚回顶部。
        val current = listState.filteredIllusts
        if (current != null && current !== previous && force && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(reselectFlow) {
        reselectFlow?.collect {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            if (selectedMainTab == 1) {
                // 追更页签：可回滚则回顶，否则刷新追更列表。
                if (watchlistListState.firstVisibleItemIndex > 0) {
                    watchlistListState.animateScrollToItem(0)
                } else {
                    watchlistRefreshing = true
                    watchlistRetryCount++
                }
            } else if (gridState.firstVisibleItemIndex > 0) {
                gridState.animateScrollToItem(0)
            } else {
                isManualRefreshing = true
                retryCount++
            }
        }
    }

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            listState.loadMore()
        }
    }

    val triggerManualRefresh: () -> Unit = {
        if (selectedMainTab == 1) {
            watchlistRefreshing = true
            watchlistRetryCount++
        } else {
            isManualRefreshing = true
            retryCount++
        }
    }

    // 未登录提示对话框：位于 Scaffold 外层，确保能覆盖整个页面。
    OverlayDialog(
        title = strings.dialogNeedLogin,
        summary = strings.dialogNeedLoginSummary,
        show = showLoginDialog,
        onDismissRequest = { showLoginDialog = false },
    ) {
        TextButton(
            text = strings.btnCancelLogin,
            onClick = { showLoginDialog = false },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = strings.btnGoLogin,
            onClick = {
                showLoginDialog = false
                onLoginClick()
            },
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
                        title = if (selectedMainTab == 1) strings.watchlistTitle else strings.tabNew,
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                        actions = {
                            IconButton(
                                onClick = triggerManualRefresh,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Refresh,
                                    contentDescription = strings.refresh,
                                )
                            }
                            if (isLoggedIn == false) {
                                Button(
                                    onClick = onLoginClick,
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.padding(end = 12.dp),
                                ) {
                                    Text(strings.btnGoLogin)
                                }
                            }
                            if (isLoggedIn == true) {
                                val account = currentAccount
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
                        selectedTabIndex = selectedMainTab,
                        onTabSelected = { index -> selectedMainTab = index },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isLoggedIn == true && selectedMainTab == 0) {
                        LiquidFilterBar(
                            items = restrictOptions,
                            selectedItem = restrictOptions[selectedRestrictIndex],
                            onItemSelected = { selectedRestrictIndex = restrictOptions.indexOf(it) },
                            labelProvider = { it.first },
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
            when (isLoggedIn) {
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
                    if (selectedMainTab == 1) {
                        // 追更页签：漫画系列追更列表。
                        val currentSeries = watchlistState.items
                        when {
                            currentSeries == null && watchlistState.initialError == null -> LoadingPlaceholder(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                            currentSeries == null && watchlistState.initialError != null -> ErrorPlaceholder(
                                error = watchlistState.initialError,
                                onRetry = { triggerManualRefresh() },
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
                                        isRefreshing = watchlistRefreshing,
                                        onRefresh = triggerManualRefresh,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                        topAppBarScrollBehavior = scrollBehavior,
                                    ) {
                                        WatchlistSeriesList(
                                            series = currentSeries,
                                            state = watchlistListState,
                                            onSeriesClick = onSeriesClick,
                                            onRemoveClick = { seriesToRemove = it },
                                            onLoadMore = ::loadMoreWatchlist,
                                            isLoadingMore = watchlistState.isLoadingMore,
                                            loadMoreError = watchlistState.loadMoreError,
                                            hasMore = watchlistState.nextUrl != null,
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
                        val currentIllusts = listState.filteredIllusts
                        when {
                            currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                            currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
                                error = listState.initialError,
                                onRetry = { triggerManualRefresh() },
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
                                        isRefreshing = isManualRefreshing,
                                        onRefresh = triggerManualRefresh,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                        topAppBarScrollBehavior = scrollBehavior,
                                    ) {
                                        IllustStaggeredGrid(
                                            illusts = currentIllusts,
                                            state = gridState,
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
                                            hasMore = listState.nextUrl != null,
                                            isLoadingMore = listState.isLoadingMore,
                                            loadMoreError = listState.loadMoreError,
                                            onLoadMore = ::loadMore,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 取消追更二次确认（参照 HistoryScreen 的 OverlayDialog 确认模式）。
            val removingSeries = seriesToRemove
            OverlayDialog(
                title = strings.watchlistTitle,
                summary = strings.watchlistRemoveConfirm,
                show = removingSeries != null,
                onDismissRequest = { if (!isRemovingWatchlist) seriesToRemove = null },
            ) {
                TextButton(
                    text = strings.cancel,
                    onClick = { if (!isRemovingWatchlist) seriesToRemove = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = strings.confirm,
                    enabled = !isRemovingWatchlist,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val series = removingSeries ?: return@TextButton
                        isRemovingWatchlist = true
                        coroutineScope.launch {
                            try {
                                suspendRunCatchingNonCancel {
                                    watchlistRepository.deleteMangaWatchlist(series.id)
                                }
                                    .onSuccess {
                                        // 删除成功后强制刷新追更列表。
                                        watchlistRefreshing = true
                                        watchlistRetryCount++
                                    }
                                    .onFailure { error ->
                                        watchlistToast = ToastData(
                                            "${strings.watchlistTitle}${strings.loadFailed}: " +
                                                PixivApiErrorFormatter.format(error, strings),
                                            ToastType.Error,
                                        )
                                    }
                            } finally {
                                // 协程取消或异常时也必须重置状态，避免确认按钮永久禁用。
                                isRemovingWatchlist = false
                                seriesToRemove = null
                            }
                        }
                    },
                )
            }

            ToastMessage(
                toast = watchlistToast,
                onDismiss = { watchlistToast = null },
            )
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

        item(key = "watchlist_pagination_footer", contentType = "watchlist_pagination_footer") {
            // 分页 footer 三态与搜索结果列表共用同一组件，观感与文案统一。
            PagingLoadMoreFooter(
                hasMore = hasMore,
                isLoadingMore = isLoadingMore,
                loadMoreError = loadMoreError,
                onRetry = onLoadMore,
            )
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
            .clip(RoundedCornerShape(12.dp))
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
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                text = item.publishedContentCount.toString(),
                style = MiuixTheme.textStyles.footnote1,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = item.lastPublishedContentDatetime?.take(16)?.replace('T', ' ') ?: "",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
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


