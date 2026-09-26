/**
 * 搜索页结果区（由 SearchScreen 拆分而来）。
 *
 * 包含作品搜索结果瀑布流、画师搜索结果列表，以及日期筛选的防抖工具函数。
 * 各子组件自带完整的请求状态机（加载 / 错误 / 分页），状态声明保留在各自组件内部，
 * 由 SearchScreen 通过参数下发查询条件与列表状态。
 */

package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchIllustResultGrid(
    query: String,
    sort: String,
    filterState: SearchFilterState,
    repository: SearchRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    onIllustClick: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues? = null,
) {
    val searchTarget = filterState.searchTarget
    val searchAiType = filterState.searchAiType
    val bookmarkThreshold = filterState.bookmarkThreshold
    val ugoiraFilter = filterState.ugoiraFilter
    val ratioFilter = filterState.ratioFilter
    val startDate = filterState.startDate.takeIf { it.isNotBlank() }
    val endDate = filterState.endDate.takeIf { it.isNotBlank() }
    val effectiveContentPadding = contentPadding ?: PaddingValues(
        start = 8.dp,
        top = 8.dp,
        end = 8.dp,
        bottom = LocalBottomBarContentPadding.current,
    )
    // 根据收藏数阈值构建实际搜索词：先清除原有 \d+users入り，再按需追加 " ${value}users入り"。
    val searchWord = remember(query, bookmarkThreshold) {
        val cleanQuery = query.replace(Regex("""\s*\d+users入り"""), "").trim()
        if (bookmarkThreshold > 0) "$cleanQuery ${bookmarkThreshold}users入り" else cleanQuery
    }

    // 搜索结果重试计数，作为 produceState 的 key 触发重新加载。
    var retryCount by rememberSaveable(
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        startDate,
        endDate,
    ) { mutableIntStateOf(0) }

    // 对日期输入做防抖，避免用户逐字输入时频繁请求。
    val effectiveStartDate = debouncedSearchDate(startDate)
    val effectiveEndDate = debouncedSearchDate(endDate)

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> =
        banRepository.filterIllusts(
            rawIllusts = rawIllusts,
            banAIIllust = settingsRepository.banAIIllust,
            hideR18 = settingsRepository.hIsNotAllow,
        )

    fun applyClientFilters(list: List<Illust>, ugoira: Int, ratio: Int): List<Illust> {
        var res = when (ugoira) {
            1 -> list.filter { IllustType.isUgoira(it.type) }
            2 -> list.filterNot { IllustType.isUgoira(it.type) }
            else -> list
        }
        res = when (ratio) {
            1 -> res.filter { it.width > it.height }
            2 -> res.filter { it.height > it.width }
            3 -> res.filter { it.width == it.height }
            else -> res
        }
        return res
    }

    // 统一 UI 状态机（单向数据流 UDF）
    var illustsState by remember { mutableStateOf<List<Illust>?>(null) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var initialError by remember { mutableStateOf<Throwable?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    var requestGeneration by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        effectiveStartDate,
        effectiveEndDate,
        retryCount,
        settingsRepository.filterChangeVersion,
    ) {
        requestGeneration++
        val generation = requestGeneration
        illustsState = null
        nextUrl = null
        initialError = null
        loadMoreError = null
        isLoadingMore = false
        val searchResult = suspendRunCatchingNonCancel {
            repository.searchIllustResponse(
                word = searchWord,
                sort = sort,
                searchTarget = searchTarget,
                searchAiType = searchAiType,
                startDate = effectiveStartDate,
                endDate = effectiveEndDate,
            )
        }
        searchResult.onSuccess { response ->
            if (generation == requestGeneration) {
                illustsState = filterBanned(response.illusts)
                nextUrl = response.nextUrl
                initialError = null
                loadMoreError = null
            }
        }.onFailure { error ->
            if (generation == requestGeneration) {
                initialError = error
            }
        }
    }

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        val generation = requestGeneration
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel {
                repository.searchIllustResponse(
                    word = searchWord,
                    sort = sort,
                    searchTarget = searchTarget,
                    searchAiType = searchAiType,
                    startDate = effectiveStartDate,
                    endDate = effectiveEndDate,
                    nextUrl = currentNextUrl,
                )
            }.onSuccess { response ->
                if (generation == requestGeneration) {
                    val filtered = filterBanned(response.illusts)
                    illustsState = (illustsState.orEmpty()).appendDistinct(filtered)
                    nextUrl = response.nextUrl
                }
            }.onFailure { error ->
                if (generation == requestGeneration) {
                    loadMoreError = error
                }
            }
            if (generation == requestGeneration) {
                isLoadingMore = false
            }
        }
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val currentIllusts = illustsState
    when {
        currentIllusts == null && initialError == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        currentIllusts == null && initialError != null -> ErrorPlaceholder(
            error = initialError,
            onRetry = { retryCount++ },
            modifier = Modifier.fillMaxSize(),
        )
        currentIllusts != null -> {
            val filteredIllusts = remember(currentIllusts, ugoiraFilter, ratioFilter) {
                applyClientFilters(currentIllusts, ugoiraFilter, ratioFilter)
            }
            if (filteredIllusts.isEmpty() && !isLoadingMore) {
                if (nextUrl != null) {
                    LaunchedEffect(filteredIllusts.isEmpty(), nextUrl) {
                        loadMore()
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            InfiniteProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.loading,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    EmptyPlaceholder(
                        message = strings.searchEmptyIllust,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                IllustStaggeredGrid(
                    illusts = filteredIllusts,
                    onIllustClick = onIllustClick,
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = effectiveContentPadding,
                    hasMore = nextUrl != null,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    onLoadMore = ::loadMore,
                )
            }
        }
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun SearchUserResultList(
    query: String,
    repository: SearchRepository,
    settingsRepository: SettingsRepository,
    onUserClick: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues? = null,
) {
    val effectiveContentPadding = contentPadding ?: PaddingValues(bottom = LocalBottomBarContentPadding.current)
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    // 画师搜索结果重试计数，作为 LaunchedEffect 的 key 触发重新加载。
    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    // 统一 UI 状态机（单向数据流 UDF）
    var previewsState by remember { mutableStateOf<List<UserPreview>?>(null) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var initialError by remember { mutableStateOf<Throwable?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    var requestGeneration by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(query, retryCount, settingsRepository.filterChangeVersion) {
        requestGeneration++
        val generation = requestGeneration
        previewsState = null
        nextUrl = null
        initialError = null
        loadMoreError = null
        isLoadingMore = false
        val userResult = suspendRunCatchingNonCancel { repository.searchUserResponse(query) }
        userResult.onSuccess { response ->
            if (generation == requestGeneration) {
                previewsState = response.userPreviews
                nextUrl = response.nextUrl
                initialError = null
                loadMoreError = null
            }
        }.onFailure { error ->
            if (generation == requestGeneration) {
                initialError = error
            }
        }
    }

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        val generation = requestGeneration
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel { repository.searchUserResponse(query, nextUrl = currentNextUrl) }
                .onSuccess { response ->
                    if (generation == requestGeneration) {
                        previewsState = (previewsState.orEmpty()).appendDistinct(response.userPreviews)
                        nextUrl = response.nextUrl
                    }
                }
                .onFailure { error ->
                    if (generation == requestGeneration) {
                        loadMoreError = error
                    }
                }
            if (generation == requestGeneration) {
                isLoadingMore = false
            }
        }
    }

    val currentPreviews = previewsState
    val shouldLoadMore by remember(nextUrl, isLoadingMore, loadMoreError, currentPreviews?.size) {
        derivedStateOf {
            if (nextUrl == null || isLoadingMore || loadMoreError != null || currentPreviews.isNullOrEmpty()) {
                false
            } else {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
                lastVisibleIndex >= totalItems - 4
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    when {
        currentPreviews == null && initialError == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        currentPreviews == null && initialError != null -> ErrorPlaceholder(
            error = initialError,
            onRetry = { retryCount++ },
            modifier = Modifier.fillMaxSize(),
        )
        currentPreviews != null -> {
            if (currentPreviews.isEmpty()) {
                EmptyPlaceholder(
                    message = strings.searchEmptyUser,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = effectiveContentPadding,
                    ) {
                        items(
                            items = currentPreviews,
                            key = { it.user.id },
                            contentType = { "user_preview_item" },
                        ) { preview ->
                            UserPreviewItem(
                                preview = preview,
                                onClick = { onUserClick(preview.user.id) },
                            )
                        }

                        if (isLoadingMore || loadMoreError != null || (nextUrl == null && currentPreviews.isNotEmpty())) {
                            item(key = "search_user_footer", contentType = "footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        isLoadingMore -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                InfiniteProgressIndicator(modifier = Modifier.size(20.dp))
                                                Text(
                                                    text = strings.loadingMore,
                                                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote1,
                                                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                )
                                            }
                                        }
                                        loadMoreError != null -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Text(
                                                    text = strings.loadMoreFailedRetry,
                                                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote1,
                                                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.error,
                                                )
                                                TextButton(
                                                    text = strings.retry,
                                                    onClick = ::loadMore,
                                                )
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = strings.noMoreData,
                                                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote1,
                                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerticalScrollBar(
                        adapter = rememberScrollBarAdapter(listState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        trackPadding = effectiveContentPadding,
                    )
                }
            }
        }
    }
}

/**
 * 将日期字符串延迟 500ms 后返回，避免用户逐字输入时频繁触发搜索。
 * 空字符串或格式不符合 YYYY-MM-DD 时返回 null，表示不应用该日期筛选。
 */
@Composable
internal fun debouncedSearchDate(date: String?): String? {
    if (date == null) return null
    var debounced by remember { mutableStateOf(date) }
    LaunchedEffect(date) {
        delay(500)
        debounced = date
    }
    return debounced.takeIf { it.matches(SearchDateRegex) }
}

private val SearchDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")