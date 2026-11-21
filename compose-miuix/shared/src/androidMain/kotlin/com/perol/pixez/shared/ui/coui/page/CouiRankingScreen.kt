package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import com.perol.pixez.shared.ui.coui.component.CouiChipTabRow
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiMainScaffold
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.RankingFeedRestoreCache
import com.perol.pixez.shared.ui.state.RankingMode
import com.perol.pixez.shared.ui.state.rememberRankingFeedState

/**
 * COUI 排行榜页：复用 [rememberRankingFeedState] 的分页/日期/恢复逻辑，仅替换视觉壳。
 *
 * 骨架用一级页 [CouiMainScaffold]；榜单类型 chip 栏作为正文头部（瀑布流 header / 占位正文首段），
 * 可随内容滚进折叠顶栏覆盖区。日期入口在顶栏，选中日期后以文本形态展示。
 */
@Composable
internal fun CouiRankingScreen(
    onIllustClick: (Long) -> Unit,
    feedRestore: RankingFeedRestoreCache,
) {
    val strings = LocalStrings.current
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

    CouiMainScaffold(
        title = strings.tabRanking,
        actions = {
            if (feed.dateInput.isNotBlank()) {
                CouiTopBarTextAction(
                    text = feed.dateInput,
                    onClick = { showDateDialog = true },
                )
            } else {
                CouiTopBarIconButton(
                    onClick = { showDateDialog = true },
                    icon = CouiIcons.Calendar,
                    contentDescription = strings.rankingDateLabel,
                )
            }
            CouiTopBarIconButton(
                onClick = feed.refresh,
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
            )
        },
        topBarBottom = {
            CouiChipTabRow(
                chips = rankingChips,
                selectedKey = feed.selectedMode.code,
                onSelect = { key ->
                    RankingMode.entries.firstOrNull { it.code == key }?.let(feed.onModeSelected)
                },
            )
        },
    ) { padding ->
        val currentIllusts = feed.listState.filteredIllusts
        when {
            currentIllusts == null && feed.listState.initialError == null -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentIllusts == null && feed.listState.initialError != null -> CouiErrorPlaceholder(
                error = feed.listState.initialError,
                onRetry = feed.refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentIllusts != null && currentIllusts.isEmpty() -> CouiPlaceholder(
                message = strings.rankingEmpty,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentIllusts != null -> CouiIllustGrid(
                illusts = currentIllusts,
                state = feed.gridState,
                onIllustClick = onIllustClick,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    end = 10.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                hasMore = feed.listState.nextUrl != null,
                isLoadingMore = feed.listState.isLoadingMore,
                loadMoreError = feed.listState.loadMoreError,
                isRefreshing = feed.listState.isRefreshing,
                onLoadMore = feed.loadMore,
            )
        }
    }
}

/** 顶栏日期入口：已选日期时以文本展示，长日期不挤压图标按钮。 */
@Composable
private fun CouiTopBarTextAction(
    text: String,
    onClick: () -> Unit,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    // 顶栏内容色由库按滚动/API 能力注入，缺失时退回强调色。
    val contentColor = LocalCouiContentColor.current.takeOrElse { CouiTheme.colorScheme.accent }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = pressInteraction, list = false)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = contentColor,
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
            CouiTextInput(
                value = date,
                onValueChange = onDateChange,
                placeholder = strings.rankingDateLabel,
                containerColor = scheme.surface,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = scheme.onSurface,
                    fontSize = 15.sp,
                ),
                modifier = Modifier.weight(1f),
            )
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
    val pressInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.container)
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = pressInteraction, enabled = enabled, list = false)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
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
