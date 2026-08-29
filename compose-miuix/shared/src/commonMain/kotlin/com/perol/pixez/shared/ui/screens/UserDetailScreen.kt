package com.perol.pixez.shared.ui.screens

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
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
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.buildUserCopyInfo
import com.perol.pixez.shared.ui.components.buildUserShareLink
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

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
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()
    val backdrop = LocalBackdrop.current
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = userDetail?.user?.name ?: "",
                color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                modifier = Modifier.topAppBarBlur(backdrop = backdrop, tintColor = colorScheme.surface),
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
                                                onSuccess = { toastMessage = strings.copiedToClipboard },
                                                onFailure = { e -> toastMessage = "${strings.copy}${strings.loadFailed}: ${e.message}" },
                                            )
                                        }
                                    ),
                                    DropdownItem(
                                        text = strings.menuCopyLink,
                                        onClick = {
                                            val link = buildUserShareLink(currentDetail.user.id)
                                            runCatching { clipboard.copy(link) }.fold(
                                                onSuccess = { toastMessage = strings.copiedToClipboard },
                                                onFailure = { e -> toastMessage = "${strings.copy}${strings.loadFailed}: ${e.message}" },
                                            )
                                        }
                                    ),
                                    DropdownItem(
                                        text = strings.menuShareLink,
                                        onClick = {
                                            val link = buildUserShareLink(currentDetail.user.id)
                                            runCatching { share.share(link, currentDetail.user.name) }.fold(
                                                onSuccess = { toastMessage = strings.share },
                                                onFailure = { e -> toastMessage = "${strings.share}${strings.loadFailed}: ${e.message}" },
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
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                result.isSuccess && userDetail != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        followError?.let { error ->
                            Text(
                                text = error,
                                color = MiuixTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        UserProfileHeader(
                            userDetail = userDetail,
                            isFollowed = isFollowed,
                            isLoading = isFollowLoading,
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
                                                followError = e.message ?: "${strings.follow}${strings.loadFailed}"
                                            }
                                        } finally {
                                            isFollowLoading = false
                                        }
                                    }
                                }
                            },
                            onFollowListClick = { onFollowListClick(userDetail.user.id) },
                            onFollowerListClick = { onFollowerListClick(userDetail.user.id) },
                            modifier = Modifier.padding(16.dp),
                        )
                        UserDetailTabContent(
                            userId = userId,
                            onIllustClick = onIllustClick,
                            repository = repository,
                            banRepository = banRepository,
                            settingsRepository = settingsRepository,
                        )
                    }
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { retryCount++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ToastMessage(
                message = toastMessage,
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
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    val strings = LocalStrings.current
    // 主 Tab 选中状态：0 = 作品，1 = 收藏。
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(strings.userWorkTab, strings.userBookmarkTab)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (selectedTabIndex) {
                0 -> UserWorksTab(
                    userId = userId,
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                )
                1 -> UserBookmarksTab(
                    userId = userId,
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
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
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

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
        userId,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserIllustsResponse(userId) }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(userId) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(userId) { mutableStateOf<String?>(null) }
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
        onLoadMore = ::loadMore,
        onIllustClick = onIllustClick,
        onRetry = { retryCount++ },
        emptyText = strings.userNoWorks,
    )
}

/**
 * 收藏 Tab：加载并展示用户公开/私密收藏插画。
 */
@Composable
private fun UserBookmarksTab(
    userId: Int,
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    val strings = LocalStrings.current
    // 收藏可见性：0 = 公开(public)，1 = 私密(private)。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val restrictTabs = listOf(strings.userPublicRestrict, strings.userPrivateRestrict)
    val restrict = if (selectedRestrictIndex == 0) "public" else "private"

    // 切换用户、可见性选项或重试时重新加载；加载完成后过滤掉被屏蔽作品。
    var retryCount by rememberSaveable(userId, restrict) { mutableIntStateOf(0) }

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
        userId,
        restrict,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, restrict) }
        value = illustsResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(userId, restrict) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(userId, restrict) { mutableStateOf<String?>(null) }
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
            suspendRunCatchingNonCancel { repository.getUserBookmarksResponse(userId, restrict, nextUrl = currentNextUrl) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            tabs = restrictTabs,
            selectedTabIndex = selectedRestrictIndex,
            onTabSelected = { selectedRestrictIndex = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            IllustTabBody(
                state = state.value,
                illusts = illusts,
                hasMore = nextUrl != null,
                isLoadingMore = isLoadingMore,
                loadMoreError = loadMoreError,
                onLoadMore = ::loadMore,
                onIllustClick = onIllustClick,
                onRetry = { retryCount++ },
                emptyText = if (restrict == "public") {
                    strings.userNoBookmarks.format(strings.userPublicRestrict)
                } else {
                    strings.userNoBookmarks.format(strings.userPrivateRestrict)
                },
            )
        }
    }
}

/**
 * Tab 内容通用容器：处理加载 / 空态 / 错误 / 列表展示。
 */
@Composable
private fun IllustTabBody(
    state: Result<Pair<List<Illust>, String?>>?,
    illusts: List<Illust>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    onLoadMore: () -> Unit,
    onIllustClick: (Int) -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
) {
    when {
        state == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        state.isSuccess -> {
            if (illusts.isEmpty() && !isLoadingMore) {
                EmptyPlaceholder(
                    message = emptyText,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                IllustStaggeredGrid(
                    illusts = illusts,
                    onIllustClick = onIllustClick,
                    modifier = Modifier.fillMaxSize(),
                    hasMore = hasMore,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    onLoadMore = onLoadMore,
                )
            }
        }
        else -> ErrorPlaceholder(
            error = state.exceptionOrNull(),
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
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
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 背景图：仅在存在非空背景图链接时展示，位于头像上方。
        userDetail.profile.backgroundImageUrl?.takeIf { it.isNotBlank() }?.let { backgroundUrl ->
            PixivAsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        PixivAsyncImage(
            model = userDetail.user.profileImageUrls.medium,
            contentDescription = userDetail.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = userDetail.user.name,
            style = MiuixTheme.textStyles.title1,
        )
        Text(
            text = "@${userDetail.user.account}",
            style = MiuixTheme.textStyles.footnote1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = userDetail.user.comment ?: "",
            style = MiuixTheme.textStyles.body2,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val followCount = userDetail.profile.totalFollowUsers
            Text(
                text = strings.userFollowCount.format(followCount),
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .clickable(enabled = followCount > 0, onClick = onFollowListClick)
                    .padding(4.dp),
            )
            val followerCount = userDetail.profile.totalMypixivUsers
            Text(
                text = strings.userFollowerCount.format(followerCount),
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .clickable(enabled = followerCount > 0, onClick = onFollowerListClick)
                    .padding(4.dp),
            )
        }
        // 外部链接：仅在存在非空链接时展示。
        val externalLinks: List<Pair<String, String>> = listOfNotNull(
            userDetail.profile.twitterUrl?.takeIf { it.isNotBlank() }?.let { "Twitter" to it },
            userDetail.profile.webpage?.takeIf { it.isNotBlank() }?.let { strings.userWebpage to it },
            userDetail.profile.pawooUrl?.takeIf { it.isNotBlank() }?.let { "Pawoo" to it },
        )
        if (externalLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                externalLinks.forEach { (label, url) ->
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { runCatching { openBrowser(url) } }
                            .padding(4.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFollowClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(text = if (isFollowed) strings.followed else strings.follow)
        }
    }
}
