package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.window.WindowDialog

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * 最新/关注页：展示已登录用户关注画师的最新插画。
 *
 * 未登录时显示登录入口；登录后支持 all/public/private 三种可见性筛选。
 */
@Composable
fun NewScreen(
    onIllustClick: (Int) -> Unit,
    onLoginClick: () -> Unit,
    repository: IllustRepository,
    accountRepository: AccountRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    reselectFlow: kotlinx.coroutines.flow.Flow<Unit>? = null,
) {
    val strings = LocalStrings.current
    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settingsRepository.hasUnreadFeedBadge = false
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        isLoggedIn = suspendRunCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
    }

    // 可见性筛选：0=all, 1=public, 2=private。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val restrictOptions: List<Pair<String, String>> = listOf(
        strings.searchUgoiraAll to "all",
        strings.userPublicRestrict to "public",
        strings.userPrivateRestrict to "private",
    )
    val currentRestrict = restrictOptions[selectedRestrictIndex].second

    // retryCount 作为 produceState 的 key，点击重试或切换筛选时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

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

    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        repository,
        currentRestrict,
        retryCount,
        isLoggedIn,
        banRepository,
        settingsRepository,
    ) {
        val res = when (isLoggedIn) {
            true -> {
                val followResult = suspendRunCatchingNonCancel { repository.getFollowIllustsResponse(currentRestrict) }
                followResult.map { filterBanned(it.illusts) to it.nextUrl }
            }
            false -> Result.success(emptyList<Illust>() to null)
            null -> null
        }
        isManualRefreshing = false
        value = res
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
            suspendRunCatchingNonCancel { repository.getFollowIllustsResponse(restrict = currentRestrict, nextUrl = currentNextUrl) }
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

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.tabNew,
                scrollBehavior = scrollBehavior,
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
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            Text(strings.btnGoLogin)
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            when (isLoggedIn) {
                false -> {
                    EmptyPlaceholder(
                        message = strings.loginNewNeedLogin,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())

                true -> {
                    // 登录后显示筛选器与列表。
                    RestrictSelector(
                        options = restrictOptions.map { it.first },
                        selectedIndex = selectedRestrictIndex,
                        onSelect = { selectedRestrictIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    val result = state.value
                    when {
                        result == null -> LoadingPlaceholder(modifier = Modifier.weight(1f))
                        result.isSuccess -> {
                            if (illusts.isEmpty()) {
                                EmptyPlaceholder(
                                    message = strings.loginNewEmpty,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                PullToRefresh(
                                    isRefreshing = isManualRefreshing,
                                    onRefresh = triggerManualRefresh,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    IllustStaggeredGrid(
                                        illusts = illusts,
                                        state = gridState,
                                        onIllustClick = onIllustClick,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            Button(
                onClick = { onSelect(index) },
                colors = if (index == selectedIndex) {
                    ButtonDefaults.buttonColorsPrimary()
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(label)
            }
        }
    }
}

