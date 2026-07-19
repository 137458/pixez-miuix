package com.perol.pixez.shared.ui.screens

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsKeys
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.UserPreviewItem
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    // 搜索类型：0 = 作品，1 = 画师。
    var searchTypeIndex by rememberSaveable { mutableIntStateOf(0) }
    val searchTypes = listOf("作品", "画师")

    // 搜索筛选状态：排序、搜索目标、AI 类型与收藏数阈值（仅作品搜索有效）。
    // searchAiType：0 表示包含 AI 生成作品，1 表示排除。
    // bookmarkThreshold：0 表示不追加收藏数条件，非 0 时搜索词追加 " ${value}users入り"。
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

    // 热门标签重试计数，作为 produceState 的 key 触发重新加载。
    var trendRetryCount by rememberSaveable { mutableIntStateOf(0) }

    // 进入页面时加载真实热门标签。
    val trendState = produceState<Result<List<TrendTag>>?>(
        initialValue = null,
        repository,
        trendRetryCount,
    ) {
        value = runCatchingNonCancel { repository.getTrendTags() }
    }

    // 搜索历史从旧 Flutter 设置中读取并持久化回写。
    var searchHistory by rememberSaveable {
        mutableStateOf(settingsRepository.getStringList(SettingsKeys.SEARCH_HISTORY).orEmpty())
    }
    val updateHistory: (List<String>) -> Unit = { newHistory ->
        searchHistory = newHistory
        settingsRepository.setStringList(SettingsKeys.SEARCH_HISTORY, newHistory)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SearchBar(
                inputField = {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {
                            if (query.isNotBlank()) {
                                expanded = true
                                // 将新搜索词加入历史（去重，最多保留 20 条）。
                                updateHistory(
                                    buildList {
                                        add(query)
                                        addAll(searchHistory.filter { it != query })
                                    }.take(20)
                                )
                            }
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        label = "搜索作品或画师",
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // SearchBar 展开状态下的内容区域，M4 暂空。
            }
        },
    ) { paddingValues ->
        if (expanded && query.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                TabRow(
                    tabs = searchTypes,
                    selectedTabIndex = searchTypeIndex,
                    onTabSelected = { searchTypeIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (searchTypeIndex == 0) {
                    SearchFilterBar(
                        sort = sort,
                        onSortChange = { sort = it },
                        searchTarget = searchTarget,
                        onSearchTargetChange = { searchTarget = it },
                        searchAiType = searchAiType,
                        onSearchAiTypeChange = { searchAiType = it },
                        bookmarkThreshold = bookmarkThreshold,
                        onBookmarkThresholdChange = { bookmarkThreshold = it },
                    )
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
                            repository = repository,
                            onIllustClick = onIllustClick,
                        )
                        1 -> SearchUserResultList(
                            query = query,
                            repository = repository,
                            onUserClick = onUserClick,
                        )
                    }
                }
            }
        } else {
            val trendResult = trendState.value
            SearchSuggestions(
                paddingValues = paddingValues,
                trendTags = trendResult?.getOrNull().orEmpty(),
                searchHistory = searchHistory,
                isLoadingTrend = trendResult == null,
                trendError = trendResult?.exceptionOrNull(),
                onTagClick = { tag ->
                    query = tag
                    expanded = true
                },
                onClearHistory = { updateHistory(emptyList()) },
                onRetryTrend = { trendRetryCount++ },
            )
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
    repository: SearchRepository,
    onIllustClick: (Int) -> Unit,
) {
    // 根据收藏数阈值构建实际搜索词：非 0 时追加 " ${value}users入り"。
    val searchWord = remember(query, bookmarkThreshold) {
        if (bookmarkThreshold > 0) "$query ${bookmarkThreshold}users入り" else query
    }

    // 搜索结果重试计数，作为 produceState 的 key 触发重新加载。
    var retryCount by rememberSaveable(searchWord, sort, searchTarget, searchAiType) { mutableIntStateOf(0) }

    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        retryCount,
    ) {
        value = runCatchingNonCancel {
            repository.searchIllust(
                word = searchWord,
                sort = sort,
                searchTarget = searchTarget,
                searchAiType = searchAiType,
            )
        }
    }

    val result = state.value
    when {
        result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        result.isSuccess -> {
            val illusts = result.getOrNull().orEmpty()
            if (illusts.isEmpty()) {
                EmptyPlaceholder(
                    message = "未找到相关作品",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                IllustStaggeredGrid(
                    illusts = illusts,
                    onIllustClick = onIllustClick,
                    modifier = Modifier.fillMaxSize(),
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
) {
    // 画师搜索结果重试计数，作为 produceState 的 key 触发重新加载。
    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    val state = produceState<Result<List<UserPreview>>?>(
        initialValue = null,
        query,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.searchUser(query) }
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
                    modifier = Modifier.fillMaxSize(),
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

/**
 * 搜索筛选栏：排序、搜索目标、AI 类型与收藏数阈值切换。
 */
@Composable
private fun SearchFilterBar(
    sort: String,
    onSortChange: (String) -> Unit,
    searchTarget: String,
    onSearchTargetChange: (String) -> Unit,
    searchAiType: Int,
    onSearchAiTypeChange: (Int) -> Unit,
    bookmarkThreshold: Int,
    onBookmarkThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortOptions = listOf(
        "最新" to "date_desc",
        "旧序" to "date_asc",
        "人气" to "popular_desc",
    )
    val targetOptions = listOf(
        "标签部分" to "partial_match_for_tags",
        "标签完全" to "exact_match_for_tags",
        "标题说明" to "title_and_caption",
    )
    val bookmarkOptions = listOf(
        "默认" to 0,
        "100" to 100,
        "250" to 250,
        "500" to 500,
        "1000" to 1000,
        "5000" to 5000,
        "10000" to 10000,
        "20000" to 20000,
        "30000" to 30000,
        "50000" to 50000,
    )

    val selectedSortIndex = sortOptions.indexOfFirst { it.second == sort }.coerceAtLeast(0)
    val selectedTargetIndex = targetOptions.indexOfFirst { it.second == searchTarget }.coerceAtLeast(0)
    val selectedBookmarkIndex = bookmarkOptions.indexOfFirst { it.second == bookmarkThreshold }.coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabRow(
            tabs = sortOptions.map { it.first },
            selectedTabIndex = selectedSortIndex,
            onTabSelected = { onSortChange(sortOptions[it].second) },
        )
        TabRow(
            tabs = targetOptions.map { it.first },
            selectedTabIndex = selectedTargetIndex,
            onTabSelected = { onSearchTargetChange(targetOptions[it].second) },
        )
        // AI 生成作品开关：开启时 searchAiType = 0（包含 AI），关闭时 = 1（排除 AI）。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "AI 生成作品",
                style = MiuixTheme.textStyles.body1,
            )
            Switch(
                checked = searchAiType == 0,
                onCheckedChange = { onSearchAiTypeChange(if (it) 0 else 1) },
            )
        }
        // 收藏数阈值选择：选择后在搜索词后追加 " ${value}users入り"。
        TabRow(
            tabs = bookmarkOptions.map { it.first },
            selectedTabIndex = selectedBookmarkIndex,
            onTabSelected = { onBookmarkThresholdChange(bookmarkOptions[it].second) },
        )
    }
}

@Composable
private fun SearchSuggestions(
    paddingValues: PaddingValues,
    trendTags: List<TrendTag>,
    searchHistory: List<String>,
    isLoadingTrend: Boolean,
    trendError: Throwable?,
    onTagClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetryTrend: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues,
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
            else -> items(trendTags, key = { it.tag }) { tag ->
                Text(
                    text = tag.translatedName ?: tag.tag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagClick(tag.tag) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }

        item {
            SmallTitle(
                text = "搜索历史",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }

        items(searchHistory, key = { it }) { history ->
            Text(
                text = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTagClick(history) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = MiuixTheme.textStyles.body1,
            )
        }

        if (searchHistory.isNotEmpty()) {
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
