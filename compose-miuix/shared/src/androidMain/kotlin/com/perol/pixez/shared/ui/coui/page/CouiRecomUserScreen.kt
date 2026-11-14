package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 推荐画师页：固定顶栏 + 用户预览卡片列表。
 *
 * 分页、账号切换作废与触底预加载与 MIUIX 版一致；差异只有皮肤层：
 * 固定顶栏刷新入口、COUI 卡片行与三态占位，列表收敛到平板可读宽度，
 * 不再提供下拉刷新（与全部 COUI 页面一致）。
 */
@Composable
internal fun CouiRecomUserScreen(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    val repository = LocalUserRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current

    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 登录状态与账号身份：登录/登出/切号时作废旧账号列表并重拉首屏。
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.recomUserTitle,
            onBack = onBack,
        ) {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = triggerManualRefresh,
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            val currentItems = pagedList.items
            when {
                currentItems == null && pagedList.initialError == null ->
                    CouiStatePlaceholder(
                        message = strings.loading,
                        modifier = Modifier.fillMaxSize(),
                    )

                currentItems != null -> if (currentItems.isEmpty()) {
                    CouiStatePlaceholder(
                        message = strings.recomUserEmpty,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 8.dp,
                            end = 12.dp,
                            bottom = LocalBottomBarContentPadding.current,
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
                                CouiPagingFooter(
                                    state = footerState,
                                    onRetry = ::loadMore,
                                )
                            }
                        }
                    }
                }

                else -> CouiStatePlaceholder(
                    message = couiErrorMessage(pagedList.initialError),
                    actionText = strings.retry,
                    onAction = { triggerManualRefresh() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
