/**
 * 搜索页结果区（由 SearchScreen 拆分而来）。
 *
 * 包含作品搜索结果瀑布流、画师/小说搜索结果列表，以及日期筛选的防抖工具函数。
 * 作品结果的分页/竞态/加载态收敛到共享状态机 IllustPagedListState（架构审查 高-4b）；
 * 画师与小说结果共享 [SearchPagedListColumn] 骨架（账号身份跟踪 + PagedListState 分页 +
 * 占位与分页 footer 三态），仓库调用与空态文案由各自入口注入。
 */

package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.NovelRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.NovelCard
import com.perol.pixez.shared.ui.components.PagingLoadMoreFooter
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchIllustResultGrid(
    query: String,
    sort: String,
    filterState: SearchFilterState,
    repository: SearchRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    onIllustClick: (Long) -> Unit,
    scrollBehavior: ScrollBehavior,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues? = null,
) {
    val effectiveContentPadding = contentPadding ?: PaddingValues(
        start = 8.dp,
        top = 8.dp,
        end = 8.dp,
        bottom = LocalBottomBarContentPadding.current,
    )
    val resultsState = rememberSearchIllustResultsState(
        query = query,
        sort = sort,
        filterState = filterState,
        repository = repository,
        banRepository = banRepository,
        settingsRepository = settingsRepository,
    )
    val listState = resultsState.listState
    val coroutineScope = rememberCoroutineScope()

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            listState.loadMore()
        }
    }

    val strings = LocalStrings.current
    val currentIllusts = listState.filteredIllusts
    when {
        currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(
            modifier = Modifier
                .fillMaxSize()
                .padding(effectiveContentPadding),
        )
        currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
            error = listState.initialError,
            onRetry = resultsState.retry,
            modifier = Modifier
                .fillMaxSize()
                .padding(effectiveContentPadding),
        )
        currentIllusts != null -> {
            val filteredIllusts = remember(currentIllusts, filterState.ugoiraFilter, filterState.ratioFilter) {
                applySearchClientFilters(currentIllusts, filterState.ugoiraFilter, filterState.ratioFilter)
            }
            if (filteredIllusts.isEmpty() && !listState.isLoadingMore) {
                when (
                    resolveSearchEmptyPhase(
                        hasMore = listState.nextUrl != null,
                        isLoadingMore = listState.isLoadingMore,
                        hasLoadMoreError = listState.loadMoreError != null,
                    )
                ) {
                    // 翻页失败：自动翻页已停（shouldPreloadMore 在 hasLoadMoreError 时返回 false），
                    // 必须给出重试入口，否则空结果页会永久停在「加载中」。
                    SearchEmptyPhase.Retry -> ErrorPlaceholder(
                        error = listState.loadMoreError,
                        onRetry = ::loadMore,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(effectiveContentPadding),
                    )
                    SearchEmptyPhase.LoadingMore -> {
                        LaunchedEffect(filteredIllusts.isEmpty(), listState.nextUrl) {
                            loadMore()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(effectiveContentPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                InfiniteProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = strings.loading,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                    SearchEmptyPhase.Empty -> EmptyPlaceholder(
                        message = strings.searchEmptyIllust,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(effectiveContentPadding),
                    )
                }
            } else {
                IllustStaggeredGrid(
                    illusts = filteredIllusts,
                    onIllustClick = onIllustClick,
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = effectiveContentPadding,
                    hasMore = listState.nextUrl != null,
                    isLoadingMore = listState.isLoadingMore,
                    loadMoreError = listState.loadMoreError,
                    onLoadMore = ::loadMore,
                )
            }
        }
    }
}

/**
 * 搜索结果为空时的后续状态：仍在翻页 / 翻页失败待重试 / 确实没有结果。
 */
internal enum class SearchEmptyPhase {
    LoadingMore,
    Retry,
    Empty,
}

/**
 * 空列表分支的渲染态判定：失败重试优先于继续翻页，两者都优先于空态。
 * 抽成纯函数以便直接断言分支优先级（组合内的分支本身无法在 JVM 测试中驱动）。
 */
internal fun resolveSearchEmptyPhase(
    hasMore: Boolean,
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
): SearchEmptyPhase = when {
    hasLoadMoreError -> SearchEmptyPhase.Retry
    hasMore || isLoadingMore -> SearchEmptyPhase.LoadingMore
    else -> SearchEmptyPhase.Empty
}

/**
 * 画师搜索结果列表：筛选设置变更版本号作为重载 key（设置变化即整表重拉，保持原实现语义），
 * 分页与骨架细节见 [SearchPagedListColumn]。
 */
@Composable
internal fun SearchUserResultList(
    query: String,
    repository: SearchRepository,
    settingsRepository: SettingsRepository,
    onUserClick: (Long) -> Unit,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues? = null,
) {
    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    SearchPagedListColumn(
        query = query,
        reloadTrigger = filterChangeVersion,
        emptyMessage = LocalStrings.current.searchEmptyUser,
        itemKey = { it.user.id },
        itemContentType = "user_preview_item",
        footerKey = "search_user_footer",
        scrollBehavior = scrollBehavior,
        listState = listState,
        contentPadding = contentPadding,
        fetch = { nextUrl ->
            val response = repository.searchUserResponse(query, nextUrl = nextUrl)
            PagedListPage(response.userPreviews, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
        itemContent = { preview ->
            UserPreviewItem(
                preview = preview,
                onClick = { onUserClick(preview.user.id) },
            )
        },
    )
}

/**
 * 小说搜索结果列表：纯 word 直搜（/v1/search/novel，不带 sort/search_target 筛选），
 * 竖向 NovelCard 列表 + 触底分页。分页/竞态/账号跟踪与画师结果列表同构，
 * 由共享骨架 [SearchPagedListColumn] 承载；小说结果不做 R18/AI 筛选（本批次范围外），
 * 无筛选重载语义，reloadTrigger 传常量。
 */
@Composable
internal fun SearchNovelResultList(
    query: String,
    novelRepository: NovelRepository,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues? = null,
) {
    SearchPagedListColumn(
        query = query,
        reloadTrigger = Unit,
        emptyMessage = LocalStrings.current.noData,
        itemKey = { it.id },
        itemContentType = "novel_card_item",
        footerKey = "search_novel_footer",
        scrollBehavior = scrollBehavior,
        listState = listState,
        contentPadding = contentPadding,
        fetch = { nextUrl ->
            val response = novelRepository.searchNovels(query, nextUrl = nextUrl)
            PagedListPage(response.novels, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
        itemContent = { novel ->
            NovelCard(
                novel = novel,
                onClick = { onNovelClick(novel.id) },
                onUserClick = onUserClick,
            )
        },
    )
}

/**
 * 画师/小说搜索结果列表的共享骨架：账号身份跟踪 + [PagedListState] 分页 +
 * Loading/Error/Empty 占位 + LazyColumn 内容槽 + 触底预加载 + 分页 footer 三态。
 *
 * 数据源以 [fetch] 注入（仓库调用由各入口保持原样），翻页合并以 [mergeItems] 注入；
 * [reloadTrigger] 追加进重载效应 key（画师列表传 filterChangeVersion，小说列表传常量），
 * 其余行为（重试计数、账号切换作废、触底阈值、footer 挂载条件）与拆分前逐项等价。
 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun <T> SearchPagedListColumn(
    query: String,
    reloadTrigger: Any?,
    emptyMessage: String,
    itemKey: (T) -> Any,
    itemContentType: String,
    footerKey: String,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues? = null,
    fetch: suspend (nextUrl: String?) -> PagedListPage<T>,
    mergeItems: (old: List<T>, new: List<T>) -> List<T>,
    itemContent: @Composable (item: T) -> Unit,
) {
    val effectiveContentPadding = contentPadding ?: PaddingValues(bottom = LocalBottomBarContentPadding.current)
    // 登录状态与账号身份跟踪：登录/登出/切号（loginEventFlow）到达时作废旧账号结果并重拉首屏。
    val accountRepository = LocalAccountRepository.current
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 挂起上下文读取当前账号：suspendRunCatchingNonCancel 捕获异常并保留取消语义（同 NewScreen）。
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }
    // 搜索结果重试计数，作为 LaunchedEffect 的 key 触发重新加载。
    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    // 分页状态机单点实现（含 generation 竞态守卫），替代手抄的请求代数与标志位。
    val pagedList = remember(query) {
        PagedListState(
            fetch = fetch,
            mergeItems = mergeItems,
        )
    }
    val coroutineScope = rememberCoroutineScope()

    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 user/isLoggedIn）时作废旧账号结果并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(query, reloadTrigger, retryCount, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        pagedList.loadFirst(force = true)
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    val currentItems = pagedList.items
    // 四条件守卫与阈值单源在状态机内（UI-2），此处只喂布局上报的末项索引。
    val shouldLoadMore by remember(pagedList) {
        derivedStateOf {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
            )
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    when {
        currentItems == null && pagedList.initialError == null -> LoadingPlaceholder(
            modifier = Modifier
                .fillMaxSize()
                .padding(effectiveContentPadding),
        )
        currentItems == null && pagedList.initialError != null -> ErrorPlaceholder(
            error = pagedList.initialError,
            onRetry = { retryCount++ },
            modifier = Modifier
                .fillMaxSize()
                .padding(effectiveContentPadding),
        )
        currentItems != null -> {
            if (currentItems.isEmpty()) {
                EmptyPlaceholder(
                    message = emptyMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(effectiveContentPadding),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = effectiveContentPadding,
                    ) {
                        items(
                            items = currentItems,
                            key = itemKey,
                            contentType = { itemContentType },
                        ) { item ->
                            itemContent(item)
                        }

                        // 三态 footer 收敛到统一组件（UI-3）：Hidden 时不挂 item。
                        val footerState = pagedList.footerState
                        if (footerState.phase != PagingFooterPhase.Hidden) {
                            item(key = footerKey, contentType = "footer") {
                                PagingLoadMoreFooter(
                                    state = footerState,
                                    onRetry = ::loadMore,
                                )
                            }
                        }
                    }

                    VerticalScrollBar(
                        adapter = rememberScrollBarAdapter(listState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        trackPadding = effectiveContentPadding,
                    )
                }
            }
        }
    }
}

