/**
 * 搜索页筛选面板区（由 SearchScreen 拆分而来）。
 *
 * 包含搜索筛选底部抽屉及其内部可复用的分组子组件（标签选项卡、收藏数档位、
 * 比例选项卡、发布时间范围、底部操作按钮）。抽屉的草稿状态仍由抽屉自身持有，
 * 仅在用户点击「确定」时通过 onApply 提交给 SearchScreen 生效。
 */

package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.components.LiquidFilterChip
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索筛选底部抽屉：匹配目标、AI 作品、收藏数门槛、动图过滤与时间范围。
 *
 * 采用本地草稿状态，仅在用户点击「确定」时统一提交生效，避免频繁触发网络请求。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterBottomSheet(
    onDismissRequest: () -> Unit,
    filterState: SearchFilterState,
    onApply: (SearchFilterState) -> Unit,
) {
    val strings = LocalStrings.current
    val targetOptions: List<Pair<String, String>> = listOf(
        strings.searchTargetPartialTag to "partial_match_for_tags",
        strings.searchTargetExactTag to "exact_match_for_tags",
        strings.searchTargetTitleCaption to "title_and_caption",
    )
    val bookmarkOptions: List<Pair<String, Int>> = remember(strings) {
        com.perol.pixez.shared.ui.AppConstants.Search.BOOKMARK_THRESHOLDS.map { threshold ->
            if (threshold == 0) strings.searchUgoiraAll to 0
            else "${threshold}+" to threshold
        }
    }
    val ugoiraOptions: List<Pair<String, Int>> = listOf(
        strings.searchUgoiraAll to 0,
        strings.searchUgoiraOnly to 1,
        strings.searchUgoiraExclude to 2,
    )

    var draftState by remember(filterState) { mutableStateOf(filterState) }

    val selectedTargetIndex = targetOptions.indexOfFirst { it.second == draftState.searchTarget }.coerceAtLeast(0)
    val selectedUgoiraIndex = ugoiraOptions.indexOfFirst { it.second == draftState.ugoiraFilter }.coerceAtLeast(0)

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
            SearchFilterTabGroup(
                title = strings.searchTargetPartialTag,
                tabs = targetOptions.map { it.first },
                selectedIndex = selectedTargetIndex,
                onTabSelected = { draftState = draftState.copy(searchTarget = targetOptions[it].second) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.interactionSettingHNotAllow)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = strings.interactionSettingHNotAllow,
                        summary = if (draftState.hIsNotAllow) strings.interactionSettingHNotAllowSummaryOn else strings.interactionSettingHNotAllowSummaryOff,
                        checked = draftState.hIsNotAllow,
                        onCheckedChange = { draftState = draftState.copy(hIsNotAllow = it) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(text = strings.filterAi)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = strings.searchAiIncludeWorks,
                        checked = draftState.searchAiType == 0,
                        onCheckedChange = { draftState = draftState.copy(searchAiType = if (it) 0 else 1) },
                    )
                }
            }

            SearchFilterBookmarkChips(
                options = bookmarkOptions,
                selectedThreshold = draftState.bookmarkThreshold,
                onSelect = { draftState = draftState.copy(bookmarkThreshold = it) },
            )

            SearchFilterTabGroup(
                title = strings.searchTypeIllust,
                tabs = ugoiraOptions.map { it.first },
                selectedIndex = selectedUgoiraIndex,
                onTabSelected = { draftState = draftState.copy(ugoiraFilter = ugoiraOptions[it].second) },
            )

            SearchFilterRatioTabs(
                ratioFilter = draftState.ratioFilter,
                onRatioSelected = { draftState = draftState.copy(ratioFilter = it) },
            )

            SearchFilterDateRange(
                startDate = draftState.startDate,
                endDate = draftState.endDate,
                onStartDateChange = { draftState = draftState.copy(startDate = it) },
                onEndDateChange = { draftState = draftState.copy(endDate = it) },
            )

            SearchFilterActionButtons(
                onReset = { draftState = SearchFilterState() },
                onConfirm = {
                    onApply(
                        draftState.copy(
                            startDate = draftState.startDate.trim(),
                            endDate = draftState.endDate.trim(),
                        )
                    )
                    onDismissRequest()
                },
            )
        }
    }
}

/**
 * 「小标题 + 选项卡」筛选分组，用于匹配目标、动图过滤与比例等档位选择。
 */
@Composable
internal fun SearchFilterTabGroup(
    title: String,
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = title)
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedIndex,
            onTabSelected = onTabSelected,
        )
    }
}

/**
 * 收藏数门槛档位分组：以流式标签展示各预设阈值。
 */
@Composable
internal fun SearchFilterBookmarkChips(
    options: List<Pair<String, Int>>,
    selectedThreshold: Int,
    onSelect: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = strings.userBookmarkTab)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (label, value) ->
                LiquidFilterChip(
                    text = label,
                    selected = selectedThreshold == value,
                    textStyle = MiuixTheme.textStyles.footnote1,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

/**
 * 作品比例筛选分组：全部 / 横图 / 竖图 / 方图。
 */
@Composable
internal fun SearchFilterRatioTabs(
    ratioFilter: Int,
    onRatioSelected: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val ratioOptions: List<Pair<String, Int>> = listOf(
        strings.searchRatioAll to 0,
        strings.searchRatioHorizontal to 1,
        strings.searchRatioVertical to 2,
        strings.searchRatioSquare to 3,
    )
    val selectedRatioIndex = ratioOptions.indexOfFirst { it.second == ratioFilter }.coerceAtLeast(0)

    SearchFilterTabGroup(
        title = strings.searchRatioTitle,
        tabs = ratioOptions.map { it.first },
        selectedIndex = selectedRatioIndex,
        onTabSelected = { onRatioSelected(ratioOptions[it].second) },
    )
}

/**
 * 发布时间范围分组：起止日期文本输入。
 */
@Composable
internal fun SearchFilterDateRange(
    startDate: String,
    endDate: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = strings.publishDate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = startDate,
                onValueChange = onStartDateChange,
                label = strings.searchDateRangeStart,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = strings.searchDateRangeTo,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            TextField(
                value = endDate,
                onValueChange = onEndDateChange,
                label = strings.searchDateRangeEnd,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 筛选抽屉底部操作按钮：重置全部 / 确定提交。
 */
@Composable
internal fun SearchFilterActionButtons(
    onReset: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.surfaceContainer,
                contentColor = MiuixTheme.colorScheme.onSurface,
            ),
        ) {
            Text(text = strings.searchResetAll)
        }
        Button(
            onClick = onConfirm,
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