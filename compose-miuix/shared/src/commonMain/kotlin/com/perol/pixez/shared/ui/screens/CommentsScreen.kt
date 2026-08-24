package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Comment
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.perol.pixez.shared.ui.components.CommentEmojiText
import com.perol.pixez.shared.ui.components.PixivEmojis
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.*
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.emoji_304

/**
 * 作品评论页：展示指定作品的用户评论列表，支持流式分页加载、下拉刷新与发表评论。
 */
@Composable
fun CommentsScreen(
    illustId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: IllustRepository,
    accountRepository: AccountRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试或发表评论后自增触发重新加载。
    var retryCount by rememberSaveable(illustId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(illustId) { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }
    // 发送状态在进程恢复时不应持久化，否则可能因发送中断而永久卡死。
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    // 登录状态：未登录时禁用发送按钮，与 Hello/New/Spotlight 等页面保持一致。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        isLoggedIn = suspendRunCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }
    // 回复目标：选中某条评论时非空，发送时作为 parent_comment_id。
    // 使用 remember：进程恢复时丢失回复目标不会导致功能异常，用户可重新点击回复。
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val state = produceState<Result<Pair<List<Comment>, String?>>?>(
        initialValue = null,
        illustId,
        retryCount,
    ) {
        val commentResult = suspendRunCatchingNonCancel { repository.getIllustCommentsResponse(illustId) }
        isManualRefreshing = false
        value = commentResult.map { it.comments to it.nextUrl }
    }

    var comments by remember(illustId) { mutableStateOf(listOf<Comment>()) }
    var nextUrl by remember(illustId) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(state.value) {
        state.value?.onSuccess { (initialComments, initialNextUrl) ->
            comments = initialComments
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
            suspendRunCatchingNonCancel { repository.getIllustCommentsResponse(illustId, nextUrl = currentNextUrl) }
                .onSuccess { response ->
                    comments = comments + response.comments
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

    // 触底预加载：当滑到最后 4 项时自动请求下一页
    val shouldLoadMore by remember(comments.size, nextUrl, isLoadingMore, loadMoreError) {
        derivedStateOf {
            val totalCount = comments.size
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.commentsTitle,
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
        bottomBar = {
            CommentInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                isSending = isSending,
                isLoggedIn = isLoggedIn,
                replyTarget = replyTarget,
                onCancelReply = { replyTarget = null },
                onSend = {
                    if (isSending || inputText.isBlank() || isLoggedIn != true) return@CommentInputBar
                    coroutineScope.launch {
                        isSending = true
                        sendError = null
                        suspendRunCatchingNonCancel {
                            repository.postComment(
                                illustId = illustId,
                                comment = inputText,
                                parentCommentId = replyTarget?.id,
                            )
                        }.onSuccess {
                            inputText = ""
                            replyTarget = null
                            triggerManualRefresh()
                        }.onFailure { e ->
                            sendError = e.message ?: "${strings.commentsSend}${strings.loadFailed}"
                        }
                        isSending = false
                    }
                },
                error = sendError,
            )
        },
    ) { paddingValues ->
        val result = state.value
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.padding(paddingValues))
            result.isSuccess -> {
                if (comments.isEmpty()) {
                    EmptyPlaceholder(
                        message = strings.noComments,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    PullToRefresh(
                        isRefreshing = isManualRefreshing,
                        onRefresh = triggerManualRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = paddingValues,
                        ) {
                            items(
                                items = comments,
                                key = { it.id ?: it.hashCode() },
                                contentType = { "comment_item" },
                            ) { comment ->
                                CommentItem(
                                    comment = comment,
                                    onUserClick = onUserClick,
                                    onReplyClick = { replyTarget = comment },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            item(key = "comment_pagination_footer", contentType = "comment_pagination_footer") {
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
                                            text = strings.commentsNoMore,
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}


/**
 * 评论输入栏：位于页面底部，提供输入框、表情面板切换、发送按钮与回复目标提示。
 */
@Composable
private fun CommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    isLoggedIn: Boolean?,
    replyTarget: Comment?,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        error?.let {
            Text(
                text = it,
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        replyTarget?.let { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.commentsReplyTo.format(target.user?.name.orEmpty(), target.comment.orEmpty()),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onCancelReply) {
                    Text(strings.cancel)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { showEmojiPanel = !showEmojiPanel },
                enabled = !isSending && isLoggedIn != false,
            ) {
                Image(
                    painter = painterResource(Res.drawable.emoji_304),
                    contentDescription = strings.commentsEmojiPicker,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = text,
                onValueChange = onTextChange,
                label = when {
                    isLoggedIn == false -> strings.commentsLoginToComment
                    replyTarget != null -> strings.commentsReplyPlaceholder
                    else -> strings.commentsPublishPlaceholder
                },
                modifier = Modifier.weight(1f),
                enabled = !isSending && isLoggedIn != false,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                enabled = text.isNotBlank() && !isSending && isLoggedIn == true,
            ) {
                if (isSending) {
                    Text(strings.commentsSending)
                } else {
                    Icon(
                        imageVector = MiuixIcons.Send,
                        contentDescription = strings.commentsSend,
                    )
                }
            }
        }

        if (showEmojiPanel && isLoggedIn != false) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = PixivEmojis.allEmojis,
                    key = { it.code },
                    contentType = { "emoji_item" },
                ) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onTextChange(text + emoji.code)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(emoji.resource),
                            contentDescription = emoji.code,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    onUserClick: (Int) -> Unit,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val userId = comment.user?.id
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (userId != null) {
                        Modifier.clickable { onUserClick(userId) }
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            comment.user?.let { user ->
                PixivAsyncImage(
                    model = user.profileImageUrls.medium,
                    contentDescription = user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MiuixTheme.textStyles.body1,
                    )
                    Text(
                        text = "@${user.account}",
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            } ?: run {
                Text(
                    text = strings.commentsAnonymousUser,
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 回复对象提示
        if (comment.parentComment?.user != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "To ${comment.parentComment.user.name}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
        }

        // 评论正文（支持行内表情图文混排）
        if (!comment.comment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            CommentEmojiText(
                text = comment.comment,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        // Stamp 图片/贴纸
        val stampUrl = comment.stamp?.stampUrl
        if (!stampUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            PixivAsyncImage(
                model = stampUrl,
                contentDescription = "Stamp",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }

        comment.date?.let { date ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                style = MiuixTheme.textStyles.footnote2,
            )
        }
        if (comment.id != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.commentsReplyAction,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onReplyClick),
            )
        }
    }
}
