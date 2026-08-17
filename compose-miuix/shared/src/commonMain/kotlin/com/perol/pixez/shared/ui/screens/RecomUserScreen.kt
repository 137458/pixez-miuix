package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

/**
 * 推荐用户列表页。
 *
 * 支持分页加载：首次加载后保留 Pixiv 返回的 next_url，
 * 用户点击底部「加载更多」时继续请求下一页并追加到列表。
 */
@Composable
fun RecomUserScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: UserRepository,
) {
    // 首次加载失败时通过自增 retryCount 触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    // 首次加载状态：仅用于获取第一页数据。
    val initialState = produceState<Result<UserPreviewsResponse>?>(
        initialValue = null,
        repository,
        retryCount,
    ) {
        // 当前处于 produceState 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        value = suspendRunCatchingNonCancel { repository.getRecommendedUsers() }
    }

    // 累积的推荐用户列表与下一页链接。
    var previews by remember { mutableStateOf(listOf<UserPreview>()) }
    var nextUrl by remember { mutableStateOf<String?>(null) }

    // 加载更多相关状态。
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }

    val coroutineScope = rememberCoroutineScope()

    /**
     * 首次加载成功后初始化列表；失败或重试时由 produceState 重新触发。
     * 配置变更后 previews 会重置，此时重新从 initialState 恢复。
     */
    LaunchedEffect(initialState.value) {
        initialState.value?.onSuccess { response ->
            previews = response.userPreviews
            nextUrl = response.nextUrl
        }
    }

    /**
     * 加载下一页推荐用户。
     * 仅在 nextUrl 非空且未处于加载态时执行。
     */
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

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "为你推荐",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
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
                    val currentPreviews = previews
                    if (currentPreviews.isEmpty()) {
                        EmptyPlaceholder(
                            message = "暂无推荐用户",
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = paddingValues,
                        ) {
                            items(
                                items = currentPreviews,
                                key = { it.user.id },
                            ) { preview ->
                                UserPreviewItem(
                                    preview = preview,
                                    onClick = { onUserClick(preview.user.id) },
                                )
                            }

                            item(key = "load_more") {
                                LoadMoreFooter(
                                    nextUrl = nextUrl,
                                    isLoading = isLoadingMore,
                                    error = loadMoreError,
                                    onLoadMore = ::loadMore,
                                    onRetry = ::loadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { retryCount++ },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
        }
    }
}

/**
 * 推荐用户列表底部加载更多区域。
 */
@Composable
private fun LoadMoreFooter(
    nextUrl: String?,
    isLoading: Boolean,
    error: Throwable?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            isLoading -> LoadingPlaceholder(modifier = Modifier.fillMaxWidth())
            error != null -> ErrorPlaceholder(
                error = error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            nextUrl != null -> TextButton(
                text = "加载更多",
                onClick = onLoadMore,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> Text(
                text = "没有更多了",
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
