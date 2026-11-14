package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiPageScaffold
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiSpotlightCard
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.SpotlightCategory
import com.perol.pixez.shared.ui.screens.labelFor
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberSpotlightFeedState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI Spotlight 发现页：复用 [rememberSpotlightFeedState] 的分类分页与滚动逻辑，仅替换视觉壳。
 */
@Composable
internal fun CouiSpotlightScreen(
    onArticleClick: (SpotlightArticle) -> Unit,
) {
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
    val feed = rememberSpotlightFeedState()
    val chips = remember(strings) {
        SpotlightCategory.entries.map { CouiFilterChip(key = it.code, label = it.labelFor(strings)) }
    }

    CouiPageScaffold(
        title = strings.spotlightTitle,
        actions = {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = feed.refresh,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CouiTheme.colorScheme.background)
                .padding(padding),
        ) {
            CouiFilterChipBar(
                chips = chips,
                selectedKey = feed.selectedCategory.code,
                onSelect = { key ->
                    SpotlightCategory.entries.firstOrNull { it.code == key }
                        ?.let(feed.onCategorySelected)
                },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                val articles = feed.articles
                when {
                    articles == null && feed.initialError == null -> CouiStatePlaceholder(
                        message = strings.loading,
                    )
                    articles == null && feed.initialError != null -> CouiStatePlaceholder(
                        message = couiErrorMessage(feed.initialError),
                        actionText = strings.retry,
                        onAction = feed.refresh,
                    )
                    articles != null && articles.isEmpty() -> CouiStatePlaceholder(
                        message = strings.noData,
                    )
                    articles != null -> LazyVerticalStaggeredGrid(
                        columns = feed.columns,
                        state = feed.gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = bottomBarPadding + 8.dp,
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
                                CouiPagingFooter(
                                    state = feed.footerState,
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
