package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.buildUserCopyInfo
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户详情页：头部信息 + 「作品 / 收藏」Tab 切换。
 */
@Composable
fun UserDetailScreen(
    userId: Int,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    onFollowListClick: (Int) -> Unit,
    onFollowerListClick: (Int) -> Unit,
    repository: UserRepository,
    bookmarkRepository: BookmarkRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    accountRepository: AccountRepository? = null,
    initialTab: Int = 0,
) {
    // 重试计数，作为 produceState 的 key 触发用户资料重新加载。
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val strings = LocalStrings.current

    val currentAccountState = produceState<AccountPersist?>(initialValue = null, accountRepository) {
        accountRepository?.let { repo ->
            value = suspendRunCatchingNonCancel { repo.currentAccount() }.getOrNull()
            repo.loginEventFlow.collect {
                value = suspendRunCatchingNonCancel { repo.currentAccount() }.getOrNull()
            }
        }
    }
    val isCurrentUser = remember(currentAccountState.value, userId) {
        val currentId = currentAccountState.value?.userId?.toIntOrNull()
        currentId != null && currentId == userId
    }

    // 用户资料加载失败时整页进入错误态；成功后再展示 Tab 内容。
    val detailState = produceState<Result<UserDetail>?>(
        initialValue = null,
        userId,
        repository,
        retryCount,
    ) {
        // 当前处于 produceState 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        value = suspendRunCatchingNonCancel { repository.getUserDetail(userId) }
    }

    val result = detailState.value
    val userDetail = result?.getOrNull()
    var isFollowed by rememberSaveable(userDetail) {
        mutableStateOf(userDetail?.user?.isFollowed ?: false)
    }
    // 进行中标志使用 remember 而非 rememberSaveable：进程恢复后协程不会恢复，避免关注按钮被永久禁用。
    var isFollowLoading by remember { mutableStateOf(false) }
    var followError by rememberSaveable { mutableStateOf<String?>(null) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var toastType by rememberSaveable { mutableStateOf(ToastType.Normal) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    LaunchedEffect(result) {
        if (result != null) {
            isManualRefreshing = false
        }
    }

    // 「更多操作」下拉菜单项：复制信息 / 复制链接 / 分享链接。
    // remember 保留在主入口作用域，菜单项通过参数下发给顶栏子组件。
    val moreEntry = remember(userDetail) {
        userDetail?.let { detail ->
            buildUserMoreDropdownEntry(
                strings = strings,
                detail = detail,
                clipboard = clipboard,
                share = share,
                onToast = { message, type ->
                    toastMessage = message
                    toastType = type
                },
            )
        }
    }

    // 关注 / 取关操作，由内容区回调触发。
    val onFollowToggle: (UserDetail) -> Unit = { detail ->
        if (!isFollowLoading) {
            coroutineScope.launch {
                try {
                    isFollowLoading = true
                    followError = null
                    suspendRunCatchingNonCancel {
                        if (isFollowed) {
                            bookmarkRepository.unfollowUser(detail.user.id)
                        } else {
                            bookmarkRepository.followUser(detail.user.id)
                        }
                    }.onSuccess {
                        isFollowed = !isFollowed
                    }.onFailure { e ->
                        val err = e.message ?: "${strings.follow}${strings.loadFailed}"
                        followError = err
                        toastMessage = err
                        toastType = ToastType.Error
                    }
                } finally {
                    isFollowLoading = false
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            UserDetailTopAppBar(
                title = userDetail?.user?.name ?: "",
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
                moreEntry = moreEntry,
                onBack = onBack,
                onRefresh = triggerManualRefresh,
                strings = strings,
            )
        },
    ) { paddingValues ->
        UserDetailContent(
            userId = userId,
            result = result,
            userDetail = userDetail,
            isCurrentUser = isCurrentUser,
            isFollowed = isFollowed,
            isFollowLoading = isFollowLoading,
            isManualRefreshing = isManualRefreshing,
            initialTab = initialTab,
            paddingValues = paddingValues,
            scrollBehavior = scrollBehavior,
            backdrop = backdrop,
            bookmarkRepository = bookmarkRepository,
            repository = repository,
            banRepository = banRepository,
            settingsRepository = settingsRepository,
            onFollowToggle = onFollowToggle,
            onFollowListClick = onFollowListClick,
            onFollowerListClick = onFollowerListClick,
            onIllustClick = onIllustClick,
            onRefresh = triggerManualRefresh,
            toastMessage = toastMessage,
            toastType = toastType,
            onToastDismiss = { toastMessage = null },
        )
    }
}

/**
 * 构建用户页「更多操作」下拉菜单项：复制信息 / 复制链接 / 分享链接。
 *
 * @param onToast 操作结果提示回调（消息, 类型）
 */
private fun buildUserMoreDropdownEntry(
    strings: AppStrings,
    detail: UserDetail,
    clipboard: IllustClipboard,
    share: IllustShare,
    onToast: (String, ToastType) -> Unit,
): DropdownEntry = DropdownEntry(
    items = listOf(
        DropdownItem(
            text = strings.menuCopyInfo,
            onClick = {
                val text = buildUserCopyInfo(detail)
                runCatching { clipboard.copy(text) }.fold(
                    onSuccess = {
                        onToast(strings.copiedToClipboard, ToastType.Success)
                    },
                    onFailure = { e ->
                        onToast("${strings.copy}${strings.loadFailed}: ${e.message}", ToastType.Error)
                    },
                )
            }
        ),
        DropdownItem(
            text = strings.menuCopyLink,
            onClick = {
                val link = AppConstants.Urls.pixivUserUrl(detail.user.id)
                runCatching { clipboard.copy(link) }.fold(
                    onSuccess = {
                        onToast(strings.copiedToClipboard, ToastType.Success)
                    },
                    onFailure = { e ->
                        onToast("${strings.copy}${strings.loadFailed}: ${e.message}", ToastType.Error)
                    },
                )
            }
        ),
        DropdownItem(
            text = strings.menuShareLink,
            onClick = {
                val link = AppConstants.Urls.pixivUserUrl(detail.user.id)
                runCatching { share.share(link, detail.user.name) }.fold(
                    onSuccess = {
                        onToast(strings.share, ToastType.Success)
                    },
                    onFailure = { e ->
                        onToast("${strings.share}${strings.loadFailed}: ${e.message}", ToastType.Error)
                    },
                )
            }
        ),
    )
)

/**
 * 用户详情页顶栏：返回、手动刷新与「更多操作」下拉菜单。
 */
@Composable
private fun UserDetailTopAppBar(
    title: String,
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior,
    moreEntry: DropdownEntry?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    strings: AppStrings,
) {
    val colorScheme = MiuixTheme.colorScheme
    BlurredBar(
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
    ) {
        TopAppBar(
            title = title,
            scrollBehavior = scrollBehavior,
            color = if (backdrop != null) Color.Transparent else colorScheme.surface,
            navigationIcon = {
                TooltipBox(text = strings.back) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = strings.refresh,
                    )
                }
                if (moreEntry != null) {
                    TooltipBox(text = strings.menuMoreActions) {
                        OverlayIconDropdownMenu(
                            entry = moreEntry,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.More,
                                contentDescription = strings.menuMoreActions,
                            )
                        }
                    }
                }
            },
        )
    }
}

/**
 * 用户详情页主体内容区：处理加载 / 错误 / 成功（Tab 内容）三态，并承载 Toast 提示。
 */
@Composable
private fun UserDetailContent(
    userId: Int,
    result: Result<UserDetail>?,
    userDetail: UserDetail?,
    isCurrentUser: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    isManualRefreshing: Boolean,
    initialTab: Int,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    bookmarkRepository: BookmarkRepository,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    onFollowToggle: (UserDetail) -> Unit,
    onFollowListClick: (Int) -> Unit,
    onFollowerListClick: (Int) -> Unit,
    onIllustClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    toastMessage: String?,
    toastType: ToastType,
    onToastDismiss: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .blurBackdropSource(backdrop),
    ) {
        when {
            result == null -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
            result.isSuccess && userDetail != null -> {
                UserDetailTabContent(
                    userId = userId,
                    userDetail = userDetail,
                    isCurrentUser = isCurrentUser,
                    isFollowed = isFollowed,
                    isFollowLoading = isFollowLoading,
                    onFollowClick = { onFollowToggle(userDetail) },
                    onFollowListClick = { onFollowListClick(userDetail.user.id) },
                    onFollowerListClick = { onFollowerListClick(userDetail.user.id) },
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                    initialTab = initialTab,
                    topPadding = paddingValues.calculateTopPadding(),
                    scrollBehavior = scrollBehavior,
                    isRefreshing = isManualRefreshing,
                    onRefresh = onRefresh,
                )
            }
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
        ToastMessage(
            message = toastMessage,
            type = toastType,
            onDismiss = onToastDismiss,
        )
    }
}

/**
 * 用户详情页 Tab 内容：作品 / 收藏。
 */
@Composable
private fun UserDetailTabContent(
    userId: Int,
    userDetail: UserDetail,
    isCurrentUser: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onFollowListClick: () -> Unit,
    onFollowerListClick: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    initialTab: Int = 0,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val strings = LocalStrings.current
    var selectedTabIndex by rememberSaveable(userId, initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf(strings.userWorkTab, strings.userBookmarkTab)

    val worksGridState = rememberLazyStaggeredGridState()
    val bookmarksGridState = rememberLazyStaggeredGridState()

    var worksLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedTabIndex == 0) }
    var bookmarksLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedTabIndex == 1) }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) worksLoadedOnce = true
        if (selectedTabIndex == 1) bookmarksLoadedOnce = true
    }

    val isCurrentListAtTop by remember(selectedTabIndex) {
        derivedStateOf {
            if (selectedTabIndex == 0) {
                worksGridState.firstVisibleItemIndex == 0 && worksGridState.firstVisibleItemScrollOffset == 0
            } else {
                bookmarksGridState.firstVisibleItemIndex == 0 && bookmarksGridState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    LaunchedEffect(selectedTabIndex, isCurrentListAtTop) {
        if (isCurrentListAtTop) {
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }

    val headerContent: @Composable () -> Unit = {
        UserDetailTabHeader(
            userDetail = userDetail,
            isCurrentUser = isCurrentUser,
            isFollowed = isFollowed,
            isFollowLoading = isFollowLoading,
            onFollowClick = onFollowClick,
            onFollowListClick = onFollowListClick,
            onFollowerListClick = onFollowerListClick,
            onIllustClick = onIllustClick,
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 作品 Tab（保持组合生命周期，切换不销毁、不重新加载）
        val isWorksActive = selectedTabIndex == 0
        if (isWorksActive || worksLoadedOnce) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (isWorksActive) 1f else 0f
                        translationX = if (isWorksActive) 0f else 100000f
                    }
                    .then(if (!isWorksActive) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                UserWorksTab(
                    userId = userId,
                    header = headerContent,
                    gridState = worksGridState,
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                    topPadding = topPadding,
                    scrollBehavior = scrollBehavior,
                    isRefreshing = isRefreshing && isWorksActive,
                    onRefresh = onRefresh,
                )
            }
        }

        // 收藏 Tab（保持组合生命周期，切换不销毁、不重新加载）
        val isBookmarksActive = selectedTabIndex == 1
        if (isBookmarksActive || bookmarksLoadedOnce) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (isBookmarksActive) 1f else 0f
                        translationX = if (isBookmarksActive) 0f else 100000f
                    }
                    .then(if (!isBookmarksActive) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                UserBookmarksTab(
                    userId = userId,
                    header = headerContent,
                    gridState = bookmarksGridState,
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                    topPadding = topPadding,
                    scrollBehavior = scrollBehavior,
                    isRefreshing = isRefreshing && isBookmarksActive,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

/**
 * 用户详情页头部组合：资料区 [UserProfileHeader] 与「作品 / 收藏」Tab 行。
 */
@Composable
private fun UserDetailTabHeader(
    userDetail: UserDetail,
    isCurrentUser: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onFollowListClick: () -> Unit,
    onFollowerListClick: () -> Unit,
    onIllustClick: (Int) -> Unit,
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        UserProfileHeader(
            userDetail = userDetail,
            isCurrentUser = isCurrentUser,
            isFollowed = isFollowed,
            isLoading = isFollowLoading,
            onFollowClick = onFollowClick,
            onFollowListClick = onFollowListClick,
            onFollowerListClick = onFollowerListClick,
            onIllustClick = onIllustClick,
        )
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}