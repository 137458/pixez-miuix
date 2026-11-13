package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.IllustFeedRestoreCache
import com.perol.pixez.shared.ui.state.rememberHelloFeedState
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.extended.Settings
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.components.blurBackdropSource

/**
 * 首页/推荐页：顶部标题栏 + 真实推荐插画瀑布流。
 */
@Composable
fun HelloScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRecomUserClick: () -> Unit,
    feedRestore: IllustFeedRestoreCache,
    reselectFlow: kotlinx.coroutines.flow.Flow<Unit>? = null,
) {
    val strings = LocalStrings.current
    val bottomBarPadding = LocalBottomBarContentPadding.current
    // 推荐页业务状态由皮肤无关的 HelloFeedState 提供，MIUIX/COUI 只替换 UI 壳。
    val feed = rememberHelloFeedState(feedRestore, reselectFlow, onLoginClick)
    val listState = feed.listState
    val gridState = feed.gridState
    val isManualRefreshing = feed.isManualRefreshing
    val user = feed.user
    val isLoggedIn = feed.isLoggedIn
    val showLoginDialog = feed.showLoginDialog

    // 未登录提示对话框：位于 Scaffold 外层，确保能覆盖整个页面。
    OverlayDialog(
        title = strings.dialogNeedLogin,
        summary = strings.dialogNeedLoginSummary,
        show = showLoginDialog,
        onDismissRequest = { feed.dismissLoginDialog() },
    ) {
        TextButton(
            text = strings.btnCancelLogin,
            onClick = { feed.dismissLoginDialog() },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = strings.btnGoLogin,
            onClick = { feed.goLogin() },
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val triggerManualRefresh: () -> Unit = feed.refresh

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    BlurTopBarScaffold(
        modifier = Modifier.fillMaxSize(),
        title = strings.tabRecommend,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(
                onClick = triggerManualRefresh,
            ) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.refresh,
                )
            }
            if (isLoggedIn == false) {
                IconButton(
                    onClick = onLoginClick,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Contacts,
                        contentDescription = strings.goLogin,
                    )
                }
            }
            if (isLoggedIn == true) {
                IconButton(
                    onClick = onRecomUserClick,
                ) {
                    Icon(
                        imageVector = MiuixIcons.AddCircle,
                        contentDescription = strings.recomUserTitle,
                    )
                }
                val currentUser = user
                val targetUserId = currentUser?.userId?.toLongOrNull()
                if (currentUser != null && targetUserId != null) {
                    IconButton(
                        onClick = { onUserClick(targetUserId) },
                    ) {
                        PixivAsyncImage(
                            model = currentUser.userImage,
                            contentDescription = strings.myProfile,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
            IconButton(
                onClick = onSettingsClick,
            ) {
                Icon(
                    imageVector = MiuixIcons.Settings,
                    contentDescription = strings.tabSettings,
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
            val currentIllusts = listState.filteredIllusts
            when {
                currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
                currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
                    error = listState.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts != null -> {
                    if (currentIllusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.noData,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                            topAppBarScrollBehavior = scrollBehavior,
                        ) {
                            IllustStaggeredGrid(
                                illusts = currentIllusts,
                                state = gridState,
                                onIllustClick = onIllustClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    top = paddingValues.calculateTopPadding() + 8.dp,
                                    end = 8.dp,
                                    bottom = bottomBarPadding,
                                ),
                                hasMore = listState.nextUrl != null,
                                isLoadingMore = listState.isLoadingMore,
                                loadMoreError = listState.loadMoreError,
                                onLoadMore = feed.loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}

