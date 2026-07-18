package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 最新作品页：M4 先复用推荐接口填充内容，M5 接入关注系统后替换为 /v2/illust/follow。
 */
@Composable
fun NewScreen(
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        repository,
        retryCount,
    ) {
        value = runCatching { repository.getNew() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = "最新")
        },
    ) { paddingValues ->
        val result = state.value
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                result == null -> LoadingPlaceholder(modifier = Modifier.weight(1f))
                result.isSuccess -> {
                    val illusts = result.getOrNull().orEmpty()
                    if (illusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = "暂无最新内容",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        IllustStaggeredGrid(
                            illusts = illusts,
                            onIllustClick = onIllustClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(paddingValues),
                        )
                    }
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { retryCount++ },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
