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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
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
    repository: UserRepository,
    bookmarkRepository: BookmarkRepository,
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
        value = runCatchingNonCancel { repository.getUserDetail(userId) }
    }

    val result = detailState.value
    val userDetail = result?.getOrNull()
    var isFollowed by rememberSaveable(userDetail) {
        mutableStateOf(userDetail?.user?.isFollowed ?: false)
    }
    var isFollowLoading by rememberSaveable { mutableStateOf(false) }
    var followError by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = userDetail?.user?.name ?: "用户详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.padding(paddingValues))
            result.isSuccess && userDetail != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                                        runCatchingNonCancel {
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
                        modifier = Modifier.padding(16.dp),
                    )
                    UserDetailTabContent(
                        userId = userId,
                        onIllustClick = onIllustClick,
                        repository = repository,
                    )
                }
            }
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier.padding(paddingValues),
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
                )
                1 -> UserBookmarksTab(
                    userId = userId,
                    onIllustClick = onIllustClick,
                    repository = repository,
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
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        userId,
        repository,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getUserIllusts(userId) }
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
) {
    // 收藏可见性：0 = 公开(public)，1 = 私密(private)。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val restrictTabs = listOf("公开", "私密")
    val restrict = if (selectedRestrictIndex == 0) "public" else "private"

    // 切换用户、可见性选项或重试时重新加载。
    var retryCount by rememberSaveable(userId, restrict) { mutableIntStateOf(0) }
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        userId,
        restrict,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getUserBookmarks(userId, restrict) }
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
