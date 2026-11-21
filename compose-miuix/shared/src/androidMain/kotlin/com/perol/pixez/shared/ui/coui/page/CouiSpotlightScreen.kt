package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiSpotlightCard
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiMainScaffold
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.SpotlightCategory
import com.perol.pixez.shared.ui.screens.labelFor
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberSpotlightFeedState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI Spotlight 发现页：复用 [rememberSpotlightFeedState] 的分类分页与滚动逻辑，仅替换视觉壳。
 *
 * 骨架用一级页 [CouiMainScaffold]；分类 chip 栏作为瀑布流第一行（FullLine span）随内容滚动，
 * 首屏三态用 core 占位组件，护栏只出现在列表 `contentPadding` 与占位正文的 `.padding()`。
 */
@Composable
internal fun CouiSpotlightScreen(
    onArticleClick: (SpotlightArticle) -> Unit,
) {
    val strings = LocalStrings.current
    val feed = rememberSpotlightFeedState()
    val chips = remember(strings) {
        SpotlightCategory.entries.map { CouiFilterChip(key = it.code, label = it.labelFor(strings)) }
    }

    // 头部筛选条：随列表滚进顶栏覆盖区。
    val headerContent: @Composable () -> Unit = {
        CouiFilterChipBar(
            chips = chips,
            selectedKey = feed.selectedCategory.code,
            onSelect = { key ->
                SpotlightCategory.entries.firstOrNull { it.code == key }
                    ?.let(feed.onCategorySelected)
            },
        )
    }

    CouiMainScaffold(
        title = strings.spotlightTitle,
        actions = {
            CouiTopBarIconButton(
                onClick = feed.refresh,
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
            )
        },
    ) { padding ->
        val articles = feed.articles
        when {
            articles == null && feed.initialError == null -> CouiSpotlightStateBody(
                message = strings.loading,
                padding = padding,
                header = headerContent,
            )

            articles == null && feed.initialError != null -> CouiSpotlightStateBody(
                error = feed.initialError,
                onRetry = feed.refresh,
                padding = padding,
                header = headerContent,
            )

            articles != null && articles.isEmpty() -> CouiSpotlightStateBody(
                message = strings.noData,
                padding = padding,
                header = headerContent,
            )

            articles != null -> LazyVerticalStaggeredGrid(
                columns = feed.columns,
                state = feed.gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(
                    key = "coui_spotlight_header",
                    contentType = "coui_spotlight_header",
                    span = StaggeredGridItemSpan.FullLine,
                ) { headerContent() }
                itemsIndexed(
                    items = articles,
                    key = { _, item -> "${feed.selectedCategory.code}_${item.id}" },
                    contentType = { _, _ -> "coui_spotlight" },
                ) { _, article ->
                    CouiSpotlightCard(
                        article = article,
                        onClick = { onArticleClick(article) },
                    )
                }
                if (feed.footerState.phase != PagingFooterPhase.Hidden) {
                    item(
                        span = StaggeredGridItemSpan.FullLine,
                        contentType = "coui_spotlight_footer",
                    ) {
                        CouiLoadMoreFooter(
                            state = feed.footerState,
                            onRetry = feed.loadMore,
                        )
                    }
                }
            }
        }
    }
}

/** 头部 chip 栏 + 居中占位：加载 / 错误 / 空态共用同一滚动起点。 */
@Composable
private fun CouiSpotlightStateBody(
    padding: PaddingValues,
    header: @Composable () -> Unit,
    message: String? = null,
    error: Throwable? = null,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),
    ) {
        header()
        if (error != null) {
            CouiErrorPlaceholder(
                error = error,
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        } else if (message != null) {
            CouiPlaceholder(
                message = message,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
