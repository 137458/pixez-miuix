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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.switch.Switch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.data.model.SauceNaoParser
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.SearchFilterState

/**
 * COUI 搜索筛选弹窗：匹配目标、H 不显示、AI 作品、收藏门槛、动图/比例过滤与发布时间范围。
 *
 * 采用草稿状态，仅在确认时统一提交；弹窗为官方 `Dialog`，手机端贴底、宽屏居中，
 * API 33+ 且窗口可采样背景时启用玻璃材质。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiSearchFilterDialog(
    filterState: SearchFilterState,
    onDismissRequest: () -> Unit,
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

    var draftState by remember(filterState) { mutableStateOf(filterState) }

    Dialog(
        onDismissRequest = onDismissRequest,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.searchFilter,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.confirm,
            onClick = {
                onApply(
                    draftState.copy(
                        startDate = draftState.startDate.trim(),
                        endDate = draftState.endDate.trim(),
                    ),
                )
                onDismissRequest()
            },
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
        ),
        dismissAction = CouiDialogAction(text = strings.cancel, onClick = onDismissRequest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CouiFilterSection(title = strings.searchTargetPartialTag) {
                CouiFilterChipBar(
                    chips = targetOptions.map { (label, value) -> CouiFilterChip(key = value, label = label) },
                    selectedKey = draftState.searchTarget,
                    onSelect = { key -> draftState = draftState.copy(searchTarget = key) },
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
                CouiFilterChipBar(
                    chips = ugoiraOptions.map { (label, value) -> CouiFilterChip(key = value.toString(), label = label) },
                    selectedKey = draftState.ugoiraFilter.toString(),
                    onSelect = { key ->
                        key.toIntOrNull()?.let { draftState = draftState.copy(ugoiraFilter = it) }
                    },
                )
            }

            CouiFilterSection(title = strings.searchRatioTitle) {
                CouiFilterChipBar(
                    chips = ratioOptions.map { (label, value) -> CouiFilterChip(key = value.toString(), label = label) },
                    selectedKey = draftState.ratioFilter.toString(),
                    onSelect = { key ->
                        key.toIntOrNull()?.let { draftState = draftState.copy(ratioFilter = it) }
                    },
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
 * COUI SauceNAO 结果弹窗：上传中 / 失败重试 / 空结果 / 结果列表四态。
 *
 * 与 MIUIX 抽屉同一套状态与回调，渲染改为官方 `Dialog` 与 COUI 卡片行。
 */
@Composable
internal fun CouiSauceNaoDialog(
    isLoading: Boolean,
    error: String?,
    results: List<SauceNaoResult>,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    onPickAnother: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismissRequest,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.menuSauceNao,
        message = error,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.cancel,
            onClick = onDismissRequest,
        ),
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CouiDialogTextAction(text = strings.sauceNaoPickImage, onClick = onPickAnother)
                    CouiDialogTextAction(text = strings.retry, onClick = onRetry, primary = true)
                }
            }

            results.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = strings.searchEmptyIllust,
                        kind = CouiTextKind.ItemSummary,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                    )
                    CouiDialogTextAction(text = strings.sauceNaoPickImage, onClick = onPickAnother)
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${strings.menuSauceNao} (${results.size})",
                            kind = CouiTextKind.GroupFooter,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        CouiDialogTextAction(text = strings.sauceNaoPickImage, onClick = onPickAnother)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        overscrollEffect = rememberCouiOverscrollEffect(),
                    ) {
                        items(results, key = { it.illustId }) { item ->
                            CouiSauceNaoResultRow(
                                item = item,
                                onClick = {
                                    onDismissRequest()
                                    onIllustClick(item.illustId)
                                },
                            )
                        }
                    }
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

@Composable
private fun CouiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    kind = CouiTextKind.GroupFooter,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** COUI 风格日期输入框：COUI 无文本输入原语，用 `BasicTextField` 套官方容器样式。 */
@Composable
private fun CouiDateField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 弹窗内的文字动作：主次两档，主档用强调色。 */
@Composable
private fun CouiDialogTextAction(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    Text(
        text = text,
        kind = CouiTextKind.ItemTitle,
        color = if (primary) CouiTheme.colorScheme.accent else CouiTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .couiPressFeedback(pressInteraction, list = false)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
