package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.repository.NovelRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch

/**
 * COUI 画师搜索结果：共享分页骨架 + COUI 用户行，账号切换与筛选版本变更语义与原实现一致。
 */
@Composable
internal fun CouiSearchUserResultList(
    query: String,
    repository: SearchRepository,
    settingsRepository: SettingsRepository,
    onUserClick: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    CouiSearchPagedColumn(
        query = query,
        reloadTrigger = filterChangeVersion,
        emptyMessage = LocalStrings.current.searchEmptyUser,
        itemKey = { it.user.id },
        contentPadding = contentPadding,
        fetch = { nextUrl ->
            val response = repository.searchUserResponse(query, nextUrl = nextUrl)
            PagedListPage(response.userPreviews, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
    ) { preview ->
        CouiUserPreviewRow(
            preview = preview,
            onClick = { onUserClick(preview.user.id) },
        )
    }
}

/**
 * COUI 小说搜索结果：纯 word 直搜，无筛选重载语义，分页与账号跟踪同画师列表。
 */
@Composable
internal fun CouiSearchNovelResultList(
    query: String,
    novelRepository: NovelRepository,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
    contentPadding: PaddingValues,
) {
    CouiSearchPagedColumn(
        query = query,
        reloadTrigger = Unit,
        emptyMessage = LocalStrings.current.noData,
        itemKey = { it.id },
        contentPadding = contentPadding,
        fetch = { nextUrl ->
            val response = novelRepository.searchNovels(query, nextUrl = nextUrl)
            PagedListPage(response.novels, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
    ) { novel ->
        CouiNovelRow(
            novel = novel,
            onClick = { onNovelClick(novel.id) },
            onUserClick = onUserClick,
        )
    }
}

/**
 * 画师/小说结果共用的分页骨架：账号身份跟踪 + [PagedListState] 分页 + COUI 占位与 footer。
 *
 * 与 MIUIX 版 [com.perol.pixez.shared.ui.screens.SearchResultSection] 的行为逐项等价，
 * 差异仅在渲染层：无 MIUIX 滚动条与折叠顶栏联动，卡片改为 COUI 卡片容器。
 */
@Composable
private fun <T> CouiSearchPagedColumn(
    query: String,
    reloadTrigger: Any?,
    emptyMessage: String,
    itemKey: (T) -> Any,
    contentPadding: PaddingValues,
    fetch: suspend (nextUrl: String?) -> PagedListPage<T>,
    mergeItems: (old: List<T>, new: List<T>) -> List<T>,
    itemContent: @Composable (item: T) -> Unit,
) {
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 登录状态与账号身份跟踪：登录/登出/切号（loginEventFlow）到达时作废旧账号结果并重拉首屏。
    val accountRepository = LocalAccountRepository.current
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 挂起上下文读取当前账号：suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }

    var retryCount by rememberSaveable(query) { mutableIntStateOf(0) }

    val pagedList = remember(query) {
        PagedListState(
            fetch = fetch,
            mergeItems = mergeItems,
        )
    }

    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(query, reloadTrigger, retryCount, isLoggedIn, user) {
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        pagedList.loadFirst(force = true)
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    val currentItems = pagedList.items
    val shouldLoadMore by remember(pagedList) {
        derivedStateOf {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
            )
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMore()
    }

    when {
        currentItems == null && pagedList.initialError == null -> CouiStatePlaceholder(
            message = strings.loading,
        )
        currentItems == null -> CouiStatePlaceholder(
            message = pagedList.initialError?.let { PixivApiErrorFormatter.format(it, strings) } ?: strings.retry,
            actionText = strings.retry,
            onAction = { retryCount++ },
        )
        currentItems.isEmpty() -> CouiStatePlaceholder(message = emptyMessage)
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = rememberCouiOverscrollEffect(),
        ) {
            items(
                items = currentItems,
                key = itemKey,
            ) { item ->
                CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    itemContent(item)
                }
            }

            val footerState = pagedList.footerState
            if (footerState.phase != PagingFooterPhase.Hidden) {
                item(key = "coui_search_paged_footer", contentType = "footer") {
                    CouiPagingFooter(state = footerState, onRetry = ::loadMore)
                }
            }
        }
    }
}

/** COUI 画师结果行：头像、名称与账号，附最近三张作品预览（按设置过滤 R18/AI）。 */
@Composable
private fun CouiUserPreviewRow(
    preview: UserPreview,
    onClick: () -> Unit,
) {
    val settings = LocalSettingsRepository.current
    val hIsNotAllow = settings.hIsNotAllow
    val banAIIllust = settings.banAIIllust
    val changeVersion = settings.changeVersion.collectAsState().value

    val displayIllusts = remember(preview.illusts, hIsNotAllow, banAIIllust, changeVersion) {
        preview.illusts.filter { illust ->
            (!hIsNotAllow || !illust.isR18()) &&
                (!banAIIllust || illust.illustAIType != 2)
        }.take(3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${preview.user.account}",
                kind = CouiTextKind.GroupFooter,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        displayIllusts.forEach { illust ->
            PixivAsyncImage(
                model = illust.imageUrls.squareMedium,
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

/** COUI 小说结果行：封面、系列、标题、作者与字数/收藏统计。 */
@Composable
private fun CouiNovelRow(
    novel: Novel,
    onClick: () -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val isAI = remember(novel.novelAIType) { novel.novelAIType == 2 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            PixivAsyncImage(
                model = novel.imageUrls.medium.ifBlank { novel.imageUrls.squareMedium },
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isAI) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CouiTheme.colorScheme.accent)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "AI",
                        kind = CouiTextKind.GroupFooter,
                        color = CouiTheme.colorScheme.onAccent,
                        maxLines = 1,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val seriesTitle = novel.series?.title
            if (!seriesTitle.isNullOrBlank()) {
                Text(
                    text = seriesTitle,
                    kind = CouiTextKind.GroupFooter,
                    color = CouiTheme.colorScheme.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = novel.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = if (onUserClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onUserClick(novel.user.id) }
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
                    kind = CouiTextKind.GroupFooter,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.formatNovelWordCount(novel.textLength),
                    kind = CouiTextKind.GroupFooter,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = "${novel.totalBookmarks} ${strings.bookmark}",
                    kind = CouiTextKind.GroupFooter,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
