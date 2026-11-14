package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiPageScaffold
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.RankingFeedRestoreCache
import com.perol.pixez.shared.ui.state.RankingMode
import com.perol.pixez.shared.ui.state.rememberRankingFeedState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 排行榜页：复用 [rememberRankingFeedState] 的分页/日期/恢复逻辑，仅替换视觉壳。
 */
@Composable
internal fun CouiRankingScreen(
    onIllustClick: (Long) -> Unit,
    feedRestore: RankingFeedRestoreCache,
) {
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
    val feed = rememberRankingFeedState(feedRestore)
    val rankingChips = remember(strings) {
        RankingMode.entries.map { CouiFilterChip(key = it.code, label = it.label(strings)) }
    }
    var showDateDialog by rememberSaveable { mutableStateOf(false) }

    if (showDateDialog) {
        CouiRankingDateDialog(
            date = feed.dateInput,
            onDateChange = feed.onDateInputChange,
            onDismiss = { showDateDialog = false },
        )
    }

    CouiPageScaffold(
        title = strings.tabRanking,
        actions = {
            if (feed.dateInput.isNotBlank()) {
                CouiTopBarTextAction(
                    text = feed.dateInput,
                    onClick = { showDateDialog = true },
                )
            } else {
                CouiActionButton(
                    icon = MiuixIcons.Recent,
                    contentDescription = strings.rankingDateLabel,
                    onClick = { showDateDialog = true },
                )
            }
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = feed.refresh,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CouiTheme.colorScheme.background)
                .padding(padding),
        ) {
            CouiFilterChipBar(
                chips = rankingChips,
                selectedKey = feed.selectedMode.code,
                onSelect = { key ->
                    RankingMode.entries.firstOrNull { it.code == key }?.let(feed.onModeSelected)
                },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                val currentIllusts = feed.listState.filteredIllusts
                when {
                    currentIllusts == null && feed.listState.initialError == null -> CouiStatePlaceholder(
                        message = strings.loading,
                    )
                    currentIllusts == null && feed.listState.initialError != null -> CouiStatePlaceholder(
                        message = feed.listState.initialError?.message ?: strings.retry,
                        actionText = strings.retry,
                        onAction = feed.refresh,
                    )
                    currentIllusts != null && currentIllusts.isEmpty() -> CouiStatePlaceholder(
                        message = strings.rankingEmpty,
                    )
                    currentIllusts != null -> CouiIllustGrid(
                        illusts = currentIllusts,
                        state = feed.gridState,
                        onIllustClick = onIllustClick,
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            top = 4.dp,
                            end = 10.dp,
                            bottom = bottomBarPadding + 8.dp,
                        ),
                        hasMore = feed.listState.nextUrl != null,
                        isLoadingMore = feed.listState.isLoadingMore,
                        loadMoreError = feed.listState.loadMoreError,
                        onLoadMore = feed.loadMore,
                    )
                }
            }
        }
    }
}

/** 顶栏日期入口：已选日期时以文本展示，长日期不挤压图标按钮。 */
@Composable
private fun CouiTopBarTextAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.accent,
            maxLines = 1,
        )
    }
}

/**
 * 日期筛选弹窗：使用 COUI 官方 Dialog，输入区作为自定义内容，
 * 玻璃材质按能力边界自动降级为不透明。
 */
@Composable
private fun CouiRankingDateDialog(
    date: String,
    onDateChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    // 与 MIUIX 版一致：日期格式校验仅在非空时生效。
    val isValid = date.isEmpty() || date.matches(RankingDateRegex)
    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.rankingDateLabel,
        message = if (isValid) null else strings.rankingDateFormatError,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.confirm,
            onClick = onDismiss,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
        ),
        dismissAction = CouiDialogAction(text = strings.cancel, onClick = onDismiss),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (date.isEmpty()) {
                    Text(
                        text = strings.rankingDateLabel,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = date,
                    onValueChange = onDateChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = scheme.onSurface,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CouiDialogTextButton(
                text = strings.actionClear,
                enabled = date.isNotEmpty(),
                onClick = {
                    onDateChange("")
                    onDismiss()
                },
            )
        }
    }
}

/** COUI 弹窗内的内联文本按钮：库无通用 Button，自绘胶囊并保持 >= 44dp 热区。 */
@Composable
private fun CouiDialogTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val scheme = CouiTheme.colorScheme
    val contentColor = if (enabled) scheme.onSurface else scheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = contentColor,
            maxLines = 1,
        )
    }
}

private val RankingDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
