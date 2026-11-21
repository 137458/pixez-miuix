package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberAccountSession
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * COUI 推荐画师页：二级页骨架 + 用户预览卡片列表。
 *
 * 分页、账号切换作废、触底预加载与三态判定与 MIUIX 版一致；差异只有皮肤层：
 * 容器由 [CouiSecondaryScaffold] 唯一提供，刷新入口用顶栏图标按钮，
 * 列表护栏进 `contentPadding`，footer 收敛到 core 的 [CouiLoadMoreFooter]。
 * 仍不提供下拉刷新（与全部 COUI 页面一致）。
 */
@Composable
internal fun CouiRecomUserScreen(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    val repository = LocalUserRepository.current
    val strings = LocalStrings.current

    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时刷新，供页面作废旧账号数据并重拉首屏。
    val (user, isLoggedIn) = rememberAccountSession()

    val pagedList = remember(repository) {
        PagedListState(
            fetch = { nextUrl ->
                val response = if (nextUrl == null) {
                    repository.getRecommendedUsers()
                } else {
                    repository.getRecommendedUsers(nextUrl)
                }
                PagedListPage(response.userPreviews, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinct(new) },
        )
    }
    val coroutineScope = rememberCoroutineScope()

    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(repository, retryCount, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        val force = isManualRefreshing
        pagedList.loadFirst(force = force)
        isManualRefreshing = false
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember(pagedList) {
        derivedStateOf {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
            )
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    CouiSecondaryScaffold(
        title = strings.recomUserTitle,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = triggerManualRefresh,
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
            )
        },
    ) { padding ->
        val currentItems = pagedList.items
        when {
            currentItems == null && pagedList.initialError == null -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            currentItems != null -> if (currentItems.isEmpty()) {
                CouiPlaceholder(
                    message = strings.recomUserEmpty,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(padding),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        end = 12.dp,
                        bottom = padding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    overscrollEffect = rememberCouiOverscrollEffect(),
                ) {
                    items(
                        items = currentItems,
                        key = { it.user.id },
                        contentType = { "coui_recom_user" },
                    ) { preview ->
                        CouiUserPreviewRow(
                            preview = preview,
                            onClick = { onUserClick(preview.user.id) },
                        )
                    }

                    val footerState = pagedList.footerState
                    if (footerState.phase != PagingFooterPhase.Hidden) {
                        item(key = "coui_recom_user_footer", contentType = "footer") {
                            CouiLoadMoreFooter(
                                state = footerState,
                                onRetry = ::loadMore,
                            )
                        }
                    }
                }
            }

            else -> CouiErrorPlaceholder(
                error = pagedList.initialError,
                onRetry = { triggerManualRefresh() },
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )
        }
    }
}
