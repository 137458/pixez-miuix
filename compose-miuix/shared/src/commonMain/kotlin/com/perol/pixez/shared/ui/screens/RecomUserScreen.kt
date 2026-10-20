package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.perol.pixez.shared.ui.AppConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.Refresh
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 推荐用户列表页：支持触底自动流式加载与下拉刷新。
 */
@Composable
fun RecomUserScreen(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalUserRepository.current
    val accountRepository = LocalAccountRepository.current
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时作废旧账号列表并重拉首屏。
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 挂起上下文读取当前账号：suspendRunCatchingNonCancel 捕获异常并保留取消语义（同 NewScreen）。
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user != null
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user != null
        }
    }

    // 分页状态机单点实现（含 generation 竞态守卫），替代手抄的请求代数与标志位。
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

    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 user/isLoggedIn）时作废旧账号列表并重拉首屏。
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

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(pagedList) {
        derivedStateOf {
            val totalCount = pagedList.items?.size ?: 0
            if (totalCount == 0 ||
                pagedList.nextUrl == null ||
                pagedList.isLoadingMore ||
                pagedList.loadMoreError != null
            ) {
                false
            } else {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
                lastVisibleItem >= totalCount - AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    val strings = LocalStrings.current
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    BlurTopBarScaffold(
        modifier = Modifier.fillMaxSize(),
        title = strings.recomUserTitle,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            IconButton(onClick = triggerManualRefresh) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.refresh,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            val currentItems = pagedList.items
            when {
                currentItems == null && pagedList.initialError == null ->
                    LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
                currentItems != null -> when {
                    currentItems.isEmpty() -> {
                        EmptyPlaceholder(
                            message = strings.recomUserEmpty,
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                        )
                    }
                    else -> {
                            PullToRefresh(
                                isRefreshing = isManualRefreshing,
                                onRefresh = triggerManualRefresh,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                topAppBarScrollBehavior = scrollBehavior,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                            .fillMaxWidth()
                                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                                        contentPadding = paddingValues,
                                    ) {
                                        items(
                                            items = currentItems,
                                            key = { it.user.id },
                                            contentType = { "user_preview_item" },
                                        ) { preview ->
                                            UserPreviewItem(
                                                preview = preview,
                                                onClick = { onUserClick(preview.user.id) },
                                            )
                                        }

                                        item(key = "load_more_footer", contentType = "footer") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                when {
                                                    pagedList.isLoadingMore -> InfiniteProgressIndicator()
                                                    pagedList.loadMoreError != null -> Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = strings.loadFailed,
                                                            style = MiuixTheme.textStyles.body2,
                                                            color = MiuixTheme.colorScheme.error,
                                                        )
                                                        Button(onClick = ::loadMore) {
                                                            Text(text = strings.retry)
                                                        }
                                                    }
                                                    pagedList.nextUrl == null -> Text(
                                                        text = strings.noMoreData,
                                                        style = MiuixTheme.textStyles.footnote1,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> ErrorPlaceholder(
                    error = pagedList.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
        }
    }
}

