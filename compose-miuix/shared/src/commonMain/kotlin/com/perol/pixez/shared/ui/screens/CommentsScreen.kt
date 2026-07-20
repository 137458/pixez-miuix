package com.perol.pixez.shared.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品评论页：展示指定作品的用户评论列表，并支持发表评论。
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
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var inputText by rememberSaveable { mutableStateOf("") }
    // 发送状态在进程恢复时不应持久化，否则可能因发送中断而永久卡死。
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    // 登录状态：未登录时禁用发送按钮，与 Hello/New/Spotlight 等页面保持一致。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        isLoggedIn = runCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }
    // 回复目标：选中某条评论时非空，发送时作为 parent_comment_id。
    // 使用 remember：进程恢复时丢失回复目标不会导致功能异常，用户可重新点击回复。
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val state = produceState<Result<List<Comment>>?>(
        initialValue = null,
        illustId,
        repository,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getIllustComments(illustId) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "评论",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
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
                        runCatchingNonCancel {
                            repository.postComment(
                                illustId = illustId,
                                comment = inputText,
                                parentCommentId = replyTarget?.id,
                            )
                        }.onSuccess {
                            inputText = ""
                            replyTarget = null
                            retryCount++
                        }.onFailure { e ->
                            sendError = e.message ?: "发送失败"
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
                val comments = result.getOrNull().orEmpty()
                if (comments.isEmpty()) {
                    EmptyPlaceholder(
                        message = "暂无评论",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = paddingValues,
                    ) {
                        items(comments, key = { it.id ?: it.hashCode() }) { comment ->
                            CommentItem(
                                comment = comment,
                                onUserClick = onUserClick,
                                onReplyClick = { replyTarget = comment },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

/**
 * 评论输入栏：位于页面底部，提供输入框、发送按钮与回复目标提示。
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
                    text = "回复 ${target.user?.name.orEmpty()}：${target.comment.orEmpty()}",
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onCancelReply) {
                    Text("取消")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                label = when {
                    isLoggedIn == false -> "登录后发表评论"
                    replyTarget != null -> "回复…"
                    else -> "发表评论…"
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
                    Text("发送中")
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                    )
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
                    text = "未知用户",
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = comment.comment.orEmpty(),
            style = MiuixTheme.textStyles.body2,
        )
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
                text = "回复",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onReplyClick),
            )
        }
    }
}
