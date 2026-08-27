package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator


import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.components.miuixTopBarBlur
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.model.UserPreview

import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsKeys
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.LocalBottomBarVisibility
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页：搜索栏 + 真实热门标签 + 历史记录，输入后展示真实搜索结果。
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
    var startDate by rememberSaveable {
        mutableStateOf(settingsRepository.searchStartDate)
    }
    var endDate by rememberSaveable {
        mutableStateOf(settingsRepository.searchEndDate)
    }

    // 筛选条件变化时持久化回写设置。
    LaunchedEffect(sort) {
        settingsRepository.searchSort = sort
    }
    LaunchedEffect(searchTarget) {
        settingsRepository.searchTarget = searchTarget
    }
    LaunchedEffect(searchAiType) {
        settingsRepository.searchAiType = searchAiType
    }
    LaunchedEffect(bookmarkThreshold) {
        settingsRepository.searchBookmarkThreshold = bookmarkThreshold
    }
    LaunchedEffect(ugoiraFilter) {
        settingsRepository.searchUgoiraFilter = ugoiraFilter
    }
    LaunchedEffect(startDate) {
        settingsRepository.searchStartDate = startDate
    }
    LaunchedEffect(endDate) {
        settingsRepository.searchEndDate = endDate
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val backdrop = com.perol.pixez.shared.ui.components.LocalBackdrop.current
            TopAppBar(
                title = strings.tabSearch,
                modifier = Modifier.miuixTopBarBlur(backdrop),
                color = if (backdrop != null) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.ui.graphics.Color.Unspecified,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        var showFilterSheet by rememberSaveable { mutableStateOf(false) }
        val bottomBarVisibility = LocalBottomBarVisibility.current
        LaunchedEffect(showFilterSheet) {
            bottomBarVisibility.value = !showFilterSheet
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            SearchBar(
                inputField = {
                    InputField(
                        query = query,
                        onQueryChange = {
                            query = it
                            if (it.isBlank()) isSearching = false
                        },
                        onSearch = {
                            if (query.isNotBlank()) {
                                isSearching = true
                                // 将新搜索词加入历史（去重，最多保留 20 条）。
                                updateHistory(
                                    (listOf(query) + searchHistory.filter { it != query }).take(20)
                                )
                            }
                        },
                        expanded = false,
                        onExpandedChange = { },
                        label = strings.searchPlaceholder,
                    )
                },
                expanded = false,
                onExpandedChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                // SearchBar 展开状态下的内容区域，M4 暂空。
            }

            if (isSearching && query.isNotBlank()) {
                // 搜索类型切换：作品 / 画师
                TabRow(
                    tabs = searchTypes,
                    selectedTabIndex = searchTypeIndex,
                    onTabSelected = { searchTypeIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )

                if (searchTypeIndex == 0) {
                    val sortLabel = when (sort) {
                        "date_asc" -> strings.searchSortOldest
                        "popular_desc" -> strings.searchSortPopular
                        else -> strings.searchSortLatest
                    }
                    val hasActiveFilters = bookmarkThreshold > 0 || searchAiType != 0 || ugoiraFilter != 0 ||
                        startDate.isNotBlank() || endDate.isNotBlank() || searchTarget != "partial_match_for_tags"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChipButton(
                            text = strings.searchSortLabel.format(sortLabel),
                            isSelected = sort != "date_desc",
                            onClick = {
                                sort = when (sort) {
                                    "date_desc" -> "popular_desc"
                                    "popular_desc" -> "date_asc"
                                    else -> "date_desc"
                                }
                            },
                        )

                        FilterChipButton(
                            text = if (searchAiType == 0) strings.searchAiInclude else strings.searchAiExclude,
                            isSelected = searchAiType != 0,
                            onClick = {
                                searchAiType = if (searchAiType == 0) 1 else 0
                            },
                        )

                        if (bookmarkThreshold > 0) {
                            FilterChipButton(
                                text = "${bookmarkThreshold}+ ✕",
                                isSelected = true,
                                onClick = {
                                    bookmarkThreshold = 0
                                },
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        FilterChipButton(
                            text = if (hasActiveFilters) strings.searchFilterHasSelected else strings.searchFilter,
                            isSelected = hasActiveFilters,
                            onClick = { showFilterSheet = true },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (searchTypeIndex) {
                        0 -> SearchIllustResultGrid(
                            query = query,
                            sort = sort,
                            searchTarget = searchTarget,
                            searchAiType = searchAiType,
                            bookmarkThreshold = bookmarkThreshold,
                            ugoiraFilter = ugoiraFilter,
                            startDate = startDate.takeIf { it.isNotBlank() },
                            endDate = endDate.takeIf { it.isNotBlank() },
                            repository = repository,
                            banRepository = banRepository,
                            settingsRepository = settingsRepository,
                            onIllustClick = onIllustClick,
                            scrollBehavior = scrollBehavior,
                        )
                        1 -> SearchUserResultList(
                            query = query,
                            repository = repository,
                            onUserClick = onUserClick,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }

                if (showFilterSheet) {
                    SearchFilterBottomSheet(
                        onDismissRequest = { showFilterSheet = false },
                        searchTarget = searchTarget,
                        searchAiType = searchAiType,
                        bookmarkThreshold = bookmarkThreshold,
                        ugoiraFilter = ugoiraFilter,
                        startDate = startDate,
                        endDate = endDate,
                        onApply = { newTarget, newAiType, newBookmark, newUgoira, newStart, newEnd ->
                            searchTarget = newTarget
                            searchAiType = newAiType
                            bookmarkThreshold = newBookmark
                            ugoiraFilter = newUgoira
                            startDate = newStart
                            endDate = newEnd
                        },
                    )
                }
            } else {
                val trendResult = trendState.value
                SearchSuggestions(
                    trendTags = trendResult?.getOrNull().orEmpty(),
                    searchHistory = searchHistory,
                    isLoadingTrend = trendResult == null,
                    trendError = trendResult?.exceptionOrNull(),
                    scrollBehavior = scrollBehavior,
                    onTagClick = { tag ->
                        query = tag
                        isSearching = true
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
 * 搜索结果页统一的快捷筛选/排序芯片按钮。
 */
@Composable
private fun FilterChipButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * 搜索筛选底部抽屉：匹配目标、AI 作品、收藏数门槛、动图过滤与时间范围。
 *
 * 采用本地草稿状态，仅在用户点击「确定」时统一提交生效，避免频繁触发网络请求。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterBottomSheet(
    onDismissRequest: () -> Unit,
    searchTarget: String,
    searchAiType: Int,
    bookmarkThreshold: Int,
    ugoiraFilter: Int,
    startDate: String,
    endDate: String,
    onApply: (
        target: String,
        aiType: Int,
        bookmark: Int,
        ugoira: Int,
        start: String,
        end: String,
    ) -> Unit,
) {
    val strings = LocalStrings.current
    val targetOptions: List<Pair<String, String>> = listOf(
        strings.searchTargetPartialTag to "partial_match_for_tags",
        strings.searchTargetExactTag to "exact_match_for_tags",
        strings.searchTargetTitleCaption to "title_and_caption",
    )
    val bookmarkOptions: List<Pair<String, Int>> = listOf(
        strings.searchUgoiraAll to 0,
        "100+" to 100,
        "250+" to 250,
        "500+" to 500,
        "1000+" to 1000,
        "5000+" to 5000,
        "10000+" to 10000,
    )
    val ugoiraOptions: List<Pair<String, Int>> = listOf(
        strings.searchUgoiraAll to 0,
        strings.searchUgoiraOnly to 1,
        strings.searchUgoiraExclude to 2,
    )

    var draftTarget by remember(searchTarget) { mutableStateOf(searchTarget) }
    var draftAiType by remember(searchAiType) { mutableIntStateOf(searchAiType) }
    var draftBookmark by remember(bookmarkThreshold) { mutableIntStateOf(bookmarkThreshold) }
    var draftUgoira by remember(ugoiraFilter) { mutableIntStateOf(ugoiraFilter) }
    var draftStartDate by remember(startDate) { mutableStateOf(startDate) }
    var draftEndDate by remember(endDate) { mutableStateOf(endDate) }

    val selectedTargetIndex = targetOptions.indexOfFirst { it.second == draftTarget }.coerceAtLeast(0)
    val selectedUgoiraIndex = ugoiraOptions.indexOfFirst { it.second == draftUgoira }.coerceAtLeast(0)

    OverlayBottomSheet(
        show = true,
        title = strings.searchFilter,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.searchTargetExactTag)
                TabRow(
                    tabs = targetOptions.map { it.first },
                    selectedTabIndex = selectedTargetIndex,
                    onTabSelected = { draftTarget = targetOptions[it].second },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.filterAi)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = strings.searchAiIncludeWorks,
                        checked = draftAiType == 0,
                        onCheckedChange = { draftAiType = if (it) 0 else 1 },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.userBookmarkTab)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    bookmarkOptions.forEach { (label, value) ->
                        FilterChipButton(
                            text = label,
                            isSelected = draftBookmark == value,
                            onClick = { draftBookmark = value },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.searchTypeIllust)
                TabRow(
                    tabs = ugoiraOptions.map { it.first },
                    selectedTabIndex = selectedUgoiraIndex,
                    onTabSelected = { draftUgoira = ugoiraOptions[it].second },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.rankingDateLabel.substringBefore(" "))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = draftStartDate,
                        onValueChange = { draftStartDate = it },
                        label = strings.searchDateRangeStart,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = strings.searchDateRangeTo,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    TextField(
                        value = draftEndDate,
                        onValueChange = { draftEndDate = it },
                        label = strings.searchDateRangeEnd,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        draftTarget = "partial_match_for_tags"
                        draftAiType = 0
                        draftBookmark = 0
                        draftUgoira = 0
                        draftStartDate = ""
                        draftEndDate = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.surfaceContainer,
                        contentColor = MiuixTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(text = strings.searchResetAll)
                }
                Button(
                    onClick = {
                        onApply(
                            draftTarget,
                            draftAiType,
                            draftBookmark,
                            draftUgoira,
                            draftStartDate.trim(),
                            draftEndDate.trim(),
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary,
                        contentColor = MiuixTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = strings.confirm)
                }
            }
        }
    }
}

@Composable
private fun SearchIllustResultGrid(
    query: String,
    sort: String,
    searchTarget: String,
    searchAiType: Int,
    bookmarkThreshold: Int,
    ugoiraFilter: Int,
    startDate: String?,
    endDate: String?,
    repository: SearchRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    onIllustClick: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
) {
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

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> {
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        val hIsNotAllow = settingsRepository.hIsNotAllow
        return rawIllusts.filter {
            it.id !in bannedIds &&
                it.user.id !in bannedUserIds &&
                (!banAIIllust || it.illustAIType != 2) &&
                (!hIsNotAllow || (it.xRestrict == 0 && it.tags.none { tag -> tag.name.equals("R-18", ignoreCase = true) || tag.name.equals("R-18G", ignoreCase = true) })) &&
                !banRepository.isBannedByTags(
                    banTags,
                    it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                )
        }
    }

    fun applyUgoiraFilter(list: List<Illust>, filter: Int): List<Illust> {
        return when (filter) {
            1 -> list.filter { it.type == "ugoira" }
            2 -> list.filter { it.type != "ugoira" }
            else -> list
        }
    }

    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        effectiveStartDate ?: "",
        effectiveEndDate ?: "",
        retryCount,
        banRepository,
        settingsRepository,
    ) {
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
        value = searchResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(searchWord, sort, searchTarget, searchAiType, effectiveStartDate, effectiveEndDate) {
        mutableStateOf(listOf<Illust>())
    }
    var nextUrl by remember(searchWord, sort, searchTarget, searchAiType, effectiveStartDate, effectiveEndDate) {
        mutableStateOf<String?>(null)
    }
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
                val filtered = filterBanned(response.illusts)
                illusts = illusts + filtered
                nextUrl = response.nextUrl
            }.onFailure { error ->
                loadMoreError = error
            }
            isLoadingMore = false
        }
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val result = state.value
    when {
        result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        result.isSuccess -> {
            val filteredIllusts = remember(illusts, ugoiraFilter) {
                applyUgoiraFilter(illusts, ugoiraFilter)
            }
            if (filteredIllusts.isEmpty() && !isLoadingMore) {
                EmptyPlaceholder(
                    message = strings.searchEmptyIllust,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                IllustStaggeredGrid(
                    illusts = filteredIllusts,
                    onIllustClick = onIllustClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    hasMore = nextUrl != null,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    onLoadMore = ::loadMore,
                )
            }
        }
        else -> ErrorPlaceholder(
            error = result.exceptionOrNull(),
            onRetry = { retryCount++ },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun SearchUserResultList(
    query: String,
    repository: SearchRepository,
    onUserClick: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    // 画师搜索结果重试计数，作为 produceState 的 key 触发重新加载。
    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    val state = produceState<Result<com.perol.pixez.shared.data.model.UserPreviewsResponse>?>(
        initialValue = null,
        query,
        retryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.searchUserResponse(query) }
    }

    var previews by remember(query) { mutableStateOf(listOf<UserPreview>()) }
    var nextUrl by remember(query) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(state.value) {
        state.value?.onSuccess { response ->
            previews = response.userPreviews
            nextUrl = response.nextUrl
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
            suspendRunCatchingNonCancel { repository.searchUserResponse(query, nextUrl = currentNextUrl) }
                .onSuccess { response ->
                    previews = previews + response.userPreviews
                    nextUrl = response.nextUrl
                }
                .onFailure { error ->
                    loadMoreError = error
                }
            isLoadingMore = false
        }
    }

    val shouldLoadMore by remember(nextUrl, isLoadingMore, loadMoreError, previews.size) {
        derivedStateOf {
            if (nextUrl == null || isLoadingMore || loadMoreError != null || previews.isEmpty()) {
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

    val result = state.value
    when {
        result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        result.isSuccess -> {
            if (previews.isEmpty()) {
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
                        contentPadding = PaddingValues(bottom = 100.dp),
                    ) {
                        items(
                            items = previews,
                            key = { it.user.id },
                            contentType = { "user_preview_item" },
                        ) { preview ->
                            UserPreviewItem(
                                preview = preview,
                                onClick = { onUserClick(preview.user.id) },
                            )
                        }

                        if (isLoadingMore || loadMoreError != null || (nextUrl == null && previews.isNotEmpty())) {
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
                        trackPadding = PaddingValues(bottom = 100.dp),
                    )
                }
            }
        }
        else -> ErrorPlaceholder(
            error = result.exceptionOrNull(),
            onRetry = { retryCount++ },
            modifier = Modifier.fillMaxSize(),
        )
    }
}


@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun SearchSuggestions(
    trendTags: List<TrendTag>,
    searchHistory: List<String>,
    isLoadingTrend: Boolean,
    trendError: Throwable?,
    scrollBehavior: ScrollBehavior,
    onTagClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetryTrend: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val listState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SmallTitle(
                text = strings.searchHotTags,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }

        when {
            isLoadingTrend -> item {
                LoadingPlaceholder(modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
            trendError != null -> item {
                ErrorPlaceholder(
                    error = trendError,
                    onRetry = onRetryTrend,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            trendTags.isEmpty() -> item {
                EmptyPlaceholder(
                    message = strings.searchHotTagsEmpty,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            else -> item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        trendTags.forEach { tag ->
                            Text(
                                text = tag.translatedName ?: tag.tag,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTagClick(tag.tag) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MiuixTheme.textStyles.body1,
                            )
                        }
                    }
                }
            }
        }

        if (searchHistory.isNotEmpty()) {
            item {
                SmallTitle(
                    text = strings.searchHistory,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        searchHistory.forEach { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = history,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onTagClick(history) }
                                        .padding(end = 8.dp),
                                    style = MiuixTheme.textStyles.body1,
                                )
                                IconButton(
                                    onClick = { onHistoryRemove(history) },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Close,
                                        contentDescription = strings.btnDelete,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    text = strings.searchClearHistory,
                    onClick = onClearHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    VerticalScrollBar(
        adapter = rememberScrollBarAdapter(listState),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight(),
        trackPadding = PaddingValues(bottom = 100.dp),
    )
}
}

/**
 * 将日期字符串延迟 500ms 后返回，避免用户逐字输入时频繁触发搜索。
 * 空字符串或格式不符合 YYYY-MM-DD 时返回 null，表示不应用该日期筛选。
 */
@Composable
private fun debouncedSearchDate(date: String?): String? {
    if (date == null) return null
    var debounced by remember { mutableStateOf(date) }
    LaunchedEffect(date) {
        delay(500)
        debounced = date
    }
    return debounced.takeIf { it.matches(SearchDateRegex) }
}

private val SearchDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
