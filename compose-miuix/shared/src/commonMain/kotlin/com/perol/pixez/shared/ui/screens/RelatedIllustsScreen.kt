package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * 相关作品页：展示与指定作品相关的推荐插画列表。
 */
@Composable
fun RelatedIllustsScreen(
    illustId: Int,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable(illustId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(illustId) { mutableStateOf(false) }

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> {
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        return rawIllusts.filter {
            it.id !in bannedIds &&
                it.user.id !in bannedUserIds &&
                (!banAIIllust || it.illustAIType != 2) &&
                !banRepository.isBannedByTags(
                    banTags,
                    it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                )
        }
    }

    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        illustId,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getIllustRelatedResponse(illustId) }
        isManualRefreshing = false
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(illustId) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(illustId) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.value) {
        state.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            illusts = initialIllusts
            nextUrl = initialNextUrl
            isLoadingMore = false
            loadMoreError = null
        }
    }

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel { repository.getIllustRelatedResponse(illustId, nextUrl = currentNextUrl) }
                .onSuccess { response ->
                    val filtered = filterBanned(response.illusts)
                    illusts = illusts + filtered
                    nextUrl = response.nextUrl
                }
                .onFailure { error ->
                    loadMoreError = error
                }
            isLoadingMore = false
        }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "相关作品",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = triggerManualRefresh) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "刷新",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val result = state.value
        when {
            result == null -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
            result.isSuccess -> {
                if (illusts.isEmpty()) {
                    EmptyPlaceholder(
                        message = "暂无相关作品",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    PullToRefresh(
                        isRefreshing = isManualRefreshing,
                        onRefresh = triggerManualRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        IllustStaggeredGrid(
                            illusts = illusts,
                            onIllustClick = onIllustClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                top = paddingValues.calculateTopPadding() + 8.dp,
                                end = 8.dp,
                                bottom = 100.dp,
                            ),
                            hasMore = nextUrl != null,
                            isLoadingMore = isLoadingMore,
                            loadMoreError = loadMoreError,
                            onLoadMore = ::loadMore,
                        )
                    }
                }
            }
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { triggerManualRefresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

