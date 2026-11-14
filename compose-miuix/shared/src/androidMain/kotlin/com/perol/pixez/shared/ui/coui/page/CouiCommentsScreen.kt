package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Comment
import com.perol.pixez.shared.data.model.CommentResponse
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.components.CommentEmojiText
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.PixivEmojis
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagedLoadOutcome
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.emoji_304
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Send

/**
 * COUI 评论页：固定顶栏 + 评论卡片列表 + 底部输入栏。
 *
 * 业务编排与 MIUIX 版一致：账号切换作废列表、流式分页与触底预加载、下拉刷新改为顶栏
 * 刷新（皮肤内不做下拉刷新手势）、回复目标、表情面板、发表后重拉首屏并回顶。
 * COUI 库无文本输入组件，输入框用基础文本字段按 COUI 胶囊样式自绘，交互等价。
 */
@Composable
internal fun CouiCommentsScreen(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    isNovel: Boolean = false,
) {
    val repository = LocalIllustRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current

    var retryCount by rememberSaveable(illustId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(illustId) { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }
    // 发送状态不持久化：进程恢复后协程不会恢复，避免发送按钮永久卡死。
    var isSending by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }

    // isNovel 在本页生命周期内固定：一次性选定评论读取与发表端点。
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

    // 账号身份跟踪：登录/登出/切号时作废旧列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(illustId, retryCount, isLoggedIn, user) {
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        val force = isManualRefreshing
        val outcome = pagedList.loadFirst(force = force)
        isManualRefreshing = false
        if (force && outcome == PagedLoadOutcome.Written && listState.firstVisibleItemIndex > 0) {
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

    // 触底预加载：四条件守卫与阈值单源在状态机内，此处只喂末项索引。
    val shouldLoadMore by remember(pagedList) {
        derivedStateOf {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
            )
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMore()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.commentsTitle,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = triggerManualRefresh,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // 阅读列：列表与三态占位统一收敛到平板可读宽度。
            Box(
                modifier = Modifier
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                when {
                currentComments == null && pagedList.initialError == null -> CouiStatePlaceholder(
                    message = strings.loading,
                    modifier = Modifier.fillMaxSize(),
                )
                currentComments == null -> CouiStatePlaceholder(
                    message = couiErrorMessage(pagedList.initialError),
                    actionText = strings.retry,
                    onAction = triggerManualRefresh,
                    modifier = Modifier.fillMaxSize(),
                )
                currentComments.isEmpty() -> CouiStatePlaceholder(
                    message = strings.noComments,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = currentComments,
                        key = { it.id ?: it.hashCode() },
                        contentType = { "comment_item" },
                    ) { comment ->
                        CouiCommentItem(
                            comment = comment,
                            onUserClick = onUserClick,
                            onReplyClick = { replyTarget = comment },
                        )
                    }

                    val footerState = pagedList.footerState
                    if (footerState.phase != PagingFooterPhase.Hidden) {
                        item(key = "comment_pagination_footer", contentType = "comment_pagination_footer") {
                            CouiPagingFooter(
                                state = footerState,
                                onRetry = ::loadMore,
                                noMoreText = strings.commentsNoMore,
                            )
                        }
                    }
                }
            }
        }
        }

        CouiCommentInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            isSending = isSending,
            isLoggedIn = isLoggedIn,
            replyTarget = replyTarget,
            onCancelReply = { replyTarget = null },
            onSend = {
                if (isSending || inputText.isBlank() || isLoggedIn != true) return@CouiCommentInputBar
                coroutineScope.launch {
                    isSending = true
                    suspendRunCatchingNonCancel {
                        submitComment(inputText, replyTarget?.id)
                    }.onSuccess {
                        inputText = ""
                        replyTarget = null
                        triggerManualRefresh()
                    }.onFailure { e ->
                        // COUI 无内联错误色令牌，失败改用皮肤内的错误 Toast 提示。
                        toastSink(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
                    }
                    isSending = false
                }
            },
        )
    }
}

/** 底部输入栏：表情切换、胶囊输入框、发送按钮与回复目标提示。 */
@Composable
private fun CouiCommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    isLoggedIn: Boolean?,
    replyTarget: Comment?,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }
    val inputEnabled = !isSending && isLoggedIn != false
    val emojiInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        replyTarget?.let { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.commentsReplyTo.format(
                        target.user?.name.orEmpty(),
                        target.comment.orEmpty(),
                    ),
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                CouiCancelButton(text = strings.cancel, onClick = onCancelReply)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .couiPressFeedback(emojiInteraction, enabled = inputEnabled, list = false)
                    .clickable(
                        interactionSource = emojiInteraction,
                        indication = null,
                        enabled = inputEnabled,
                        onClick = { showEmojiPanel = !showEmojiPanel },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.emoji_304),
                    contentDescription = strings.commentsEmojiPicker,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.container)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = when {
                            isLoggedIn == false -> strings.commentsLoginToComment
                            replyTarget != null -> strings.commentsReplyPlaceholder
                            else -> strings.commentsPublishPlaceholder
                        },
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = inputEnabled,
                    textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(scheme.accent),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CouiSendButton(
                isSending = isSending,
                enabled = text.isNotBlank() && !isSending && isLoggedIn == true,
                strings = strings,
                onClick = onSend,
            )
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
                            .clickable { onTextChange(text + emoji.code) },
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

/** 取消回复按钮：容器底色小胶囊。 */
@Composable
private fun CouiCancelButton(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CouiTheme.colorScheme.container)
            .couiPressFeedback(interactionSource, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** 发送按钮：可发送时强调色胶囊，发送中展示文案，不可发送时降级为容器底色。 */
@Composable
private fun CouiSendButton(
    isSending: Boolean,
    enabled: Boolean,
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) scheme.accent else scheme.container)
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSending) {
            Text(
                text = strings.commentsSending,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        } else {
            Image(
                painter = rememberVectorPainter(MiuixIcons.Send),
                contentDescription = strings.commentsSend,
                colorFilter = ColorFilter.tint(if (enabled) scheme.onAccent else scheme.onSurfaceVariant),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** COUI 评论卡片：头像/账号、回复对象、正文、贴纸、时间与回复动作。 */
@Composable
private fun CouiCommentItem(
    comment: Comment,
    onUserClick: (Long) -> Unit,
    onReplyClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val userId = comment.user?.id

    CouiCard {
        Column(modifier = Modifier.padding(12.dp)) {
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
                            kind = CouiTextKind.ItemTitle,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "@${user.account}",
                            kind = CouiTextKind.ItemSummary,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                } ?: run {
                    Text(
                        text = strings.commentsAnonymousUser,
                        kind = CouiTextKind.ItemTitle,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (comment.parentComment?.user != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${strings.commentsReplyAction} @${comment.parentComment.user.name}",
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.accentOnSurface,
                )
            }

            if (!comment.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                CommentEmojiText(
                    text = comment.comment,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                    color = scheme.onSurface,
                )
            }

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
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (comment.id != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.commentsReplyAction,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.accentOnSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onReplyClick)
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}
