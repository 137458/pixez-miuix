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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.SpotlightArticle
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
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberSpotlightFeedState
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
    // Spotlight 业务状态由皮肤无关的 SpotlightFeedState 提供，MIUIX/COUI 只替换 UI 壳。
    val strings = LocalStrings.current
    val feed = rememberSpotlightFeedState()
    val selectedCategory = feed.selectedCategory
    val currentGridState = feed.gridState

    val scrollBehavior = MiuixScrollBehavior()

    val effectiveColumns = feed.columns

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
                            IconButton(onClick = feed.refresh) {
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
                                feed.onCategorySelected(category)
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
            val currentArticles = feed.articles
            when {
                currentArticles == null && feed.initialError == null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )

                currentArticles == null && feed.initialError != null -> ErrorPlaceholder(
                    error = feed.initialError,
                    onRetry = feed.refresh,
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
                    isRefreshing = feed.isRefreshing,
                    onRefresh = feed.refresh,
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
                        val footerState = feed.footerState
                        if (footerState.phase != PagingFooterPhase.Hidden) {
                            item(
                                span = StaggeredGridItemSpan.FullLine,
                                contentType = "load_more_footer",
                            ) {
                                PagingLoadMoreFooter(
                                    state = footerState,
                                    onRetry = feed.loadMore,
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
