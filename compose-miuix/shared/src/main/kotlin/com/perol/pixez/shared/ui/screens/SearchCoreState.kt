package com.perol.pixez.shared.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalSearchRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 搜索页核心状态：查询词、搜索态、搜索类型、筛选条件、历史记录与热门标签。
 *
 * 皮肤无关：MIUIX 搜索页与 COUI 搜索页共用本实现，皮肤层只替换顶栏、输入框与
 * 历史/热搜的视觉壳，筛选持久化、历史去重上限、ID/URL 分流与热门标签屏蔽规则
 * 只在里维护一份。
 */
internal class SearchCoreState(
    val query: String,
    val isSearching: Boolean,
    val searchTypeIndex: Int,
    val sort: String,
    val filterState: SearchFilterState,
    val searchHistory: List<String>,
    val trendTags: List<TrendTag>?,
    val isTrendLoading: Boolean,
    val trendError: Throwable?,
    val queryTarget: SearchQueryTarget?,
    val onQueryChange: (String) -> Unit,
    val onSearch: () -> Unit,
    val onExitSearch: () -> Unit,
    val onSearchTypeSelected: (Int) -> Unit,
    val onTagClick: (String) -> Unit,
    val onHistoryRemove: (String) -> Unit,
    val onClearHistory: () -> Unit,
    val onRetryTrend: () -> Unit,
    val onApplyFilters: (SearchFilterState) -> Unit,
    val onSortClick: () -> Unit,
    val onAiTypeClick: () -> Unit,
    val onClearBookmarkThreshold: () -> Unit,
)

@Composable
internal fun rememberSearchCoreState(
    initialQuery: String,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
): SearchCoreState {
    val repository = LocalSearchRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val banRepository = LocalBanRepository.current

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    // 存在初始查询词或点击标签后进入搜索结果模式。
    var isSearching by rememberSaveable { mutableStateOf(initialQuery.isNotBlank()) }

    // 搜索类型：0 = 作品，1 = 画师，2 = 小说。
    var searchTypeIndex by rememberSaveable { mutableIntStateOf(0) }

    // 搜索筛选状态：排序、搜索目标、AI 类型、收藏数阈值、Ugoira 过滤与时间范围（仅作品搜索有效）。
    var sort by rememberSaveable { mutableStateOf(settingsRepository.searchSort) }
    var searchTarget by rememberSaveable { mutableStateOf(settingsRepository.searchTarget) }
    var searchAiType by rememberSaveable { mutableIntStateOf(settingsRepository.searchAiType) }
    var bookmarkThreshold by rememberSaveable { mutableIntStateOf(settingsRepository.searchBookmarkThreshold) }
    var ugoiraFilter by rememberSaveable { mutableIntStateOf(settingsRepository.searchUgoiraFilter) }
    var ratioFilter by rememberSaveable { mutableIntStateOf(0) }
    var startDate by rememberSaveable { mutableStateOf(settingsRepository.searchStartDate) }
    var endDate by rememberSaveable { mutableStateOf(settingsRepository.searchEndDate) }

    // 筛选条件变化时持久化回写设置。
    // 首帧的值本身就来自设置，判定见 shouldPersistFilter：值未变化时不写入，
    // 避免重复持久化与 filterChangeVersion 无谓自增（写入会触发下游本地重过滤）。
    LaunchedEffect(sort) {
        if (shouldPersistFilter(settingsRepository.searchSort, sort)) settingsRepository.searchSort = sort
    }
    LaunchedEffect(searchTarget) {
        if (shouldPersistFilter(settingsRepository.searchTarget, searchTarget)) {
            settingsRepository.searchTarget = searchTarget
        }
    }
    LaunchedEffect(searchAiType) {
        if (shouldPersistFilter(settingsRepository.searchAiType, searchAiType)) {
            settingsRepository.searchAiType = searchAiType
        }
    }
    LaunchedEffect(bookmarkThreshold) {
        if (shouldPersistFilter(settingsRepository.searchBookmarkThreshold, bookmarkThreshold)) {
            settingsRepository.searchBookmarkThreshold = bookmarkThreshold
        }
    }
    LaunchedEffect(ugoiraFilter) {
        if (shouldPersistFilter(settingsRepository.searchUgoiraFilter, ugoiraFilter)) {
            settingsRepository.searchUgoiraFilter = ugoiraFilter
        }
    }
    LaunchedEffect(startDate) {
        if (shouldPersistFilter(settingsRepository.searchStartDate, startDate)) {
            settingsRepository.searchStartDate = startDate
        }
    }
    LaunchedEffect(endDate) {
        if (shouldPersistFilter(settingsRepository.searchEndDate, endDate)) {
            settingsRepository.searchEndDate = endDate
        }
    }

    // 热门标签重试计数，作为 produceState 的 key 触发重新加载。
    var trendRetryCount by rememberSaveable { mutableIntStateOf(0) }

    // 进入页面时加载真实热门标签。
    val trendState = produceState<Result<List<TrendTag>>?>(
        initialValue = null,
        repository,
        trendRetryCount,
    ) {
        value = suspendRunCatchingNonCancel {
            val tags = repository.getTrendTags()
            // 热门标签同样受屏蔽标签规则约束（含原生名、翻译名及正则规则），避免已屏蔽内容被再次推荐。
            banRepository.filterBannedTags(banRepository.getAllBanTags(), tags) { listOfNotNull(it.tag, it.translatedName) }
        }
    }

    // 搜索历史从设置中读取并持久化回写。
    var searchHistory by rememberSaveable { mutableStateOf(settingsRepository.searchHistory) }
    val updateHistory: (List<String>) -> Unit = { newHistory ->
        searchHistory = newHistory
        settingsRepository.searchHistory = newHistory
    }

    // 纯数字 ID 按当前搜索类型分流：1 = 画师，2 = 小说，其余（含 0）= 作品。
    val navigateToId: (Long) -> Unit = { id ->
        when (searchTypeIndex) {
            1 -> onUserClick(id)
            2 -> onNovelClick(id)
            else -> onIllustClick(id)
        }
    }

    val navigateToTarget: (SearchQueryTarget) -> Unit = { target ->
        when (target) {
            is SearchQueryTarget.IllustId -> onIllustClick(target.id)
            is SearchQueryTarget.UserId -> onUserClick(target.id)
            is SearchQueryTarget.NumericId -> navigateToId(target.id)
        }
    }

    val onSearch: () -> Unit = {
        // 归一化后再写入历史与解析直达目标：空白词由 normalizeSearchWord 判定为忽略。
        val word = normalizeSearchWord(query)
        if (word != null) {
            // 将新搜索词加入历史（置顶去重，最多保留 SEARCH_HISTORY_LIMIT 条）。
            updateHistory(mergeSearchHistory(searchHistory, word))
            val target = parseSearchQueryTarget(word)
            if (target == null) {
                isSearching = true
            } else {
                navigateToTarget(target)
            }
        }
    }

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
    val trendResult = trendState.value

    return SearchCoreState(
        query = query,
        isSearching = isSearching,
        searchTypeIndex = searchTypeIndex,
        sort = sort,
        filterState = currentFilterState,
        searchHistory = searchHistory,
        trendTags = trendResult?.getOrNull(),
        isTrendLoading = trendResult == null,
        trendError = trendResult?.exceptionOrNull(),
        queryTarget = remember(query) { parseSearchQueryTarget(query) },
        onQueryChange = { newQuery ->
            query = newQuery
            if (newQuery.isBlank()) isSearching = false
        },
        onSearch = onSearch,
        onExitSearch = {
            isSearching = false
            query = ""
        },
        onSearchTypeSelected = { searchTypeIndex = it },
        onTagClick = { tag ->
            val target = parseSearchQueryTarget(tag)
            if (target != null) {
                navigateToTarget(target)
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
        onApplyFilters = { newState ->
            searchTarget = newState.searchTarget
            searchAiType = newState.searchAiType
            bookmarkThreshold = newState.bookmarkThreshold
            ugoiraFilter = newState.ugoiraFilter
            ratioFilter = newState.ratioFilter
            startDate = newState.startDate
            endDate = newState.endDate
            settingsRepository.hIsNotAllow = newState.hIsNotAllow
        },
        onSortClick = {
            sort = when (sort) {
                "date_desc" -> "popular_desc"
                "popular_desc" -> "date_asc"
                else -> "date_desc"
            }
        },
        onAiTypeClick = { searchAiType = if (searchAiType == 0) 1 else 0 },
        onClearBookmarkThreshold = { bookmarkThreshold = 0 },
    )
}

/** 搜索历史保留条数上限。 */
internal const val SEARCH_HISTORY_LIMIT = 20

/**
 * 归一化一次搜索提交的输入词：去掉首尾空白，空白词返回 null 表示本次提交被忽略。
 */
internal fun normalizeSearchWord(raw: String): String? = raw.trim().takeIf { it.isNotBlank() }

/**
 * 把新搜索词写入历史：置顶、去重（先剔除历史中的同词，只保留新位置），并按 [limit] 截断。
 */
internal fun mergeSearchHistory(
    history: List<String>,
    word: String,
    limit: Int = SEARCH_HISTORY_LIMIT,
): List<String> = (listOf(word) + history.filter { it != word }).take(limit)

/**
 * 筛选设置回写判定：仅当算子内的值与已持久化的值不同才需要写入。
 *
 * 算子初值取自设置本身，若不加判定，首帧与每次重组都会重复写设置，
 * 进而自增 filterChangeVersion 触发下游无谓的本地重过滤。
 */
internal fun <T> shouldPersistFilter(persisted: T, local: T): Boolean = persisted != local

