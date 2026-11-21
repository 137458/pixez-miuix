package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.Item
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.search.SearchBar
import com.itos.coui.search.rememberCouiSearchBarState
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.PlatformPhotoPicker
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.LocalSauceNaoRepository
import com.perol.pixez.shared.ui.LocalSearchRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.core.CouiActionRow
import com.perol.pixez.shared.ui.coui.core.CouiBodyTitle
import com.perol.pixez.shared.ui.coui.core.CouiErrorMessage
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiItemTitle
import com.perol.pixez.shared.ui.coui.core.CouiLoadingPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSearchScaffold
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiSingleGroup
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.LocalBottomBarVisibility
import com.perol.pixez.shared.ui.screens.SearchCoreState
import com.perol.pixez.shared.ui.screens.SearchEmptyPhase
import com.perol.pixez.shared.ui.screens.SearchFilterState
import com.perol.pixez.shared.ui.screens.SearchQueryTarget
import com.perol.pixez.shared.ui.screens.applySearchClientFilters
import com.perol.pixez.shared.ui.screens.rememberSearchCoreState
import com.perol.pixez.shared.ui.screens.rememberSearchIllustResultsState
import com.perol.pixez.shared.ui.screens.resolveSearchEmptyPhase
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

/**
 * COUI 搜索页：官方折叠顶栏骨架承载搜索栏、搜索类型胶囊、快捷筛选、历史/热搜与作品瀑布流。
 *
 * 查询词、搜索态、筛选条件、历史与热搜统一由 [rememberSearchCoreState] 承载；
 * 作品结果用 [rememberSearchIllustResultsState] + COUI 瀑布流。官方常驻搜索框挂在顶栏的
 * 搜索槽里（顶栏作用域的采样源），正文首个滚动项只剩搜索方式胶囊，随正文滚入顶栏覆盖区。
 */
@Composable
internal fun CouiSearchScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
    initialQuery: String = "",
) {
    val strings = LocalStrings.current
    val searchRepository = LocalSearchRepository.current
    val novelRepository = LocalNovelRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val banRepository = LocalBanRepository.current
    val sauceNaoRepository = LocalSauceNaoRepository.current
    val photoPicker = remember { PlatformPhotoPicker() }
    val coroutineScope = rememberCoroutineScope()

    val core = rememberSearchCoreState(
        initialQuery = initialQuery,
        onIllustClick = onIllustClick,
        onUserClick = onUserClick,
        onNovelClick = onNovelClick,
    )

    // 展开态由页面持有：库只按宿主给的目标值做动画。
    // 带初始查询进页（深链/分享）时才直接展开并聚焦，普通进入不抢占键盘。
    var searchExpanded by rememberSaveable { mutableStateOf(initialQuery.isNotBlank()) }
    val searchBarState = rememberCouiSearchBarState(expanded = searchExpanded)

    var showSauceNaoSheet by remember { mutableStateOf(false) }
    var isSauceNaoLoading by remember { mutableStateOf(false) }
    var sauceNaoError by remember { mutableStateOf<String?>(null) }
    var sauceNaoResults by remember { mutableStateOf<List<SauceNaoResult>>(emptyList()) }
    var currentSauceNaoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var currentSauceNaoName by remember { mutableStateOf<String?>(null) }

    val performSauceNaoSearch: (ByteArray, String?) -> Unit = { bytes, fileName ->
        currentSauceNaoBytes = bytes
        currentSauceNaoName = fileName
        showSauceNaoSheet = true
        isSauceNaoLoading = true
        sauceNaoError = null
        sauceNaoResults = emptyList()
        coroutineScope.launch {
            try {
                sauceNaoResults = sauceNaoRepository.search(bytes, fileName)
            } catch (e: Exception) {
                // 错误文案统一走 PixivApiErrorFormatter 本地化输出，原始异常消息不直透 UI。
                sauceNaoError = PixivApiErrorFormatter.format(e, strings)
            } finally {
                isSauceNaoLoading = false
            }
        }
    }

    val launchPickPhoto: () -> Unit = {
        photoPicker.pickPhoto(title = strings.menuSauceNao) { bytes, fileName ->
            if (bytes != null && bytes.isNotEmpty()) {
                performSauceNaoSearch(bytes, fileName)
            }
        }
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val bottomBarVisibility = LocalBottomBarVisibility.current
    LaunchedEffect(showFilterSheet) {
        bottomBarVisibility.value = !showFilterSheet
    }

    val showingResults = core.isSearching && core.query.isNotBlank()
    val suggestionsListState = rememberLazyListState()
    val searchTypeChips = remember(strings) {
        listOf(
            CouiFilterChip(key = "0", label = strings.searchTypeIllust),
            CouiFilterChip(key = "1", label = strings.searchTypeUser),
            CouiFilterChip(key = "2", label = strings.searchTypeNovel),
        )
    }

    CouiSearchScaffold(
        title = strings.tabSearch,
        searchState = searchBarState,
        searchBar = { collapsedFraction ->
            SearchBar(
                state = searchBarState,
                query = core.query,
                onQueryChange = core.onQueryChange,
                onExpandedChange = { expanded ->
                    if (expanded) {
                        searchExpanded = true
                    } else {
                        searchExpanded = false
                        core.onExitSearch()
                    }
                },
                placeholder = strings.searchPlaceholder,
                cancelText = strings.cancel,
                clearDescription = strings.btnDelete,
                onSearch = core.onSearch,
                shadowProgress = collapsedFraction,
            )
        },
    ) { padding ->
        val header: @Composable () -> Unit = {
            CouiSearchChipsHeader(
                showingResults = showingResults,
                searchTypeChips = searchTypeChips,
                searchTypeIndex = core.searchTypeIndex,
                onSearchTypeSelected = core.onSearchTypeSelected,
                onSauceNaoClick = launchPickPhoto,
                quickFilterRow = if (core.searchTypeIndex == 0) {
                    {
                        CouiSearchQuickFilterRow(
                            core = core,
                            onOpenFilter = { showFilterSheet = true },
                        )
                    }
                } else {
                    null
                },
            )
        }

        if (showingResults) {
            val listPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 8.dp,
            )
            when (core.searchTypeIndex) {
                0 -> CouiIllustSearchResults(
                    query = core.query,
                    sort = core.sort,
                    filterState = core.filterState,
                    repository = searchRepository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                    onIllustClick = onIllustClick,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = listPadding.calculateTopPadding(),
                        end = 8.dp,
                        bottom = listPadding.calculateBottomPadding(),
                    ),
                    header = header,
                )

                1 -> CouiSearchUserResultList(
                    query = core.query,
                    repository = searchRepository,
                    settingsRepository = settingsRepository,
                    onUserClick = onUserClick,
                    contentPadding = listPadding,
                    header = header,
                )

                else -> CouiSearchNovelResultList(
                    query = core.query,
                    novelRepository = novelRepository,
                    onNovelClick = onNovelClick,
                    onUserClick = onUserClick,
                    contentPadding = listPadding,
                    header = header,
                )
            }
        } else {
            CouiSearchSuggestions(
                core = core,
                onIllustClick = onIllustClick,
                onUserClick = onUserClick,
                listState = suggestionsListState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
                header = header,
            )
        }
    }

    if (showFilterSheet) {
        CouiSearchFilterDialog(
            filterState = core.filterState,
            onDismissRequest = { showFilterSheet = false },
            onApply = core.onApplyFilters,
        )
    }

    if (showSauceNaoSheet) {
        CouiSauceNaoDialog(
            isLoading = isSauceNaoLoading,
            error = sauceNaoError,
            results = sauceNaoResults,
            onDismissRequest = { showSauceNaoSheet = false },
            onRetry = {
                currentSauceNaoBytes?.let { bytes ->
                    performSauceNaoSearch(bytes, currentSauceNaoName)
                } ?: launchPickPhoto()
            },
            onPickAnother = launchPickPhoto,
            onIllustClick = onIllustClick,
        )
    }
}

/**
 * 正文首个滚动项：搜索方式胶囊、搜索类型胶囊与作品快捷筛选。
 *
 * 常驻搜索框已上移到顶栏的搜索槽（顶栏作用域能拿到不含自身的采样源），正文只留胶囊；
 * 顶栏没有 actions 槽，以图搜图这类页面动作放在这一行的胶囊里。
 */
@Composable
private fun CouiSearchChipsHeader(
    showingResults: Boolean,
    searchTypeChips: List<CouiFilterChip>,
    searchTypeIndex: Int,
    onSearchTypeSelected: (Int) -> Unit,
    onSauceNaoClick: () -> Unit,
    quickFilterRow: (@Composable () -> Unit)? = null,
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = rememberCouiOverscrollEffect(),
        ) {
            item(key = "sauceNao") {
                CouiAssistChip(
                    text = strings.menuSauceNao,
                    selected = false,
                    onClick = onSauceNaoClick,
                )
            }
        }
        if (showingResults) {
            CouiFilterChipBar(
                chips = searchTypeChips,
                selectedKey = searchTypeIndex.toString(),
                onSelect = { key -> key.toIntOrNull()?.let(onSearchTypeSelected) },
            )
            if (quickFilterRow != null) {
                quickFilterRow()
            }
        }
    }
}

/** 结果区占位外壳：正文首项固定在正文顶部，占位内容居中铺满剩余高度。 */
@Composable
private fun CouiSearchStateBody(
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        header()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            content()
        }
    }
}

/** 作品搜索快捷筛选：排序循环、AI 过滤、收藏数门槛清除与筛选入口，均可独立选中。 */
@Composable
private fun CouiSearchQuickFilterRow(
    core: SearchCoreState,
    onOpenFilter: () -> Unit,
) {
    val strings = LocalStrings.current
    val sortLabel = when (core.sort) {
        "date_asc" -> strings.searchSortOldest
        "popular_desc" -> strings.searchSortPopular
        else -> strings.searchSortLatest
    }
    val bookmarkThreshold = core.filterState.bookmarkThreshold
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "sort") {
            CouiAssistChip(
                text = strings.searchSortLabel.format(sortLabel),
                selected = core.sort != "date_desc",
                onClick = core.onSortClick,
            )
        }
        item(key = "ai") {
            CouiAssistChip(
                text = if (core.filterState.searchAiType == 0) strings.searchAiInclude else strings.searchAiExclude,
                selected = core.filterState.searchAiType != 0,
                onClick = core.onAiTypeClick,
            )
        }
        if (bookmarkThreshold > 0) {
            item(key = "bookmark") {
                CouiAssistChip(
                    text = "${bookmarkThreshold}+ ✕",
                    selected = true,
                    onClick = core.onClearBookmarkThreshold,
                )
            }
        }
        item(key = "filter") {
            CouiAssistChip(
                text = if (core.filterState.hasActiveFilters) strings.searchFilterHasSelected else strings.searchFilter,
                selected = core.filterState.hasActiveFilters,
                onClick = onOpenFilter,
            )
        }
    }
}

/** 作品搜索结果：共享分页状态 + COUI 瀑布流，空列表沿用失败重试优先的三态判定。 */
@Composable
private fun CouiIllustSearchResults(
    query: String,
    sort: String,
    filterState: SearchFilterState,
    repository: SearchRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    onIllustClick: (Long) -> Unit,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val resultsState = rememberSearchIllustResultsState(
        query = query,
        sort = sort,
        filterState = filterState,
        repository = repository,
        banRepository = banRepository,
        settingsRepository = settingsRepository,
    )
    val listState = resultsState.listState
    val gridState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()

    fun loadMore() {
        coroutineScope.launch { listState.loadMore() }
    }

    val currentIllusts = listState.filteredIllusts
    when {
        currentIllusts == null && listState.initialError == null -> CouiSearchStateBody(
            contentPadding = contentPadding,
            header = header,
        ) {
            CouiLoadingPlaceholder()
        }

        currentIllusts == null -> CouiSearchStateBody(
            contentPadding = contentPadding,
            header = header,
        ) {
            CouiErrorPlaceholder(
                error = listState.initialError,
                onRetry = resultsState.retry,
            )
        }

        else -> {
            val filtered = remember(currentIllusts, filterState.ugoiraFilter, filterState.ratioFilter) {
                applySearchClientFilters(currentIllusts, filterState.ugoiraFilter, filterState.ratioFilter)
            }
            if (filtered.isEmpty() && !listState.isLoadingMore) {
                when (
                    resolveSearchEmptyPhase(
                        hasMore = listState.nextUrl != null,
                        isLoadingMore = listState.isLoadingMore,
                        hasLoadMoreError = listState.loadMoreError != null,
                    )
                ) {
                    SearchEmptyPhase.Retry -> CouiSearchStateBody(
                        contentPadding = contentPadding,
                        header = header,
                    ) {
                        CouiPlaceholder(
                            message = CouiErrorMessage(listState.loadMoreError),
                            actionText = strings.retry,
                            onAction = ::loadMore,
                        )
                    }

                    SearchEmptyPhase.LoadingMore -> {
                        LaunchedEffect(filtered.isEmpty(), listState.nextUrl) { loadMore() }
                        CouiSearchStateBody(
                            contentPadding = contentPadding,
                            header = header,
                        ) {
                            CouiLoadingPlaceholder()
                        }
                    }

                    SearchEmptyPhase.Empty -> CouiSearchStateBody(
                        contentPadding = contentPadding,
                        header = header,
                    ) {
                        CouiPlaceholder(message = strings.searchEmptyIllust)
                    }
                }
            } else {
                CouiIllustGrid(
                    illusts = filtered,
                    state = gridState,
                    onIllustClick = onIllustClick,
                    contentPadding = contentPadding,
                    header = header,
                    hasMore = listState.nextUrl != null,
                    isLoadingMore = listState.isLoadingMore,
                    loadMoreError = listState.loadMoreError,
                    isRefreshing = listState.isRefreshing,
                    onLoadMore = ::loadMore,
                )
            }
        }
    }
}

/** 未进入搜索态时的推荐区：搜索头、ID/URL 直达入口、热门标签与搜索历史。 */
@Composable
private fun CouiSearchSuggestions(
    core: SearchCoreState,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val queryTarget = core.queryTarget
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_search_header") {
            header()
        }

        if (queryTarget != null) {
            item(key = "coui_search_target") {
                val showIllustTarget = queryTarget !is SearchQueryTarget.UserId
                val showUserTarget = queryTarget !is SearchQueryTarget.IllustId
                CouiSettingsGroup {
                    if (showIllustTarget) {
                        CouiActionRow(
                            title = "${strings.copyTextChipIllustId}: ${queryTarget.id}",
                            onClick = { onIllustClick(queryTarget.id) },
                        )
                    }
                    if (showUserTarget) {
                        if (showIllustTarget) {
                            CouiRowDivider()
                        }
                        CouiActionRow(
                            title = "${strings.copyTextChipUserId}: ${queryTarget.id}",
                            onClick = { onUserClick(queryTarget.id) },
                        )
                    }
                }
            }
        }

        item(key = "coui_search_hot_title") {
            CouiBodyTitle(text = strings.searchHotTags)
        }

        when {
            core.isTrendLoading -> item(key = "coui_search_hot_loading") {
                CouiSearchHint(text = strings.loading)
            }

            core.trendError != null -> item(key = "coui_search_hot_error") {
                CouiSearchHint(
                    text = CouiErrorMessage(core.trendError),
                    actionText = strings.retry,
                    onAction = core.onRetryTrend,
                )
            }

            core.trendTags.isNullOrEmpty() -> item(key = "coui_search_hot_empty") {
                CouiSearchHint(text = strings.searchHotTagsEmpty)
            }

            else -> item(key = "coui_search_hot_tags") {
                CouiSearchTrendGroup(
                    tags = core.trendTags.orEmpty(),
                    onTagClick = core.onTagClick,
                )
            }
        }

        if (core.searchHistory.isNotEmpty()) {
            item(key = "coui_search_history_title") {
                CouiBodyTitle(text = strings.searchHistory)
            }
            item(key = "coui_search_history_list") {
                CouiSettingsGroup {
                    core.searchHistory.forEachIndexed { index, history ->
                        if (index > 0) {
                            CouiRowDivider()
                        }
                        Item(
                            title = { CouiItemTitle(text = history) },
                            trailing = {
                                CouiActionButton(
                                    icon = MiuixIcons.Close,
                                    contentDescription = strings.btnDelete,
                                    onClick = { core.onHistoryRemove(history) },
                                )
                            },
                            onClick = { core.onTagClick(history) },
                        )
                    }
                }
            }
            item(key = "coui_search_history_clear") {
                CouiSingleGroup {
                    CouiActionRow(
                        title = strings.searchClearHistory,
                        onClick = core.onClearHistory,
                        destructive = true,
                    )
                }
            }
        }
    }
}

/** 热门标签分组：整卡行点击后按原始 tag 起搜。 */
@Composable
private fun CouiSearchTrendGroup(
    tags: List<TrendTag>,
    onTagClick: (String) -> Unit,
) {
    CouiSettingsGroup {
        tags.forEachIndexed { index, tag ->
            if (index > 0) {
                CouiRowDivider()
            }
            CouiActionRow(
                title = tag.translatedName ?: tag.tag,
                onClick = { onTagClick(tag.tag) },
            )
        }
    }
}

/** 推荐区提示卡：加载 / 错误重试 / 空态共用，动作以 core 文本按钮呈现。 */
@Composable
private fun CouiSearchHint(
    text: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CouiSingleGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CouiItemSummary(text = text)
            if (actionText != null && onAction != null) {
                CouiTextButton(text = actionText, onClick = onAction, filled = false)
            }
        }
    }
}
