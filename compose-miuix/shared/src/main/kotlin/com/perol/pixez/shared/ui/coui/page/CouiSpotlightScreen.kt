package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
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
import com.perol.pixez.shared.ui.coui.component.CouiChipTabRow
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiSpotlightCard
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiMainScaffold
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.SpotlightCategory
import com.perol.pixez.shared.ui.screens.labelFor
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberSpotlightFeedState

/**
 * COUI Spotlight 发现页：复用 [rememberSpotlightFeedState] 的分类分页与滚动逻辑，
 * 顶栏下方固定 COUI 原厂 [CouiChipTabRow]（API 34+ 凝光毛玻璃采样正文）。
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

    CouiMainScaffold(
        title = strings.spotlightTitle,
        actions = {
            CouiTopBarIconButton(
                onClick = feed.refresh,
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
            )
        },
        topBarBottom = {
            CouiChipTabRow(
                chips = chips,
                selectedKey = feed.selectedCategory.code,
                onSelect = { key ->
                    SpotlightCategory.entries.firstOrNull { it.code == key }
                        ?.let(feed.onCategorySelected)
                },
            )
        },
    ) { padding ->
        val articles = feed.articles
        when {
            articles == null && feed.initialError == null -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            articles == null && feed.initialError != null -> CouiErrorPlaceholder(
                error = feed.initialError,
                onRetry = feed.refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            articles != null && articles.isEmpty() -> CouiPlaceholder(
                message = strings.noData,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
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
