package com.perol.pixez.shared.ui.state

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
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 推荐页的皮肤无关状态集合：分页状态机、登录态、恢复快照与刷新/翻页动作。
 *
 * MIUIX 与 COUI 两套页面共用本实现，皮肤只替换 UI 壳，不复制业务与竞态逻辑。
 */
internal class HelloFeedState(
    val listState: IllustPagedListState,
    val gridState: LazyStaggeredGridState,
    val isManualRefreshing: Boolean,
    val user: AccountPersist?,
    val isLoggedIn: Boolean?,
    val showLoginDialog: Boolean,
    val refresh: () -> Unit,
    val loadMore: () -> Unit,
    val dismissLoginDialog: () -> Unit,
    val goLogin: () -> Unit,
)

@Composable
internal fun rememberHelloFeedState(
    feedRestore: IllustFeedRestoreCache,
    reselectFlow: Flow<Unit>?,
    onLoginClick: () -> Unit,
): HelloFeedState {
    val repository = LocalIllustRepository.current
    val accountRepository = LocalAccountRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
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
        // 解密失败/被吊销后账号行仍保留但 token 为空，登录态必须看凭证而不是账号行。
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = accountRepository.currentAccount()
            isLoggedIn = user.isLoggedIn
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
    // 统一 UI 状态机（单向数据流 UDF），首帧优先取恢复快照，防止退出详情页返回时白屏闪烁与列表位置重置。
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
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        ).also { state ->
            feedRestore.snapshot?.let { snapshot ->
                val raw = snapshot.illustIds.mapNotNull(repository::getCachedIllust)
                if (snapshot.illustIds.isEmpty() || raw.isNotEmpty()) {
                    state.restore(raw, snapshot.nextUrl)
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()
    val hapticFeedback = LocalHapticFeedback.current

    // 加载或刷新推荐插画数据
    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 user/isLoggedIn）时作废旧账号列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(isLoggedIn, user, retryCount, filterChangeVersion) {
        // 登录态未检测完成（null）时不发起加载；登录分支在 fetch 闭包内动态读取。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) listState.reset()
        loadedForAccount = user
        val force = isManualRefreshing
        // 高-3：过滤条件变化且已持有原始列表时，仅对原始列表本地重过滤（零网络），不重新请求。
        if (!force && listState.refilterIfChanged()) {
            return@LaunchedEffect
        }
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        forceNextFetch = force
        val outcome = listState.loadFirst(force = force)
        forceNextFetch = false
        // 对齐原实现：请求结束（成功或失败）即复位手动刷新指示。
        isManualRefreshing = false
        // UI-8：写入成功由状态机回报，不再比较列表引用。
        if (outcome == PagedLoadOutcome.Written) {
            listState.rawIllusts?.let { feedRestore.update(it, listState.nextUrl) }
            if (force && gridState.firstVisibleItemIndex > 0) {
                gridState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(reselectFlow) {
        reselectFlow?.collect {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (gridState.firstVisibleItemIndex > 0) {
                gridState.animateScrollToItem(0)
            } else {
                isManualRefreshing = true
                retryCount++
            }
        }
    }

    val refresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    return HelloFeedState(
        listState = listState,
        gridState = gridState,
        isManualRefreshing = isManualRefreshing,
        user = user,
        isLoggedIn = isLoggedIn,
        showLoginDialog = showLoginDialog,
        refresh = refresh,
        loadMore = {
            coroutineScope.launch {
                // UI-8：翻页写入成功才写恢复快照；失败/被拒绝/竞态失配都不写。
                if (listState.loadMore() == PagedLoadOutcome.Written) {
                    listState.rawIllusts?.let { feedRestore.update(it, listState.nextUrl) }
                }
            }
        },
        dismissLoginDialog = {
            showLoginDialog = false
        },
        goLogin = {
            showLoginDialog = false
            onLoginClick()
        },
    )
}
