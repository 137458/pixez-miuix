package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalWatchlistRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * 插画系列页：展示指定系列内的作品列表，支持流式分页、下拉刷新与追更/取消追更切换。
 */
@Composable
fun IllustSeriesScreen(
    seriesId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalIllustRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val watchlistRepository = LocalWatchlistRepository.current
    var retryCount by rememberSaveable(seriesId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(seriesId) { mutableStateOf(false) }

    val strings = LocalStrings.current

    // 系列 ID 变更时重建状态机并重置标题；翻页响应不含标题更新。
    var seriesTitle by remember(seriesId) { mutableStateOf("") }
    // 追更状态：null 表示系列详情未加载（隐藏切换按钮）；首页响应的 watchlist_added 为准。
    var watchlistAdded by remember(seriesId) { mutableStateOf<Boolean?>(null) }
    // 追更切换进行中，防重复提交。
    var isTogglingWatchlist by remember { mutableStateOf(false) }
    var watchlistToast by remember { mutableStateOf<ToastData?>(null) }
    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链为高-3「原始/过滤」双列表本地重过滤。系列标题在 fetch 闭包内仅于首页请求（nextUrl == null）
    // 时更新：切系列时 remember(seriesId) 重建闭包，旧闭包写入的是旧 State 对象，不污染新页面。
    val listState = remember(seriesId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getIllustSeriesResponse(seriesId, nextUrl = nextUrl)
                if (nextUrl == null) {
                    seriesTitle = response.illustSeriesDetail?.title ?: ""
                    watchlistAdded = response.illustSeriesDetail?.watchlistAdded
                }
                IllustPagedPage(response.illusts.orEmpty(), response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // 系列 ID 变更、重试或手动刷新时重新加载作品；过滤条件变化仅本地重过滤（高-3）。
    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(seriesId, retryCount, filterChangeVersion) {
        val force = isManualRefreshing
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

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            listState.loadMore()
        }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    // 追更/取消追更：乐观翻转本地状态，失败回滚并 Toast。
    fun toggleWatchlist() {
        val currentAdded = watchlistAdded ?: return
        if (isTogglingWatchlist) return
        coroutineScope.launch {
            isTogglingWatchlist = true
            try {
                watchlistAdded = !currentAdded
                suspendRunCatchingNonCancel {
                    if (currentAdded) {
                        watchlistRepository.deleteMangaWatchlist(seriesId)
                    } else {
                        watchlistRepository.addMangaWatchlist(seriesId)
                    }
                }
                    .onFailure { error ->
                        watchlistAdded = currentAdded
                        watchlistToast = ToastData(
                            "${strings.watchlistTitle}${strings.loadFailed}: " +
                                PixivApiErrorFormatter.format(error, strings),
                            ToastType.Error,
                        )
                    }
            } finally {
                // 协程取消或异常时也必须复位，避免切换按钮永久禁用。
                isTogglingWatchlist = false
            }
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    BlurTopBarScaffold(
        modifier = Modifier.fillMaxSize(),
        title = seriesTitle,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            // 追更切换：系列详情加载完成（watchlistAdded 非 null）后展示。
            if (watchlistAdded != null) {
                IconButton(
                    onClick = ::toggleWatchlist,
                    enabled = !isTogglingWatchlist,
                ) {
                    Text(
                        text = if (watchlistAdded == true) strings.watchlistAdded else strings.watchlistAdd,
                        style = MiuixTheme.textStyles.body1,
                        color = if (watchlistAdded == true) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                    )
                }
            }
            IconButton(onClick = triggerManualRefresh) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.refresh,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
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
                            message = strings.seriesEmpty,
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
                                onIllustClick = onIllustClick,
                                state = gridState,
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

            // 追更操作失败提示。
            ToastMessage(
                toast = watchlistToast,
                onDismiss = { watchlistToast = null },
            )
        }
    }
}

