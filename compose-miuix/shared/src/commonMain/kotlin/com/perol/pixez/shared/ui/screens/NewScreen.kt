package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.LiquidFilterBar
import top.yukonga.miuix.kmp.blur.Backdrop
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
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
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import com.perol.pixez.shared.ui.components.blurBackdropSource

/**
 * 最新/关注页：展示已登录用户关注画师的最新插画。
 *
 * 未登录时显示登录入口；登录后支持 all/public/private 三种可见性筛选。
 */
@Composable
fun NewScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onLoginClick: () -> Unit,
    reselectFlow: kotlinx.coroutines.flow.Flow<Unit>? = null,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalIllustRepository.current
    val accountRepository = LocalAccountRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settingsRepository.hasUnreadFeedBadge = false
        currentAccount = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = currentAccount != null
        accountRepository.loginEventFlow.collect {
            currentAccount = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = currentAccount != null
        }
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

    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链为高-3「原始/过滤」双列表本地重过滤。状态机以可见性筛选为 remember key：
    // all/public/private 切换时整体重建（等价原实现的清空重拉），retryCount 重试复用同一实例。
    val listState = remember(currentRestrict) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getFollowIllustsResponse(restrict = currentRestrict, nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(isLoggedIn, currentRestrict, retryCount, filterChangeVersion) {
        // 未登录（false）时 UI 层展示登录占位，状态机保持空；登录态未检测完成（null）不发起加载。
        if (isLoggedIn != true) return@LaunchedEffect
        val force = isManualRefreshing
        // 高-3：过滤条件变化且已持有原始列表时，仅对原始列表本地重过滤（零网络），不重新请求。
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val previous = listState.filteredIllusts
        listState.loadFirst(force = force)
        isManualRefreshing = false
        // 引用变化 = 本轮成功替换了列表（失败与竞态失配时状态机不写入）；force 刷新成功后滚回顶部。
        val current = listState.filteredIllusts
        if (current != null && current !== previous && force && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(reselectFlow) {
        reselectFlow?.collect {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            if (gridState.firstVisibleItemIndex > 0) {
                gridState.animateScrollToItem(0)
            } else {
                isManualRefreshing = true
                retryCount++
            }
        }
    }

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            listState.loadMore()
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
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = strings.tabNew,
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) Color.Transparent else colorScheme.surface,
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
                            if (isLoggedIn == true) {
                                val account = currentAccount
                                val targetUserId = account?.userId?.toLongOrNull()
                                if (account != null && targetUserId != null) {
                                    IconButton(
                                        onClick = { onUserClick(targetUserId) },
                                    ) {
                                        PixivAsyncImage(
                                            model = account.userImage,
                                            contentDescription = strings.myProfile,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape),
                                        )
                                    }
                                }
                            }
                        },
                    )
                    if (isLoggedIn == true) {
                        LiquidFilterBar(
                            items = restrictOptions,
                            selectedItem = restrictOptions[selectedRestrictIndex],
                            onItemSelected = { selectedRestrictIndex = restrictOptions.indexOf(it) },
                            labelProvider = { it.first },
                            backdrop = backdrop,
                            isScrollable = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            when (isLoggedIn) {
                false -> {
                    EmptyPlaceholder(
                        message = strings.loginNewNeedLogin,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                }

                null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )

                true -> {
                    val currentIllusts = listState.filteredIllusts
                    when {
                        currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                        currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
                            error = listState.initialError,
                            onRetry = { triggerManualRefresh() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                        currentIllusts != null -> {
                            if (currentIllusts.isEmpty()) {
                                EmptyPlaceholder(
                                    message = strings.loginNewEmpty,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues),
                                )
                            } else {
                                PullToRefresh(
                                    isRefreshing = isManualRefreshing,
                                    onRefresh = triggerManualRefresh,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                    topAppBarScrollBehavior = scrollBehavior,
                                ) {
                                    IllustStaggeredGrid(
                                        illusts = currentIllusts,
                                        state = gridState,
                                        onIllustClick = onIllustClick,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                                        contentPadding = PaddingValues(
                                            start = 8.dp,
                                            top = paddingValues.calculateTopPadding() + 8.dp,
                                            end = 8.dp,
                                            bottom = LocalBottomBarContentPadding.current,
                                        ),
                                        hasMore = listState.nextUrl != null,
                                        isLoadingMore = listState.isLoadingMore,
                                        loadMoreError = listState.loadMoreError,
                                        onLoadMore = ::loadMore,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


