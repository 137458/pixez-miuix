package com.perol.pixez.shared.ui.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.data.model.appendDistinct as appendDistinctSeries
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalWatchlistRepository
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** 动态页可见性筛选，code 与 Pixiv API 参数保持一致。 */
internal enum class NewFeedRestrict(val code: String) {
    ALL("all"),
    PUBLIC("public"),
    PRIVATE("private"),
    ;

    fun label(strings: AppStrings): String = when (this) {
        ALL -> strings.searchUgoiraAll
        PUBLIC -> strings.userPublicRestrict
        PRIVATE -> strings.userPrivateRestrict
    }
}

/**
 * 「动态 / 追更」页的皮肤无关状态集合：登录态、可见性筛选、动态分页状态机、
 * 追更分页状态机与取消追更动作。
 *
 * MIUIX 与 COUI 两套页面共用本实现，皮肤只替换 UI 壳，不复制业务与竞态逻辑。
 */
internal class NewFeedState(
    val selectedMainTab: Int,
    val isLoggedIn: Boolean?,
    val currentAccount: AccountPersist?,
    val selectedRestrict: NewFeedRestrict,
    val listState: IllustPagedListState,
    val gridState: LazyStaggeredGridState,
    val isManualRefreshing: Boolean,
    val showLoginDialog: Boolean,
    val watchlistState: PagedListState<MangaSeries>,
    val watchlistListState: LazyListState,
    val watchlistRefreshing: Boolean,
    val seriesToRemove: MangaSeries?,
    val isRemovingWatchlist: Boolean,
    val onMainTabSelected: (Int) -> Unit,
    val onRestrictSelected: (NewFeedRestrict) -> Unit,
    val refresh: () -> Unit,
    val loadMore: () -> Unit,
    val loadMoreWatchlist: () -> Unit,
    val dismissLoginDialog: () -> Unit,
    val goLogin: () -> Unit,
    val requestRemoveSeries: (MangaSeries) -> Unit,
    val dismissRemoveSeries: () -> Unit,
    val confirmRemoveSeries: (onError: (Throwable) -> Unit) -> Unit,
)

@Composable
internal fun rememberNewFeedState(
    reselectFlow: Flow<Unit>?,
    onLoginClick: () -> Unit,
): NewFeedState {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalIllustRepository.current
    val accountRepository = LocalAccountRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val watchlistRepository = LocalWatchlistRepository.current
    val hapticFeedback = LocalHapticFeedback.current
    // 顶部主页签：0 = 动态，1 = 追更。
    var selectedMainTab by rememberSaveable { mutableIntStateOf(0) }
    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settingsRepository.hasUnreadFeedBadge = false
        currentAccount = suspendRunCatchingNonCancel { accountRepository.currentAccount() }
            .onFailure { Napier.w(tag = "CurrentAccount", throwable = it) { "加载当前账号失败(NewScreen)" } }
            .getOrNull()
        isLoggedIn = currentAccount.isLoggedIn
        accountRepository.loginEventFlow.collect {
            currentAccount = suspendRunCatchingNonCancel { accountRepository.currentAccount() }
                .onFailure { Napier.w(tag = "CurrentAccount", throwable = it) { "登录事件后加载当前账号失败(NewScreen)" } }
                .getOrNull()
            isLoggedIn = currentAccount.isLoggedIn
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
    }

    // 可见性筛选：all/public/private 切换时整体重建列表状态机（等价原实现的清空重拉）。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedRestrict = NewFeedRestrict.entries[
        selectedRestrictIndex.coerceIn(0, NewFeedRestrict.entries.lastIndex)
    ]

    // retryCount 作为加载副作用 key，点击重试或切换筛选时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链为高-3「原始/过滤」双列表本地重过滤。
    val listState = remember(selectedRestrict) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getFollowIllustsResponse(
                    restrict = selectedRestrict.code,
                    nextUrl = nextUrl,
                )
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value

    // ---------- 追更页签：漫画系列追更列表 ----------
    var watchlistRefreshing by rememberSaveable { mutableStateOf(false) }
    var watchlistRetryCount by rememberSaveable { mutableIntStateOf(0) }
    // 待取消追更的系列，非 null 时显示确认框。
    var seriesToRemove by remember { mutableStateOf<MangaSeries?>(null) }
    // 取消追更进行中，禁用确认按钮防重复提交。
    var isRemovingWatchlist by remember { mutableStateOf(false) }

    // 追更列表分页状态机（generation 竞态守卫），按系列 ID 幂等合并去重。
    val watchlistState = remember {
        PagedListState(
            fetch = { nextUrl ->
                val response = watchlistRepository.getMangaWatchlist(nextUrl = nextUrl)
                PagedListPage(response.series, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinctSeries(new) },
        )
    }
    val watchlistListState = rememberLazyListState()

    // 进入追更页签 / 重试 / 手动刷新时加载首屏；已持有数据且非强制时状态机内部早退。
    LaunchedEffect(isLoggedIn, selectedMainTab, watchlistRetryCount) {
        if (isLoggedIn != true || selectedMainTab != 1) return@LaunchedEffect
        val force = watchlistRefreshing
        watchlistState.loadFirst(force = force)
        watchlistRefreshing = false
    }

    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 currentAccount/isLoggedIn）时作废旧账号列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(isLoggedIn, currentAccount, selectedRestrict, retryCount, filterChangeVersion) {
        // 登录态未检测完成（null）时不发起加载；登出/切号先作废旧账号列表（动态与追更一并作废）。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != currentAccount
        if (accountSwitched) listState.reset()
        if (accountSwitched) watchlistState.reset()
        loadedForAccount = currentAccount
        // 未登录（false）时 UI 层展示登录占位，状态机保持空。
        if (isLoggedIn != true) return@LaunchedEffect
        val force = isManualRefreshing
        // 高-3：过滤条件变化且已持有原始列表时，仅对原始列表本地重过滤（零网络），不重新请求。
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val outcome = listState.loadFirst(force = force)
        isManualRefreshing = false
        // UI-8：写入成功由状态机回报，不再比较列表引用；仅强制刷新成功才回顶。
        if (outcome == PagedLoadOutcome.Written && force && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(reselectFlow) {
        reselectFlow?.collect {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (selectedMainTab == 1) {
                // 追更页签：可回滚则回顶，否则刷新追更列表。
                if (watchlistListState.firstVisibleItemIndex > 0) {
                    watchlistListState.animateScrollToItem(0)
                } else {
                    watchlistRefreshing = true
                    watchlistRetryCount++
                }
            } else if (gridState.firstVisibleItemIndex > 0) {
                gridState.animateScrollToItem(0)
            } else {
                isManualRefreshing = true
                retryCount++
            }
        }
    }

    val refresh: () -> Unit = {
        if (selectedMainTab == 1) {
            watchlistRefreshing = true
            watchlistRetryCount++
        } else {
            isManualRefreshing = true
            retryCount++
        }
    }

    return NewFeedState(
        selectedMainTab = selectedMainTab,
        isLoggedIn = isLoggedIn,
        currentAccount = currentAccount,
        selectedRestrict = selectedRestrict,
        listState = listState,
        gridState = gridState,
        isManualRefreshing = isManualRefreshing,
        showLoginDialog = showLoginDialog,
        watchlistState = watchlistState,
        watchlistListState = watchlistListState,
        watchlistRefreshing = watchlistRefreshing,
        seriesToRemove = seriesToRemove,
        isRemovingWatchlist = isRemovingWatchlist,
        onMainTabSelected = { selectedMainTab = it },
        onRestrictSelected = { selectedRestrictIndex = it.ordinal },
        refresh = refresh,
        loadMore = {
            coroutineScope.launch {
                listState.loadMore()
            }
        },
        loadMoreWatchlist = {
            coroutineScope.launch {
                watchlistState.loadMore()
            }
        },
        dismissLoginDialog = {
            showLoginDialog = false
        },
        goLogin = {
            showLoginDialog = false
            onLoginClick()
        },
        requestRemoveSeries = { seriesToRemove = it },
        dismissRemoveSeries = {
            if (!isRemovingWatchlist) seriesToRemove = null
        },
        confirmRemoveSeries = { onError ->
            val series = seriesToRemove
            if (series != null) {
                isRemovingWatchlist = true
                coroutineScope.launch {
                    try {
                        suspendRunCatchingNonCancel {
                            watchlistRepository.deleteMangaWatchlist(series.id)
                        }
                            .onSuccess {
                                // 删除成功后强制刷新追更列表。
                                watchlistRefreshing = true
                                watchlistRetryCount++
                            }
                            .onFailure(onError)
                    } finally {
                        // 协程取消或异常时也必须重置状态，避免确认按钮永久禁用。
                        isRemovingWatchlist = false
                        seriesToRemove = null
                    }
                }
            }
        },
    )
}
