package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import androidx.compose.ui.graphics.Color
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.actionFailureToast
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadingPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalWatchlistRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.PagedLoadOutcome
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 插画系列页：二级页骨架 + 系列作品瀑布流。
 *
 * 分页、过滤重算、强制刷新回顶与追更/取消追更（乐观翻转 + 失败回滚）与 MIUIX 版一致；
 * 差异只有皮肤层：容器由 [CouiSecondaryScaffold] 唯一提供，追更文本动作与刷新图标按钮
 * 进骨架 `actions` 槽，三态占位改用 core 组件，取消追更二次确认仍用 COUI `Dialog`。
 */
@Composable
internal fun CouiIllustSeriesScreen(
    seriesId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val repository = LocalIllustRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val watchlistRepository = LocalWatchlistRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current

    var retryCount by rememberSaveable(seriesId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(seriesId) { mutableStateOf(false) }

    // 系列 ID 变更时重建状态机并重置标题；翻页响应不含标题更新。
    var seriesTitle by remember(seriesId) { mutableStateOf("") }
    // 追更状态：null 表示系列详情未加载（隐藏切换按钮）；首页响应的 watchlist_added 为准。
    var watchlistAdded by remember(seriesId) { mutableStateOf<Boolean?>(null) }
    var isTogglingWatchlist by remember { mutableStateOf(false) }
    var pendingUnwatchConfirm by remember { mutableStateOf(false) }

    val listState = remember(seriesId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getIllustSeriesResponse(seriesId, nextUrl = nextUrl)
                if (nextUrl == null) {
                    seriesTitle = response.illustSeriesDetail?.title ?: ""
                    watchlistAdded = response.illustSeriesDetail?.watchlistAdded
                }
                IllustPagedPage(response.illusts.orEmpty(), response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(seriesId, retryCount, filterChangeVersion) {
        val force = isManualRefreshing
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val outcome = listState.loadFirst(force = force)
        isManualRefreshing = false
        // 强制刷新成功才回顶，与 MIUIX 版一致。
        if (outcome == PagedLoadOutcome.Written && force && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    fun loadMore() {
        coroutineScope.launch { listState.loadMore() }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    /** 追更/取消追更：乐观翻转本地状态，失败回滚并 Toast。 */
    fun toggleWatchlist() {
        val currentAdded = watchlistAdded ?: return
        if (isTogglingWatchlist) return
        coroutineScope.launch {
            isTogglingWatchlist = true
            try {
                watchlistAdded = !currentAdded
                suspendRunCatchingNonCancel {
                    if (currentAdded) {
                        watchlistRepository.deleteMangaWatchlist(seriesId)
                    } else {
                        watchlistRepository.addMangaWatchlist(seriesId)
                    }
                }.onFailure { error ->
                    watchlistAdded = currentAdded
                    toastSink(actionFailureToast(strings.watchlistTitle, error, strings))
                }
            } finally {
                isTogglingWatchlist = false
            }
        }
    }

    CouiSecondaryScaffold(
        title = seriesTitle,
        onBack = onBack,
        actions = {
            // 追更切换：系列详情加载完成（watchlistAdded 非 null）后展示。
            val currentAdded = watchlistAdded
            if (currentAdded != null) {
                CouiSeriesWatchlistAction(
                    text = if (currentAdded) strings.watchlistAdded else strings.watchlistAdd,
                    enabled = !isTogglingWatchlist,
                    highlighted = currentAdded,
                    onClick = {
                        // 取消追更为破坏性操作，先二次确认；追更直接执行。
                        if (currentAdded) {
                            pendingUnwatchConfirm = true
                        } else {
                            toggleWatchlist()
                        }
                    },
                )
            }
            CouiTopBarIconButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = triggerManualRefresh,
            )
        },
    ) { padding ->
        // 占位态贴齐骨架内边距：左右由库保证为 0，这里只吃上下与窗口内边距。
        val stateModifier = Modifier
            .padding(padding)
            .consumeWindowInsets(padding)
        val currentIllusts = listState.filteredIllusts
        when {
            currentIllusts == null && listState.initialError == null ->
                CouiLoadingPlaceholder(modifier = stateModifier)

            currentIllusts == null && listState.initialError != null ->
                CouiErrorPlaceholder(
                    error = listState.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = stateModifier,
                )

            currentIllusts != null -> if (currentIllusts.isEmpty()) {
                CouiPlaceholder(
                    message = strings.seriesEmpty,
                    modifier = stateModifier,
                )
            } else {
                CouiIllustGrid(
                    illusts = currentIllusts,
                    state = gridState,
                    onIllustClick = onIllustClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(padding),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        end = 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                    ),
                    hasMore = listState.nextUrl != null,
                    isLoadingMore = listState.isLoadingMore,
                    loadMoreError = listState.loadMoreError,
                    isRefreshing = listState.isRefreshing,
                    onLoadMore = ::loadMore,
                )
            }
        }
    }

    if (pendingUnwatchConfirm) {
        val toggling = isTogglingWatchlist
        Dialog(
            onDismissRequest = { if (!toggling) pendingUnwatchConfirm = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.watchlistTitle,
            message = strings.watchlistRemoveConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                enabled = !toggling,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    pendingUnwatchConfirm = false
                    toggleWatchlist()
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { if (!isTogglingWatchlist) pendingUnwatchConfirm = false },
            ),
        )
    }
}

/** 顶栏追更文本动作：追更中禁用，已追更时以强调色标示，避免图标无法表达两种文案。 */
@Composable
private fun CouiSeriesWatchlistAction(
    text: String,
    enabled: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    val scheme = CouiTheme.colorScheme
    // 顶栏内容色由库按滚动/API 能力注入，缺失时退回强调色。
    val injectedColor = LocalCouiContentColor.current
    val contentColor = if (injectedColor == Color.Unspecified) scheme.accent else injectedColor
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = pressInteraction, list = false)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = when {
                !enabled -> contentColor.copy(alpha = 0.38f)
                highlighted -> scheme.accent
                else -> contentColor
            },
            maxLines = 1,
        )
    }
}
