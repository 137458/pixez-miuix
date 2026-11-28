package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
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
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiErrorMessage
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import kotlinx.coroutines.launch

private const val INLINE_RELATED_COLUMN_COUNT = 3

/**
 * COUI 详情页内嵌的相关作品区块：按 illustId 拉取推荐作品，固定 3 列，触底自动翻页。
 *
 * 详情页主体是 [androidx.compose.foundation.lazy.LazyColumn]，内嵌懒网格会带来无限高度测量
 * 与嵌套滚动冲突，故这里用非懒的分栏布局复用 [CouiIllustCard]：高度由内容决定，滚动仍由外层承接。
 * 首屏请求由模块进入屏幕触发（懒加载），未进入视口时不发任何网络请求；触底哨兵进入视口
 * 时自动拉取下一页，竞态/在途/失败守卫由 [IllustPagedListState] 单点承担。
 *
 * 与 MIUIX 版行为对齐，但皮肤层仅使用 COUI 组件，零 MIUIX / Material3 引用。
 */
@Composable
internal fun CouiRelatedIllustsInline(
    illustId: Long,
    onIllustClick: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val repository = LocalIllustRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    val windowHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    val coroutineScope = rememberCoroutineScope()
    val changeVersion = settingsRepository.changeVersion.collectAsState().value

    var retryCount by rememberSaveable(illustId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(illustId) { mutableStateOf(false) }
    // 视口门控：模块与触底哨兵分别上报可见性，未进入屏幕时不触碰网络。
    var isModuleVisible by remember(illustId) { mutableStateOf(false) }
    var isFooterVisible by remember(illustId) { mutableStateOf(false) }

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

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(illustId, retryCount, filterChangeVersion, isModuleVisible) {
        // 懒加载：模块尚未进入屏幕时不发首屏请求。
        if (!isModuleVisible) return@LaunchedEffect
        val force = isManualRefreshing
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        listState.loadFirst(force = force)
        isManualRefreshing = false
    }

    val footerState = listState.footerState
    val shouldLoadMore = isFooterVisible &&
        footerState.hasMore &&
        !footerState.isLoadingMore &&
        footerState.loadMoreError == null
    // 翻页在外部作用域启动：LaunchedEffect 的 key 会随 isLoadingMore 翻转而取消自身协程，
    // 若把挂起的 loadMore() 直接放在 effect 内会被中途取消，故在外部作用域启动。
    fun loadMore() {
        coroutineScope.launch {
            listState.loadMore()
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                isModuleVisible = bounds.bottom > 0f && bounds.top < windowHeight
            },
    ) {
        // 标题由调用处的 CouiDetailSectionCard 统一渲染（与标签卡片同款），这里只负责网格内容。
        when {
            listState.filteredIllusts == null && listState.initialError == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                CouiPlaceholder(message = strings.loading)
            }

            listState.filteredIllusts == null && listState.initialError != null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                CouiPlaceholder(
                    message = CouiErrorMessage(listState.initialError),
                    actionText = strings.retry,
                    onAction = {
                        isManualRefreshing = true
                        retryCount++
                    },
                )
            }

            listState.filteredIllusts.isNullOrEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                CouiPlaceholder(message = strings.relatedIllustsEmpty)
            }

            else -> {
                val visibleIllusts = listState.filteredIllusts!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(INLINE_RELATED_COLUMN_COUNT) { column ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            visibleIllusts.forEachIndexed { index, illust ->
                                if (index % INLINE_RELATED_COLUMN_COUNT == column) {
                                    val menuAnchor = rememberCouiMenuAnchor()
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                                        CouiIllustCard(
                                            illust = illust,
                                            changeVersion = changeVersion,
                                            onClick = { onIllustClick?.invoke(illust.id) },
                                            onLongClick = { menuAnchor.open { menuExpanded = true } },
                                        )
                                        CouiIllustActionMenu(
                                            illust = illust,
                                            expanded = menuExpanded,
                                            anchor = menuAnchor,
                                            onDismiss = { menuExpanded = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 触底哨兵：进入屏幕且仍有下一页时自动翻页（在途/竞态守卫由状态机内部承担）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    isFooterVisible = bounds.bottom > 0f && bounds.top < windowHeight
                },
        )
        CouiLoadMoreFooter(
            state = footerState,
            onRetry = ::loadMore,
        )
    }
}
