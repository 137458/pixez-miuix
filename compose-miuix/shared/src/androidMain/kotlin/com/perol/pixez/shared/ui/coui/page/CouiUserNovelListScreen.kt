package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 用户小说列表页：固定顶栏 + 小说卡片列表。
 *
 * 分页、账号切换重拉与刷新口径与 MIUIX 版一致；差异只有皮肤层：
 * 固定顶栏刷新入口、COUI 卡片行、三态占位与平板可读宽度，不做下拉刷新。
 * 作者行在传入 [onUserClick] 时可点击跳转画师详情，与 MIUIX 版一致。
 */
@Composable
internal fun CouiUserNovelListScreen(
    userId: Long,
    authorName: String,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)?,
) {
    val repository = LocalNovelRepository.current
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

    val pagedList = remember(userId) {
        PagedListState(
            fetch = { nextUrl ->
                val response = repository.getUserNovels(userId, nextUrl = nextUrl)
                PagedListPage(response.novels, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinct(new) },
        )
    }
    val coroutineScope = rememberCoroutineScope()

    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(userId, retryCount, isLoggedIn, user) {
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

    val pageTitle = remember(authorName, strings.userNovels) {
        if (authorName.isNotBlank()) {
            "$authorName - ${strings.userNovels}"
        } else {
            strings.userNovels
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = pageTitle,
            onBack = onBack,
        ) {
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
                        message = strings.userNovelsEmpty,
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        overscrollEffect = rememberCouiOverscrollEffect(),
                    ) {
                        items(
                            items = currentItems,
                            key = { it.id },
                            contentType = { "coui_novel_card" },
                        ) { novel ->
                            CouiNovelCard(
                                novel = novel,
                                onClick = { onNovelClick(novel.id) },
                                onUserClick = onUserClick,
                            )
                        }

                        val footerState = pagedList.footerState
                        if (footerState.phase != PagingFooterPhase.Hidden) {
                            item(key = "coui_novel_list_footer", contentType = "footer") {
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

/** COUI 小说卡：3:4 封面 + 系列/标题/作者/字数与收藏统计，AI 作品带角标。 */
@Composable
internal fun CouiNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    onUserClick: ((Long) -> Unit)?,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    val isAI = remember(novel.novelAIType) { novel.novelAIType == 2 }

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
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            PixivAsyncImage(
                model = novel.imageUrls.medium.ifBlank { novel.imageUrls.squareMedium },
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (isAI) {
                Text(
                    text = "AI",
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.container)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .height(84.dp * 4 / 3),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                val seriesTitle = novel.series?.title
                if (!seriesTitle.isNullOrBlank()) {
                    Text(
                        text = seriesTitle,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = novel.title,
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Column {
                Row(
                    modifier = if (onUserClick != null) {
                        val authorPress = remember { MutableInteractionSource() }
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .couiPressFeedback(authorPress)
                            .clickable(
                                interactionSource = authorPress,
                                indication = null,
                            ) { onUserClick(novel.user.id) }
                    } else {
                        Modifier
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PixivAsyncImage(
                        model = novel.user.profileImageUrls.medium,
                        contentDescription = novel.user.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = novel.user.name,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = strings.formatNovelWordCount(novel.textLength),
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Image(
                            painter = rememberVectorPainter(MiuixIcons.Favorites),
                            contentDescription = strings.bookmark,
                            colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "${novel.totalBookmarks}",
                            kind = CouiTextKind.ItemSummary,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
