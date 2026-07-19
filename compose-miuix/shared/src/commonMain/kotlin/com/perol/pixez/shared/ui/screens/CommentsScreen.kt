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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Comment
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品评论页：展示指定作品的用户评论列表。
 */
@Composable
fun CommentsScreen(
    illustId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: IllustRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

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

@Composable
private fun CommentItem(
    comment: Comment,
    onUserClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
    }
}
