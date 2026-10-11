package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRow

/**
 * 用户详情页的 Tab 区块集合：作品列表、收藏列表（公开/私密）与共用的列表状态容器。
 *
 * 三个组件均为纯状态驱动，页面级状态由 [UserDetailTabContent] 持有并通过参数下发。
 */

/**
 * 作品 Tab：加载并展示用户作品列表。
 */
@Composable
internal fun UserWorksTab(
    userId: Long,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onIllustClick: (Long) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    // retryCount 自增（错误重试 / 下拉刷新）触发重新加载。
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState，替代原 produceState + remember(key)
    // 桥接的手写状态机（报告点名的第三种写法）。状态机以 userId 为 remember key。
    val listState = remember(userId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserIllustsResponse(userId, nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()

    // retryCount 自增走强制刷新（等价原 produceState 重跑总是重新请求）；tab 保持组合复用与
    // 首次进入时 loadFirst 缓存早退不重拉；filterChangeVersion 变化仅本地重过滤（高-3 语义）。
    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(userId, retryCount, filterChangeVersion) {
        if (listState.refilterIfChanged()) return@LaunchedEffect
        listState.loadFirst(force = retryCount > 0)
    }

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            listState.loadMore()
        }
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    IllustTabBody(
        illusts = listState.filteredIllusts,
        initialError = listState.initialError,
        hasMore = listState.nextUrl != null,
        isLoadingMore = listState.isLoadingMore,
        loadMoreError = listState.loadMoreError,
        header = header,
        gridState = gridState,
        onLoadMore = ::loadMore,
        onIllustClick = onIllustClick,
        onRetry = {
            retryCount++
            onRefresh()
        },
        emptyText = strings.userNoWorks,
        topPadding = topPadding,
        scrollBehavior = scrollBehavior,
        isRefreshing = isRefreshing,
        onRefresh = {
            retryCount++
            onRefresh()
        },
    )
}

/**
 * 收藏 Tab：加载并展示用户公开/私密收藏插画。
 */
@Composable
internal fun UserBookmarksTab(
    userId: Long,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onIllustClick: (Long) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val strings = LocalStrings.current
    // 收藏可见性：0 = 公开(public)，1 = 私密(private)。
    var selectedRestrictIndex by rememberSaveable(userId) { mutableIntStateOf(0) }
    val restrictTabs = listOf(strings.userPublicRestrict, strings.userPrivateRestrict)

    val coroutineScope = rememberCoroutineScope()

    // 高-4c：公开/私密收藏各持一份 IllustPagedListState（替代原两份 produceState + 桥接状态机）。
    // 原加载一次标志（loadedOnce）由「仅激活当前 tab + loadFirst 缓存早退」自然承担。
    var publicRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val publicState = remember(userId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserBookmarksResponse(userId, "public", nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    var privateRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val privateState = remember(userId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserBookmarksResponse(userId, "private", nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }

    // 仅加载当前选中 tab（未选中保持空，lazy）；切回已加载 tab 时 loadFirst 缓存早退；
    // 各自 retryCount 自增走强制刷新（等价原 produceState 重跑）；filterChangeVersion 变化时
    // 当前 tab 本地重过滤（高-3 语义），未选中 tab 在下次选中时经 refilterIfChanged 补上。
    val filterChangeVersion2 = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(selectedRestrictIndex, publicRetryCount, privateRetryCount, filterChangeVersion2) {
        val isPublic = selectedRestrictIndex == 0
        val activeState = if (isPublic) publicState else privateState
        val activeRetryCount = if (isPublic) publicRetryCount else privateRetryCount
        if (activeState.refilterIfChanged()) return@LaunchedEffect
        activeState.loadFirst(force = activeRetryCount > 0)
    }

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        val activeState = if (selectedRestrictIndex == 0) publicState else privateState
        coroutineScope.launch {
            activeState.loadMore()
        }
    }

    val bookmarkHeader: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (header != null) header()
            TabRow(
                tabs = restrictTabs,
                selectedTabIndex = selectedRestrictIndex,
                onTabSelected = { selectedRestrictIndex = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }

    val isPublic = selectedRestrictIndex == 0
    val activeState = if (isPublic) publicState else privateState

    IllustTabBody(
        illusts = activeState.filteredIllusts,
        initialError = activeState.initialError,
        hasMore = activeState.nextUrl != null,
        isLoadingMore = activeState.isLoadingMore,
        loadMoreError = activeState.loadMoreError,
        header = bookmarkHeader,
        gridState = gridState,
        onLoadMore = ::loadMore,
        onIllustClick = onIllustClick,
        onRetry = {
            if (isPublic) publicRetryCount++ else privateRetryCount++
            onRefresh()
        },
        emptyText = if (isPublic) {
            strings.userNoBookmarks.format(strings.userPublicRestrict)
        } else {
            strings.userNoBookmarks.format(strings.userPrivateRestrict)
        },
        topPadding = topPadding,
        scrollBehavior = scrollBehavior,
        isRefreshing = isRefreshing,
        onRefresh = {
            if (isPublic) publicRetryCount++ else privateRetryCount++
            onRefresh()
        },
    )
}

/**
 * Tab 内容通用容器：处理加载 / 空态 / 错误 / 列表展示。
 * 即使处于加载态或错误态，头部信息（UserProfileHeader + TabRow）始终保持常驻展示，避免全屏闪烁。
 *
 * 三态由 [illusts] 与 [initialError] 表达：illusts == null && initialError == null 为加载中，
 * illusts == null && initialError != null 为失败，否则为已加载（含空列表）。
 */
@Composable
internal fun IllustTabBody(
    illusts: List<Illust>?,
    initialError: Throwable?,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onLoadMore: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    when {
        illusts == null && initialError == null -> {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding),
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = topPadding + 8.dp,
                        end = 8.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                ) {
                    if (header != null) {
                        item(
                            key = "loading_tab_header",
                            contentType = "grid_custom_header",
                        ) {
                            header()
                        }
                    }
                    item(
                        key = "loading_tab_indicator",
                        contentType = "loading_indicator",
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            InfiniteProgressIndicator()
                        }
                    }
                }
            }
        }
        illusts != null && illusts.isEmpty() && !isLoadingMore -> {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding),
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = topPadding + 8.dp,
                        end = 8.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                ) {
                    if (header != null) {
                        item(
                            key = "empty_tab_header",
                            contentType = "grid_custom_header",
                        ) {
                            header()
                        }
                    }
                    item(
                        key = "empty_placeholder_item",
                        contentType = "empty_placeholder",
                    ) {
                        EmptyPlaceholder(
                            message = emptyText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }
            }
        }
        illusts != null -> {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding),
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                IllustStaggeredGrid(
                    illusts = illusts,
                    onIllustClick = onIllustClick,
                    state = gridState,
                    header = header,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = topPadding + 8.dp,
                        end = 8.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    hasMore = hasMore,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    onLoadMore = onLoadMore,
                )
            }
        }
        else -> {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding),
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = topPadding + 8.dp,
                        end = 8.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                ) {
                    if (header != null) {
                        item(
                            key = "error_tab_header",
                            contentType = "grid_custom_header",
                        ) {
                            header()
                        }
                    }
                    item(
                        key = "error_placeholder_item",
                        contentType = "error_placeholder",
                    ) {
                        ErrorPlaceholder(
                            error = initialError,
                            onRetry = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }
            }
        }
    }
}