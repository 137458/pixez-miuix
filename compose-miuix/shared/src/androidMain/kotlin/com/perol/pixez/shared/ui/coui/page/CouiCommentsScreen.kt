package com.perol.pixez.shared.ui.coui.page

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.layout.rememberCouiImeInsets
import com.itos.coui.layout.Scaffold
import com.itos.coui.messageinput.CouiMessageInputAction
import com.itos.coui.messageinput.MessageInputBar
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.Comment
import com.perol.pixez.shared.data.model.CommentResponse
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.ui.components.CommentEmojiText
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.PixivEmojis
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.COUI_MESSAGE_INPUT_MIN_SDK
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiCommentComposer
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiTopBar
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagedLoadOutcome
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberAccountSession
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.emoji_304
import pixez_miuix.shared.generated.resources.Res
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * COUI 评论页：COUI Scaffold（固定顶栏 + 评论列表 + 玻璃输入栏）。
 *
 * 骨架用库的 Scaffold：它自建正文背景源并把顶栏、底栏画在采样层之外，输入栏的玻璃才能
 * 折射到滚动中的真实内容；页面只在容器上做一次 IME 避让，输入栏自己消费剩余导航栏。
 *
 * 输入栏双路径：API 34 及以上走库的 MessageInputBar（需 FloatingRenderer），渲染器预建
 * 失败或版本不足时回退自绘胶囊输入栏，两条路径共用同一份 TextFieldState 草稿。
 *
 * 业务编排与 MIUIX 版一致：账号切换作废列表、流式分页与触底预加载、下拉刷新改为顶栏
 * 刷新（皮肤内不做下拉刷新手势）、回复目标、表情面板、发表后重拉首屏并回顶。
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
    // 草稿用 TextFieldState 单源：库输入栏与自绘输入框共用同一份缓冲，也避免 String 往返把光标顶回行首。
    val draft = rememberTextFieldState()
    val inputInteraction = remember { MutableInteractionSource() }
    val inputFocused by inputInteraction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }
    // 底栏在根坐标中的位置：点击判定要分清「点正文收键盘」与「点输入栏本身」。
    var bottomBarBounds by remember { mutableStateOf(Rect.Zero) }
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    // 渲染器在进 UI 前预建：失败时整条 MessageInputBar 路径让位给自绘输入栏，不出现半渲染的玻璃栏。
    val rendererOutcome = remember {
        if (Build.VERSION.SDK_INT >= COUI_MESSAGE_INPUT_MIN_SDK) {
            runCatching { CouiFloatingRenderer.standalone() }
        } else {
            null
        }
    }
    // 发送状态不持久化：进程恢复后协程不会恢复，避免发送按钮永久卡死。
    var isSending by remember { mutableStateOf(false) }
    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时刷新，供页面作废旧账号数据并重拉首屏。
    val (user, isLoggedIn) = rememberAccountSession()
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

    val canSend = CouiCommentComposer.canSend(draft.text.toString(), isSending, isLoggedIn)
    val inputEnabled = !isSending && isLoggedIn != false
    val placeholder = when {
        isLoggedIn == false -> strings.commentsLoginToComment
        replyTarget != null -> strings.commentsReplyPlaceholder
        else -> strings.commentsPublishPlaceholder
    }

    fun insertEmoji(code: String) {
        val draftSelection = draft.selection
        val insertion = CouiCommentComposer.placeEmoji(draft.text, draftSelection.start, draftSelection.end, code)
        draft.edit {
            replace(0, length, insertion.text)
            selection = TextRange(insertion.cursor)
        }
    }

    val submit: () -> Unit = submit@{
        val comment = draft.text.toString()
        if (!CouiCommentComposer.canSend(comment, isSending, isLoggedIn)) return@submit
        coroutineScope.launch {
            isSending = true
            suspendRunCatchingNonCancel {
                submitComment(comment, replyTarget?.id)
            }.onSuccess {
                draft.edit { replace(0, length, "") }
                replyTarget = null
                triggerManualRefresh()
            }.onFailure { e ->
                // COUI 无内联错误色令牌，失败改用皮肤内的错误 Toast 提示。
                toastSink(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
            }
            isSending = false
        }
    }

    // 键盘将出现时收起表情面板：两者争同一块底部空间，同时展开会互相顶掉。
    LaunchedEffect(inputFocused) {
        if (inputFocused) showEmojiPanel = false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            // IME 避让只在页面级做一次：Scaffold 与输入栏都不再各自处理键盘。
            .windowInsetsPadding(rememberCouiImeInsets())
            .onGloballyPositioned { pageOrigin = it.positionInRoot() }
            .pointerInput(Unit) {
                // 在 Initial 阶段旁观按下：只观察不消费，子节点照样拿到事件，点击卡片也能收起键盘。
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!bottomBarBounds.contains(pageOrigin + down.position)) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showEmojiPanel = false
                    }
                }
            },
        topBar = {
            CouiTopBar(
                title = strings.commentsTitle,
                onBack = onBack,
                actions = {
                    CouiTopBarIconButton(
                        icon = CouiIcons.Refresh,
                        contentDescription = strings.refresh,
                        onClick = triggerManualRefresh,
                    )
                },
            )
        },
        bottomBar = {
            CouiCommentBottomBar(
                draft = draft,
                inputInteraction = inputInteraction,
                placeholder = placeholder,
                inputEnabled = inputEnabled,
                canSend = canSend,
                isSending = isSending,
                isLoggedIn = isLoggedIn,
                replyTarget = replyTarget,
                showEmojiPanel = showEmojiPanel,
                renderer = rendererOutcome?.getOrNull(),
                onToggleEmojiPanel = {
                    val opening = !showEmojiPanel
                    showEmojiPanel = opening
                    // 表情面板占用键盘位置：展开时先收键盘，避免两者叠加把输入栏顶到半空。
                    if (opening) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                },
                onCancelReply = { replyTarget = null },
                onPlaceEmoji = ::insertEmoji,
                onSubmit = submit,
                onBoundsChanged = { bottomBarBounds = it },
            )
        },
    ) { contentPadding ->
        when {
            currentComments == null && pagedList.initialError == null -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding),
            )
            currentComments == null -> CouiErrorPlaceholder(
                error = pagedList.initialError,
                onRetry = triggerManualRefresh,
                modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding),
            )
            currentComments.isEmpty() -> CouiPlaceholder(
                message = strings.noComments,
                modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().consumeWindowInsets(contentPadding),
                overscrollEffect = rememberCouiOverscrollEffect(),
                // 列表滚到输入栏下方：玻璃采样到的是真实滚动内容，而不是被裁掉的空白。
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    end = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    items = currentComments,
                    key = { index, comment -> comment.id ?: "local_comment_$index" },
                    contentType = { _, _ -> "comment_item" },
                ) { _, comment ->
                    CouiCommentItem(
                        comment = comment,
                        onUserClick = onUserClick,
                        onReplyClick = { replyTarget = comment },
                    )
                }

                val footerState = pagedList.footerState
                if (footerState.phase != PagingFooterPhase.Hidden) {
                    item(key = "comment_pagination_footer", contentType = "comment_pagination_footer") {
                        CouiLoadMoreFooter(
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

/** COUI 底栏槽：回复目标、表情面板与输入栏自下而上堆叠。 */
@Composable
private fun CouiCommentBottomBar(
    draft: TextFieldState,
    inputInteraction: MutableInteractionSource,
    placeholder: String,
    inputEnabled: Boolean,
    canSend: Boolean,
    isSending: Boolean,
    isLoggedIn: Boolean?,
    replyTarget: Comment?,
    showEmojiPanel: Boolean,
    renderer: CouiFloatingRenderer?,
    onToggleEmojiPanel: () -> Unit,
    onCancelReply: () -> Unit,
    onPlaceEmoji: (String) -> Unit,
    onSubmit: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) },
    ) {
        if (replyTarget != null) {
            CouiCommentReplyBanner(target = replyTarget, onCancelReply = onCancelReply)
        }
        // 面板放在输入栏之上：放下面会把输入栏顶离底边，并与输入栏自带的导航栏留白叠加。
        if (showEmojiPanel && isLoggedIn != false) {
            CouiCommentEmojiPanel(onPick = onPlaceEmoji)
        }
        if (renderer != null) {
            CouiLibraryMessageInputBar(
                draft = draft,
                renderer = renderer,
                placeholder = placeholder,
                enabled = inputEnabled,
                canSend = canSend,
                isSending = isSending,
                interactionSource = inputInteraction,
                onToggleEmojiPanel = onToggleEmojiPanel,
                onSubmit = onSubmit,
            )
        } else {
            CouiCommentInputRow(
                draft = draft,
                placeholder = placeholder,
                enabled = inputEnabled,
                canSend = canSend,
                isSending = isSending,
                interactionSource = inputInteraction,
                onToggleEmojiPanel = onToggleEmojiPanel,
                onSubmit = onSubmit,
            )
        }
    }
}

/** 回复目标提示：显示被回复的账号与摘要，右侧可取消。 */
@Composable
private fun CouiCommentReplyBanner(
    target: Comment,
    onCancelReply: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 底栏画在正文之上：提示条自带底色，列表内容不会从它背后透出来。
            .background(scheme.background)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = strings.commentsReplyTo.format(target.user?.name.orEmpty(), target.comment.orEmpty()),
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

/** 表情面板：占据键盘位置的固定高度网格，选中后按光标插入草稿。 */
@Composable
private fun CouiCommentEmojiPanel(onPick: (String) -> Unit) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(scheme.container),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(PixivEmojis.allEmojis, key = { it.code }, contentType = { "emoji_item" }) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(emoji.code) },
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

/** API 34 及以上：库的玻璃消息输入栏，前置表情、主操作发送。 */
@RequiresApi(COUI_MESSAGE_INPUT_MIN_SDK)
@Composable
private fun CouiLibraryMessageInputBar(
    draft: TextFieldState,
    renderer: CouiFloatingRenderer,
    placeholder: String,
    enabled: Boolean,
    canSend: Boolean,
    isSending: Boolean,
    interactionSource: MutableInteractionSource,
    onToggleEmojiPanel: () -> Unit,
    onSubmit: () -> Unit,
) {
    val strings = LocalStrings.current
    MessageInputBar(
        state = draft,
        onSubmit = onSubmit,
        enabled = enabled,
        renderer = renderer,
        placeholder = placeholder,
        interactionSource = interactionSource,
        // backdrop 由 Scaffold 经 LocalCouiBackdrop 提供，这里不再自建采样源。
        leadingAction = CouiMessageInputAction(
            icon = painterResource(Res.drawable.emoji_304),
            contentDescription = strings.commentsEmojiPicker,
            onClick = onToggleEmojiPanel,
            enabled = enabled,
        ),
        primaryAction = CouiMessageInputAction(
            icon = rememberVectorPainter(CouiIcons.Send),
            contentDescription = if (isSending) strings.commentsSending else strings.commentsSend,
            onClick = onSubmit,
            enabled = canSend,
        ),
    )
}

/** API 31-33 或渲染器不可用：自绘胶囊输入栏，键盘避让由页面统一处理。 */
@Composable
private fun CouiCommentInputRow(
    draft: TextFieldState,
    placeholder: String,
    enabled: Boolean,
    canSend: Boolean,
    isSending: Boolean,
    interactionSource: MutableInteractionSource,
    onToggleEmojiPanel: () -> Unit,
    onSubmit: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val emojiInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 底色先铺满全宽，再把导航栏避让放到内层，导航栏条带与栏体同色。
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .couiPressFeedback(emojiInteraction)
                .clickable(
                    interactionSource = emojiInteraction,
                    indication = null,
                    enabled = enabled,
                    onClick = onToggleEmojiPanel,
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
        CouiTextInput(
            state = draft,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            enabled = enabled,
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            interactionSource = interactionSource,
        )
        Spacer(modifier = Modifier.width(8.dp))
        CouiSendButton(
            isSending = isSending,
            enabled = canSend,
            strings = strings,
            onClick = onSubmit,
        )
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
                painter = rememberVectorPainter(CouiIcons.Send),
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