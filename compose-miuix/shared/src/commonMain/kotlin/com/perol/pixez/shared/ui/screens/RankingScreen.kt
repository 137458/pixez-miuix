package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LiquidFilterBar
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.RankingFeedRestoreCache
import com.perol.pixez.shared.ui.state.RankingMode
import com.perol.pixez.shared.ui.state.rememberRankingFeedState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 排行榜页：支持日/周/月等模式切换，展示真实排行榜数据。
 */
@Composable
fun RankingScreen(
    onIllustClick: (Long) -> Unit,
    feedRestore: RankingFeedRestoreCache,
) {
    // 排行榜业务状态由皮肤无关的 RankingFeedState 提供，MIUIX/COUI 只替换 UI 壳。
    val feed = rememberRankingFeedState(feedRestore)
    val selectedMode = feed.selectedMode
    val dateInput = feed.dateInput
    val listState = feed.listState
    val gridState = feed.gridState
    val isManualRefreshing = feed.isManualRefreshing
    val triggerManualRefresh: () -> Unit = feed.refresh

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var showDateDialog by rememberSaveable { mutableStateOf(false) }

    // 日期筛选弹窗
    OverlayDialog(
        title = strings.rankingDateLabel,
        show = showDateDialog,
        onDismissRequest = { showDateDialog = false },
    ) {
        RankingDateInput(
            date = dateInput,
            onDateChange = feed.onDateInputChange,
            onClear = {
                feed.onDateInputChange("")
                showDateDialog = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        TextButton(
            text = strings.cancel,
            onClick = { showDateDialog = false },
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            text = strings.confirm,
            onClick = { showDateDialog = false },
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = strings.tabRanking,
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) androidx.compose.ui.graphics.Color.Transparent else colorScheme.surface,
                        actions = {
                            if (dateInput.isNotBlank()) {
                                TextButton(
                                    text = dateInput,
                                    onClick = { showDateDialog = true },
                                )
                            } else {
                                top.yukonga.miuix.kmp.basic.TooltipBox(text = strings.rankingDateLabel) {
                                    IconButton(
                                        onClick = { showDateDialog = true },
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Recent,
                                            contentDescription = strings.rankingDateLabel,
                                        )
                                    }
                                }
                            }
                            top.yukonga.miuix.kmp.basic.TooltipBox(text = strings.refresh) {
                                IconButton(
                                    onClick = triggerManualRefresh,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Refresh,
                                        contentDescription = strings.refresh,
                                    )
                                }
                            }
                        },
                    )
                    LiquidFilterBar(
                        items = RankingMode.entries,
                        selectedItem = selectedMode,
                        onItemSelected = feed.onModeSelected,
                        labelProvider = { it.label(strings) },
                        backdrop = backdrop,
                        isScrollable = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            val currentIllusts = listState.filteredIllusts
            when {
                currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
                    error = listState.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts != null -> {
                    if (currentIllusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.rankingEmpty,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                            topAppBarScrollBehavior = scrollBehavior,
                        ) {
                            IllustStaggeredGrid(
                                illusts = currentIllusts,
                                onIllustClick = onIllustClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    top = paddingValues.calculateTopPadding() + 8.dp,
                                    end = 8.dp,
                                    bottom = LocalBottomBarContentPadding.current,
                                ),
                                hasMore = listState.nextUrl != null,
                                isLoadingMore = listState.isLoadingMore,
                                loadMoreError = listState.loadMoreError,
                                onLoadMore = feed.loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * 排行榜日期输入区：TextField + 清空按钮。
 *
 * 接收 YYYY-MM-DD 格式；清空后恢复最新榜单。
 */
@Composable
private fun RankingDateInput(
    date: String,
    onDateChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val isValid = date.isEmpty() || date.matches(RankingDateRegex)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = date,
                onValueChange = onDateChange,
                label = strings.rankingDateLabel,
                modifier = Modifier.weight(1f),
                enabled = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClear,
                enabled = date.isNotEmpty(),
            ) {
                Text(strings.actionClear)
            }
        }
        if (!isValid) {
            Text(
                text = strings.rankingDateFormatError,
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val RankingDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
