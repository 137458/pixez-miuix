package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页：搜索栏 + 真实热门标签 + 历史记录，输入后展示真实搜索结果。
 */
@Composable
fun SearchScreen(
    onIllustClick: (Int) -> Unit,
    repository: SearchRepository,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

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

    // 搜索历史仍用内存占位，M4 后续接入 SettingsRepository 持久化。
    var searchHistory by rememberSaveable { mutableStateOf(listOf<String>()) }

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
                                searchHistory = buildList {
                                    add(query)
                                    addAll(searchHistory.filter { it != query })
                                }.take(20)
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
            SearchResultGrid(
                query = query,
                repository = repository,
                onIllustClick = onIllustClick,
                contentPadding = paddingValues,
            )
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
                onClearHistory = { searchHistory = emptyList() },
                onRetryTrend = { trendRetryCount++ },
            )
        }
    }
}

@Composable
private fun SearchResultGrid(
    query: String,
    repository: SearchRepository,
    onIllustClick: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    // 搜索结果重试计数，作为 produceState 的 key 触发重新加载。
    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        query,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.searchIllust(query) }
    }

    val result = state.value
    when {
        result == null -> LoadingPlaceholder()
        result.isSuccess -> {
            val illusts = result.getOrNull().orEmpty()
            if (illusts.isEmpty()) {
                EmptyPlaceholder(message = "未找到相关作品")
            } else {
                IllustStaggeredGrid(
                    illusts = illusts,
                    onIllustClick = onIllustClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
        }
        else -> ErrorPlaceholder(
            error = result.exceptionOrNull(),
            onRetry = { retryCount++ },
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
