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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.skin.CouiUserListKind
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 用户列表页：关注 / 粉丝 / 相关画师共用一套实现。
 *
 * 业务编排与三份 MIUIX 页逐条对齐：账号切换或手动刷新时重拉首屏，
 * 触底按 [PagedListState.shouldPreloadMore] 续页；差异只有皮肤层：
 * 固定顶栏刷新入口、COUI 卡片行与三态占位，列表收敛到平板可读宽度，
 * 不再提供下拉刷新（与全部 COUI 页面一致）。
 */
@Composable
internal fun CouiUserListScreen(
    kind: CouiUserListKind,
    userId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    val repository = LocalUserRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current

    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(userId) { mutableStateOf(false) }

    val pagedList = remember(userId, kind) {
        PagedListState(
            fetch = { nextUrl ->
                val response = when (kind) {
                    CouiUserListKind.Following ->
                        repository.getUserFollowingResponse(userId, nextUrl = nextUrl)

                    CouiUserListKind.Followers ->
                        repository.getUserFollowersResponse(userId = userId, nextUrl = nextUrl)

                    CouiUserListKind.Related ->
                        repository.getUserRelated(userId, nextUrl = nextUrl)
                }
                PagedListPage(response.userPreviews, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinct(new) },
        )
    }
    val coroutineScope = rememberCoroutineScope()

    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(userId, retryCount, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        val force = isManualRefreshing
        pagedList.loadFirst(force = force)
        isManualRefreshing = false
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember(pagedList) {
        derivedStateOf {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
            )
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMore()
        }
    }

    val title = when (kind) {
        CouiUserListKind.Following -> strings.userFollowTitle
        CouiUserListKind.Followers -> strings.userFollowerTitle
        CouiUserListKind.Related -> strings.relatedUsers
    }
    val emptyMessage = when (kind) {
        CouiUserListKind.Following -> strings.userFollowEmpty
        CouiUserListKind.Followers -> strings.userFollowerEmpty
        CouiUserListKind.Related -> strings.relatedUsersEmpty
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
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
                onClick = triggerManualRefresh,
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            val currentItems = pagedList.items
            when {
                currentItems == null && pagedList.initialError == null ->
                    CouiStatePlaceholder(
                        message = strings.loading,
                        modifier = Modifier.fillMaxSize(),
                    )

                currentItems != null -> if (currentItems.isEmpty()) {
                    CouiStatePlaceholder(
                        message = emptyMessage,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 8.dp,
                            end = 12.dp,
                            bottom = LocalBottomBarContentPadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        overscrollEffect = rememberCouiOverscrollEffect(),
                    ) {
                        items(
                            items = currentItems,
                            key = { it.user.id },
                            contentType = { "coui_user_preview" },
                        ) { preview ->
                            CouiUserPreviewRow(
                                preview = preview,
                                onClick = { onUserClick(preview.user.id) },
                            )
                        }

                        val footerState = pagedList.footerState
                        if (footerState.phase != PagingFooterPhase.Hidden) {
                            item(key = "coui_user_list_footer", contentType = "footer") {
                                CouiPagingFooter(
                                    state = footerState,
                                    onRetry = ::loadMore,
                                )
                            }
                        }
                    }
                }

                else -> CouiStatePlaceholder(
                    message = couiErrorMessage(pagedList.initialError),
                    actionText = strings.retry,
                    onAction = { triggerManualRefresh() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** 用户预览卡：头像 + 昵称/账号 + 最近作品缩略图（沿用 MIUIX 的过滤口径）。 */
@Composable
private fun CouiUserPreviewRow(
    preview: UserPreview,
    onClick: () -> Unit,
) {
    val settings = LocalSettingsRepository.current
    val hIsNotAllow = settings.hIsNotAllow
    val banAIIllust = settings.banAIIllust
    val changeVersion = settings.changeVersion.collectAsState().value
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }

    val displayIllusts = remember(preview.illusts, hIsNotAllow, banAIIllust, changeVersion) {
        preview.illusts.filter { illust ->
            (!hIsNotAllow || !illust.isR18()) &&
                (!banAIIllust || illust.illustAIType != 2)
        }.take(3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card)
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PixivAsyncImage(
            model = preview.user.profileImageUrls.medium,
            contentDescription = preview.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.user.name,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${preview.user.account}",
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        displayIllusts.forEach { illust ->
            PixivAsyncImage(
                model = illust.imageUrls.squareMedium,
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
    }
}
