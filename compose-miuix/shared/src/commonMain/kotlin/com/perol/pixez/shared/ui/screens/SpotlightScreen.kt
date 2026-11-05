package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.LocalPixivisionRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LiquidFilterBar
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PagingLoadMoreFooter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.SpotlightGridColumnPolicy
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.resolveGridColumns
import com.perol.pixez.shared.ui.components.toStaggeredGridCells
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Spotlight 特辑分类枚举。
 */
enum class SpotlightCategory(
    val code: String,
) {
    ALL("all"),
    ILLUST("illust"),
    MANGA("manga"),
    NOVEL("novel"),
}

fun SpotlightCategory.labelFor(strings: AppStrings): String = when (this) {
    SpotlightCategory.ALL -> strings.categoryAll
    SpotlightCategory.ILLUST -> strings.categoryIllust
    SpotlightCategory.MANGA -> strings.categoryManga
    SpotlightCategory.NOVEL -> strings.categoryNovel
}

/**
 * Spotlight 发现页：支持分类切换（全部/插画/漫画/小说）与无限向下滚动分页加载。
 *
 * 架构审查 UI-5：分类分页不再手写第四套状态机，每个分类各持一个 [PagedListState]——
 * 竞态守卫、翻页失败重试与触底阈值与其余列表页同源；分类切换保留各自列表与游标，
 * 首屏缓存命中直接 [PagedListState.restore]。
 */
@Composable
fun SpotlightScreen(
    onArticleClick: (SpotlightArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalPixivisionRepository.current
    val strings = LocalStrings.current
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

    val scrollBehavior = MiuixScrollBehavior()

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

    val effectiveColumns = rememberSpotlightGridColumns()

    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = strings.spotlightTitle,
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                        actions = {
                            IconButton(onClick = { refreshCurrentCategory() }) {
                                Icon(
                                    imageVector = MiuixIcons.Refresh,
                                    contentDescription = strings.refresh,
                                )
                            }
                        },
                    )
                    // 分类选择横条
                    LiquidFilterBar(
                        items = SpotlightCategory.entries,
                        selectedItem = selectedCategory,
                        onItemSelected = { category ->
                            if (selectedCategory != category) {
                                selectedCategory = category
                            }
                        },
                        labelProvider = { it.labelFor(strings) },
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
            val currentArticles = currentCategoryState.items
            when {
                currentArticles == null && currentCategoryState.initialError == null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )

                currentArticles == null && currentCategoryState.initialError != null -> ErrorPlaceholder(
                    error = currentCategoryState.initialError,
                    onRetry = { refreshCurrentCategory() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )

                currentArticles != null && currentArticles.isEmpty() -> EmptyPlaceholder(
                    message = strings.noData,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )

                else -> PullToRefresh(
                    isRefreshing = currentCategoryState.isRefreshing,
                    onRefresh = { refreshCurrentCategory() },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                    topAppBarScrollBehavior = scrollBehavior,
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = effectiveColumns,
                        state = currentGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = paddingValues.calculateTopPadding() + 12.dp,
                            end = 16.dp,
                            bottom = LocalBottomBarContentPadding.current,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                    ) {
                        itemsIndexed(
                            items = currentArticles.orEmpty(),
                            key = { _, item -> "${selectedCategory.code}_${item.id}" },
                            contentType = { _, _ -> "spotlight_article" },
                        ) { _, article ->
                            SpotlightArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) },
                            )
                        }

                        // 统一三态 footer（UI-3）：翻页失败给出重试入口，代替此前静默停在半截。
                        val footerState = currentCategoryState.footerState
                        if (footerState.phase != PagingFooterPhase.Hidden) {
                            item(
                                span = StaggeredGridItemSpan.FullLine,
                                contentType = "load_more_footer",
                            ) {
                                PagingLoadMoreFooter(
                                    state = footerState,
                                    onRetry = { loadMoreCurrentCategory() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单张 Spotlight 文章卡片：封面图 + 标题 + 副标题。
 */
@Composable
internal fun SpotlightArticleCard(
    article: SpotlightArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PixivAsyncImage(
                model = article.thumbnail,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = article.title,
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.pureTitle.isNotBlank() && article.pureTitle != article.title) {
                    Text(
                        text = article.pureTitle,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Spotlight 特辑网格的列数策略（SpotlightScreen 与 SpotlightDetailScreen 共用）：
 * 列数换算收敛到 UI-9 的纯函数，策略数值见 [SpotlightGridColumnPolicy] 与 AppConstants.Layout。
 */
@Composable
internal fun rememberSpotlightGridColumns(): StaggeredGridCells {
    val settings = LocalSettingsRepository.current
    val changeVersion = settings.changeVersion.collectAsState().value
    return remember(settings.crossAdapt, settings.crossAdapterWidth, settings.crossCount, changeVersion) {
        resolveGridColumns(
            adapt = settings.crossAdapt == true,
            adapterWidthDp = settings.crossAdapterWidth,
            configuredCount = settings.crossCount,
            policy = SpotlightGridColumnPolicy,
        ).toStaggeredGridCells()
    }
}
