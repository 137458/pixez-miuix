package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import kotlinx.coroutines.launch
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.LocalBottomBarVisibility
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页：搜索栏 + 真实热门标签 + 历史记录，输入后展示真实搜索结果。
 *
 * 本函数仅保留状态声明、数据加载副作用与 Scaffold 骨架编排：
 * 顶部栏见 [SearchTopAppBar] / [SearchInputBar] / [SearchResultFilterBar]，
 * 结果区见 [SearchIllustResultGrid] / [SearchUserResultList]，
 * 筛选面板见 [SearchFilterBottomSheet]，推荐区见 [SearchSuggestions]。
 */
@Composable
fun SearchScreen(
    onIllustClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    repository: SearchRepository,
    settingsRepository: SettingsRepository,
    banRepository: BanRepository,
    initialQuery: String = "",
) {
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    // 存在初始查询词或点击标签后进入搜索结果模式。
    var isSearching by rememberSaveable { mutableStateOf(initialQuery.isNotBlank()) }

    // 搜索类型：0 = 作品，1 = 画师。
    var searchTypeIndex by rememberSaveable { mutableIntStateOf(0) }
    val searchTypes = listOf(strings.searchTypeIllust, strings.searchTypeUser)

    // 搜索筛选状态：排序、搜索目标、AI 类型、收藏数阈值、Ugoira 过滤与时间范围（仅作品搜索有效）。
    var sort by rememberSaveable {
        mutableStateOf(settingsRepository.searchSort)
    }
    var searchTarget by rememberSaveable {
        mutableStateOf(settingsRepository.searchTarget)
    }
    var searchAiType by rememberSaveable {
        mutableIntStateOf(settingsRepository.searchAiType)
    }
    var bookmarkThreshold by rememberSaveable {
        mutableIntStateOf(settingsRepository.searchBookmarkThreshold)
    }
    var ugoiraFilter by rememberSaveable {
        mutableIntStateOf(settingsRepository.searchUgoiraFilter)
    }
    var ratioFilter by rememberSaveable {
        mutableIntStateOf(0)
    }
    var startDate by rememberSaveable {
        mutableStateOf(settingsRepository.searchStartDate)
    }
    var endDate by rememberSaveable {
        mutableStateOf(settingsRepository.searchEndDate)
    }

    // 筛选条件变化时持久化回写设置。
    // 首帧的值本身就来自设置，比较后再写入，避免重复持久化与 changeVersion 无谓自增。
    LaunchedEffect(sort) {
        if (settingsRepository.searchSort != sort) settingsRepository.searchSort = sort
    }
    LaunchedEffect(searchTarget) {
        if (settingsRepository.searchTarget != searchTarget) settingsRepository.searchTarget = searchTarget
    }
    LaunchedEffect(searchAiType) {
        if (settingsRepository.searchAiType != searchAiType) settingsRepository.searchAiType = searchAiType
    }
    LaunchedEffect(bookmarkThreshold) {
        if (settingsRepository.searchBookmarkThreshold != bookmarkThreshold) {
            settingsRepository.searchBookmarkThreshold = bookmarkThreshold
        }
    }
    LaunchedEffect(ugoiraFilter) {
        if (settingsRepository.searchUgoiraFilter != ugoiraFilter) {
            settingsRepository.searchUgoiraFilter = ugoiraFilter
        }
    }
    LaunchedEffect(startDate) {
        if (settingsRepository.searchStartDate != startDate) settingsRepository.searchStartDate = startDate
    }
    LaunchedEffect(endDate) {
        if (settingsRepository.searchEndDate != endDate) settingsRepository.searchEndDate = endDate
    }

    // 热门标签重试计数，作为 produceState 的 key 触发重新加载。
    var trendRetryCount by rememberSaveable { mutableIntStateOf(0) }

    // 进入页面时加载真实热门标签。
    val trendState = produceState<Result<List<TrendTag>>?>(
        initialValue = null,
        repository,
        trendRetryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.getTrendTags() }
    }

    // 搜索历史从设置中读取并持久化回写。
    var searchHistory by rememberSaveable {
        mutableStateOf(settingsRepository.searchHistory)
    }
    val updateHistory: (List<String>) -> Unit = { newHistory ->
        searchHistory = newHistory
        settingsRepository.searchHistory = newHistory
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    val suggestionsListState = rememberLazyListState()
    val illustGridState = rememberLazyStaggeredGridState()
    val userListState = rememberLazyListState()

    var isSearchCollapsed by rememberSaveable { mutableStateOf(false) }

    val isCurrentListAtTop by remember(isSearching, query, searchTypeIndex) {
        derivedStateOf {
            if (!isSearching || query.isBlank()) {
                suggestionsListState.firstVisibleItemIndex == 0 && suggestionsListState.firstVisibleItemScrollOffset == 0
            } else if (searchTypeIndex == 0) {
                illustGridState.firstVisibleItemIndex == 0 && illustGridState.firstVisibleItemScrollOffset == 0
            } else {
                userListState.firstVisibleItemIndex == 0 && userListState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    LaunchedEffect(isCurrentListAtTop) {
        if (isCurrentListAtTop) {
            isSearchCollapsed = false
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }

    val searchNestedScrollConnection = remember(scrollBehavior) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f) {
                    // 手指向上推（向下浏览内容）：
                    // 1. 同步累加 contentOffset，驱动 BlurredBar 渐进式毛玻璃在滑动时平滑淡入
                    scrollBehavior.state.contentOffset += delta

                    // 2. 当向上滑动超过阈值时协同折叠大标题与全宽搜索框
                    if (delta < -8f) {
                        val limit = scrollBehavior.state.heightOffsetLimit
                        if (limit < 0f) {
                            scrollBehavior.state.heightOffset = (scrollBehavior.state.heightOffset + delta).coerceIn(limit, 0f)
                        }
                        if (!isSearchCollapsed) isSearchCollapsed = true
                    }
                }
                // 注意：手指往下拉时不在此处盲目展开！必须等列表真正滚回最顶端时才展开，避免在列表中间浏览时误触发放大。
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // 手指向下拉回：
                if (consumed.y > 0f) {
                    scrollBehavior.state.contentOffset = (scrollBehavior.state.contentOffset + consumed.y).coerceAtMost(0f)
                }

                // 当列表已滑动到最顶端无法再继续往下拉（available.y > 0 表示列表已触顶过冲）：
                // 顺畅展开大标题与搜索框，并归零 contentOffset
                if (available.y > 0f) {
                    scrollBehavior.state.contentOffset = (scrollBehavior.state.contentOffset + available.y).coerceAtMost(0f)
                    if (available.y > 4f) {
                        if (isSearchCollapsed) isSearchCollapsed = false
                        val limit = scrollBehavior.state.heightOffsetLimit
                        if (limit < 0f) {
                            scrollBehavior.state.heightOffset = (scrollBehavior.state.heightOffset + available.y).coerceIn(limit, 0f)
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val bottomBarVisibility = LocalBottomBarVisibility.current
    LaunchedEffect(showFilterSheet) {
        bottomBarVisibility.value = !showFilterSheet
    }

    val currentTopBarTitle = if (isSearching && query.isNotBlank()) query else strings.tabSearch

    // 当前筛选条件快照：顶部筛选标签行、结果区与筛选抽屉共用同一份值对象。
    val currentFilterState = SearchFilterState(
        searchTarget = searchTarget,
        searchAiType = searchAiType,
        bookmarkThreshold = bookmarkThreshold,
        ugoiraFilter = ugoiraFilter,
        ratioFilter = ratioFilter,
        startDate = startDate,
        endDate = endDate,
        hIsNotAllow = settingsRepository.hIsNotAllow,
    )

    // 顶栏返回 / 展开时统一归零滚动偏移，让大标题与搜索框平滑复位。
    val onResetSearchScroll: () -> Unit = {
        coroutineScope.launch {
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SearchTopAppBar(
                        title = currentTopBarTitle,
                        scrollBehavior = scrollBehavior,
                        backdrop = backdrop,
                        isSearching = isSearching,
                        isSearchCollapsed = isSearchCollapsed,
                        onBack = {
                            isSearching = false
                            query = ""
                            isSearchCollapsed = false
                            onResetSearchScroll()
                        },
                        onExpand = {
                            isSearchCollapsed = false
                            onResetSearchScroll()
                        },
                    )

                    val queryTarget = remember(query) { parseSearchQueryTarget(query) }

                    SearchInputBar(
                        isSearchCollapsed = isSearchCollapsed,
                        query = query,
                        onQueryChange = {
                            query = it
                            if (it.isBlank()) isSearching = false
                        },
                        onSearch = {
                            val trimmed = query.trim()
                            if (trimmed.isNotBlank()) {
                                // 将新搜索词加入历史（去重，最多保留 20 条）。
                                updateHistory(
                                    (listOf(trimmed) + searchHistory.filter { it != trimmed }).take(20)
                                )
                                when (val target = parseSearchQueryTarget(trimmed)) {
                                    is SearchQueryTarget.IllustId -> onIllustClick(target.id)
                                    is SearchQueryTarget.UserId -> onUserClick(target.id)
                                    is SearchQueryTarget.NumericId -> {
                                        if (searchTypeIndex == 1) {
                                            onUserClick(target.id)
                                        } else {
                                            onIllustClick(target.id)
                                        }
                                    }
                                    null -> isSearching = true
                                }
                            }
                        },
                    )

                    if (isSearching && query.isNotBlank()) {
                        SearchResultFilterBar(
                            isSearchCollapsed = isSearchCollapsed,
                            searchTypes = searchTypes,
                            searchTypeIndex = searchTypeIndex,
                            onSearchTypeSelected = { searchTypeIndex = it },
                            sort = sort,
                            onSortClick = {
                                sort = when (sort) {
                                    "date_desc" -> "popular_desc"
                                    "popular_desc" -> "date_asc"
                                    else -> "date_desc"
                                }
                            },
                            searchAiType = searchAiType,
                            onAiTypeClick = { searchAiType = if (searchAiType == 0) 1 else 0 },
                            bookmarkThreshold = bookmarkThreshold,
                            onClearBookmarkThreshold = { bookmarkThreshold = 0 },
                            hasActiveFilters = currentFilterState.hasActiveFilters,
                            onOpenFilter = { showFilterSheet = true },
                            backdrop = backdrop,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        val contentTopPadding = paddingValues.calculateTopPadding() + 8.dp
        val effectiveContentPadding = remember(paddingValues) {
            PaddingValues(
                start = 8.dp,
                top = contentTopPadding,
                end = 8.dp,
                bottom = bottomBarPadding,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop)
                .nestedScroll(searchNestedScrollConnection),
        ) {
            if (isSearching && query.isNotBlank()) {
                when (searchTypeIndex) {
                    0 -> SearchIllustResultGrid(
                        query = query,
                        sort = sort,
                        filterState = currentFilterState,
                        repository = repository,
                        banRepository = banRepository,
                        settingsRepository = settingsRepository,
                        onIllustClick = onIllustClick,
                        scrollBehavior = scrollBehavior,
                        gridState = illustGridState,
                        contentPadding = effectiveContentPadding,
                    )
                    1 -> SearchUserResultList(
                        query = query,
                        repository = repository,
                        settingsRepository = settingsRepository,
                        onUserClick = onUserClick,
                        scrollBehavior = scrollBehavior,
                        listState = userListState,
                        contentPadding = effectiveContentPadding,
                    )
                }

                if (showFilterSheet) {
                    SearchFilterBottomSheet(
                        onDismissRequest = { showFilterSheet = false },
                        filterState = currentFilterState,
                        onApply = { newState ->
                            searchTarget = newState.searchTarget
                            searchAiType = newState.searchAiType
                            bookmarkThreshold = newState.bookmarkThreshold
                            ugoiraFilter = newState.ugoiraFilter
                            ratioFilter = newState.ratioFilter
                            startDate = newState.startDate
                            endDate = newState.endDate
                            settingsRepository.hIsNotAllow = newState.hIsNotAllow
                        },
                    )
                }
            } else {
                val trendResult = trendState.value
                val currentTarget = remember(query) { parseSearchQueryTarget(query) }
                SearchSuggestions(
                    trendTags = trendResult?.getOrNull().orEmpty(),
                    searchHistory = searchHistory,
                    isLoadingTrend = trendResult == null,
                    trendError = trendResult?.exceptionOrNull(),
                    scrollBehavior = scrollBehavior,
                    listState = suggestionsListState,
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        top = contentTopPadding,
                        end = 0.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    queryTarget = currentTarget,
                    onIllustIdClick = onIllustClick,
                    onUserIdClick = onUserClick,
                    onTagClick = { tag ->
                        val target = parseSearchQueryTarget(tag)
                        if (target != null) {
                            when (target) {
                                is SearchQueryTarget.IllustId -> onIllustClick(target.id)
                                is SearchQueryTarget.UserId -> onUserClick(target.id)
                                is SearchQueryTarget.NumericId -> {
                                    if (searchTypeIndex == 1) onUserClick(target.id) else onIllustClick(target.id)
                                }
                            }
                        } else {
                            query = tag
                            isSearching = true
                        }
                    },
                    onHistoryRemove = { history ->
                        updateHistory(searchHistory.filter { it != history })
                    },
                    onClearHistory = { updateHistory(emptyList()) },
                    onRetryTrend = { trendRetryCount++ },
                )
            }
        }
    }
}


/**
 * 搜索筛选条件状态封装，消除 Data Clump 代码坏味道。
 */
data class SearchFilterState(
    val searchTarget: String = "partial_match_for_tags",
    val searchAiType: Int = 0,
    val bookmarkThreshold: Int = 0,
    val ugoiraFilter: Int = 0,
    val ratioFilter: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val hIsNotAllow: Boolean = false,
) {
    val hasActiveFilters: Boolean
        get() = bookmarkThreshold > 0 || searchAiType != 0 || ugoiraFilter != 0 || ratioFilter != 0 ||
            startDate.isNotBlank() || endDate.isNotBlank() || searchTarget != "partial_match_for_tags" ||
            hIsNotAllow
}

sealed interface SearchQueryTarget {
    val id: Int
    data class NumericId(override val id: Int) : SearchQueryTarget
    data class IllustId(override val id: Int) : SearchQueryTarget
    data class UserId(override val id: Int) : SearchQueryTarget
}

private val ILLUST_URL_REGEX = Regex("""(?:artworks/|illust_id=|pixiv://illusts?/)(\d+)""", RegexOption.IGNORE_CASE)
private val USER_URL_REGEX = Regex("""(?:users/|member\.php\?id=|pixiv://users?/)(\d+)""", RegexOption.IGNORE_CASE)

internal fun parseSearchQueryTarget(rawQuery: String): SearchQueryTarget? {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return null

    if (trimmed.all { it.isDigit() }) {
        val numeric = trimmed.toIntOrNull()
        if (numeric != null && numeric > 0) {
            return SearchQueryTarget.NumericId(numeric)
        }
        return null
    }

    ILLUST_URL_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.let {
        return SearchQueryTarget.IllustId(it)
    }

    USER_URL_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.let {
        return SearchQueryTarget.UserId(it)
    }

    return null
}