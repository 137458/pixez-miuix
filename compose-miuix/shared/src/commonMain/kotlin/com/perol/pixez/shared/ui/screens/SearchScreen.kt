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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsKeys
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.navigation.LocalBottomBarVisibility
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
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
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
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    // 存在初始查询词或点击标签后进入搜索结果模式。
    var isSearching by rememberSaveable { mutableStateOf(initialQuery.isNotBlank()) }

    // 搜索类型：0 = 作品，1 = 画师。
    var searchTypeIndex by rememberSaveable { mutableIntStateOf(0) }
    val searchTypes = listOf("作品", "画师")

    // 搜索筛选状态：排序、搜索目标、AI 类型、收藏数阈值、Ugoira 过滤与时间范围（仅作品搜索有效）。
    // searchAiType：0 表示包含 AI 生成作品，1 表示排除。
    // bookmarkThreshold：0 表示不追加收藏数条件，非 0 时搜索词追加 " ${value}users入り"。
    // ugoiraFilter：0 表示全部，1 表示仅动图，2 表示排除动图。
    // startDate / endDate：格式 YYYY-MM-DD，空字符串表示未选择。
    var sort by rememberSaveable {
        mutableStateOf(
            settingsRepository.getString(SettingsKeys.SEARCH_SORT, "date_desc") ?: "date_desc",
        )
    }
    var searchTarget by rememberSaveable {
        mutableStateOf(
            settingsRepository.getString(
                SettingsKeys.SEARCH_TARGET,
                "partial_match_for_tags",
            ) ?: "partial_match_for_tags",
        )
    }
    var searchAiType by rememberSaveable {
        mutableIntStateOf(settingsRepository.getInt(SettingsKeys.SEARCH_AI_TYPE, 0))
    }
    var bookmarkThreshold by rememberSaveable {
        mutableIntStateOf(settingsRepository.getInt(SettingsKeys.SEARCH_BOOKMARK_THRESHOLD, 0))
    }
    var ugoiraFilter by rememberSaveable {
        mutableIntStateOf(settingsRepository.getInt(SettingsKeys.SEARCH_UGOIRA_FILTER, 0))
    }
    var startDate by rememberSaveable {
        mutableStateOf(settingsRepository.getString(SettingsKeys.SEARCH_START_DATE, "") ?: "")
    }
    var endDate by rememberSaveable {
        mutableStateOf(settingsRepository.getString(SettingsKeys.SEARCH_END_DATE, "") ?: "")
    }

    // 筛选条件变化时持久化回写设置。
    LaunchedEffect(sort) {
        settingsRepository.setString(SettingsKeys.SEARCH_SORT, sort)
    }
    LaunchedEffect(searchTarget) {
        settingsRepository.setString(SettingsKeys.SEARCH_TARGET, searchTarget)
    }
    LaunchedEffect(searchAiType) {
        settingsRepository.setInt(SettingsKeys.SEARCH_AI_TYPE, searchAiType)
    }
    LaunchedEffect(bookmarkThreshold) {
        settingsRepository.setInt(SettingsKeys.SEARCH_BOOKMARK_THRESHOLD, bookmarkThreshold)
    }
    LaunchedEffect(ugoiraFilter) {
        settingsRepository.setInt(SettingsKeys.SEARCH_UGOIRA_FILTER, ugoiraFilter)
    }
    LaunchedEffect(startDate) {
        settingsRepository.setString(SettingsKeys.SEARCH_START_DATE, startDate)
    }
    LaunchedEffect(endDate) {
        settingsRepository.setString(SettingsKeys.SEARCH_END_DATE, endDate)
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

    // 搜索历史从旧 Flutter 设置中读取并持久化回写。
    var searchHistory by rememberSaveable {
        mutableStateOf(settingsRepository.getStringList(SettingsKeys.SEARCH_HISTORY).orEmpty())
    }
    val updateHistory: (List<String>) -> Unit = { newHistory ->
        searchHistory = newHistory
        settingsRepository.setStringList(SettingsKeys.SEARCH_HISTORY, newHistory)
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "搜索",
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
                                    buildList {
                                        add(query)
                                        addAll(searchHistory.filter { it != query })
                                    }.take(20)
                                )
                            }
                        },
                        expanded = false,
                        onExpandedChange = { },
                        label = "搜索作品或画师",
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
                        "date_asc" -> "旧序"
                        "popular_desc" -> "人气"
                        else -> "最新"
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
                            text = "排序: $sortLabel",
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
                            text = if (searchAiType == 0) "包含AI" else "排除AI",
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
                            text = if (hasActiveFilters) "筛选 (已选)" else "筛选",
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
    val targetOptions = listOf(
        "标签部分" to "partial_match_for_tags",
        "标签完全" to "exact_match_for_tags",
        "标题说明" to "title_and_caption",
    )
    val bookmarkOptions = listOf(
        "全部" to 0,
        "100+" to 100,
        "250+" to 250,
        "500+" to 500,
        "1000+" to 1000,
        "5000+" to 5000,
        "10000+" to 10000,
    )
    val ugoiraOptions = listOf(
        "全部" to 0,
        "仅动图" to 1,
        "排除动图" to 2,
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
        title = "搜索筛选",
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
                SmallTitle(text = "匹配目标")
                TabRow(
                    tabs = targetOptions.map { it.first },
                    selectedTabIndex = selectedTargetIndex,
                    onTabSelected = { draftTarget = targetOptions[it].second },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = "AI 作品")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "包含 AI 生成作品",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = draftAiType == 0,
                            onCheckedChange = { draftAiType = if (it) 0 else 1 },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = "收藏数门槛")
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
                SmallTitle(text = "作品类型")
                TabRow(
                    tabs = ugoiraOptions.map { it.first },
                    selectedTabIndex = selectedUgoiraIndex,
                    onTabSelected = { draftUgoira = ugoiraOptions[it].second },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = "发布日期范围")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = draftStartDate,
                        onValueChange = { draftStartDate = it },
                        label = "开始 (YYYY-MM-DD)",
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "至",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    TextField(
                        value = draftEndDate,
                        onValueChange = { draftEndDate = it },
                        label = "结束 (YYYY-MM-DD)",
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
                    Text(text = "重置全部")
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
                    Text(text = "确定")
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

    // 在协程中执行搜索并应用屏蔽过滤：先请求作品列表，再并行获取屏蔽 ID/标签/AI 设置，
    // 最后过滤掉被屏蔽的作品、画师、标签，以及全局设置的 AI 作品。
    val state = produceState<Result<List<Illust>>?>(
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
        val illustsResult = suspendRunCatchingNonCancel {
            repository.searchIllust(
                word = searchWord,
                sort = sort,
                searchTarget = searchTarget,
                searchAiType = searchAiType,
                startDate = effectiveStartDate,
                endDate = effectiveEndDate,
            )
        }
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        value = illustsResult.map { illusts ->
            illusts.filter {
                it.id !in bannedIds &&
                    it.user.id !in bannedUserIds &&
                    (!banAIIllust || it.illustAIType != 2) &&
                    !banRepository.isBannedByTags(
                        banTags,
                        it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                    )
            }
        }
    }

    val result = state.value
    when {
        result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        result.isSuccess -> {
            val illusts = result.getOrNull().orEmpty()
            // 根据 Ugoira 筛选条件本地过滤作品类型。
            val filteredIllusts = remember(illusts, ugoiraFilter) {
                when (ugoiraFilter) {
                    1 -> illusts.filter { it.type == "ugoira" }
                    2 -> illusts.filter { it.type != "ugoira" }
                    else -> illusts
                }
            }
            if (filteredIllusts.isEmpty()) {
                EmptyPlaceholder(
                    message = "未找到相关作品",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                IllustStaggeredGrid(
                    illusts = filteredIllusts,
                    onIllustClick = onIllustClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
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

@Composable
private fun SearchUserResultList(
    query: String,
    repository: SearchRepository,
    onUserClick: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
) {
    // 画师搜索结果重试计数，作为 produceState 的 key 触发重新加载。
    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    val state = produceState<Result<List<UserPreview>>?>(
        initialValue = null,
        query,
        retryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.searchUser(query) }
    }

    val result = state.value
    when {
        result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        result.isSuccess -> {
            val previews = result.getOrNull().orEmpty()
            if (previews.isEmpty()) {
                EmptyPlaceholder(
                    message = "未找到相关画师",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(
                        items = previews,
                        key = { it.user.id },
                    ) { preview ->
                        UserPreviewItem(
                            preview = preview,
                            onClick = { onUserClick(preview.user.id) },
                        )
                    }
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
    LazyColumn(
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
                text = "热门标签",
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
                    message = "暂无热门标签",
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
                    text = "搜索历史",
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
                                        contentDescription = "删除",
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    text = "清空历史",
                    onClick = onClearHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
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
