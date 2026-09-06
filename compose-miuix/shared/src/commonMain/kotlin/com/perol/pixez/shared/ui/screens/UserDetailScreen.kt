package com.perol.pixez.shared.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.buildUserCopyInfo
import com.perol.pixez.shared.ui.AppConstants
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.blur.layerBackdrop

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
    initialTab: Int = 0,
) {
    // 重试计数，作为 produceState 的 key 触发用户资料重新加载。
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val strings = LocalStrings.current

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
    var isFollowLoading by rememberSaveable { mutableStateOf(false) }
    var followError by rememberSaveable { mutableStateOf<String?>(null) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var toastType by rememberSaveable { mutableStateOf(ToastType.Normal) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    LaunchedEffect(result) {
        if (result != null) {
            isManualRefreshing = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = userDetail?.user?.name ?: "",
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
                        IconButton(onClick = triggerManualRefresh) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = strings.refresh,
                            )
                        }
                        if (userDetail != null) {
                            val currentDetail = userDetail
                            val entry = remember(currentDetail) {
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(
                                            text = strings.menuCopyInfo,
                                            onClick = {
                                                val text = buildUserCopyInfo(currentDetail)
                                                runCatching { clipboard.copy(text) }.fold(
                                                    onSuccess = {
                                                        toastMessage = strings.copiedToClipboard
                                                        toastType = ToastType.Success
                                                    },
                                                    onFailure = { e ->
                                                        toastMessage = "${strings.copy}${strings.loadFailed}: ${e.message}"
                                                        toastType = ToastType.Error
                                                    },
                                                )
                                            }
                                        ),
                                        DropdownItem(
                                            text = strings.menuCopyLink,
                                            onClick = {
                                                val link = AppConstants.Urls.pixivUserUrl(currentDetail.user.id)
                                                runCatching { clipboard.copy(link) }.fold(
                                                    onSuccess = {
                                                        toastMessage = strings.copiedToClipboard
                                                        toastType = ToastType.Success
                                                    },
                                                    onFailure = { e ->
                                                        toastMessage = "${strings.copy}${strings.loadFailed}: ${e.message}"
                                                        toastType = ToastType.Error
                                                    },
                                                )
                                            }
                                        ),
                                        DropdownItem(
                                            text = strings.menuShareLink,
                                            onClick = {
                                                val link = AppConstants.Urls.pixivUserUrl(currentDetail.user.id)
                                                runCatching { share.share(link, currentDetail.user.name) }.fold(
                                                    onSuccess = {
                                                        toastMessage = strings.share
                                                        toastType = ToastType.Success
                                                    },
                                                    onFailure = { e ->
                                                        toastMessage = "${strings.share}${strings.loadFailed}: ${e.message}"
                                                        toastType = ToastType.Error
                                                    },
                                                )
                                            }
                                        ),
                                    )
                                )
                            }
                            TooltipBox(text = strings.menuMoreActions) {
                                OverlayIconDropdownMenu(
                                    entry = entry,
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
        },
    ) { paddingValues ->
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
                        isFollowed = isFollowed,
                        isFollowLoading = isFollowLoading,
                        onFollowClick = {
                            if (!isFollowLoading) {
                                coroutineScope.launch {
                                    try {
                                        isFollowLoading = true
                                        followError = null
                                        suspendRunCatchingNonCancel {
                                            if (isFollowed) {
                                                bookmarkRepository.unfollowUser(userDetail.user.id)
                                            } else {
                                                bookmarkRepository.followUser(userDetail.user.id)
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
                        },
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
                        onRefresh = triggerManualRefresh,
                    )
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = triggerManualRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            ToastMessage(
                message = toastMessage,
                type = toastType,
                onDismiss = { toastMessage = null },
            )
        }
    }
}

/**
 * 用户详情页 Tab 内容：作品 / 收藏。
 */
@Composable
private fun UserDetailTabContent(
    userId: Int,
    userDetail: UserDetail,
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
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            UserProfileHeader(
                userDetail = userDetail,
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
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
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
                    },
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
                    },
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
 * 作品 Tab：加载并展示用户作品列表。
 */
@Composable
private fun UserWorksTab(
    userId: Int,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> =
        banRepository.filterIllusts(
            rawIllusts = rawIllusts,
            banAIIllust = settingsRepository.banAIIllust,
            hideR18 = settingsRepository.hIsNotAllow,
        )

    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        userId,
        retryCount,
        banRepository,
        settingsRepository.changeVersion,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserIllustsResponse(userId) }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(userId, settingsRepository.changeVersion) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(userId, settingsRepository.changeVersion) { mutableStateOf<String?>(null) }
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
            suspendRunCatchingNonCancel { repository.getUserIllustsResponse(userId, nextUrl = currentNextUrl) }
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

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    IllustTabBody(
        state = state.value,
        illusts = illusts,
        hasMore = nextUrl != null,
        isLoadingMore = isLoadingMore,
        loadMoreError = loadMoreError,
        header = header,
        gridState = gridState,
        onLoadMore = ::loadMore,
        onIllustClick = onIllustClick,
        onRetry = {
            retryCount++
            onRefresh()
        },
        emptyText = strings.userNoWorks,
        topPadding = topPadding,
        scrollBehavior = scrollBehavior,
        isRefreshing = isRefreshing,
        onRefresh = {
            retryCount++
            onRefresh()
        },
    )
}

/**
 * 收藏 Tab：加载并展示用户公开/私密收藏插画。
 */
@Composable
private fun UserBookmarksTab(
    userId: Int,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val strings = LocalStrings.current
    // 收藏可见性：0 = 公开(public)，1 = 私密(private)。
    var selectedRestrictIndex by rememberSaveable(userId) { mutableIntStateOf(0) }
    val restrictTabs = listOf(strings.userPublicRestrict, strings.userPrivateRestrict)

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> =
        banRepository.filterIllusts(
            rawIllusts = rawIllusts,
            banAIIllust = settingsRepository.banAIIllust,
            hideR18 = settingsRepository.hIsNotAllow,
        )

    val coroutineScope = rememberCoroutineScope()

    // 公开收藏状态缓存
    var publicRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var publicLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedRestrictIndex == 0) }
    LaunchedEffect(selectedRestrictIndex) {
        if (selectedRestrictIndex == 0) publicLoadedOnce = true
    }
    val publicState = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        userId,
        publicRetryCount,
        publicLoadedOnce,
        banRepository,
        settingsRepository.changeVersion,
    ) {
        if (!publicLoadedOnce) return@produceState
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "public") }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }
    var publicIllusts by remember(userId, settingsRepository.changeVersion) { mutableStateOf(listOf<Illust>()) }
    var publicNextUrl by remember(userId, settingsRepository.changeVersion) { mutableStateOf<String?>(null) }
    var publicIsLoadingMore by remember { mutableStateOf(false) }
    var publicLoadMoreError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(publicState.value) {
        publicState.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            publicIllusts = initialIllusts
            publicNextUrl = initialNextUrl
            publicIsLoadingMore = false
            publicLoadMoreError = null
        }
    }

    // 私密收藏状态缓存
    var privateRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var privateLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedRestrictIndex == 1) }
    LaunchedEffect(selectedRestrictIndex) {
        if (selectedRestrictIndex == 1) privateLoadedOnce = true
    }
    val privateState = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        userId,
        privateRetryCount,
        privateLoadedOnce,
        banRepository,
        settingsRepository.changeVersion,
    ) {
        if (!privateLoadedOnce) return@produceState
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "private") }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }
    var privateIllusts by remember(userId, settingsRepository.changeVersion) { mutableStateOf(listOf<Illust>()) }
    var privateNextUrl by remember(userId, settingsRepository.changeVersion) { mutableStateOf<String?>(null) }
    var privateIsLoadingMore by remember { mutableStateOf(false) }
    var privateLoadMoreError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(privateState.value) {
        privateState.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            privateIllusts = initialIllusts
            privateNextUrl = initialNextUrl
            privateIsLoadingMore = false
            privateLoadMoreError = null
        }
    }

    fun loadMore() {
        if (selectedRestrictIndex == 0) {
            val currentNextUrl = publicNextUrl ?: return
            if (publicIsLoadingMore) return
            coroutineScope.launch {
                publicIsLoadingMore = true
                publicLoadMoreError = null
                suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "public", nextUrl = currentNextUrl) }
                    .onSuccess { response ->
                        val filtered = filterBanned(response.illusts)
                        publicIllusts = publicIllusts + filtered
                        publicNextUrl = response.nextUrl
                    }
                    .onFailure { error ->
                        publicLoadMoreError = error
                    }
                publicIsLoadingMore = false
            }
        } else {
            val currentNextUrl = privateNextUrl ?: return
            if (privateIsLoadingMore) return
            coroutineScope.launch {
                privateIsLoadingMore = true
                privateLoadMoreError = null
                suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, "private", nextUrl = currentNextUrl) }
                    .onSuccess { response ->
                        val filtered = filterBanned(response.illusts)
                        privateIllusts = privateIllusts + filtered
                        privateNextUrl = response.nextUrl
                    }
                    .onFailure { error ->
                        privateLoadMoreError = error
                    }
                privateIsLoadingMore = false
            }
        }
    }

    val bookmarkHeader: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (header != null) header()
            TabRow(
                tabs = restrictTabs,
                selectedTabIndex = selectedRestrictIndex,
                onTabSelected = { selectedRestrictIndex = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }

    val isPublic = selectedRestrictIndex == 0
    val currentState = if (isPublic) publicState.value else privateState.value
    val currentIllusts = if (isPublic) publicIllusts else privateIllusts
    val currentHasMore = if (isPublic) publicNextUrl != null else privateNextUrl != null
    val currentIsLoadingMore = if (isPublic) publicIsLoadingMore else privateIsLoadingMore
    val currentLoadMoreError = if (isPublic) publicLoadMoreError else privateLoadMoreError

    IllustTabBody(
        state = currentState,
        illusts = currentIllusts,
        hasMore = currentHasMore,
        isLoadingMore = currentIsLoadingMore,
        loadMoreError = currentLoadMoreError,
        header = bookmarkHeader,
        gridState = gridState,
        onLoadMore = ::loadMore,
        onIllustClick = onIllustClick,
        onRetry = {
            if (isPublic) publicRetryCount++ else privateRetryCount++
            onRefresh()
        },
        emptyText = if (isPublic) {
            strings.userNoBookmarks.format(strings.userPublicRestrict)
        } else {
            strings.userNoBookmarks.format(strings.userPrivateRestrict)
        },
        topPadding = topPadding,
        scrollBehavior = scrollBehavior,
        isRefreshing = isRefreshing,
        onRefresh = {
            if (isPublic) publicRetryCount++ else privateRetryCount++
            onRefresh()
        },
    )
}

/**
 * Tab 内容通用容器：处理加载 / 空态 / 错误 / 列表展示。
 * 即使处于加载态或错误态，头部信息（UserProfileHeader + TabRow）始终保持常驻展示，避免全屏闪烁。
 */
@Composable
private fun IllustTabBody(
    state: Result<Pair<List<Illust>, String?>>?,
    illusts: List<Illust>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    header: (@Composable () -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onLoadMore: () -> Unit,
    onIllustClick: (Int) -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
    topPadding: Dp = 0.dp,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    when {
        state == null -> {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding),
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = topPadding + 8.dp,
                        end = 8.dp,
                        bottom = 100.dp,
                    ),
                ) {
                    if (header != null) {
                        item(
                            key = "loading_tab_header",
                            contentType = "grid_custom_header",
                        ) {
                            header()
                        }
                    }
                    item(
                        key = "loading_tab_indicator",
                        contentType = "loading_indicator",
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            InfiniteProgressIndicator()
                        }
                    }
                }
            }
        }
        state.isSuccess -> {
            if (illusts.isEmpty() && !isLoadingMore) {
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topPadding),
                    topAppBarScrollBehavior = scrollBehavior,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            top = topPadding + 8.dp,
                            end = 8.dp,
                            bottom = 100.dp,
                        ),
                    ) {
                        if (header != null) {
                            item(
                                key = "empty_tab_header",
                                contentType = "grid_custom_header",
                            ) {
                                header()
                            }
                        }
                        item(
                            key = "empty_placeholder_item",
                            contentType = "empty_placeholder",
                        ) {
                            EmptyPlaceholder(
                                message = emptyText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                            )
                        }
                    }
                }
            } else {
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topPadding),
                    topAppBarScrollBehavior = scrollBehavior,
                ) {
                    IllustStaggeredGrid(
                        illusts = illusts,
                        onIllustClick = onIllustClick,
                        state = gridState,
                        header = header,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            top = topPadding + 8.dp,
                            end = 8.dp,
                            bottom = 100.dp,
                        ),
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        loadMoreError = loadMoreError,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
        else -> {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding),
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = topPadding + 8.dp,
                        end = 8.dp,
                        bottom = 100.dp,
                    ),
                ) {
                    if (header != null) {
                        item(
                            key = "error_tab_header",
                            contentType = "grid_custom_header",
                        ) {
                            header()
                        }
                    }
                    item(
                        key = "error_placeholder_item",
                        contentType = "error_placeholder",
                    ) {
                        ErrorPlaceholder(
                            error = state.exceptionOrNull(),
                            onRetry = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun UserProfileHeader(
    userDetail: UserDetail,
    isFollowed: Boolean,
    isLoading: Boolean,
    onFollowClick: () -> Unit,
    onFollowListClick: () -> Unit,
    onFollowerListClick: () -> Unit,
    modifier: Modifier = Modifier,
    onIllustClick: (Int) -> Unit = {},
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 第一行：头像 + 名字/账号 + 关注按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PixivAsyncImage(
                model = userDetail.user.profileImageUrls.medium,
                contentDescription = userDetail.user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = userDetail.user.name,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${userDetail.user.account}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = onFollowClick,
                enabled = !isLoading,
                colors = if (isFollowed) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = if (isFollowed) strings.followed else strings.follow,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }

        // 第二行：关注数 / 粉丝数 / 外部链接（紧凑横向排布）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val followCount = userDetail.profile.totalFollowUsers
            Text(
                text = strings.userFollowCount.format(followCount),
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = followCount > 0, onClick = onFollowListClick)
                    .padding(vertical = 2.dp),
            )

            val followerCount = userDetail.profile.totalMypixivUsers
            Text(
                text = strings.userFollowerCount.format(followerCount),
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = followerCount > 0, onClick = onFollowerListClick)
                    .padding(vertical = 2.dp),
            )

            // 外部链接
            userDetail.profile.twitterUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Text(
                    text = "Twitter",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { runCatching { openBrowser(url) } }
                        .padding(vertical = 2.dp),
                )
            }
            userDetail.profile.pawooUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Text(
                    text = "Pawoo",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { runCatching { openBrowser(url) } }
                        .padding(vertical = 2.dp),
                )
            }
            userDetail.profile.webpage?.takeIf { it.isNotBlank() }?.let { url ->
                Text(
                    text = strings.userWebpage,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { runCatching { openBrowser(url) } }
                        .padding(vertical = 2.dp),
                )
            }
        }

        // 第三行：个性签名/简介（若存在，支持超链接解析与折叠）
        userDetail.user.comment?.takeIf { it.isNotBlank() }?.let { bio ->
            HtmlCaptionText(
                html = bio,
                onIllustClick = onIllustClick,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                collapsible = true,
                collapsedMaxLines = 2,
            )
        }
    }
}
