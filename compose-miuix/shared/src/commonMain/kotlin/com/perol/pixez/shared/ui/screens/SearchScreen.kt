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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.FakeData
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页：搜索栏 + 热门标签 + 历史记录，输入后展示结果网格。
 */
@Composable
fun SearchScreen(
    onIllustClick: (Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    // M3 阶段缓存假数据，避免重组时重新生成。
    val searchResultIllusts = remember { FakeData.illusts(count = 12) }
    val trendTags = remember { FakeData.trendTags() }
    val searchHistory = remember { FakeData.searchHistory() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SearchBar(
                inputField = {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { expanded = query.isNotBlank() },
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
                // SearchBar 展开状态下的内容区域，M3 暂空。
            }
        },
    ) { paddingValues ->
        if (expanded && query.isNotBlank()) {
            // 搜索结果：使用与首页相同的插画网格。
            IllustStaggeredGrid(
                illusts = searchResultIllusts,
                onIllustClick = onIllustClick,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
            )
        } else {
            SearchSuggestions(
                paddingValues = paddingValues,
                trendTags = trendTags,
                searchHistory = searchHistory,
                onTagClick = { tag ->
                    query = tag
                    expanded = true
                },
            )
        }
    }
}

@Composable
private fun SearchSuggestions(
    paddingValues: PaddingValues,
    trendTags: List<String>,
    searchHistory: List<String>,
    onTagClick: (String) -> Unit,
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
        items(trendTags) { tag ->
            Text(
                text = tag,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTagClick(tag) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = MiuixTheme.textStyles.body1,
            )
        }
        item {
            SmallTitle(
                text = "搜索历史",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
        items(searchHistory) { history ->
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
                onClick = { /* M4 接入 SettingsRepository 后实现 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
