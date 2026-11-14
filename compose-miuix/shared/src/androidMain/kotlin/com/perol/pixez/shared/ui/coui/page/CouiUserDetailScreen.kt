package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.menu.CouiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.buildUserCopyInfo
import com.perol.pixez.shared.ui.components.runClipboardShare
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiIllustGrid
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.GridScrollPosition
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.UserDetailPageState
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * COUI 用户详情页（画师详情）：固定顶栏 + 资料区 +「作品 / 收藏」瀑布流。
 *
 * 业务编排与 MIUIX 版一致：资料加载/失败回退、关注切换、作品与公开/私密收藏分页、
 * 滚动位置与分页状态经 [UserDetailPageState] 跨组合保留、更多菜单（相关画师/小说/
 * 复制/分享）。皮肤差异仅在视觉层与顶栏形态：COUI 顶栏为固定条（无折叠毛玻璃），
 * 列表不做下拉刷新，刷新入口保留在顶栏图标上。
 */
@Composable
internal fun CouiUserDetailScreen(
    userId: Long,
    pageState: UserDetailPageState,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onFollowListClick: (Long) -> Unit,
    onFollowerListClick: (Long) -> Unit,
    onRelatedUsersClick: ((Long) -> Unit)? = null,
    onUserNovelsClick: ((Long, String) -> Unit)? = null,
    initialTab: Int = 0,
) {
    val repository = LocalUserRepository.current
    val bookmarkRepository = LocalBookmarkRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val accountRepository = LocalAccountRepository.current
    val toastSink = LocalToastSink.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()

    // 重试计数：顶栏刷新与错误占位重试都经此触发资料重新加载。
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    val currentAccountState = produceState<AccountPersist?>(initialValue = null, accountRepository) {
        value = suspendRunCatchingNonCancel { accountRepository.currentAccount() }
            .onFailure { Napier.w(tag = "CurrentAccount", throwable = it) { "加载当前账号失败(CouiUserDetailScreen)" } }
            .getOrNull()
        accountRepository.loginEventFlow.collect {
            value = suspendRunCatchingNonCancel { accountRepository.currentAccount() }
                .onFailure { Napier.w(tag = "CurrentAccount", throwable = it) { "登录事件后加载当前账号失败(CouiUserDetailScreen)" } }
                .getOrNull()
        }
    }
    val isCurrentUser = remember(currentAccountState.value, userId) {
        val currentId = currentAccountState.value?.userId?.toLongOrNull()
        currentId != null && currentId == userId
    }

    // 初值取保留状态里的最近资料：栈换层重建组合后首帧即渲染内容，避免出栈动画落点空白。
    val detailState = produceState<Result<UserDetail>?>(
        initialValue = pageState.userDetail?.let { Result.success(it) },
        userId,
        repository,
        retryCount,
    ) {
        val loaded = suspendRunCatchingNonCancel { repository.getUserDetail(userId) }
        val cached = pageState.userDetail
        if (loaded.isSuccess) {
            pageState.userDetail = loaded.getOrNull()
            value = loaded
        } else {
            value = cached?.let { Result.success(it) } ?: loaded
        }
    }

    val result = detailState.value
    val userDetail = result?.getOrNull()
    var isFollowed by rememberSaveable(userDetail) {
        mutableStateOf(userDetail?.user?.isFollowed ?: false)
    }
    // 进行中标志用 remember：进程恢复后协程不会恢复，避免关注按钮被永久禁用。
    var isFollowLoading by remember { mutableStateOf(false) }

    val onFollowToggle: (UserDetail) -> Unit = { detail ->
        if (!isFollowLoading) {
            coroutineScope.launch {
                try {
                    isFollowLoading = true
                    suspendRunCatchingNonCancel {
                        if (isFollowed) {
                            bookmarkRepository.unfollowUser(detail.user.id)
                        } else {
                            bookmarkRepository.followUser(detail.user.id)
                        }
                    }.onSuccess {
                        isFollowed = !isFollowed
                    }.onFailure { e ->
                        toastSink(
                            ToastData(
                                "${strings.follow}: ${PixivApiErrorFormatter.format(e, strings)}",
                                ToastType.Error,
                            )
                        )
                    }
                } finally {
                    isFollowLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiUserDetailTopBar(
            title = userDetail?.user?.name.orEmpty(),
            detail = userDetail,
            strings = strings,
            onBack = onBack,
            onRefresh = { retryCount++ },
            onRelatedUsersClick = onRelatedUsersClick,
            onUserNovelsClick = onUserNovelsClick,
            onToast = { toastSink(it) },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                result == null -> CouiStatePlaceholder(
                    message = strings.loading,
                    modifier = Modifier.fillMaxSize(),
                )
                userDetail != null -> CouiUserDetailTabs(
                    userId = userId,
                    pageState = pageState,
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
                    onToast = toastSink,
                )
                else -> CouiStatePlaceholder(
                    message = couiErrorMessage(result.exceptionOrNull()),
                    actionText = strings.retry,
                    onAction = { retryCount++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * COUI 用户页顶栏：返回 + 名字 + 刷新 + 更多菜单。
 * 固定条形态（不随滚动折叠），与 COUI 插画详情顶栏保持一致。
 */
@Composable
private fun CouiUserDetailTopBar(
    title: String,
    detail: UserDetail?,
    strings: AppStrings,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRelatedUsersClick: ((Long) -> Unit)?,
    onUserNovelsClick: ((Long, String) -> Unit)?,
    onToast: (ToastData) -> Unit,
) {
    val menuAnchor = rememberCouiMenuAnchor()
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CouiTheme.colorScheme.background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CouiActionButton(
            icon = MiuixIcons.Back,
            contentDescription = strings.back,
            onClick = onBack,
        )
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = CouiTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        CouiActionButton(
            icon = MiuixIcons.Refresh,
            contentDescription = strings.refresh,
            onClick = onRefresh,
        )
        if (detail != null) {
            Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                CouiActionButton(
                    icon = MiuixIcons.More,
                    contentDescription = strings.menuMoreActions,
                    onClick = { menuAnchor.open { menuExpanded = true } },
                )
                CouiUserMoreMenu(
                    detail = detail,
                    expanded = menuExpanded,
                    anchor = menuAnchor,
                    strings = strings,
                    onRelatedUsersClick = onRelatedUsersClick,
                    onUserNovelsClick = onUserNovelsClick,
                    onDismiss = { menuExpanded = false },
                    onToast = onToast,
                )
            }
        }
    }
}

/** COUI 更多菜单：与 MIUIX 版同一批动作（相关画师/小说、复制信息/链接、分享链接）。 */
@Composable
private fun CouiUserMoreMenu(
    detail: UserDetail,
    expanded: Boolean,
    anchor: CouiMenuAnchor,
    strings: AppStrings,
    onRelatedUsersClick: ((Long) -> Unit)?,
    onUserNovelsClick: ((Long, String) -> Unit)?,
    onDismiss: () -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val userUrl = remember(detail.user.id) { AppConstants.Urls.pixivUserUrl(detail.user.id) }

    val items = buildList {
        if (onRelatedUsersClick != null) {
            add(CouiMenuItem(value = "related_users", text = strings.relatedUsers))
        }
        if (onUserNovelsClick != null) {
            add(CouiMenuItem(value = "user_novels", text = strings.userNovels))
        }
        add(CouiMenuItem(value = "copy_info", text = strings.menuCopyInfo, dividerBefore = true))
        add(CouiMenuItem(value = "copy_link", text = strings.menuCopyLink))
        add(CouiMenuItem(value = "share", text = strings.menuShareLink))
    }

    Menu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        anchor = anchor,
        headerText = strings.menuMoreActions,
        items = items,
        onItemClick = { item ->
            onDismiss()
            when (item.value) {
                "related_users" -> onRelatedUsersClick?.invoke(detail.user.id)
                "user_novels" -> onUserNovelsClick?.invoke(detail.user.id, detail.user.name)
                "copy_info" -> runClipboardShare(
                    success = ToastData(strings.copiedToClipboard, ToastType.Success),
                    failurePrefix = "${strings.copy}${strings.failedSuffix}",
                    onToast = onToast,
                ) { clipboard.copy(buildUserCopyInfo(detail)) }
                "copy_link" -> runClipboardShare(
                    success = ToastData(strings.copiedToClipboard, ToastType.Success),
                    failurePrefix = "${strings.copy}${strings.failedSuffix}",
                    onToast = onToast,
                ) { clipboard.copy(userUrl) }
                "share" -> runClipboardShare(
                    success = ToastData(strings.share, ToastType.Success),
                    failurePrefix = "${strings.share}${strings.failedSuffix}",
                    onToast = onToast,
                ) { share.share(userUrl, detail.user.name) }
            }
        },
    )
}

/**
 * 「作品 / 收藏」双 Tab 容器：两个网格都保持组合（切走只做透明度位移），
 * 与 MIUIX 版一样避免切换时销毁列表、重建后重复请求。
 */
@Composable
private fun CouiUserDetailTabs(
    userId: Long,
    pageState: UserDetailPageState,
    userDetail: UserDetail,
    isCurrentUser: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onFollowListClick: () -> Unit,
    onFollowerListClick: () -> Unit,
    onIllustClick: (Long) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    initialTab: Int,
    onToast: (ToastData) -> Unit,
) {
    val strings = LocalStrings.current
    var selectedTabIndex by rememberSaveable(userId, initialTab) { mutableIntStateOf(initialTab) }

    val worksGridState = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = pageState.worksScroll.index,
        initialFirstVisibleItemScrollOffset = pageState.worksScroll.offset,
    )
    val bookmarksGridState = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = pageState.bookmarksScroll.index,
        initialFirstVisibleItemScrollOffset = pageState.bookmarksScroll.offset,
    )

    // 组合销毁（栈换层）时回写滚动位置；重建时由上面的初值播种。
    DisposableEffect(worksGridState, bookmarksGridState, pageState) {
        onDispose {
            pageState.worksScroll = GridScrollPosition(
                index = worksGridState.firstVisibleItemIndex,
                offset = worksGridState.firstVisibleItemScrollOffset,
            )
            pageState.bookmarksScroll = GridScrollPosition(
                index = bookmarksGridState.firstVisibleItemIndex,
                offset = bookmarksGridState.firstVisibleItemScrollOffset,
            )
        }
    }

    var worksLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedTabIndex == 0) }
    var bookmarksLoadedOnce by rememberSaveable(userId) { mutableStateOf(selectedTabIndex == 1) }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) worksLoadedOnce = true
        if (selectedTabIndex == 1) bookmarksLoadedOnce = true
    }

    val tabChips = remember(strings) {
        listOf(
            CouiFilterChip(key = "works", label = strings.userWorkTab),
            CouiFilterChip(key = "bookmarks", label = strings.userBookmarkTab),
        )
    }

    val headerContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            CouiUserProfileHeader(
                userDetail = userDetail,
                isCurrentUser = isCurrentUser,
                isFollowed = isFollowed,
                isFollowLoading = isFollowLoading,
                onFollowClick = onFollowClick,
                onFollowListClick = onFollowListClick,
                onFollowerListClick = onFollowerListClick,
                onIllustClick = onIllustClick,
                onToast = onToast,
            )
            CouiFilterChipBar(
                chips = tabChips,
                selectedKey = if (selectedTabIndex == 0) "works" else "bookmarks",
                onSelect = { key -> selectedTabIndex = if (key == "works") 0 else 1 },
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                CouiUserWorksTab(
                    userId = userId,
                    pageState = pageState,
                    header = headerContent,
                    gridState = worksGridState,
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                    emptyText = strings.userNoWorks,
                )
            }
        }

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
                CouiUserBookmarksTab(
                    userId = userId,
                    pageState = pageState,
                    header = headerContent,
                    gridState = bookmarksGridState,
                    onIllustClick = onIllustClick,
                    repository = repository,
                    banRepository = banRepository,
                    settingsRepository = settingsRepository,
                )
            }
        }
    }
}

/** 资料区：头像/账号、关注按钮、关注数/粉丝数/外链、签名。 */
@Composable
private fun CouiUserProfileHeader(
    userDetail: UserDetail,
    isCurrentUser: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onFollowListClick: () -> Unit,
    onFollowerListClick: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme

    // 外部链接统一经 scheme 白名单打开，失败提示由宿主 Toast 承载。
    val openExternalUrl: (String) -> Unit = { url ->
        openSafeUrl(url, strings, onError = { message -> onToast(ToastData(message, ToastType.Error)) })
    }

    // 阅读列：资料区在平板上收进可读宽度，避免账号/签名行横跨整屏。
    CouiReadableColumn {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userDetail.user.name,
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${userDetail.user.account}",
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!isCurrentUser) {
                CouiFollowButton(
                    isFollowed = isFollowed,
                    enabled = !isFollowLoading,
                    strings = strings,
                    onClick = onFollowClick,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val followCount = userDetail.profile.totalFollowUsers
            CouiProfileLinkText(
                text = strings.userFollowCount.format(followCount),
                enabled = followCount > 0,
                color = scheme.onSurface,
                onClick = onFollowListClick,
            )
            val followerCount = userDetail.profile.totalMypixivUsers
            CouiProfileLinkText(
                text = strings.userFollowerCount.format(followerCount),
                enabled = followerCount > 0,
                color = scheme.onSurface,
                onClick = onFollowerListClick,
            )
            userDetail.profile.twitterUrl?.takeIf { it.isNotBlank() }?.let { url ->
                CouiProfileLinkText(text = "Twitter", color = scheme.accentOnSurface) { openExternalUrl(url) }
            }
            userDetail.profile.pawooUrl?.takeIf { it.isNotBlank() }?.let { url ->
                CouiProfileLinkText(text = "Pawoo", color = scheme.accentOnSurface) { openExternalUrl(url) }
            }
            userDetail.profile.webpage?.takeIf { it.isNotBlank() }?.let { url ->
                CouiProfileLinkText(text = strings.userWebpage, color = scheme.accentOnSurface) { openExternalUrl(url) }
            }
        }

        userDetail.user.comment?.takeIf { it.isNotBlank() }?.let { bio ->
            HtmlCaptionText(
                html = bio,
                onIllustClick = onIllustClick,
                onLinkClick = openExternalUrl,
                style = MiuixTheme.textStyles.footnote2,
                color = scheme.onSurfaceVariant,
                collapsible = true,
                collapsedMaxLines = 2,
            )
        }
    }
    }
}

/** 关注按钮：未关注为强调胶囊，已关注为容器底色胶囊。 */
@Composable
private fun CouiFollowButton(
    isFollowed: Boolean,
    enabled: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isFollowed) scheme.container else scheme.accent)
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFollowed) strings.followed else strings.follow,
            kind = CouiTextKind.ItemSummary,
            color = if (isFollowed) scheme.onSurface else scheme.onAccent,
            maxLines = 1,
        )
    }
}

/** 资料区可点文本（关注数/粉丝数/外链）：无底色，按下走库的按压蒙层。 */
@Composable
private fun CouiProfileLinkText(
    text: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = if (enabled) color else color.copy(alpha = 0.5f),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .couiPressFeedback(interactionSource, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 2.dp),
    )
}

/** 作品 Tab：加载用户作品并展示，分页状态经 [UserDetailPageState] 保留。 */
@Composable
private fun CouiUserWorksTab(
    userId: Long,
    pageState: UserDetailPageState,
    header: @Composable () -> Unit,
    gridState: LazyStaggeredGridState,
    onIllustClick: (Long) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    emptyText: String,
) {
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    val listState = remember(userId, pageState) {
        pageState.works ?: IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserIllustsResponse(userId, nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        ).also { pageState.works = it }
    }
    val coroutineScope = rememberCoroutineScope()

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(userId, retryCount, filterChangeVersion) {
        if (listState.refilterIfChanged()) return@LaunchedEffect
        listState.loadFirst(force = retryCount > 0)
    }

    val loadMore: () -> Unit = {
        coroutineScope.launch { listState.loadMore() }
    }

    CouiUserTabBody(
        illusts = listState.filteredIllusts,
        initialError = listState.initialError,
        hasMore = listState.nextUrl != null,
        isLoadingMore = listState.isLoadingMore,
        loadMoreError = listState.loadMoreError,
        header = header,
        gridState = gridState,
        onLoadMore = loadMore,
        onIllustClick = onIllustClick,
        onRetry = { retryCount++ },
        emptyText = emptyText,
    )
}

/** 收藏 Tab：公开/私密切换，两套分页状态各自保留。 */
@Composable
private fun CouiUserBookmarksTab(
    userId: Long,
    pageState: UserDetailPageState,
    header: @Composable () -> Unit,
    gridState: LazyStaggeredGridState,
    onIllustClick: (Long) -> Unit,
    repository: UserRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    val strings = LocalStrings.current
    // 收藏可见性：0 = 公开(public)，1 = 私密(private)。
    var selectedRestrictIndex by rememberSaveable(userId) { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var publicRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val publicState = remember(userId, pageState) {
        pageState.publicBookmarks ?: IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserBookmarksResponse(userId, "public", nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        ).also { pageState.publicBookmarks = it }
    }
    var privateRetryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    val privateState = remember(userId, pageState) {
        pageState.privateBookmarks ?: IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserBookmarksResponse(userId, "private", nextUrl = nextUrl)
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        ).also { pageState.privateBookmarks = it }
    }

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(selectedRestrictIndex, publicRetryCount, privateRetryCount, filterChangeVersion) {
        val isPublic = selectedRestrictIndex == 0
        val activeState = if (isPublic) publicState else privateState
        val activeRetryCount = if (isPublic) publicRetryCount else privateRetryCount
        if (activeState.refilterIfChanged()) return@LaunchedEffect
        activeState.loadFirst(force = activeRetryCount > 0)
    }

    val loadMore: () -> Unit = {
        val activeState = if (selectedRestrictIndex == 0) publicState else privateState
        coroutineScope.launch { activeState.loadMore() }
    }

    val restrictChips = remember(strings) {
        listOf(
            CouiFilterChip(key = "public", label = strings.userPublicRestrict),
            CouiFilterChip(key = "private", label = strings.userPrivateRestrict),
        )
    }
    val bookmarkHeader: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            header()
            CouiFilterChipBar(
                chips = restrictChips,
                selectedKey = if (selectedRestrictIndex == 0) "public" else "private",
                onSelect = { key -> selectedRestrictIndex = if (key == "public") 0 else 1 },
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }

    val isPublic = selectedRestrictIndex == 0
    val activeState = if (isPublic) publicState else privateState

    CouiUserTabBody(
        illusts = activeState.filteredIllusts,
        initialError = activeState.initialError,
        hasMore = activeState.nextUrl != null,
        isLoadingMore = activeState.isLoadingMore,
        loadMoreError = activeState.loadMoreError,
        header = bookmarkHeader,
        gridState = gridState,
        onLoadMore = loadMore,
        onIllustClick = onIllustClick,
        onRetry = { if (isPublic) publicRetryCount++ else privateRetryCount++ },
        emptyText = if (isPublic) {
            strings.userNoBookmarks.format(strings.userPublicRestrict)
        } else {
            strings.userNoBookmarks.format(strings.userPrivateRestrict)
        },
    )
}

/**
 * Tab 内容容器：头部常驻，按三态渲染加载占位 / 空态 / 错误 / 瀑布流。
 *
 * 行为对齐 MIUIX 版 [IllustTabBody]，差异在于 COUI 列表不做下拉刷新，
 * 刷新入口收敛到顶栏图标与错误占位的重试。
 */
@Composable
private fun CouiUserTabBody(
    illusts: List<Illust>?,
    initialError: Throwable?,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    header: @Composable () -> Unit,
    gridState: LazyStaggeredGridState,
    onLoadMore: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
) {
    val strings = LocalStrings.current
    val bottomPadding = LocalBottomBarContentPadding.current
    val contentPadding = PaddingValues(
        start = 10.dp,
        top = 4.dp,
        end = 10.dp,
        bottom = bottomPadding + 8.dp,
    )

    when {
        illusts == null && initialError == null -> CouiUserTabPlaceholderList(
            header = header,
            contentPadding = contentPadding,
        ) {
            CouiStatePlaceholder(message = strings.loading)
        }
        illusts != null && illusts.isEmpty() && !isLoadingMore -> CouiUserTabPlaceholderList(
            header = header,
            contentPadding = contentPadding,
        ) {
            CouiStatePlaceholder(message = emptyText)
        }
        illusts != null -> CouiIllustGrid(
            illusts = illusts,
            state = gridState,
            onIllustClick = onIllustClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            header = header,
            hasMore = hasMore,
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
            onLoadMore = onLoadMore,
        )
        else -> CouiUserTabPlaceholderList(
            header = header,
            contentPadding = contentPadding,
        ) {
            CouiStatePlaceholder(
                message = couiErrorMessage(initialError),
                actionText = strings.retry,
                onAction = onRetry,
            )
        }
    }
}

/** 非列表态（加载/空/错误）：头部依旧常驻，占位区固定高度，避免整页闪烁。 */
@Composable
private fun CouiUserTabPlaceholderList(
    header: @Composable () -> Unit,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "coui_user_tab_header", contentType = "coui_user_tab_header") {
            header()
        }
        item(key = "coui_user_tab_placeholder", contentType = "coui_user_tab_placeholder") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            ) {
                content()
            }
        }
    }
}
