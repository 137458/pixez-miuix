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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
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
    userId: Int,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> =
        banRepository.filterIllusts(
            rawIllusts = rawIllusts,
            banAIIllust = settingsRepository.banAIIllust,
            hideR18 = settingsRepository.hIsNotAllow,
        )

    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        userId,
        retryCount,
        banRepository,
        settingsRepository.filterChangeVersion,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserIllustsResponse(userId) }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(userId, settingsRepository.filterChangeVersion) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(userId, settingsRepository.filterChangeVersion) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.value) {
        state.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            illusts = initialIllusts
            nextUrl = initialNextUrl
            isLoadingMore = false
            loadMoreError = null
        }
    }

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel { repository.getUserIllustsResponse(userId, nextUrl = currentNextUrl) }
                .onSuccess { response ->
                    val filtered = filterBanned(response.illusts)
                    illusts = illusts.appendDistinct(filtered)
                    nextUrl = response.nextUrl
                }
                .onFailure { error ->
                    loadMoreError = error
                }
            isLoadingMore = false
        }
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    IllustTabBody(
        state = state.value,
        illusts = illusts,
        hasMore = nextUrl != null,
        isLoadingMore = isLoadingMore,
        loadMoreError = loadMoreError,
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
    userId: Int,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onIllustClick: (Int) -> Unit,
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

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> =
        banRepository.filterIllusts(
            rawIllusts = rawIllusts,
            banAIIllust = settingsRepository.banAIIllust,
            hideR18 = settingsRepository.hIsNotAllow,
        )

    val coroutineScope = rememberCoroutineScope()

    // 公开收藏状态缓存
    var publicRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var publicLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedRestrictIndex == 0) }
    LaunchedEffect(selectedRestrictIndex) {
        if (selectedRestrictIndex == 0) publicLoadedOnce = true
    }
    val publicState = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        userId,
        publicRetryCount,
        publicLoadedOnce,
        banRepository,
        settingsRepository.filterChangeVersion,
    ) {
        if (!publicLoadedOnce) return@produceState
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "public") }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }
    var publicIllusts by remember(userId, settingsRepository.filterChangeVersion) { mutableStateOf(listOf<Illust>()) }
    var publicNextUrl by remember(userId, settingsRepository.filterChangeVersion) { mutableStateOf<String?>(null) }
    var publicIsLoadingMore by remember { mutableStateOf(false) }
    var publicLoadMoreError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(publicState.value) {
        publicState.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            publicIllusts = initialIllusts
            publicNextUrl = initialNextUrl
            publicIsLoadingMore = false
            publicLoadMoreError = null
        }
    }

    // 私密收藏状态缓存
    var privateRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var privateLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedRestrictIndex == 1) }
    LaunchedEffect(selectedRestrictIndex) {
        if (selectedRestrictIndex == 1) privateLoadedOnce = true
    }
    val privateState = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        userId,
        privateRetryCount,
        privateLoadedOnce,
        banRepository,
        settingsRepository.filterChangeVersion,
    ) {
        if (!privateLoadedOnce) return@produceState
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "private") }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }
    var privateIllusts by remember(userId, settingsRepository.filterChangeVersion) { mutableStateOf(listOf<Illust>()) }
    var privateNextUrl by remember(userId, settingsRepository.filterChangeVersion) { mutableStateOf<String?>(null) }
    var privateIsLoadingMore by remember { mutableStateOf(false) }
    var privateLoadMoreError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(privateState.value) {
        privateState.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            privateIllusts = initialIllusts
            privateNextUrl = initialNextUrl
            privateIsLoadingMore = false
            privateLoadMoreError = null
        }
    }

    fun loadMore() {
        if (selectedRestrictIndex == 0) {
            val currentNextUrl = publicNextUrl ?: return
            if (publicIsLoadingMore) return
            coroutineScope.launch {
                publicIsLoadingMore = true
                publicLoadMoreError = null
                suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "public", nextUrl = currentNextUrl) }
                    .onSuccess { response ->
                        val filtered = filterBanned(response.illusts)
                        publicIllusts = publicIllusts.appendDistinct(filtered)
                        publicNextUrl = response.nextUrl
                    }
                    .onFailure { error ->
                        publicLoadMoreError = error
                    }
                publicIsLoadingMore = false
            }
        } else {
            val currentNextUrl = privateNextUrl ?: return
            if (privateIsLoadingMore) return
            coroutineScope.launch {
                privateIsLoadingMore = true
                privateLoadMoreError = null
                suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "private", nextUrl = currentNextUrl) }
                    .onSuccess { response ->
                        val filtered = filterBanned(response.illusts)
                        privateIllusts = privateIllusts.appendDistinct(filtered)
                        privateNextUrl = response.nextUrl
                    }
                    .onFailure { error ->
                        privateLoadMoreError = error
                    }
                privateIsLoadingMore = false
            }
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
    val currentState = if (isPublic) publicState.value else privateState.value
    val currentIllusts = if (isPublic) publicIllusts else privateIllusts
    val currentHasMore = if (isPublic) publicNextUrl != null else privateNextUrl != null
    val currentIsLoadingMore = if (isPublic) publicIsLoadingMore else privateIsLoadingMore
    val currentLoadMoreError = if (isPublic) publicLoadMoreError else privateLoadMoreError

    IllustTabBody(
        state = currentState,
        illusts = currentIllusts,
        hasMore = currentHasMore,
        isLoadingMore = currentIsLoadingMore,
        loadMoreError = currentLoadMoreError,
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
 */
@Composable
internal fun IllustTabBody(
    state: Result<Pair<List<Illust>, String?>>?,
    illusts: List<Illust>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onLoadMore: () -> Unit,
    onIllustClick: (Int) -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    when {
        state == null -> {
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
        state.isSuccess -> {
            if (illusts.isEmpty() && !isLoadingMore) {
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
            } else {
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
                            error = state.exceptionOrNull(),
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