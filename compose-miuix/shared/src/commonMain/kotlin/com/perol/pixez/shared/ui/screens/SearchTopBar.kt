/**
 * 搜索页顶部栏区（由 SearchScreen 拆分而来）。
 *
 * 包含折叠式顶部标题栏、搜索输入框、搜索类型切换与快捷筛选标签行三类子组件；
 * 自身不持有任何状态，状态与业务回调仍由 SearchScreen 持有并通过参数下发。
 */

package com.perol.pixez.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.components.LiquidFilterChip
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 折叠式顶部标题栏：搜索态下提供返回按钮，折叠时提供搜索入口按钮。
 */
@Composable
internal fun SearchTopAppBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    backdrop: Backdrop?,
    isSearching: Boolean,
    isSearchCollapsed: Boolean,
    onBack: () -> Unit,
    onExpand: () -> Unit,
) {
    val strings = LocalStrings.current
    TopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
        navigationIcon = {
            if (isSearching) {
                IconButton(
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = strings.back,
                    )
                }
            }
        },
        actions = {
            if (isSearchCollapsed) {
                IconButton(
                    onClick = onExpand,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = strings.tabSearch,
                    )
                }
            }
        },
    )
}

/**
 * 搜索输入框：随折叠状态展开 / 收起。
 */
@Composable
internal fun SearchInputBar(
    isSearchCollapsed: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val strings = LocalStrings.current
    AnimatedVisibility(
        visible = !isSearchCollapsed,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 500f,
            ),
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 500f,
            ),
        ) + fadeOut(),
    ) {
        SearchBar(
            inputField = {
                InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { onSearch() },
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
    }
}

/**
 * 搜索结果筛选条：搜索类型切换（作品 / 画师）与作品搜索下的快捷筛选标签行。
 */
@Composable
internal fun SearchResultFilterBar(
    isSearchCollapsed: Boolean,
    searchTypes: List<String>,
    searchTypeIndex: Int,
    onSearchTypeSelected: (Int) -> Unit,
    sort: String,
    onSortClick: () -> Unit,
    searchAiType: Int,
    onAiTypeClick: () -> Unit,
    bookmarkThreshold: Int,
    onClearBookmarkThreshold: () -> Unit,
    hasActiveFilters: Boolean,
    onOpenFilter: () -> Unit,
    backdrop: Backdrop?,
) {
    val strings = LocalStrings.current
    AnimatedVisibility(
        visible = !isSearchCollapsed,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 500f,
            ),
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 500f,
            ),
        ) + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 搜索类型切换：作品 / 画师
            TabRow(
                tabs = searchTypes,
                selectedTabIndex = searchTypeIndex,
                onTabSelected = onSearchTypeSelected,
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidFilterChip(
                        text = strings.searchSortLabel.format(sortLabel),
                        selected = sort != "date_desc",
                        backdrop = backdrop,
                        textStyle = MiuixTheme.textStyles.footnote1,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        onClick = onSortClick,
                    )

                    LiquidFilterChip(
                        text = if (searchAiType == 0) strings.searchAiInclude else strings.searchAiExclude,
                        selected = searchAiType != 0,
                        backdrop = backdrop,
                        textStyle = MiuixTheme.textStyles.footnote1,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        onClick = onAiTypeClick,
                    )

                    if (bookmarkThreshold > 0) {
                        LiquidFilterChip(
                            text = "${bookmarkThreshold}+ ✕",
                            selected = true,
                            backdrop = backdrop,
                            textStyle = MiuixTheme.textStyles.footnote1,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            onClick = onClearBookmarkThreshold,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    LiquidFilterChip(
                        text = if (hasActiveFilters) strings.searchFilterHasSelected else strings.searchFilter,
                        selected = hasActiveFilters,
                        backdrop = backdrop,
                        textStyle = MiuixTheme.textStyles.footnote1,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        onClick = onOpenFilter,
                    )
                }
            }
        }
    }
}