package com.perol.pixez.shared.ui.screens

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 插画系列页：展示指定系列内的作品列表，支持流式分页与下拉刷新。
 */
@Composable
fun IllustSeriesScreen(
    seriesId: Int,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    var retryCount by rememberSaveable(seriesId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(seriesId) { mutableStateOf(false) }

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> {
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        val hIsNotAllow = settingsRepository.hIsNotAllow
        return rawIllusts.filter {
            it.id !in bannedIds &&
                it.user.id !in bannedUserIds &&
                (!banAIIllust || it.illustAIType != 2) &&
                (!hIsNotAllow || (it.xRestrict == 0 && it.tags.none { tag -> tag.name.contains("R-18", ignoreCase = true) || tag.name.contains("R18", ignoreCase = true) })) &&
                !banRepository.isBannedByTags(
                    banTags,
                    it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                )
        }
    }

    val strings = LocalStrings.current

    val state = produceState<Result<Triple<String, List<Illust>, String?>>?>(
        initialValue = null,
        seriesId,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val seriesResult = suspendRunCatchingNonCancel { repository.getIllustSeriesResponse(seriesId) }
        isManualRefreshing = false
        value = seriesResult.map { model ->
            val title = model.illustSeriesDetail?.title ?: ""
            val filtered = filterBanned(model.illusts.orEmpty())
            Triple(title, filtered, model.nextUrl)
        }
    }

    var seriesTitle by remember(seriesId) { mutableStateOf("") }
    var illusts by remember(seriesId) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(seriesId) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.value) {
        state.value?.onSuccess { (initialTitle, initialIllusts, initialNextUrl) ->
            seriesTitle = initialTitle
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
            suspendRunCatchingNonCancel { repository.getIllustSeriesResponse(seriesId, nextUrl = currentNextUrl) }
                .onSuccess { response ->
                    val filtered = filterBanned(response.illusts.orEmpty())
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
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = seriesTitle,
                scrollBehavior = scrollBehavior,
                color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                modifier = Modifier.topAppBarBlur(backdrop = backdrop, tintColor = colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = triggerManualRefresh) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = strings.refresh,
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
                        message = strings.seriesEmpty,
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

