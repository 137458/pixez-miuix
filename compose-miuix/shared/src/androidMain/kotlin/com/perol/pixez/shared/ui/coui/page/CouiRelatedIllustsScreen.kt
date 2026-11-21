package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.core.CouiErrorMessage
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.PagedLoadOutcome
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 相关作品页：折叠顶栏 + 相关插画瀑布流。
 *
 * 业务编排与 MIUIX 版一致：分页/竞态/加载态由 [IllustPagedListState] 承担，
 * 过滤条件变化仅本地重过滤，强制刷新成功才回顶。
 * 壳层交给 [CouiSecondaryScaffold]：返回、标题、护栏与底栏留白都由骨架提供，
 * 刷新入口放在顶栏 actions 槽。皮肤差异：不做下拉刷新。
 */
@Composable
internal fun CouiRelatedIllustsScreen(
    illustId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val repository = LocalIllustRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current

    var retryCount by rememberSaveable(illustId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(illustId) { mutableStateOf(false) }

    val listState = remember(illustId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getIllustRelatedResponse(illustId, nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(illustId, retryCount, filterChangeVersion) {
        val force = isManualRefreshing
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val outcome = listState.loadFirst(force = force)
        isManualRefreshing = false
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

    CouiSecondaryScaffold(
        title = strings.relatedIllusts,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = triggerManualRefresh,
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
            )
        },
    ) { padding ->
        val currentIllusts = listState.filteredIllusts
        when {
            currentIllusts == null && listState.initialError == null -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            currentIllusts == null -> CouiPlaceholder(
                message = CouiErrorMessage(listState.initialError),
                actionText = strings.retry,
                onAction = triggerManualRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            currentIllusts.isEmpty() -> CouiPlaceholder(
                message = strings.relatedIllustsEmpty,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            else -> CouiIllustGrid(
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
