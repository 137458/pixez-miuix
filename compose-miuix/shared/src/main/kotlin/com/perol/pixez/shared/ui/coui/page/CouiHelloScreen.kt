package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiMainScaffold
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.IllustFeedRestoreCache
import com.perol.pixez.shared.ui.state.rememberHelloFeedState
import kotlinx.coroutines.flow.Flow
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * COUI 推荐页：复用 [rememberHelloFeedState] 的分页/登录/恢复逻辑，仅替换视觉壳。
 *
 * 骨架用一级页 [CouiMainScaffold]（折叠大标题、无返回），顶栏动作用 [CouiTopBarIconButton]；
 * 正文瀑布流把骨架护栏交给 `contentPadding`，首屏三态交给 [CouiPlaceholder] /
 * [CouiErrorPlaceholder]，页面自身不再铺底、不再扣安全区。
 */
@Composable
internal fun CouiHelloScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRecomUserClick: () -> Unit,
    feedRestore: IllustFeedRestoreCache,
    reselectFlow: Flow<Unit>?,
) {
    val strings = LocalStrings.current
    val feed = rememberHelloFeedState(feedRestore, reselectFlow, onLoginClick)

    CouiMainScaffold(
        title = strings.tabRecommend,
        actions = {
            CouiTopBarIconButton(
                onClick = feed.refresh,
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
            )
            if (feed.isLoggedIn == false) {
                CouiTopBarIconButton(
                    onClick = onLoginClick,
                    icon = CouiIcons.Contacts,
                    contentDescription = strings.goLogin,
                )
            }
            if (feed.isLoggedIn == true) {
                CouiTopBarIconButton(
                    onClick = onRecomUserClick,
                    icon = CouiIcons.AddCircle,
                    contentDescription = strings.recomUserTitle,
                )
                val currentUser = feed.user
                val targetUserId = currentUser?.userId?.toLongOrNull()
                if (currentUser != null && targetUserId != null) {
                    val pressInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = pressInteraction, list = false)
                            .clickable(
                                interactionSource = pressInteraction,
                                indication = null,
                            ) { onUserClick(targetUserId) },
                        contentAlignment = Alignment.Center,
                    ) {
                        PixivAsyncImage(
                            model = currentUser.userImage,
                            contentDescription = strings.myProfile,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
            CouiTopBarIconButton(
                onClick = onSettingsClick,
                icon = CouiIcons.Settings,
                contentDescription = strings.tabSettings,
            )
        },
    ) { padding ->
        val currentIllusts = feed.listState.filteredIllusts
        when {
            currentIllusts == null && feed.listState.initialError == null -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentIllusts == null && feed.listState.initialError != null -> CouiErrorPlaceholder(
                error = feed.listState.initialError,
                onRetry = feed.refresh,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentIllusts != null && currentIllusts.isEmpty() -> CouiPlaceholder(
                message = strings.noData,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentIllusts != null -> CouiIllustGrid(
                illusts = currentIllusts,
                state = feed.gridState,
                onIllustClick = onIllustClick,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
                // 护栏进列表内层：内容可以滚进顶栏覆盖区，底部留白已含底栏占位。
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    end = 10.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                hasMore = feed.listState.nextUrl != null,
                isLoadingMore = feed.listState.isLoadingMore,
                loadMoreError = feed.listState.loadMoreError,
                isRefreshing = feed.listState.isRefreshing,
                onLoadMore = feed.loadMore,
            )
        }
    }
}
