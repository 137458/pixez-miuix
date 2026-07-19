package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户关注列表页。
 */
@Composable
fun UserFollowListScreen(
    userId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: UserRepository,
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    val state = produceState<Result<List<UserPreview>>?>(
        initialValue = null,
        userId,
        repository,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getUserFollowing(userId) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "关注",
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
        when (val result = state.value) {
            null -> LoadingPlaceholder(modifier = Modifier.padding(paddingValues))
            else -> UserFollowListBody(
                result = result,
                onUserClick = onUserClick,
                onRetry = { retryCount++ },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun UserFollowListBody(
    result: Result<List<UserPreview>>,
    onUserClick: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        result.isSuccess -> {
            val previews = result.getOrNull().orEmpty()
            if (previews.isEmpty()) {
                EmptyPlaceholder(
                    message = "暂无关注用户",
                    modifier = modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
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
                }
            }
        }
        else -> ErrorPlaceholder(
            error = result.exceptionOrNull(),
            onRetry = onRetry,
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun UserPreviewItem(
    preview: UserPreview,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixivAsyncImage(
            model = preview.user.profileImageUrls.medium,
            contentDescription = preview.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = preview.user.name,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = "@${preview.user.account}",
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        preview.illusts.take(3).forEach { illust ->
            PixivAsyncImage(
                model = illust.imageUrls.squareMedium,
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}
