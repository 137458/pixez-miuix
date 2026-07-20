package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 插画系列页：展示指定系列内的作品列表。
 */
@Composable
fun IllustSeriesScreen(
    seriesId: Int,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
    banRepository: BanRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<Pair<String, List<Illust>>>?>(
        initialValue = null,
        seriesId,
        repository,
        retryCount,
        banRepository,
    ) {
        val seriesResult = runCatchingNonCancel { repository.getIllustSeries(seriesId) }
        val bannedIds = runCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        value = seriesResult.map { (title, illusts) ->
            title to illusts.filter { it.id !in bannedIds }
        }
    }

    val result = state.value
    val title = result?.getOrNull()?.first ?: "系列"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = title,
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
        when {
            result == null -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
            result.isSuccess -> {
                val illusts = result.getOrNull()?.second.orEmpty()
                if (illusts.isEmpty()) {
                    EmptyPlaceholder(
                        message = "系列内暂无作品",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    IllustStaggeredGrid(
                        illusts = illusts,
                        onIllustClick = onIllustClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
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
