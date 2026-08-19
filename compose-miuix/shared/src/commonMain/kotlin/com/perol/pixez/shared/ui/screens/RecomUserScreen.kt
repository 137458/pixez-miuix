package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.UserPreviewsResponse
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 推荐用户列表页：支持触底自动流式加载与下拉刷新。
 */
@Composable
fun RecomUserScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: UserRepository,
) {
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    val initialState = produceState<Result<UserPreviewsResponse>?>(
        initialValue = null,
        repository,
        retryCount,
    ) {
        val userResult = suspendRunCatchingNonCancel { repository.getRecommendedUsers() }
        isManualRefreshing = false
        value = userResult
    }

    var previews by remember { mutableStateOf(listOf<UserPreview>()) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialState.value) {
        initialState.value?.onSuccess { response ->
            previews = response.userPreviews
            nextUrl = response.nextUrl
            isLoadingMore = false
            loadMoreError = null
        }
    }

    fun loadMore() {
        val url = nextUrl ?: return
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel { repository.getRecommendedUsers(url) }
                .onSuccess { response ->
                    previews = previews + response.userPreviews
                    nextUrl = response.nextUrl
                }
                .onFailure { error ->
                    loadMoreError = error
                }
            isLoadingMore = false
        }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(previews.size, nextUrl, isLoadingMore, loadMoreError) {
        derivedStateOf {
            val totalCount = previews.size
            if (totalCount == 0 || nextUrl == null || isLoadingMore || loadMoreError != null) {
                false
            } else {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
                lastVisibleItem >= totalCount - 4
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    val strings = LocalStrings.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.recomUserTitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = triggerManualRefresh) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = strings.refresh,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val result = initialState.value) {
            null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
            else -> when {
                result.isSuccess -> {
                    if (previews.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.recomUserEmpty,
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentPadding = paddingValues,
                            ) {
                                items(
                                    items = previews,
                                    key = { it.user.id },
                                ) { preview ->
                                    UserPreviewItem(
                                        preview = preview,
                                        onClick = { onUserClick(preview.user.id) },
                                    )
                                }

                                item(key = "load_more_footer") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        when {
                                            isLoadingMore -> InfiniteProgressIndicator()
                                            loadMoreError != null -> Row(
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
                                            nextUrl == null -> Text(
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
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
        }
    }
}

