package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.perol.pixez.shared.ui.components.PixivAsyncImage
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
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
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
import top.yukonga.miuix.kmp.icon.extended.Settings
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.components.blurBackdropSource
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 首页/推荐页：顶部标题栏 + 真实推荐插画瀑布流。
 */
@Composable
fun HelloScreen(
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit = {},
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
    val bottomBarPadding = LocalBottomBarContentPadding.current
    // retryCount 作为 produceState 的 key，手动刷新或点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        user = accountRepository.currentAccount()
        isLoggedIn = user != null
        accountRepository.loginEventFlow.collect {
            user = accountRepository.currentAccount()
            isLoggedIn = user != null
        }
    }

    // 当登录状态检测完成且为未登录时，触发一次性登录提示弹窗。
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
    }

    // 过滤被屏蔽作品与画师
    // 统一 UI 状态机（单向数据流 UDF），首帧优先取内存缓存，防止退出详情页返回时白屏闪烁与列表位置重置。
    // 高-4b：分页/竞态/加载态收敛到 IllustPagedListState（RankingScreen 版 generation 守卫），
    // 过滤链仍为高-3「原始/过滤」双列表本地重过滤。登录态由 fetch 闭包动态读取；
    // 首页 forceRefresh 依赖 forceNextFetch 标志：loadFirst 不透传 force 给 fetch，
    // 以同一协程内先写后读保持原 getRecommendedResponse(forceRefresh = force) 语义。
    var forceNextFetch by remember { mutableStateOf(false) }
    val listState = remember {
        IllustPagedListState(
            fetch = { nextUrl ->
                if (isLoggedIn == true) {
                    val response = repository.getRecommendedResponse(
                        nextUrl = nextUrl,
                        forceRefresh = forceNextFetch,
                    )
                    IllustPagedPage(response.illusts, response.nextUrl)
                } else {
                    // 未登录 walkthrough 匿名推荐无分页：nextUrl 恒为 null。
                    val response = repository.getWalkthroughResponse(forceRefresh = forceNextFetch)
                    IllustPagedPage(response.illusts, null)
                }
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion },
        ).also { state ->
            repository.activeRecommendedIllusts?.let { cached ->
                state.restore(cached, repository.activeRecommendedNextUrl)
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    // 加载或刷新推荐插画数据
    LaunchedEffect(isLoggedIn, retryCount, settingsRepository.filterChangeVersion) {
        // 登录态未检测完成（null）时不发起加载；登录分支在 fetch 闭包内动态读取。
        if (isLoggedIn == null) return@LaunchedEffect
        val force = isManualRefreshing
        // 高-3：过滤条件变化且已持有原始列表时，仅对原始列表本地重过滤（零网络），不重新请求。
        if (!force && listState.refilterIfChanged()) {
            repository.updateRecommendedFeed(listState.filteredIllusts.orEmpty(), listState.nextUrl)
            return@LaunchedEffect
        }
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val previous = listState.filteredIllusts
        forceNextFetch = force
        listState.loadFirst(force = force)
        forceNextFetch = false
        // 对齐原实现：请求结束（成功或失败）即复位手动刷新指示；协程被取消时到不了此处，
        // 指示保留并由下一轮 force 刷新复位（与原 isManualRefreshing 语义一致）。
        isManualRefreshing = false
        // 引用变化 = 本轮成功替换了列表（失败与竞态失配时状态机不写入，对齐 RankingScreen 版守卫）；
        // 成功才同步跨页缓存并复位滚动位置。
        val current = listState.filteredIllusts
        if (current != null && current !== previous) {
            repository.updateRecommendedFeed(current, listState.nextUrl)
            if (force && gridState.firstVisibleItemIndex > 0) {
                gridState.scrollToItem(0)
            }
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
        coroutineScope.launch {
            val previous = listState.filteredIllusts
            listState.loadMore()
            // 引用变化 = 翻页成功追加；失败/被拒绝/竞态失配时列表未变，不重复写跨页缓存。
            val current = listState.filteredIllusts
            if (current != null && current !== previous) {
                repository.updateRecommendedFeed(current, listState.nextUrl)
            }
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
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.tabRecommend,
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
                            val currentUser = user
                            val targetUserId = currentUser?.userId?.toLongOrNull()
                            if (currentUser != null && targetUserId != null) {
                                IconButton(
                                    onClick = { onUserClick(targetUserId) },
                                ) {
                                    PixivAsyncImage(
                                        model = currentUser.userImage,
                                        contentDescription = strings.myProfile,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape),
                                    )
                                }
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
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            val currentIllusts = listState.filteredIllusts
            when {
                currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(paddingValues))
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
                                    bottom = bottomBarPadding,
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

