package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.FrostedTopAppBar

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.extended.Refresh
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 用户关注列表页：支持流式分页与下拉刷新。
 */
@Composable
fun UserFollowListScreen(
    userId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: UserRepository,
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(userId) { mutableStateOf(false) }

    val state = produceState<Result<Pair<List<UserPreview>, String?>>?>(
        initialValue = null,
        userId,
        retryCount,
    ) {
        val followResult = suspendRunCatchingNonCancel { repository.getUserFollowingResponse(userId) }
        isManualRefreshing = false
        value = followResult.map { it.userPreviews to it.nextUrl }
    }

    var previews by remember(userId) { mutableStateOf(listOf<UserPreview>()) }
    var nextUrl by remember(userId) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.value) {
        state.value?.onSuccess { (initialPreviews, initialNextUrl) ->
            previews = initialPreviews
            nextUrl = initialNextUrl
            isLoadingMore = false
            loadMoreError = null
        }
    }

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel { repository.getUserFollowingResponse(userId, nextUrl = currentNextUrl) }
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

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FrostedTopAppBar(
                title = strings.userFollowTitle,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
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
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val pinnedHeaderHeight = statusBarTop + 56.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface),
        ) {
            val result = state.value
            when {
                result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
                result.isSuccess -> {
                    if (previews.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.userFollowEmpty,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = pinnedHeaderHeight + 28.dp),
                            topAppBarScrollBehavior = scrollBehavior,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(backdrop),
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
                                    contentType = { "user_preview_item" },
                                ) { preview ->
                                    UserPreviewItem(
                                        preview = preview,
                                        onClick = { onUserClick(preview.user.id) },
                                    )
                                }

                                item(key = "follow_pagination_footer", contentType = "footer") {
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
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

