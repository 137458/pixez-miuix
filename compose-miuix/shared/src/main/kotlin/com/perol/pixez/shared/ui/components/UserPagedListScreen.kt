package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberAccountSession
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户列表页统一骨架：账号身份守卫 + [PagedListState] 分页 + 手动刷新 + 触底预加载 +
 * Loading/Empty/Error/列表 四态占位 + PullToRefresh 与毛玻璃顶栏。
 *
 * 用户关注、粉丝、小说、相关用户四页此前逐字重复同一骨架（含切号作废与刷新语义），
 * 差异只有数据源、条目类型与文案；本组件把这些差异全部改为参数注入，
 * 行为（切号作废、手动刷新、触底加载、四态占位、下拉刷新）与合并前逐项等价。
 *
 * @param title 顶栏标题；调用方按需在外部拼装。
 * @param emptyText 空列表文案。
 * @param emptyIcon 空态图标；默认与 [EmptyPlaceholder] 的默认值一致。
 * @param pageKey 分页身份（如用户 ID）：变化即重建状态机并重新拉首屏，重试计数与刷新标志同样按其保存。
 * @param fetch 数据源：nextUrl == null 拉首页，失败以异常抛出。
 * @param mergeItems 翻页合并策略（各模型的 appendDistinct 去重合并）。
 * @param itemKey / itemContentType 列表条目的稳定键与内容类型。
 * @param footerKey 分页 footer 的 item 键。
 * @param itemContent 条目渲染槽。
 * @param contentPadding 由顶栏内边距换算列表内边距（默认原样透传）。
 * @param verticalArrangement 列表条目间距策略（默认不留额外间距）。
 */
@Composable
internal fun <T> UserPagedListScreen(
    title: String,
    emptyText: String,
    pageKey: Any?,
    onBack: () -> Unit,
    fetch: suspend (nextUrl: String?) -> PagedListPage<T>,
    mergeItems: (old: List<T>, new: List<T>) -> List<T>,
    itemKey: (T) -> Any,
    itemContentType: String,
    footerKey: String,
    itemContent: @Composable (T) -> Unit,
    emptyIcon: ImageVector = MiuixIcons.Search,
    contentPadding: (PaddingValues) -> PaddingValues = { it },
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {

    // 数据源与合并策略经 rememberUpdatedState 取最新值：状态机按 pageKey 记忆，
    // 但宿主每次重组都会重建 lambda，不能让状态机继续调用首帧那份。
    val currentFetch by rememberUpdatedState(fetch)
    val currentMergeItems by rememberUpdatedState(mergeItems)

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时作废旧账号列表并重拉首屏。
    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时刷新，供页面作废旧账号数据并重拉首屏。
    val (user, isLoggedIn) = rememberAccountSession()

    var retryCount by rememberSaveable(pageKey) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(pageKey) { mutableStateOf(false) }

    // 分页状态机单点实现（含 generation 竞态守卫），替代手抄的请求代数与标志位。
    val pagedList = remember(pageKey) {
        PagedListState(
            fetch = { nextUrl -> currentFetch(nextUrl) },
            mergeItems = { old, new -> currentMergeItems(old, new) },
        )
    }
    val coroutineScope = rememberCoroutineScope()

    // 账号身份跟踪：登录/登出/切号时作废旧账号列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(pageKey, retryCount, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        val force = isManualRefreshing
        pagedList.loadFirst(force = force)
        isManualRefreshing = false
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val listState = rememberLazyListState()
    rememberPagingPreload(pagedList = pagedList, listState = listState, onLoadMore = ::loadMore)

    val strings = LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    BlurTopBarScaffold(
        modifier = Modifier.fillMaxSize(),
        title = title,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            IconButton(onClick = triggerManualRefresh) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.refresh,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            val currentItems = pagedList.items
            when {
                currentItems == null && pagedList.initialError == null ->
                    LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
                currentItems != null -> when {
                    currentItems.isEmpty() -> EmptyPlaceholder(
                        message = emptyText,
                        icon = emptyIcon,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                    else -> PullToRefresh(
                        isRefreshing = isManualRefreshing,
                        onRefresh = triggerManualRefresh,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                        topAppBarScrollBehavior = scrollBehavior,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                    .fillMaxWidth()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentPadding = contentPadding(paddingValues),
                                verticalArrangement = verticalArrangement,
                            ) {
                                items(
                                    items = currentItems,
                                    key = itemKey,
                                    contentType = { itemContentType },
                                ) { item ->
                                    itemContent(item)
                                }

                                // 三态 footer 收敛到统一组件（UI-3）：Hidden 时不挂 item，避免尾部留白。
                                val footerState = pagedList.footerState
                                if (footerState.phase != PagingFooterPhase.Hidden) {
                                    item(key = footerKey, contentType = "footer") {
                                        PagingLoadMoreFooter(
                                            state = footerState,
                                            onRetry = ::loadMore,
                                            appearance = PagingFooterAppearance.Compact,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> ErrorPlaceholder(
                    error = pagedList.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}
