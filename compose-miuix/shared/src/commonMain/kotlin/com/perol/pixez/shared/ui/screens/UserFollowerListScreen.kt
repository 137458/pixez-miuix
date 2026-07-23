package com.perol.pixez.shared.ui.screens

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
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 用户好P友列表页。
 */
@Composable
fun UserFollowerListScreen(
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
        value = runCatchingNonCancel { repository.getUserFollowers(userId) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "好P友",
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
        when (val result = state.value) {
            null -> LoadingPlaceholder(modifier = Modifier.padding(paddingValues))
            else -> UserFollowerListBody(
                result = result,
                onUserClick = onUserClick,
                onRetry = { retryCount++ },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun UserFollowerListBody(
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
                    message = "暂无好P友",
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
