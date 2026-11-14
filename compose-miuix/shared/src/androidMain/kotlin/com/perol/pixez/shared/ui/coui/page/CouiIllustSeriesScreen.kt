package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalWatchlistRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.PagedLoadOutcome
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 插画系列页：固定顶栏 + 系列作品瀑布流。
 *
 * 分页、过滤重算、强制刷新回顶与追更/取消追更（乐观翻转 + 失败回滚）与 MIUIX 版一致；
 * 差异只有皮肤层：固定顶栏承载刷新与追更文本动作，取消追更二次确认改用 COUI `Dialog`，
 * 三态占位收敛到平板可读宽度，不做下拉刷新。
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
                    toastSink(
                        ToastData(
                            "${strings.watchlistTitle}${strings.failedSuffix}: " +
                                PixivApiErrorFormatter.format(error, strings),
                            ToastType.Error,
                        ),
                    )
                }
            } finally {
                isTogglingWatchlist = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = seriesTitle,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 追更切换：系列详情加载完成（watchlistAdded 非 null）后展示。
            if (watchlistAdded != null) {
                CouiTextAction(
                    text = if (watchlistAdded == true) strings.watchlistAdded else strings.watchlistAdd,
                    enabled = !isTogglingWatchlist,
                    color = if (watchlistAdded == true) CouiTheme.colorScheme.accent else null,
                    onClick = {
                        // 取消追更为破坏性操作，先二次确认；追更直接执行。
                        if (watchlistAdded == true) {
                            pendingUnwatchConfirm = true
                        } else {
                            toggleWatchlist()
                        }
                    },
                )
            }
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = triggerManualRefresh,
            )
        }

        val currentIllusts = listState.filteredIllusts
        when {
            currentIllusts == null && listState.initialError == null ->
                CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
                    CouiStatePlaceholder(
                        message = strings.loading,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

            currentIllusts == null && listState.initialError != null ->
                CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
                    CouiStatePlaceholder(
                        message = couiErrorMessage(listState.initialError),
                        actionText = strings.retry,
                        onAction = { triggerManualRefresh() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

            currentIllusts != null -> if (currentIllusts.isEmpty()) {
                CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
                    CouiStatePlaceholder(
                        message = strings.seriesEmpty,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                CouiIllustGrid(
                    illusts = currentIllusts,
                    state = gridState,
                    onIllustClick = onIllustClick,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    hasMore = listState.nextUrl != null,
                    isLoadingMore = listState.isLoadingMore,
                    loadMoreError = listState.loadMoreError,
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
