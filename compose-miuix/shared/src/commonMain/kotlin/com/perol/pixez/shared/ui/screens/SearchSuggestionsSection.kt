/**
 * 搜索页推荐区（由 SearchScreen 拆分而来）。
 *
 * 包含未进入搜索态时展示的热门标签与搜索历史列表；
 * 数据（热门标签、历史记录）与回调均由 SearchScreen 通过参数下发，
 * 这里仅负责渲染与滚动条挂载，列表滚动状态由外部传入以支持顶栏折叠联动。
 */

package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun SearchSuggestions(
    trendTags: List<TrendTag>,
    searchHistory: List<String>,
    isLoadingTrend: Boolean,
    trendError: Throwable?,
    scrollBehavior: ScrollBehavior,
    onTagClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetryTrend: () -> Unit,
    queryTarget: SearchQueryTarget? = null,
    onIllustIdClick: (Int) -> Unit = {},
    onUserIdClick: (Int) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues? = null,
) {
    val effectiveContentPadding = contentPadding ?: PaddingValues(
        start = 0.dp,
        top = 0.dp,
        end = 0.dp,
        bottom = LocalBottomBarContentPadding.current,
    )
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = effectiveContentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        if (queryTarget != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    if (queryTarget !is SearchQueryTarget.UserId) {
                        Text(
                            text = "${strings.copyTextChipIllustId}: ${queryTarget.id}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIllustIdClick(queryTarget.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                    if (queryTarget !is SearchQueryTarget.IllustId) {
                        Text(
                            text = "${strings.copyTextChipUserId}: ${queryTarget.id}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserIdClick(queryTarget.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

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
        trackPadding = effectiveContentPadding,
    )
}
}