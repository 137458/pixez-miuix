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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.Scaffold
import com.itos.coui.search.SearchBar
import com.itos.coui.search.rememberCouiSearchBarState
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
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
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.LocalBottomBarVisibility
import com.perol.pixez.shared.ui.screens.SauceNaoBottomSheet
import com.perol.pixez.shared.ui.screens.SearchCoreState
import com.perol.pixez.shared.ui.screens.SearchEmptyPhase
import com.perol.pixez.shared.ui.screens.SearchFilterBottomSheet
import com.perol.pixez.shared.ui.screens.SearchFilterState
import com.perol.pixez.shared.ui.screens.SearchQueryTarget
import com.perol.pixez.shared.ui.screens.applySearchClientFilters
import com.perol.pixez.shared.ui.screens.rememberSearchCoreState
import com.perol.pixez.shared.ui.screens.rememberSearchIllustResultsState
import com.perol.pixez.shared.ui.screens.resolveSearchEmptyPhase
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Photos

/**
 * COUI 搜索页（第一阶段）：官方 SearchBar + 搜索类型胶囊 + 快捷筛选 + 历史/热搜 + 作品结果瀑布流。
 *
 * 查询词、搜索态、筛选条件、历史与热搜统一由 [rememberSearchCoreState] 承载；
 * 作品结果用 [rememberSearchIllustResultsState] + COUI 瀑布流。画师/小说结果与筛选抽屉、
 * SauceNAO 抽屉本阶段仍复用 MIUIX 实现，第二阶段再统一收口为 COUI 组件。
 */
@Composable
internal fun CouiSearchScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
    initialQuery: String = "",
) {
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
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
    var searchExpanded by rememberSaveable { mutableStateOf(true) }
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

    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val showingResults = core.isSearching && core.query.isNotBlank()
    val suggestionsListState = rememberLazyListState()
    val searchTypeChips = remember(strings) {
        listOf(
            CouiFilterChip(key = "0", label = strings.searchTypeIllust),
            CouiFilterChip(key = "1", label = strings.searchTypeUser),
            CouiFilterChip(key = "2", label = strings.searchTypeNovel),
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    BasicText(
                        text = if (showingResults) core.query else strings.tabSearch,
                        style = LocalCouiTextStyle.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (showingResults) {
                        CouiActionButton(
                            icon = MiuixIcons.Back,
                            contentDescription = strings.back,
                            onClick = core.onExitSearch,
                        )
                    }
                },
                actions = {
                    CouiActionButton(
                        icon = MiuixIcons.Photos,
                        contentDescription = strings.menuSauceNao,
                        onClick = launchPickPhoto,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = CouiTopAppBarDefaults.colors(
                    backgroundColor = CouiTheme.colorScheme.background,
                    dark = CouiTheme.isDark,
                ),
            )
        },
    ) { innerPadding ->
        val density = LocalDensity.current
        // 顶栏折叠位移在测量期读取，组合期算成字面量会冻结首帧的顶栏高度。
        val contentPadding = remember(scrollBehavior, innerPadding, density) {
            scrollBehavior.state.contentPadding(innerPadding, density)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CouiTheme.colorScheme.background)
                .padding(contentPadding),
        ) {
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
            )

            if (showingResults) {
                CouiFilterChipBar(
                    chips = searchTypeChips,
                    selectedKey = core.searchTypeIndex.toString(),
                    onSelect = { key -> key.toIntOrNull()?.let(core.onSearchTypeSelected) },
                )
                if (core.searchTypeIndex == 0) {
                    CouiSearchQuickFilterRow(
                        core = core,
                        onOpenFilter = { showFilterSheet = true },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (showingResults) {
                    val resultPadding = PaddingValues(
                        start = 8.dp,
                        top = 4.dp,
                        end = 8.dp,
                        bottom = bottomBarPadding + 8.dp,
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
                            contentPadding = resultPadding,
                        )
                        1 -> CouiSearchUserResultList(
                            query = core.query,
                            repository = searchRepository,
                            settingsRepository = settingsRepository,
                            onUserClick = onUserClick,
                            contentPadding = resultPadding,
                        )
                        else -> CouiSearchNovelResultList(
                            query = core.query,
                            novelRepository = novelRepository,
                            onNovelClick = onNovelClick,
                            onUserClick = onUserClick,
                            contentPadding = resultPadding,
                        )
                    }
                } else {
                    CouiSearchSuggestions(
                        core = core,
                        onIllustClick = onIllustClick,
                        onUserClick = onUserClick,
                        listState = suggestionsListState,
                        bottomBarPadding = bottomBarPadding,
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        SearchFilterBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            filterState = core.filterState,
            onApply = core.onApplyFilters,
        )
    }

    if (showSauceNaoSheet) {
        SauceNaoBottomSheet(
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
        currentIllusts == null && listState.initialError == null -> CouiStatePlaceholder(
            message = strings.loading,
        )
        currentIllusts == null -> CouiStatePlaceholder(
            message = listState.initialError?.let { PixivApiErrorFormatter.format(it, strings) } ?: strings.retry,
            actionText = strings.retry,
            onAction = resultsState.retry,
        )
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
                    SearchEmptyPhase.Retry -> CouiStatePlaceholder(
                        message = listState.loadMoreError?.let { PixivApiErrorFormatter.format(it, strings) }
                            ?: strings.retry,
                        actionText = strings.retry,
                        onAction = ::loadMore,
                    )
                    SearchEmptyPhase.LoadingMore -> {
                        LaunchedEffect(filtered.isEmpty(), listState.nextUrl) { loadMore() }
                        CouiStatePlaceholder(message = strings.loading)
                    }
                    SearchEmptyPhase.Empty -> CouiStatePlaceholder(message = strings.searchEmptyIllust)
                }
            } else {
                CouiIllustGrid(
                    illusts = filtered,
                    state = gridState,
                    onIllustClick = onIllustClick,
                    contentPadding = contentPadding,
                    hasMore = listState.nextUrl != null,
                    isLoadingMore = listState.isLoadingMore,
                    loadMoreError = listState.loadMoreError,
                    onLoadMore = ::loadMore,
                )
            }
        }
    }
}

/** 未进入搜索态时的推荐区：ID/URL 直达入口、热门标签与搜索历史。 */
@Composable
private fun CouiSearchSuggestions(
    core: SearchCoreState,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    listState: LazyListState,
    bottomBarPadding: Dp,
) {
    val strings = LocalStrings.current
    val queryTarget = core.queryTarget
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomBarPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (queryTarget != null) {
            item(key = "coui_search_target") {
                CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    if (queryTarget !is SearchQueryTarget.UserId) {
                        CouiSuggestionRow(
                            text = "${strings.copyTextChipIllustId}: ${queryTarget.id}",
                            onClick = { onIllustClick(queryTarget.id) },
                        )
                    }
                    if (queryTarget !is SearchQueryTarget.IllustId) {
                        CouiSuggestionRow(
                            text = "${strings.copyTextChipUserId}: ${queryTarget.id}",
                            onClick = { onUserClick(queryTarget.id) },
                        )
                    }
                }
            }
        }

        item(key = "coui_search_hot_title") {
            CouiSectionTitle(text = strings.searchHotTags)
        }

        when {
            core.isTrendLoading -> item(key = "coui_search_hot_loading") {
                CouiHintRow(text = strings.loading)
            }
            core.trendError != null -> item(key = "coui_search_hot_error") {
                CouiHintRow(
                    text = PixivApiErrorFormatter.format(core.trendError, strings),
                    actionText = strings.retry,
                    onAction = core.onRetryTrend,
                )
            }
            core.trendTags.isNullOrEmpty() -> item(key = "coui_search_hot_empty") {
                CouiHintRow(text = strings.searchHotTagsEmpty)
            }
            else -> item(key = "coui_search_hot_tags") {
                CouiTrendTagCard(tags = core.trendTags.orEmpty(), onTagClick = core.onTagClick)
            }
        }

        if (core.searchHistory.isNotEmpty()) {
            item(key = "coui_search_history_title") {
                CouiSectionTitle(text = strings.searchHistory)
            }
            item(key = "coui_search_history_list") {
                CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    core.searchHistory.forEach { history ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = history,
                                kind = CouiTextKind.ItemTitle,
                                color = CouiTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { core.onTagClick(history) }
                                    .padding(vertical = 10.dp),
                            )
                            CouiActionButton(
                                icon = MiuixIcons.Close,
                                contentDescription = strings.btnDelete,
                                onClick = { core.onHistoryRemove(history) },
                            )
                        }
                    }
                }
            }
            item(key = "coui_search_history_clear") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CouiTheme.colorScheme.card)
                        .clickable(onClick = core.onClearHistory)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.searchClearHistory,
                        kind = CouiTextKind.ItemTitle,
                        color = CouiTheme.colorScheme.accent,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiTrendTagCard(
    tags: List<TrendTag>,
    onTagClick: (String) -> Unit,
) {
    CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
        tags.forEach { tag ->
            CouiSuggestionRow(
                text = tag.translatedName ?: tag.tag,
                onClick = { onTagClick(tag.tag) },
            )
        }
    }
}

@Composable
private fun CouiSectionTitle(text: String) {
    Text(
        text = text,
        kind = CouiTextKind.GroupTitle,
        color = CouiTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun CouiSuggestionRow(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        kind = CouiTextKind.ItemTitle,
        color = CouiTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/** 推荐区占位行：加载 / 错误重试 / 空态共用。 */
@Composable
private fun CouiHintRow(
    text: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = text,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            if (actionText != null && onAction != null) {
                Text(
                    text = actionText,
                    kind = CouiTextKind.ItemTitle,
                    color = CouiTheme.colorScheme.accent,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onAction),
                )
            }
        }
    }
}
