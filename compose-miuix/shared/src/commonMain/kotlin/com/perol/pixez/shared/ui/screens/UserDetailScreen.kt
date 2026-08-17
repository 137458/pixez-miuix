package com.perol.pixez.shared.ui.screens

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
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.UserActionMenu
import com.perol.pixez.shared.ui.components.buildUserCopyInfo
import com.perol.pixez.shared.ui.components.buildUserShareLink
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    var showActionMenu by rememberSaveable { mutableStateOf(false) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = userDetail?.user?.name ?: "用户详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showActionMenu = true },
                        enabled = userDetail != null,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "更多",
                        )
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
                                                followError = e.message ?: "关注操作失败"
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

        userDetail?.let {
            UserActionMenu(
                show = showActionMenu,
                onDismissRequest = { showActionMenu = false },
                onCopyInfo = {
                    showActionMenu = false
                    val text = buildUserCopyInfo(it)
                    runCatching { clipboard.copy(text) }.fold(
                        onSuccess = { toastMessage = "已复制到剪贴板" },
                        onFailure = { e -> toastMessage = "复制失败: ${e.message}" },
                    )
                },
                onCopyLink = {
                    showActionMenu = false
                    val link = buildUserShareLink(it.user.id)
                    runCatching { clipboard.copy(link) }.fold(
                        onSuccess = { toastMessage = "链接已复制" },
                        onFailure = { e -> toastMessage = "复制失败: ${e.message}" },
                    )
                },
                onShareLink = {
                    showActionMenu = false
                    val link = buildUserShareLink(it.user.id)
                    runCatching { share.share(link, it.user.name) }.fold(
                        onSuccess = { toastMessage = "已分享" },
                        onFailure = { e -> toastMessage = "分享失败: ${e.message}" },
                    )
                },
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
    // 主 Tab 选中状态：0 = 作品，1 = 收藏。
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("作品", "收藏")

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
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        userId,
        repository,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserIllusts(userId) }
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        value = illustsResult.map { illusts ->
            illusts.filter {
                it.id !in bannedIds &&
                    it.user.id !in bannedUserIds &&
                    (!banAIIllust || it.illustAIType != 2) &&
                    !banRepository.isBannedByTags(
                        banTags,
                        it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                    )
            }
        }
    }

    IllustTabBody(
        state = state.value,
        onIllustClick = onIllustClick,
        onRetry = { retryCount++ },
        emptyText = "暂无作品",
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
    // 收藏可见性：0 = 公开(public)，1 = 私密(private)。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val restrictTabs = listOf("公开", "私密")
    val restrict = if (selectedRestrictIndex == 0) "public" else "private"

    // 切换用户、可见性选项或重试时重新加载；加载完成后过滤掉被屏蔽作品。
    var retryCount by rememberSaveable(userId, restrict) { mutableIntStateOf(0) }
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        userId,
        restrict,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val illustsResult = suspendRunCatchingNonCancel { repository.getUserBookmarks(userId, restrict) }
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        value = illustsResult.map { illusts ->
            illusts.filter {
                it.id !in bannedIds &&
                    it.user.id !in bannedUserIds &&
                    (!banAIIllust || it.illustAIType != 2) &&
                    !banRepository.isBannedByTags(
                        banTags,
                        it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                    )
            }
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
                onIllustClick = onIllustClick,
                onRetry = { retryCount++ },
                emptyText = "暂无${if (restrict == "public") "公开" else "私密"}收藏",
            )
        }
    }
}

/**
 * Tab 内容通用容器：处理加载 / 空态 / 错误 / 列表展示。
 */
@Composable
private fun IllustTabBody(
    state: Result<List<Illust>>?,
    onIllustClick: (Int) -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
) {
    when {
        state == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        state.isSuccess -> {
            val illusts = state.getOrNull().orEmpty()
            if (illusts.isEmpty()) {
                EmptyPlaceholder(
                    message = emptyText,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                IllustStaggeredGrid(
                    illusts = illusts,
                    onIllustClick = onIllustClick,
                    modifier = Modifier.fillMaxSize(),
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
                text = "关注 $followCount",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .clickable(enabled = followCount > 0, onClick = onFollowListClick)
                    .padding(4.dp),
            )
            val followerCount = userDetail.profile.totalMypixivUsers
            Text(
                text = "好P友 $followerCount",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .clickable(enabled = followerCount > 0, onClick = onFollowerListClick)
                    .padding(4.dp),
            )
        }
        // 外部链接：仅在存在非空链接时展示。
        val externalLinks = listOfNotNull(
            userDetail.profile.twitterUrl?.takeIf { it.isNotBlank() }?.let { "Twitter" to it },
            userDetail.profile.webpage?.takeIf { it.isNotBlank() }?.let { "网页" to it },
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
            Text(text = if (isFollowed) "已关注" else "关注")
        }
    }
}
