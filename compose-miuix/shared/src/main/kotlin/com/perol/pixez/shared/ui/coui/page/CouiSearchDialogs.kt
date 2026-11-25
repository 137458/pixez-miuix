package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.sheet.BottomSheet
import com.itos.coui.sheet.CouiSheetButton
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.coui.core.CouiToggleRow
import com.perol.pixez.shared.data.model.SauceNaoParser
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiSegmentedTabs
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.SearchFilterState

/**
 * COUI 搜索筛选底部面板：匹配目标、H 不显示、AI 作品、收藏门槛、动图/比例过滤与发布时间范围。
 *
 * 使用 COUI 0.0.1-alpha13+ 原生 [BottomSheet]，保持常驻组合由 [visible] 驱动显隐与退场动画；
 * 采用草稿状态，仅在确认时统一提交；API 33+ 且窗口可采样背景时自动启用模糊材质染色。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiSearchFilterSheet(
    visible: MutableState<Boolean>,
    filterState: SearchFilterState,
    onApply: (SearchFilterState) -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val targetOptions: List<Pair<String, String>> = listOf(
        strings.searchTargetPartialTag to "partial_match_for_tags",
        strings.searchTargetExactTag to "exact_match_for_tags",
        strings.searchTargetTitleCaption to "title_and_caption",
    )
    val bookmarkOptions: List<Pair<String, Int>> = remember(strings) {
        AppConstants.Search.BOOKMARK_THRESHOLDS.map { threshold ->
            if (threshold == 0) strings.searchUgoiraAll to 0
            else "${threshold}+" to threshold
        }
    }
    val ugoiraOptions: List<Pair<String, Int>> = listOf(
        strings.searchUgoiraAll to 0,
        strings.searchUgoiraOnly to 1,
        strings.searchUgoiraExclude to 2,
    )
    val ratioOptions: List<Pair<String, Int>> = listOf(
        strings.searchRatioAll to 0,
        strings.searchRatioHorizontal to 1,
        strings.searchRatioVertical to 2,
        strings.searchRatioSquare to 3,
    )

    var draftState by remember(visible.value, filterState) { mutableStateOf(filterState) }

    BottomSheet(
        visible = visible,
        nestedScrollEnabled = true,
        titleContent = {
            Text(
                text = strings.searchFilter,
                kind = CouiTextKind.SheetTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingButton = CouiSheetButton(
            onClick = { visible.value = false },
            label = strings.cancel,
            enabled = visible.value,
        ),
        trailingButton = CouiSheetButton(
            onClick = {
                onApply(
                    draftState.copy(
                        startDate = draftState.startDate.trim(),
                        endDate = draftState.endDate.trim(),
                    ),
                )
                visible.value = false
            },
            label = strings.confirm,
            enabled = visible.value,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CouiFilterSection(title = strings.searchTargetPartialTag) {
                CouiSegmentedTabs(
                    items = targetOptions.map { it.first },
                    selectedIndex = targetOptions.indexOfFirst { it.second == draftState.searchTarget }.coerceAtLeast(0),
                    onSelect = { idx ->
                        targetOptions.getOrNull(idx)?.let { (_, value) ->
                            draftState = draftState.copy(searchTarget = value)
                        }
                    },
                    small = true,
                )
            }

            CouiCard {
                CouiSwitchRow(
                    title = strings.interactionSettingHNotAllow,
                    summary = if (draftState.hIsNotAllow) {
                        strings.interactionSettingHNotAllowSummaryOn
                    } else {
                        strings.interactionSettingHNotAllowSummaryOff
                    },
                    checked = draftState.hIsNotAllow,
                    onCheckedChange = { draftState = draftState.copy(hIsNotAllow = it) },
                )
            }

            CouiFilterSection(title = strings.filterAi) {
                CouiCard {
                    CouiSwitchRow(
                        title = strings.searchAiIncludeWorks,
                        checked = draftState.searchAiType == 0,
                        onCheckedChange = { draftState = draftState.copy(searchAiType = if (it) 0 else 1) },
                    )
                }
            }

            CouiFilterSection(title = strings.userBookmarkTab) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    bookmarkOptions.forEach { (label, value) ->
                        CouiAssistChip(
                            text = label,
                            selected = draftState.bookmarkThreshold == value,
                            onClick = { draftState = draftState.copy(bookmarkThreshold = value) },
                        )
                    }
                }
            }

            CouiFilterSection(title = strings.searchTypeIllust) {
                CouiSegmentedTabs(
                    items = ugoiraOptions.map { it.first },
                    selectedIndex = ugoiraOptions.indexOfFirst { it.second == draftState.ugoiraFilter }.coerceAtLeast(0),
                    onSelect = { idx ->
                        ugoiraOptions.getOrNull(idx)?.let { (_, value) ->
                            draftState = draftState.copy(ugoiraFilter = value)
                        }
                    },
                    small = true,
                )
            }

            CouiFilterSection(title = strings.searchRatioTitle) {
                CouiSegmentedTabs(
                    items = ratioOptions.map { it.first },
                    selectedIndex = ratioOptions.indexOfFirst { it.second == draftState.ratioFilter }.coerceAtLeast(0),
                    onSelect = { idx ->
                        ratioOptions.getOrNull(idx)?.let { (_, value) ->
                            draftState = draftState.copy(ratioFilter = value)
                        }
                    },
                    small = true,
                )
            }

            CouiFilterSection(title = strings.publishDate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CouiDateField(
                        value = draftState.startDate,
                        placeholder = strings.searchDateRangeStart,
                        onValueChange = { draftState = draftState.copy(startDate = it) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = strings.searchDateRangeTo,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    CouiDateField(
                        value = draftState.endDate,
                        placeholder = strings.searchDateRangeEnd,
                        onValueChange = { draftState = draftState.copy(endDate = it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = strings.searchResetAll,
                kind = CouiTextKind.ItemTitle,
                color = scheme.accent,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { draftState = SearchFilterState() }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/**
 * COUI SauceNAO 结果底部面板：上传中 / 失败重试 / 空结果 / 结果列表四态。
 *
 * 与 MIUIX 抽屉同一套状态与回调，使用 COUI 原生 [BottomSheet]（含内嵌 `LazyColumn` 时关闭内置滚动并开启嵌套拖拽关闭）。
 */
@Composable
internal fun CouiSauceNaoSheet(
    visible: MutableState<Boolean>,
    isLoading: Boolean,
    error: String?,
    results: List<SauceNaoResult>,
    onRetry: () -> Unit,
    onPickAnother: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val strings = LocalStrings.current
    BottomSheet(
        visible = visible,
        scrollableContent = false,
        nestedScrollEnabled = true,
        titleContent = {
            Text(
                text = strings.menuSauceNao,
                kind = CouiTextKind.SheetTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingButton = CouiSheetButton(
            onClick = { visible.value = false },
            label = strings.cancel,
            enabled = visible.value,
        ),
        trailingButton = CouiSheetButton(
            onClick = onPickAnother,
            label = strings.sauceNaoPickImage,
            enabled = visible.value && !isLoading,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = strings.sauceNaoUploading,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                error != null -> {
                    Text(
                        text = error,
                        kind = CouiTextKind.ItemSummary,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                    )
                    CouiTextButton(text = strings.retry, onClick = onRetry)
                }

                results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = strings.searchEmptyIllust,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    Text(
                        text = "${strings.menuSauceNao} (${results.size})",
                        kind = CouiTextKind.GroupFooter,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        overscrollEffect = rememberCouiOverscrollEffect(),
                    ) {
                        items(results, key = { it.illustId }) { item ->
                            CouiSauceNaoResultRow(
                                item = item,
                                onClick = {
                                    visible.value = false
                                    onIllustClick(item.illustId)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun CouiFilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            kind = CouiTextKind.GroupTitle,
            color = CouiTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        content()
    }
}

/**
 * 开关行：官方 Item + Switch 版式（行高 48dp、点击整行生效、反馈与开关共享同一 InteractionSource）。
 *
 * 保留本函数名与签名，是因为 18 个页面共 40 余处调用都指向它；实现已换成官方版式。
 */
@Composable
internal fun CouiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    CouiToggleRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        summary = summary,
    )
}

/** COUI 风格日期输入框：统一走 CouiTextInput，弹窗里铺 surface 底色以区分弹层背景。 */
@Composable
private fun CouiDateField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CouiTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier.fillMaxWidth(),
        containerColor = CouiTheme.colorScheme.surface,
    )
}

@Composable
private fun CouiSauceNaoResultRow(
    item: SauceNaoResult,
    onClick: () -> Unit,
) {
    val strings: AppStrings = LocalStrings.current
    val thumbShape = remember { RoundedCornerShape(8.dp) }
    // 缩略图加载路径同口径闸门：非 HTTPS / 带凭据 / 非白名单 host 一律回退占位，不送入 coil。
    val safeThumbnail = remember(item.thumbnail) { SauceNaoParser.safeThumbnailUrl(item.thumbnail) }

    val pressInteraction = remember { MutableInteractionSource() }
    CouiCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .couiPressFeedback(pressInteraction)
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null,
                    onClick = onClick,
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (safeThumbnail != null) {
                AsyncImage(
                    model = safeThumbnail,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(thumbShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(thumbShape)
                        .background(CouiTheme.colorScheme.container),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title?.ifBlank { null } ?: "Pixiv #${item.illustId}",
                    kind = CouiTextKind.ItemTitle,
                    color = CouiTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.author.isNullOrBlank()) {
                    Text(
                        text = item.author,
                        kind = CouiTextKind.GroupFooter,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!item.similarity.isNullOrBlank()) {
                        val simText = if (item.similarity.contains('%')) item.similarity else "${item.similarity}%"
                        Text(
                            text = "${strings.sauceNaoSimilarity}: $simText",
                            kind = CouiTextKind.GroupFooter,
                            color = CouiTheme.colorScheme.accent,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = "#${item.illustId}",
                        kind = CouiTextKind.GroupFooter,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
