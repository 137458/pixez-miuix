package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.perol.pixez.shared.ui.AppConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Comment
import com.perol.pixez.shared.data.model.CommentResponse
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.perol.pixez.shared.ui.components.CommentEmojiText
import com.perol.pixez.shared.ui.components.PixivEmojis
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.derivedStateOf
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.icon.extended.*
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.emoji_304

/**
 * 作品评论页：展示指定作品的用户评论列表，支持流式分页加载、下拉刷新与发表评论。
 * [isNovel] 为 true 时走小说评论端点（/v3/novel/comments 等），否则走插画评论端点。
 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun CommentsScreen(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    isNovel: Boolean = false,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalIllustRepository.current
    val accountRepository = LocalAccountRepository.current
    // retryCount 变化触发重新加载：点击重试或发表评论后自增。
    var retryCount by rememberSaveable(illustId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(illustId) { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }
    // 发送状态在进程恢复时不应持久化，否则可能因发送中断而永久卡死。
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    // 登录状态：未登录时禁用发送按钮，与 Hello/New/Spotlight 等页面保持一致。
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user != null
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user != null
        }
    }
    // 回复目标：选中某条评论时非空，发送时作为 parent_comment_id。
    // 使用 remember：进程恢复时丢失回复目标不会导致功能异常，用户可重新点击回复。
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // isNovel 在本页生命周期内固定：按其一次性选定评论读取与发表的仓储端点，下游两处调用收敛到选中函数。
    val fetchCommentsResponse: suspend (String?) -> CommentResponse =
        if (isNovel) {
            { nextUrl -> repository.getNovelCommentsResponse(illustId, nextUrl = nextUrl) }
        } else {
            { nextUrl -> repository.getIllustCommentsResponse(illustId, nextUrl = nextUrl) }
        }
    val submitComment: suspend (comment: String, parentCommentId: Long?) -> Unit =
        if (isNovel) {
            { comment, parentCommentId ->
                repository.postNovelComment(novelId = illustId, comment = comment, parentCommentId = parentCommentId)
            }
        } else {
            { comment, parentCommentId ->
                repository.postComment(illustId = illustId, comment = comment, parentCommentId = parentCommentId)
            }
        }

    // 分页状态机单点实现（含 generation 竞态守卫），替代手抄的请求代数与标志位。
    val pagedList = remember(illustId, isNovel) {
        PagedListState(
            fetch = { nextUrl ->
                val response = fetchCommentsResponse(nextUrl)
                PagedListPage(response.comments, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinct(new) },
        )
    }

    val listState = rememberLazyListState()

    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 user/isLoggedIn）时作废旧账号评论列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(illustId, retryCount, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        val force = isManualRefreshing
        val previousItems = pagedList.items
        pagedList.loadFirst(force = force)
        isManualRefreshing = false
        // 刷新成功且列表已替换时回到顶部（含发表评论后的自动刷新）。
        if (force && previousItems != null && pagedList.items !== previousItems && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val currentComments = pagedList.items

    // 触底预加载：当滑到最后 4 项时自动请求下一页
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
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    BlurTopBarScaffold(
        modifier = Modifier.fillMaxSize(),
        title = strings.commentsTitle,
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
                            submitComment(inputText, replyTarget?.id)
                        }.onSuccess {
                            inputText = ""
                            replyTarget = null
                            // 重新刷新评论列表以显示最新评论
                            triggerManualRefresh()
                        }.onFailure { e ->
                            sendError = PixivApiErrorFormatter.format(e, strings)
                        }
                        isSending = false
                    }
                },
                error = sendError,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                currentComments == null && pagedList.initialError == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
                currentComments == null && pagedList.initialError != null -> ErrorPlaceholder(
                    error = pagedList.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
                currentComments != null -> {
                    if (currentComments.isEmpty()) {
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
                                    items = currentComments,
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
                                                text = strings.commentsNoMore,
                                                style = MiuixTheme.textStyles.footnote1,
                                            )
                                        }
                                    }
                                }
                            }

                            VerticalScrollBar(
                                adapter = rememberScrollBarAdapter(listState),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                trackPadding = paddingValues,
                            )
                        }
                    }
                }
            }
        }
    }
}
}


/**
 * 评论输入栏：位于页面底部，提供输入框、表情面板切换、发送按钮与回复目标提示。
 */
@OptIn(ExperimentalScrollBarApi::class)
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
    val strings = LocalStrings.current
    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
        ) {
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
                val emojiGridState = rememberLazyGridState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                LazyVerticalGrid(
                    state = emojiGridState,
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    modifier = Modifier.fillMaxSize(),
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

                VerticalScrollBar(
                    adapter = rememberScrollBarAdapter(emojiGridState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }
        }
    }
}
}

@Composable
private fun CommentItem(
    comment: Comment,
    onUserClick: (Long) -> Unit,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
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
                contentDescription = strings.stamp,
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
