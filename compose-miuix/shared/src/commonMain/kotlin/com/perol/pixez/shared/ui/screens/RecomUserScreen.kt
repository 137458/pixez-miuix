package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

/**
 * 推荐用户列表页。
 */
@Composable
fun RecomUserScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    repository: UserRepository,
) {
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<List<UserPreview>>?>(
        initialValue = null,
        repository,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getRecommendedUsers() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "为你推荐",
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
            else -> RecomUserListBody(
                result = result,
                onUserClick = onUserClick,
                onRetry = { retryCount++ },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun RecomUserListBody(
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
                    message = "暂无推荐用户",
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
