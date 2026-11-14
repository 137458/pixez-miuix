package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
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
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.platform.PlatformPhotoPicker
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.LocalSauceNaoRepository
import com.perol.pixez.shared.ui.LocalSearchRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.LocalBottomBarVisibility
import com.perol.pixez.shared.ui.state.CollapseAction
import com.perol.pixez.shared.ui.state.CollapseGesture
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页：搜索栏 + 真实热门标签 + 历史记录，输入后展示真实搜索结果。
 *
 * 本函数仅保留状态声明、数据加载副作用与 Scaffold 骨架编排：
 * 顶部栏见 [SearchTopAppBar] / [SearchInputBar] / [SearchResultFilterBar]，
 * 结果区见 [SearchIllustResultGrid] / [SearchUserResultList] / [SearchNovelResultList]，
 * 筛选面板见 [SearchFilterBottomSheet]，推荐区见 [SearchSuggestions]。
 */
@Composable
fun SearchScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
    initialQuery: String = "",
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalSearchRepository.current
    val novelRepository = LocalNovelRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val banRepository = LocalBanRepository.current
    val sauceNaoRepository = LocalSauceNaoRepository.current
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
    val photoPicker = remember { PlatformPhotoPicker() }

    var showSauceNaoSheet by remember { mutableStateOf(false) }
    var isSauceNaoLoading by remember { mutableStateOf(false) }
    var sauceNaoError by remember { mutableStateOf<String?>(null) }
    var sauceNaoResults by remember { mutableStateOf<List<SauceNaoResult>>(emptyList()) }
    var currentSauceNaoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var currentSauceNaoName by remember { mutableStateOf<String?>(null) }

    // 查询词、搜索态、筛选条件、历史与热搜统一由皮肤无关状态承载。
    val core = rememberSearchCoreState(
        initialQuery = initialQuery,
        onIllustClick = onIllustClick,
        onUserClick = onUserClick,
        onNovelClick = onNovelClick,
    )

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    val performSauceNaoSearch: (ByteArray, String?) -> Unit = { bytes, fileName ->
        currentSauceNaoBytes = bytes
        currentSauceNaoName = fileName
        showSauceNaoSheet = true
        isSauceNaoLoading = true
        sauceNaoError = null
        sauceNaoResults = emptyList()
        coroutineScope.launch {
            try {
                val list = sauceNaoRepository.search(bytes, fileName)
                sauceNaoResults = list
            } catch (e: Exception) {
                // 错误文案统一走 PixivApiErrorFormatter 本地化输出，原始异常消息不直透 UI。
                sauceNaoError = PixivApiErrorFormatter.format(e, strings)
            } finally {
                isSauceNaoLoading = false
            }
        }
    }

    val launchPickPhoto: () -> Unit = {
        // 标题由调用方注入（DN15：platform actual 不再读 AppStrings 单例）。
        photoPicker.pickPhoto(title = strings.menuSauceNao) { bytes, fileName ->
            if (bytes != null && bytes.isNotEmpty()) {
                performSauceNaoSearch(bytes, fileName)
            }
        }
    }

    val searchTypes = listOf(strings.searchTypeIllust, strings.searchTypeUser, strings.searchTypeNovel)

    val suggestionsListState = rememberLazyListState()
    val illustGridState = rememberLazyStaggeredGridState()
    val userListState = rememberLazyListState()
    val novelListState = rememberLazyListState()

    var isSearchCollapsed by rememberSaveable { mutableStateOf(false) }

    val isCurrentListAtTop by remember(core.isSearching, core.query, core.searchTypeIndex) {
        derivedStateOf {
            if (!core.isSearching || core.query.isBlank()) {
                suggestionsListState.isScrolledToTop()
            } else {
                when (core.searchTypeIndex) {
                    0 -> illustGridState.isScrolledToTop()
                    1 -> userListState.isScrolledToTop()
                    else -> novelListState.isScrolledToTop()
                }
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

    // 折叠时序与阈值收敛到 CollapseGesture 纯函数（UI-7，阈值见 AppConstants.Layout）；
    // 此处只负责把决策结果写回滚动状态。下拉不在此处展开，只有回顶过冲才展开。
    val searchNestedScrollConnection = remember(scrollBehavior) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val step = CollapseGesture.resolvePreScroll(
                    delta = available.y,
                    heightOffset = scrollBehavior.state.heightOffset,
                    contentOffset = scrollBehavior.state.contentOffset,
                    heightOffsetLimit = scrollBehavior.state.heightOffsetLimit,
                    isCollapsed = isSearchCollapsed,
                )
                scrollBehavior.state.heightOffset = step.heightOffset
                scrollBehavior.state.contentOffset = step.contentOffset
                if (step.action == CollapseAction.Collapse) isSearchCollapsed = true
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val step = CollapseGesture.resolvePostScroll(
                    consumed = consumed.y,
                    availableOverscroll = available.y,
                    heightOffset = scrollBehavior.state.heightOffset,
                    contentOffset = scrollBehavior.state.contentOffset,
                    heightOffsetLimit = scrollBehavior.state.heightOffsetLimit,
                    isCollapsed = isSearchCollapsed,
                )
                scrollBehavior.state.heightOffset = step.heightOffset
                scrollBehavior.state.contentOffset = step.contentOffset
                if (step.action == CollapseAction.Expand) isSearchCollapsed = false
                return Offset.Zero
            }
        }
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val bottomBarVisibility = LocalBottomBarVisibility.current
    LaunchedEffect(showFilterSheet) {
        bottomBarVisibility.value = !showFilterSheet
    }

    val currentTopBarTitle = if (core.isSearching && core.query.isNotBlank()) core.query else strings.tabSearch

    // 当前筛选条件快照：顶部筛选标签行、结果区与筛选抽屉共用同一份值对象。
    val currentFilterState = core.filterState

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
                        isSearching = core.isSearching,
                        isSearchCollapsed = isSearchCollapsed,
                        onBack = {
                            core.onExitSearch()
                            isSearchCollapsed = false
                            onResetSearchScroll()
                        },
                        onExpand = {
                            isSearchCollapsed = false
                            onResetSearchScroll()
                        },
                    )

                    val queryTarget = core.queryTarget

                    SearchInputBar(
                        isSearchCollapsed = isSearchCollapsed,
                        query = core.query,
                        onQueryChange = core.onQueryChange,
                        onSearch = core.onSearch,
                        onSauceNao = launchPickPhoto,
                    )

                    if (core.isSearching && core.query.isNotBlank()) {
                        SearchResultFilterBar(
                            isSearchCollapsed = isSearchCollapsed,
                            searchTypes = searchTypes,
                            searchTypeIndex = core.searchTypeIndex,
                            onSearchTypeSelected = core.onSearchTypeSelected,
                            sort = core.sort,
                            onSortClick = core.onSortClick,
                            searchAiType = currentFilterState.searchAiType,
                            onAiTypeClick = core.onAiTypeClick,
                            bookmarkThreshold = currentFilterState.bookmarkThreshold,
                            onClearBookmarkThreshold = core.onClearBookmarkThreshold,
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
        val effectiveContentPadding = PaddingValues(
            start = 8.dp,
            top = contentTopPadding,
            end = 8.dp,
            bottom = bottomBarPadding,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop)
                .nestedScroll(searchNestedScrollConnection),
        ) {
            if (core.isSearching && core.query.isNotBlank()) {
                when (core.searchTypeIndex) {
                    0 -> SearchIllustResultGrid(
                        query = core.query,
                        sort = core.sort,
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
                        query = core.query,
                        repository = repository,
                        settingsRepository = settingsRepository,
                        onUserClick = onUserClick,
                        scrollBehavior = scrollBehavior,
                        listState = userListState,
                        contentPadding = effectiveContentPadding,
                    )
                    2 -> SearchNovelResultList(
                        query = core.query,
                        novelRepository = novelRepository,
                        onNovelClick = onNovelClick,
                        onUserClick = onUserClick,
                        scrollBehavior = scrollBehavior,
                        listState = novelListState,
                        contentPadding = effectiveContentPadding,
                    )
                }

                if (showFilterSheet) {
                    SearchFilterBottomSheet(
                        onDismissRequest = { showFilterSheet = false },
                        filterState = currentFilterState,
                        onApply = core.onApplyFilters,
                    )
                }
            } else {
                SearchSuggestions(
                    trendTags = core.trendTags.orEmpty(),
                    searchHistory = core.searchHistory,
                    isLoadingTrend = core.isTrendLoading,
                    trendError = core.trendError,
                    scrollBehavior = scrollBehavior,
                    listState = suggestionsListState,
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        top = contentTopPadding,
                        end = 0.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    queryTarget = core.queryTarget,
                    onIllustIdClick = onIllustClick,
                    onUserIdClick = onUserClick,
                    onTagClick = core.onTagClick,
                    onHistoryRemove = core.onHistoryRemove,
                    onClearHistory = core.onClearHistory,
                    onRetryTrend = core.onRetryTrend,
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
    val id: Long
    data class NumericId(override val id: Long) : SearchQueryTarget
    data class IllustId(override val id: Long) : SearchQueryTarget
    data class UserId(override val id: Long) : SearchQueryTarget
}

/** 列表回到顶端的统一判定：首项可见且无滚动偏移。 */
private fun LazyListState.isScrolledToTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

/** 瀑布流网格回到顶端判定，与列表版同语义（两类型无公共父接口，故各留一份）。 */
private fun LazyStaggeredGridState.isScrolledToTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

private val ILLUST_URL_REGEX = Regex("""(?:artworks/|illust_id=|pixiv://illusts?/)(\d+)""", RegexOption.IGNORE_CASE)
private val USER_URL_REGEX = Regex("""(?:users/|member\.php\?id=|pixiv://users?/)(\d+)""", RegexOption.IGNORE_CASE)

internal fun parseSearchQueryTarget(rawQuery: String): SearchQueryTarget? {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return null

    if (trimmed.all { it.isDigit() }) {
        val numeric = trimmed.toLongOrNull()
        if (numeric != null && numeric > 0) {
            return SearchQueryTarget.NumericId(numeric)
        }
        return null
    }

    ILLUST_URL_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }?.let {
        return SearchQueryTarget.IllustId(it)
    }

    USER_URL_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }?.let {
        return SearchQueryTarget.UserId(it)
    }

    return null
}