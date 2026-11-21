package com.perol.pixez.shared.ui.state

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.LocalPixivisionRepository
import com.perol.pixez.shared.ui.screens.SpotlightCategory
import com.perol.pixez.shared.ui.screens.rememberSpotlightGridColumns
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Spotlight 页的皮肤无关状态集合：分类选择、每分类独立分页/滚动状态、刷新与翻页动作。
 *
 * 架构审查 UI-5：分类分页不另写状态机，每个分类各持一个 [PagedListState]；
 * MIUIX 与 COUI 只替换 UI 壳。
 */
internal class SpotlightFeedState(
    val selectedCategory: SpotlightCategory,
    val articles: List<SpotlightArticle>?,
    val initialError: Throwable?,
    val isRefreshing: Boolean,
    val footerState: PagingFooterState,
    val gridState: LazyStaggeredGridState,
    val columns: StaggeredGridCells,
    val onCategorySelected: (SpotlightCategory) -> Unit,
    val refresh: () -> Unit,
    val loadMore: () -> Unit,
)

@Composable
internal fun rememberSpotlightFeedState(): SpotlightFeedState {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalPixivisionRepository.current
    var selectedCategory by rememberSaveable { mutableStateOf(SpotlightCategory.ALL) }
    val coroutineScope = rememberCoroutineScope()

    // loadFirst 不透传 force 给 fetch，以「同一协程内先写后读」保持手动刷新跳过缓存的语义（同 HelloScreen）。
    var forceNextFetch by remember { mutableStateOf(false) }

    fun newCategoryState(category: SpotlightCategory): PagedListState<SpotlightArticle> = PagedListState(
        fetch = { nextUrl ->
            val response = repository.getSpotlightArticles(
                category = category.code,
                nextUrl = nextUrl,
                forceRefresh = forceNextFetch,
            )
            PagedListPage(response.spotlightArticles, response.nextUrl)
        },
        // 累积与去重收敛到状态机注入点（架构审查 中-8）：仓库只返回当页响应。
        mergeItems = { old, new -> old.appendDistinct(new) },
    )

    // 为每个分类分别维护状态，切换分类时保持各自的浏览位置与缓存数据；首屏缓存命中直接恢复。
    val categoryStates = remember {
        mutableStateMapOf<SpotlightCategory, PagedListState<SpotlightArticle>>().apply {
            SpotlightCategory.entries.forEach { category ->
                val state = newCategoryState(category)
                repository.getCachedSpotlightArticles(category.code)?.let { cached ->
                    if (cached.spotlightArticles.isNotEmpty()) {
                        state.restore(cached.spotlightArticles, cached.nextUrl)
                    }
                }
                put(category, state)
            }
        }
    }
    val currentCategoryState = categoryStates.getOrPut(selectedCategory) { newCategoryState(selectedCategory) }

    // 为每个分类分别维护独立的网格滚动状态。
    val gridStates = remember {
        mutableStateMapOf<SpotlightCategory, LazyStaggeredGridState>()
    }
    val currentGridState = gridStates.getOrPut(selectedCategory) {
        LazyStaggeredGridState()
    }

    // 首次进入或分类切换时发起首屏加载：已持有列表（缓存命中或此前已加载）时状态机内部早退。
    LaunchedEffect(selectedCategory) {
        currentCategoryState.loadFirst()
    }

    fun refreshCurrentCategory() {
        coroutineScope.launch {
            forceNextFetch = true
            currentCategoryState.loadFirst(force = true)
            forceNextFetch = false
        }
    }

    fun loadMoreCurrentCategory() {
        coroutineScope.launch { currentCategoryState.loadMore() }
    }

    // 监听列表滚动触底，自动触发加载下一页（四条件守卫与阈值单源在状态机内）。
    LaunchedEffect(currentGridState, currentCategoryState) {
        snapshotFlow {
            currentCategoryState.shouldPreloadMore(
                lastVisibleItemIndex = currentGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
            )
        }.distinctUntilChanged()
            .filter { it }
            .collect {
                loadMoreCurrentCategory()
            }
    }

    return SpotlightFeedState(
        selectedCategory = selectedCategory,
        articles = currentCategoryState.items,
        initialError = currentCategoryState.initialError,
        isRefreshing = currentCategoryState.isRefreshing,
        footerState = currentCategoryState.footerState,
        gridState = currentGridState,
        columns = rememberSpotlightGridColumns(),
        onCategorySelected = { selectedCategory = it },
        refresh = ::refreshCurrentCategory,
        loadMore = ::loadMoreCurrentCategory,
    )
}
