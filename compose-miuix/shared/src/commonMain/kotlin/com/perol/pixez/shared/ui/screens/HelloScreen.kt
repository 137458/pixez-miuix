package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Refresh
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import top.yukonga.miuix.kmp.icon.extended.Settings
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.components.blurBackdropSource
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 首页/推荐页：顶部标题栏 + 真实推荐插画瀑布流。
 */
@Composable
fun HelloScreen(
    onIllustClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRecomUserClick: () -> Unit,
    repository: IllustRepository,
    accountRepository: AccountRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    reselectFlow: kotlinx.coroutines.flow.Flow<Unit>? = null,
) {
    val strings = LocalStrings.current
    // retryCount 作为 produceState 的 key，手动刷新或点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        isLoggedIn = suspendRunCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    // 当登录状态检测完成且为未登录时，触发一次性登录提示弹窗。
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
    }

    // 过滤被屏蔽作品与画师
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

    // 页面进入时加载数据；已登录用推荐接口，未登录用 walkthrough 匿名接口。
    // 默认读取内存缓存，仅当 isManualRefreshing == true 时触发强制网络刷新。
    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        repository,
        banRepository,
        settingsRepository,
        retryCount,
        isLoggedIn,
    ) {
        val force = isManualRefreshing
        val responseResult = when (isLoggedIn) {

            true -> suspendRunCatchingNonCancel { repository.getRecommendedResponse(forceRefresh = force) }
                .map { it.illusts to it.nextUrl }
            false -> suspendRunCatchingNonCancel { repository.getWalkthroughResponse(forceRefresh = force) }
                .map { it.illusts to (null as String?) }
            null -> null
        }
        isManualRefreshing = false
        value = responseResult?.map { (rawIllusts, nextUrl) ->
            filterBanned(rawIllusts) to nextUrl
        }
    }

    var illusts by remember { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(reselectFlow) {
        reselectFlow?.collect {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            if (gridState.firstVisibleItemIndex > 0) {
                gridState.animateScrollToItem(0)
            } else {
                retryCount++
                isManualRefreshing = true
            }
        }
    }

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
            val nextResult = suspendRunCatchingNonCancel {
                repository.getRecommendedResponse(nextUrl = currentNextUrl)
            }
            nextResult.onSuccess { response ->
                val filtered = filterBanned(response.illusts)
                illusts = illusts + filtered
                nextUrl = response.nextUrl
            }.onFailure { error ->
                loadMoreError = error
            }
            isLoadingMore = false
        }
    }

    // 未登录提示对话框：位于 Scaffold 外层，确保能覆盖整个页面。
    WindowDialog(
        title = strings.dialogNeedLogin,
        summary = strings.dialogNeedLoginSummary,
        show = showLoginDialog,
        onDismissRequest = { showLoginDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = strings.btnCancelLogin,
                onClick = { showLoginDialog = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    showLoginDialog = false
                    onLoginClick()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.btnGoLogin)
            }
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
                title = strings.tabRecommend,
                scrollBehavior = scrollBehavior,
                color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                modifier = Modifier.topAppBarBlur(backdrop = backdrop, tintColor = colorScheme.surface),
                actions = {
                    IconButton(
                        onClick = triggerManualRefresh,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = strings.refresh,
                        )
                    }
                    if (isLoggedIn == false) {
                        IconButton(
                            onClick = onLoginClick,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Contacts,
                                contentDescription = strings.goLogin,
                            )
                        }
                    }
                    if (isLoggedIn == true) {
                        IconButton(
                            onClick = onRecomUserClick,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.AddCircle,
                                contentDescription = strings.recomUserTitle,
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Settings,
                            contentDescription = strings.tabSettings,
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
                if (illusts.isEmpty()) {
                    EmptyPlaceholder(
                        message = strings.noData,
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
                            state = gridState,
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

