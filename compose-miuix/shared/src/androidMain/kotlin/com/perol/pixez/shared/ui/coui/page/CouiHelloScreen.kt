package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiPageScaffold
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.IllustFeedRestoreCache
import com.perol.pixez.shared.ui.state.rememberHelloFeedState
import kotlinx.coroutines.flow.Flow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * COUI 推荐页：复用 [rememberHelloFeedState] 的分页/登录/恢复逻辑，仅替换视觉壳。
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
    val bottomBarPadding = LocalBottomBarContentPadding.current
    val feed = rememberHelloFeedState(feedRestore, reselectFlow, onLoginClick)

    CouiPageScaffold(
        title = strings.tabRecommend,
        actions = {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = feed.refresh,
            )
            if (feed.isLoggedIn == false) {
                CouiActionButton(
                    icon = MiuixIcons.Contacts,
                    contentDescription = strings.goLogin,
                    onClick = onLoginClick,
                )
            }
            if (feed.isLoggedIn == true) {
                CouiActionButton(
                    icon = MiuixIcons.AddCircle,
                    contentDescription = strings.recomUserTitle,
                    onClick = onRecomUserClick,
                )
                val currentUser = feed.user
                val targetUserId = currentUser?.userId?.toLongOrNull()
                if (currentUser != null && targetUserId != null) {
                    val pressInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .couiPressFeedback(pressInteraction, list = false)
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
            CouiActionButton(
                icon = MiuixIcons.Settings,
                contentDescription = strings.tabSettings,
                onClick = onSettingsClick,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CouiTheme.colorScheme.background),
        ) {
            val currentIllusts = feed.listState.filteredIllusts
            when {
                currentIllusts == null && feed.listState.initialError == null -> CouiStatePlaceholder(
                    message = strings.loading,
                    modifier = Modifier.padding(padding),
                )
                currentIllusts == null && feed.listState.initialError != null -> CouiStatePlaceholder(
                    message = couiErrorMessage(feed.listState.initialError),
                    actionText = strings.retry,
                    onAction = feed.refresh,
                    modifier = Modifier.padding(padding),
                )
                currentIllusts != null && currentIllusts.isEmpty() -> CouiStatePlaceholder(
                    message = strings.noData,
                    modifier = Modifier.padding(padding),
                )
                currentIllusts != null -> CouiIllustGrid(
                    illusts = currentIllusts,
                    state = feed.gridState,
                    onIllustClick = onIllustClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        top = 4.dp,
                        end = 10.dp,
                        bottom = bottomBarPadding + 8.dp,
                    ),
                    hasMore = feed.listState.nextUrl != null,
                    isLoadingMore = feed.listState.isLoadingMore,
                    loadMoreError = feed.listState.loadMoreError,
                    onLoadMore = feed.loadMore,
                )
            }
        }
    }
}
