package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import androidx.compose.ui.graphics.Color
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

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
 * 分类列表状态容器，维护特辑列表、分页 nextUrl 与加载状态。
 */
private class CategoryState {
    var articles by mutableStateOf<List<SpotlightArticle>>(emptyList())
    var nextUrl by mutableStateOf<String?>(null)
    var isInitialLoading by mutableStateOf(true)
    var isRefreshing by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
    var error by mutableStateOf<Throwable?>(null)
}

/**
 * Spotlight 发现页：支持分类切换（全部/插画/漫画/小说）与无限向下滚动分页加载。
 */
@Composable
fun SpotlightScreen(
    repository: IllustRepository,
    onArticleClick: (SpotlightArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var selectedCategory by rememberSaveable { mutableStateOf(SpotlightCategory.ALL) }
    val coroutineScope = rememberCoroutineScope()

    // 为每个分类分别维护状态，切换分类时保持各自的浏览位置与缓存数据。
    val categoryStates = remember {
        mutableStateMapOf<SpotlightCategory, CategoryState>().apply {
            SpotlightCategory.entries.forEach { category ->
                val cached = repository.getCachedSpotlightArticles(category.code)
                val state = CategoryState().apply {
                    if (cached != null && cached.spotlightArticles.isNotEmpty()) {
                        articles = cached.spotlightArticles
                        nextUrl = cached.nextUrl
                        isInitialLoading = false
                    }
                }
                put(category, state)
            }
        }
    }
    val currentCategoryState = categoryStates.getOrPut(selectedCategory) { CategoryState() }

    // 为每个分类分别维护独立的网格滚动状态。
    val gridStates = remember {
        mutableStateMapOf<SpotlightCategory, LazyStaggeredGridState>()
    }
    val currentGridState = gridStates.getOrPut(selectedCategory) {
        LazyStaggeredGridState()
    }

    val scrollBehavior = MiuixScrollBehavior()

    val loadCategoryData: (SpotlightCategory, Boolean) -> Unit = { category, isRefresh ->
        val targetState = categoryStates.getOrPut(category) { CategoryState() }
        if (!isRefresh && targetState.articles.isNotEmpty()) {
            // 已有缓存且非强制刷新，不再重新请求
        } else {
            coroutineScope.launch {
                if (isRefresh) {
                    targetState.isRefreshing = true
                } else if (targetState.articles.isEmpty()) {
                    targetState.isInitialLoading = true
                }
                targetState.error = null

                val result = suspendRunCatchingNonCancel {
                    repository.getSpotlightArticles(category = category.code, nextUrl = null, forceRefresh = isRefresh)
                }

                result.onSuccess { response ->
                    targetState.articles = response.spotlightArticles
                    targetState.nextUrl = response.nextUrl
                    targetState.error = null
                }.onFailure { err ->
                    targetState.error = err
                }

                targetState.isInitialLoading = false
                targetState.isRefreshing = false
            }
        }
    }

    val loadMoreCategoryData: (SpotlightCategory) -> Unit = { category ->
        val targetState = categoryStates.getOrPut(category) { CategoryState() }
        val nextUrl = targetState.nextUrl
        if (!targetState.isLoadingMore && !targetState.isRefreshing && nextUrl != null && nextUrl.isNotBlank()) {
            coroutineScope.launch {
                targetState.isLoadingMore = true
                val result = suspendRunCatchingNonCancel {
                    repository.getSpotlightArticles(category = category.code, nextUrl = nextUrl)
                }
                result.onSuccess { response ->
                    val existingIds = targetState.articles.map { it.id }.toSet()
                    val newArticles = response.spotlightArticles.filter { it.id !in existingIds }
                    targetState.articles = targetState.articles + newArticles
                    targetState.nextUrl = response.nextUrl
                }
                targetState.isLoadingMore = false
            }
        }
    }

    // 首次进入或分类切换时，若当前分类未加载过则发起初次加载。
    LaunchedEffect(selectedCategory) {
        if (currentCategoryState.articles.isEmpty() && !currentCategoryState.isRefreshing) {
            loadCategoryData(selectedCategory, false)
        }
    }

    // 监听列表滚动触底，自动触发加载下一页。
    LaunchedEffect(currentGridState, selectedCategory) {
        snapshotFlow {
            val layoutInfo = currentGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 4
        }.distinctUntilChanged()
            .filter { it }
            .collect {
                loadMoreCategoryData(selectedCategory)
            }
    }

    val settings = LocalSettingsRepository.current
    val effectiveColumns = remember(settings?.crossAdapt, settings?.crossAdapterWidth, settings?.crossCount, settings?.changeVersion) {
        if (settings?.crossAdapt == true) {
            val minWidth = (settings.crossAdapterWidth * 1.2f).toInt().coerceIn(160, 600)
            StaggeredGridCells.Adaptive(minWidth.dp)
        } else {
            val configuredCols = settings?.crossCount ?: 2
            if (configuredCols == 2) {
                StaggeredGridCells.Adaptive(240.dp)
            } else {
                StaggeredGridCells.Fixed(configuredCols.coerceIn(1, 4))
            }
        }
    }


    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.spotlightTitle,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                    actions = {
                        IconButton(onClick = { loadCategoryData(selectedCategory, true) }) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = strings.refresh,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
            ) {
                // 分类选择横条
                SpotlightCategorySelector(
                    selectedCategory = selectedCategory,
                    strings = strings,
                    onCategorySelected = { category ->
                        if (selectedCategory != category) {
                            selectedCategory = category
                        }
                    },
                )

                when {
                    currentCategoryState.isInitialLoading -> LoadingPlaceholder(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )

                    currentCategoryState.error != null && currentCategoryState.articles.isEmpty() -> ErrorPlaceholder(
                        error = currentCategoryState.error,
                        onRetry = { loadCategoryData(selectedCategory, false) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )

                    currentCategoryState.articles.isEmpty() -> EmptyPlaceholder(
                        message = strings.noData,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )

                    else -> PullToRefresh(
                        isRefreshing = currentCategoryState.isRefreshing,
                        onRefresh = { loadCategoryData(selectedCategory, true) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    ) {
                        LazyVerticalStaggeredGrid(
                            columns = effectiveColumns,
                            state = currentGridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 12.dp,
                                end = 16.dp,
                                bottom = 96.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                        ) {
                        itemsIndexed(
                            items = currentCategoryState.articles,
                            key = { _, item -> "${selectedCategory.code}_${item.id}" },
                            contentType = { _, _ -> "spotlight_article" },
                        ) { _, article ->
                            SpotlightArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) },
                            )
                        }

                        if (currentCategoryState.isLoadingMore) {
                            item(
                                span = StaggeredGridItemSpan.FullLine,
                                contentType = "loading_footer",
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = strings.loading,
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * 分类选择横条，采用 MIUIX 胶囊按钮风格。
 */
@Composable
private fun SpotlightCategorySelector(
    selectedCategory: SpotlightCategory,
    strings: AppStrings,
    onCategorySelected: (SpotlightCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = SpotlightCategory.entries,
            contentType = { "category_tab" },
        ) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
                    )
                    .clickable { onCategorySelected(category) },
            ) {
                Text(
                    text = category.labelFor(strings),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MiuixTheme.colorScheme.onPrimary
                    } else {
                        MiuixTheme.colorScheme.onSurface
                    },
                )
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
