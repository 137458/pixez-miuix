package com.perol.pixez.shared.ui.coui.page

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.menu.CouiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.platform.rememberOptimizedImageModel
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalDownloadRepository
import com.perol.pixez.shared.ui.LocalHistoryRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UgoiraPlayer
import com.perol.pixez.shared.ui.components.buildIllustDetailMoreMenuEntries
import com.perol.pixez.shared.ui.components.buildIllustShareLink
import com.perol.pixez.shared.ui.components.resolveIllustCoverUrl
import com.perol.pixez.shared.ui.components.runClipboardShare
import com.perol.pixez.shared.ui.components.toLocalizedDownloadError
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiIllustFullScreenOverlay
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.core.CouiCompactScaffold
import com.perol.pixez.shared.ui.coui.core.CouiErrorMessage
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry
import com.perol.pixez.shared.ui.navigation.animation.cardExpandDetailChromeAlpha
import com.perol.pixez.shared.ui.screens.IllustDetailActions
import com.perol.pixez.shared.ui.screens.rememberIllustDetailActions
import com.perol.pixez.shared.ui.screens.rememberSyncedDetailToggle
import com.perol.pixez.shared.ui.state.rememberAccountSession
import com.perol.pixez.shared.ui.utils.accessibleTouchTarget
import com.perol.pixez.shared.ui.utils.formatPixivDate
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 作品详情页属性徽章语义类别。 */
internal enum class CouiIllustBadgeKind {
    R18,
    AI,
    UGOIRA,
    MULTI_PAGE,
}

/** 作品详情页展示的语义徽章。 */
internal data class CouiIllustBadge(
    val label: String,
    val kind: CouiIllustBadgeKind,
)

/** 解析作品详情页头部徽章：顺序固定为 R-18 -> AI -> Ugoira -> 多页。 */
internal fun resolveCouiIllustBadges(illust: Illust): List<CouiIllustBadge> = buildList {
    if (illust.isR18()) {
        add(CouiIllustBadge(label = "R-18", kind = CouiIllustBadgeKind.R18))
    }
    if (illust.illustAIType == 2) {
        add(CouiIllustBadge(label = "AI", kind = CouiIllustBadgeKind.AI))
    }
    if (IllustType.isUgoira(illust.type)) {
        add(CouiIllustBadge(label = "GIF", kind = CouiIllustBadgeKind.UGOIRA))
    }
    if (illust.pageCount > 1) {
        add(CouiIllustBadge(label = "${illust.pageCount}P", kind = CouiIllustBadgeKind.MULTI_PAGE))
    }
}

/** 判定作者行是否显示关注按钮：本人作品隐藏关注入口。 */
internal fun shouldShowArtistFollowButton(illustUserId: Long, currentAccountUserId: Long?): Boolean =
    currentAccountUserId == null || illustUserId != currentAccountUserId

/** 计算快捷操作栏中的实时收藏数，在本地切换收藏后即时反馈。 */
internal fun resolveDisplayedBookmarkCount(
    baseCount: Int,
    initiallyBookmarked: Boolean,
    currentlyBookmarked: Boolean,
): Int {
    val delta = when {
        !initiallyBookmarked && currentlyBookmarked -> 1
        initiallyBookmarked && !currentlyBookmarked -> -1
        else -> 0
    }
    return (baseCount + delta).coerceAtLeast(0)
}

/** 已收藏心形高亮色（ColorOS 暖红）。 */
private val CouiBookmarkActiveColor = Color(0xFFFF4D6A)

/**
 * COUI 插画详情页：
 * - 紧凑悬浮顶栏（52dp [CouiCompactScaffold] + [TopAppBarButtonGroup]），进页直览画作；
 * - 窄屏为顺畅单列流，宽屏（平板/折叠屏）自动切换为左右双栏（左画作流、右信息与互动，各自独立滚动）；
 * - 信息卡内置语义徽章、一键复制 ID、四合一快捷操作条（收藏/下载/评论/分享）与作者关注按钮；
 * - 全屏查看切入纯 COUI 顶栏与 COUI 锚点菜单（[CouiIllustFullScreenOverlay]），零 MIUIX 残留。
 */
@Composable
internal fun CouiIllustDetailScreen(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)? = null,
    onNovelClick: ((Long) -> Unit)? = null,
) {
    val repository = LocalIllustRepository.current
    val settings = LocalSettingsRepository.current
    val sharedBoundsRegistry = LocalSharedBoundsRegistry.current

    DisposableEffect(sharedBoundsRegistry, illustId) {
        onDispose {
            sharedBoundsRegistry.onDetailDisposed(originIllustId = illustId)
        }
    }

    if (settings.swipeChangeArtwork) {
        val relatedState = produceState<List<Long>>(initialValue = emptyList(), illustId) {
            val list = suspendRunCatchingNonCancel { repository.getIllustRelated(illustId) }
                .onFailure { Napier.w(tag = "IllustRelated", throwable = it) { "加载相关作品失败 illustId=$illustId" } }
                .getOrNull().orEmpty()
            value = list.map { it.id }.filter { it != illustId }
        }
        val idList = remember(illustId, relatedState.value) {
            listOf(illustId) + relatedState.value
        }
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { idList.size })
        val currentDisplayedId = idList.getOrElse(pagerState.currentPage) { illustId }

        LaunchedEffect(sharedBoundsRegistry, illustId, currentDisplayedId) {
            sharedBoundsRegistry.updateDisplayedIllustId(
                originIllustId = illustId,
                displayedIllustId = currentDisplayedId,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { idList[it] },
        ) { page ->
            CouiIllustDetailSingle(
                illustId = idList[page],
                isCurrentPage = pagerState.currentPage == page,
                onBack = onBack,
                onUserClick = onUserClick,
                onCommentsClick = onCommentsClick,
                onRelatedIllustsClick = onRelatedIllustsClick,
                onIllustSeriesClick = onIllustSeriesClick,
                onTagClick = onTagClick,
                onIllustClick = onIllustClick,
                onNovelClick = onNovelClick,
                settings = settings,
            )
        }
    } else {
        LaunchedEffect(sharedBoundsRegistry, illustId) {
            sharedBoundsRegistry.updateDisplayedIllustId(
                originIllustId = illustId,
                displayedIllustId = illustId,
            )
        }

        CouiIllustDetailSingle(
            illustId = illustId,
            isCurrentPage = true,
            onBack = onBack,
            onUserClick = onUserClick,
            onCommentsClick = onCommentsClick,
            onRelatedIllustsClick = onRelatedIllustsClick,
            onIllustSeriesClick = onIllustSeriesClick,
            onTagClick = onTagClick,
            onIllustClick = onIllustClick,
            onNovelClick = onNovelClick,
            settings = settings,
        )
    }
}

@Composable
private fun CouiIllustDetailSingle(
    illustId: Long,
    isCurrentPage: Boolean,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)?,
    onNovelClick: ((Long) -> Unit)?,
    settings: SettingsRepository,
) {
    val strings = LocalStrings.current
    val repository = LocalIllustRepository.current
    val bookmarkRepository = LocalBookmarkRepository.current
    val downloadRepository = LocalDownloadRepository.current
    val banRepository = LocalBanRepository.current
    val historyRepository = LocalHistoryRepository.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val (sessionAccount, _) = rememberAccountSession()
    val currentUserId = sessionAccount?.userId?.toLongOrNull()

    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    val cachedIllust = remember(illustId) { repository.getCachedIllust(illustId) }
    val state = produceState<Result<Illust>?>(
        initialValue = cachedIllust?.let { Result.success(it) },
        illustId,
        repository,
        retryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.getIllustDetail(illustId) }
    }
    val result = state.value
    val illust = result?.getOrNull()

    LaunchedEffect(illust, isCurrentPage) {
        if (illust != null && isCurrentPage) {
            suspendRunCatchingNonCancel { historyRepository.insert(illust) }
        }
    }

    val bookmarkState = rememberSyncedDetailToggle(key = illustId, remoteValue = illust?.isBookmarked)
    val isBookmarked = bookmarkState.value
    var isBookmarkLoading by remember { mutableStateOf(false) }
    var bookmarkError by rememberSaveable { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val followState = rememberSyncedDetailToggle(key = illustId, remoteValue = illust?.user?.isFollowed)
    val isFollowed = followState.value
    var isFollowLoading by remember { mutableStateOf(false) }
    var isBanned by rememberSaveable(illustId) { mutableStateOf(false) }
    var isTempView by rememberSaveable(illustId) { mutableStateOf(false) }
    var fullScreenPageIndex by rememberSaveable(illustId) { mutableStateOf<Int?>(null) }

    val singleListState = rememberLazyListState()
    val wideImageListState = rememberLazyListState()
    val wideInfoListState = rememberLazyListState()

    val bookmarkHeartScale = remember { Animatable(1f) }
    LaunchedEffect(isBookmarked) {
        if (isBookmarked) {
            bookmarkHeartScale.animateTo(
                targetValue = 1.28f,
                animationSpec = spring(dampingRatio = 0.35f, stiffness = 700f),
            )
            bookmarkHeartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            )
        }
    }

    LaunchedEffect(illustId) {
        suspendRunCatchingNonCancel { banRepository.isBanIllust(illustId) }
            .onSuccess { isBanned = it }
    }

    val changeVersion = settings.changeVersion.collectAsState().value
    val isRestricted = remember(illust, isBanned, settings.hIsNotAllow, changeVersion) {
        isBanned || (settings.hIsNotAllow == true && illust?.isR18() == true)
    }

    val sharedBoundsRegistry = LocalSharedBoundsRegistry.current
    val isCardTransitionActive = sharedBoundsRegistry.activeTransitionIllustId != null
    val detailChromeAlpha = if (isCardTransitionActive) {
        cardExpandDetailChromeAlpha(sharedBoundsRegistry.activeTransitionExpansion)
    } else {
        1f
    }

    val actions = rememberIllustDetailActions(
        illust = illust,
        isBookmarked = isBookmarked,
        isDownloading = isDownloading,
        settings = settings,
        strings = strings,
        coroutineScope = coroutineScope,
        repository = repository,
        bookmarkRepository = bookmarkRepository,
        downloadRepository = downloadRepository,
        onBookmarkedChange = bookmarkState.setLocal,
        onBookmarkLoadingChange = { isBookmarkLoading = it },
        onBookmarkErrorChange = { bookmarkError = it },
        onDownloadingChange = { isDownloading = it },
        onToast = { data -> data?.let(toastSink) },
    )

    val onFollowToggle: () -> Unit = {
        val targetUser = illust?.user
        if (targetUser != null && !isFollowLoading) {
            coroutineScope.launch {
                try {
                    isFollowLoading = true
                    val wasFollowed = isFollowed
                    suspendRunCatchingNonCancel {
                        if (wasFollowed) {
                            bookmarkRepository.unfollowUser(targetUser.id)
                        } else {
                            bookmarkRepository.followUser(targetUser.id)
                        }
                    }.onSuccess {
                        performHapticFeedback(HapticType.Confirm)
                        followState.setLocal(!wasFollowed)
                    }.onFailure { error ->
                        performHapticFeedback(HapticType.Reject)
                        toastSink(
                            ToastData(
                                "${strings.follow}: ${PixivApiErrorFormatter.format(error, strings)}",
                                ToastType.Error,
                            ),
                        )
                    }
                } finally {
                    isFollowLoading = false
                }
            }
        }
    }

    val onShareIllust: () -> Unit = {
        illust?.let { target ->
            runClipboardShare(
                success = ToastData(strings.share, ToastType.Success),
                failurePrefix = strings.share,
                onToast = toastSink,
            ) {
                share.share(buildIllustShareLink(target), target.title)
            }
        }
    }

    val onCopyIllustId: () -> Unit = {
        illust?.let { target ->
            runClipboardShare(
                success = ToastData(strings.copiedToClipboard, ToastType.Success),
                failurePrefix = strings.copy,
                onToast = toastSink,
            ) {
                clipboard.copy(target.id.toString())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CouiCompactScaffold(
            title = illust?.title.orEmpty(),
            onBack = onBack,
            topBarModifier = Modifier.graphicsLayer { alpha = detailChromeAlpha },
            actions = {
                CouiDetailTopBarActions(
                    illust = illust,
                    isBookmarked = isBookmarked,
                    isBookmarkLoading = isBookmarkLoading,
                    bookmarkHeartScale = bookmarkHeartScale.value,
                    isDownloading = isDownloading,
                    isBanned = isBanned,
                    strings = strings,
                    coroutineScope = coroutineScope,
                    banRepository = banRepository,
                    actions = actions,
                    onBanSuccess = { isBanned = true },
                    onToast = toastSink,
                )
            },
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
            ) {
                val isWideScreen = maxWidth >= AppConstants.Layout.WIDE_SCREEN_MIN_WIDTH_DP.dp
                when {
                    result == null -> CouiPlaceholder(
                        message = strings.loading,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )

                    illust != null -> {
                        if (isRestricted && !isTempView) {
                            val restrictionMessage = buildString {
                                if (isBanned) {
                                    append(strings.menuBanWork)
                                } else {
                                    append("R-18")
                                }
                                if (illust.title.isNotBlank()) {
                                    append("\n")
                                    append(illust.title)
                                }
                            }
                            CouiPlaceholder(
                                message = restrictionMessage,
                                actionText = strings.confirm,
                                onAction = { isTempView = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                            )
                        } else {
                            val illustAspectRatio = remember(illust.width, illust.height) {
                                if (illust.width > 0 && illust.height > 0) {
                                    (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.1f, 10.0f)
                                } else {
                                    null
                                }
                            }
                            val showFollowButton = shouldShowArtistFollowButton(illust.user.id, currentUserId)
                            val displayedBookmarks = resolveDisplayedBookmarkCount(
                                baseCount = illust.totalBookmarks,
                                initiallyBookmarked = illust.isBookmarked,
                                currentlyBookmarked = isBookmarked,
                            )
                            val chromeAlpha = if (singleListState.firstVisibleItemIndex == 0) detailChromeAlpha else 1f

                            val imageItems: LazyListScope.() -> Unit = {
                                if (illust.metaPages.isNotEmpty()) {
                                    items(
                                        count = illust.metaPages.size,
                                        key = { "coui_page_$it" },
                                        contentType = { "coui_meta_page" },
                                    ) { pageIndex ->
                                        CouiDetailImagePage(
                                            illust = illust,
                                            pageIndex = pageIndex,
                                            illustAspectRatio = illustAspectRatio,
                                            settings = settings,
                                            downloadRepository = downloadRepository,
                                            coroutineScope = coroutineScope,
                                            strings = strings,
                                            onToast = { data -> data?.let(toastSink) },
                                            onPageClick = { fullScreenPageIndex = it },
                                        )
                                    }
                                } else {
                                    item(key = "coui_single_page", contentType = "coui_single_page") {
                                        CouiDetailSinglePageImage(
                                            illust = illust,
                                            illustAspectRatio = illustAspectRatio,
                                            settings = settings,
                                            repository = repository,
                                            onPageClick = { fullScreenPageIndex = it },
                                        )
                                    }
                                }
                            }

                            val detailInfoItems: LazyListScope.() -> Unit = {
                                item(key = "coui_info_card", contentType = "coui_info_card") {
                                    CouiDetailInfoCard(
                                        illust = illust,
                                        isBookmarked = isBookmarked,
                                        isBookmarkLoading = isBookmarkLoading,
                                        displayedBookmarks = displayedBookmarks,
                                        isDownloading = isDownloading,
                                        showFollowButton = showFollowButton,
                                        isFollowed = isFollowed,
                                        isFollowLoading = isFollowLoading,
                                        strings = strings,
                                        onBookmarkClick = actions::bookmark,
                                        onDownloadClick = {
                                            if (illust.pageCount > 1) actions.downloadAll() else actions.download()
                                        },
                                        onCommentsClick = { onCommentsClick(illust.id) },
                                        onShareClick = onShareIllust,
                                        onCopyIdClick = onCopyIllustId,
                                        onUserClick = onUserClick,
                                        onFollowClick = onFollowToggle,
                                        onIllustSeriesClick = onIllustSeriesClick,
                                        onDownloadAll = actions::downloadAll,
                                        modifier = Modifier.graphicsLayer { alpha = chromeAlpha },
                                    )
                                }

                                if (illust.caption.isNotBlank()) {
                                    item(key = "coui_caption", contentType = "coui_caption") {
                                        val scheme = CouiTheme.colorScheme
                                        CouiDetailSectionCard(
                                            title = strings.searchTargetTitleCaption,
                                            modifier = Modifier.graphicsLayer { alpha = chromeAlpha },
                                        ) {
                                            HtmlCaptionText(
                                                html = illust.caption,
                                                onUserClick = onUserClick,
                                                onIllustClick = onIllustClick,
                                                onIllustSeriesClick = onIllustSeriesClick,
                                                onNovelClick = onNovelClick,
                                                onTagClick = onTagClick,
                                                onLinkClick = { url ->
                                                    openSafeUrl(url, strings, onError = { toastSink(ToastData(it, ToastType.Error)) })
                                                },
                                                style = LocalCouiTextStyle.current.copy(lineHeight = 21.sp),
                                                color = scheme.onSurface,
                                                linkColor = scheme.accent,
                                                toggleStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                            )
                                        }
                                    }
                                }

                                if (illust.tags.isNotEmpty()) {
                                    item(key = "coui_tags", contentType = "coui_tags") {
                                        CouiDetailSectionCard(
                                            title = strings.tags,
                                            modifier = Modifier.graphicsLayer { alpha = chromeAlpha },
                                        ) {
                                            CouiDetailTagFlow(tags = illust.tags, onTagClick = onTagClick)
                                        }
                                    }
                                }

                                item(key = "coui_interaction", contentType = "coui_interaction") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CouiCard(
                                        modifier = Modifier
                                            .padding(horizontal = CouiPageDefaults.horizontalMargin)
                                            .graphicsLayer { alpha = chromeAlpha },
                                    ) {
                                        CouiValueRow(
                                            title = strings.commentsTitle,
                                            value = "${illust.totalComments ?: 0}",
                                            onClick = { onCommentsClick(illust.id) },
                                        )
                                        CouiRowDivider()
                                        CouiValueRow(
                                            title = strings.relatedIllusts,
                                            onClick = { onRelatedIllustsClick(illust.id) },
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }

                            if (isWideScreen) {
                                // 宽屏双栏：左栏专注画作浏览，右栏承载信息、快捷操作、简介、标签与互动，各自独立滚动。
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .consumeWindowInsets(padding),
                                ) {
                                    LazyColumn(
                                        state = wideImageListState,
                                        modifier = Modifier
                                            .weight(0.56f)
                                            .fillMaxHeight(),
                                        contentPadding = PaddingValues(
                                            start = CouiPageDefaults.horizontalMargin,
                                            top = padding.calculateTopPadding() + 8.dp,
                                            end = 8.dp,
                                            bottom = padding.calculateBottomPadding() + 24.dp,
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        overscrollEffect = rememberCouiOverscrollEffect(),
                                    ) {
                                        imageItems()
                                    }
                                    LazyColumn(
                                        state = wideInfoListState,
                                        modifier = Modifier
                                            .weight(0.44f)
                                            .fillMaxHeight(),
                                        contentPadding = PaddingValues(
                                            top = padding.calculateTopPadding(),
                                            bottom = padding.calculateBottomPadding() + 16.dp,
                                        ),
                                        overscrollEffect = rememberCouiOverscrollEffect(),
                                    ) {
                                        detailInfoItems()
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = singleListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .consumeWindowInsets(padding),
                                    contentPadding = PaddingValues(
                                        top = padding.calculateTopPadding(),
                                        bottom = padding.calculateBottomPadding(),
                                    ),
                                    overscrollEffect = rememberCouiOverscrollEffect(),
                                ) {
                                    imageItems()
                                    detailInfoItems()
                                }
                            }
                        }
                    }

                    else -> CouiPlaceholder(
                        message = CouiErrorMessage(result.exceptionOrNull()),
                        actionText = strings.retry,
                        onAction = { retryCount++ },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }

        val fullScreenPage = fullScreenPageIndex
        if (fullScreenPage != null && illust != null) {
            CouiIllustFullScreenOverlay(
                illust = illust,
                pageIndex = fullScreenPage,
                settings = settings,
                repository = repository,
                downloadRepository = downloadRepository,
                onToast = toastSink,
                onDismiss = { fullScreenPageIndex = null },
            )
        }

        LaunchedEffect(bookmarkError) {
            bookmarkError?.let {
                toastSink(ToastData(it, ToastType.Error))
                bookmarkError = null
            }
        }
    }
}

/** COUI 详情页顶栏动作：收藏（含弹性缩放与高亮着色）、下载、更多菜单。 */
@Composable
private fun CouiDetailTopBarActions(
    illust: Illust?,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    bookmarkHeartScale: Float,
    isDownloading: Boolean,
    isBanned: Boolean,
    strings: AppStrings,
    coroutineScope: CoroutineScope,
    banRepository: BanRepository,
    actions: IllustDetailActions,
    onBanSuccess: () -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val menuAnchor = rememberCouiMenuAnchor()
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    CouiTopBarIconButton(
        onClick = actions::bookmark,
        icon = if (isBookmarked) CouiIcons.HeartFill else CouiIcons.Heart,
        contentDescription = if (isBookmarked) strings.bookmarked else strings.bookmark,
        enabled = !isBookmarkLoading && illust != null,
        tint = if (isBookmarked) CouiBookmarkActiveColor else null,
        modifier = Modifier.graphicsLayer {
            scaleX = bookmarkHeartScale
            scaleY = bookmarkHeartScale
        },
    )
    CouiTopBarIconButton(
        onClick = actions::download,
        icon = CouiIcons.Download,
        contentDescription = strings.download,
        enabled = !isDownloading && illust != null,
    )
    if (illust != null) {
        Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
            CouiTopBarIconButton(
                onClick = { menuAnchor.open { menuExpanded = true } },
                icon = CouiIcons.More,
                contentDescription = strings.menuMoreActions,
            )
            CouiDetailMoreMenu(
                illust = illust,
                expanded = menuExpanded,
                anchor = menuAnchor,
                isBanned = isBanned,
                strings = strings,
                coroutineScope = coroutineScope,
                banRepository = banRepository,
                onDismiss = { menuExpanded = false },
                onBanSuccess = onBanSuccess,
                onToast = onToast,
            )
        }
    }
}

/** COUI 更多操作菜单：动作编排统一取自共享构建器 buildIllustDetailMoreMenuEntries。 */
@Composable
private fun CouiDetailMoreMenu(
    illust: Illust,
    expanded: Boolean,
    anchor: CouiMenuAnchor,
    isBanned: Boolean,
    strings: AppStrings,
    coroutineScope: CoroutineScope,
    banRepository: BanRepository,
    onDismiss: () -> Unit,
    onBanSuccess: () -> Unit,
    onToast: (ToastData) -> Unit,
) {
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val context = LocalPlatformContext.current
    val entries = buildIllustDetailMoreMenuEntries(
        strings = strings,
        illust = illust,
        isBanned = isBanned,
        banRepository = banRepository,
        onBanSuccess = onBanSuccess,
        onToast = onToast,
        clipboard = clipboard,
        share = share,
        context = context,
        coroutineScope = coroutineScope,
    )
    val flatItems = entries.flatMap { it.items }
    val items = buildList {
        var index = 0
        entries.forEachIndexed { entryIndex, entry ->
            entry.items.forEachIndexed { itemIndex, item ->
                add(
                    CouiMenuItem(
                        value = index.toString(),
                        text = item.text,
                        dividerBefore = itemIndex == 0 && entryIndex > 0,
                    ),
                )
                index += 1
            }
        }
    }
    Menu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        anchor = anchor,
        headerText = strings.menuMoreActions,
        items = items,
        onItemClick = { item ->
            onDismiss()
            flatItems.getOrNull(items.indexOfFirst { it.value == item.value })?.onClick?.invoke()
        },
    )
}

/** COUI 详情页多页作品单张图片：带加载占位底色、右上角页码角标与右下角单页下载浮钮。 */
@Composable
private fun CouiDetailImagePage(
    illust: Illust,
    pageIndex: Int,
    illustAspectRatio: Float?,
    settings: SettingsRepository,
    downloadRepository: DownloadRepository,
    coroutineScope: CoroutineScope,
    strings: AppStrings,
    onToast: (ToastData?) -> Unit,
    onPageClick: (Int) -> Unit,
) {
    val page = illust.metaPages[pageIndex]
    val changeVersion = settings.changeVersion.collectAsState().value
    val effectiveQuality = remember(illust.type, settings.pictureQuality, settings.mangaQuality, changeVersion) {
        resolveCouiIllustQuality(illust, settings)
    }
    val rawPageUrl = remember(page, effectiveQuality) {
        when (effectiveQuality) {
            0 -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
            1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
            2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
            else -> page.imageUrls?.large.orEmpty().ifEmpty { page.imageUrls?.original.orEmpty() }
        }
    }
    val pageUrl = rememberOptimizedImageModel(
        illust = illust,
        pageIndex = pageIndex,
        targetUrl = rawPageUrl,
        originalUrl = page.imageUrls?.original,
        customBasePath = settings.storePath,
        pictureSource = settings.pictureSource,
    )
    val thumbnailUrl = remember(page, pageIndex, illust, settings.feedPreviewQuality, changeVersion) {
        if (pageIndex == 0) {
            resolveIllustCoverUrl(illust.imageUrls, settings.feedPreviewQuality, allowSquare = false)
        } else {
            page.imageUrls?.medium ?: page.imageUrls?.squareMedium ?: illust.imageUrls.medium
        }
    }
    var pageLoaded by remember(pageIndex) { mutableStateOf(false) }
    val downloadInteraction = remember { MutableInteractionSource() }
    val imageModifier = Modifier
        .fillMaxWidth()
        .background(CouiTheme.colorScheme.container)
        .then(
            if ((pageIndex == 0 || !pageLoaded) && illustAspectRatio != null) {
                Modifier.aspectRatio(illustAspectRatio)
            } else {
                Modifier
            },
        )
        .illustDragAndDropSource(illust, pageIndex = pageIndex)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { onPageClick(pageIndex) }

    Box(modifier = Modifier.fillMaxWidth()) {
        PixivAsyncImage(
            model = pageUrl,
            thumbnailUrl = thumbnailUrl,
            contentDescription = "${illust.title} ($pageIndex)",
            contentScale = ContentScale.FillWidth,
            modifier = imageModifier,
            onSuccess = { pageLoaded = true },
        )

        // 右上角轻量页码角标（多页作品每页清晰标示当前进度）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(CouiCapsuleShape)
                .background(Color.Black.copy(alpha = 0.52f))
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            BasicText(
                text = "${pageIndex + 1} / ${illust.metaPages.size}",
                style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }

        // 右下角单页下载按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .accessibleTouchTarget(48.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.56f))
                .couiButtonFeedback(dark = true, interactionSource = downloadInteraction, list = false)
                .clickable(
                    interactionSource = downloadInteraction,
                    indication = null,
                    role = Role.Button,
                ) {
                    coroutineScope.launch {
                        val pageNumber = pageIndex + 1
                        onToast(ToastData("${strings.downloadStatusDownloading} P$pageNumber…", ToastType.Normal))
                        val task = downloadRepository.download(illust, pageIndex = pageIndex)
                        onToast(
                            when (task.status) {
                                DownloadStatus.Success -> ToastData("${strings.downloadStatusSuccess} (P$pageNumber)", ToastType.Success)
                                DownloadStatus.Failed -> ToastData("${strings.downloadStatusFailed}: ${task.error.toLocalizedDownloadError(strings)}", ToastType.Error)
                                else -> null
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = rememberVectorPainter(CouiIcons.Download),
                contentDescription = "${strings.download} P${pageIndex + 1}",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(18.dp),
            )
        }
    }
    if (pageIndex < illust.metaPages.lastIndex) {
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** COUI 详情页单页图片：Ugoira 播放器或普通单图。 */
@Composable
private fun CouiDetailSinglePageImage(
    illust: Illust,
    illustAspectRatio: Float?,
    settings: SettingsRepository,
    repository: IllustRepository,
    onPageClick: (Int) -> Unit,
) {
    if (IllustType.isUgoira(illust.type)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.container),
            contentAlignment = Alignment.TopCenter,
        ) {
            UgoiraPlayer(
                illust = illust,
                illustRepository = repository,
                onClick = { onPageClick(0) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        val changeVersion = settings.changeVersion.collectAsState().value
        val effectiveQuality = remember(illust.type, settings.pictureQuality, settings.mangaQuality, changeVersion) {
            resolveCouiIllustQuality(illust, settings)
        }
        val rawSingleUrl = remember(illust, effectiveQuality) {
            when (effectiveQuality) {
                0 -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
                1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                2 -> illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                else -> illust.imageUrls.large.ifEmpty { illust.metaSinglePage?.originalImageUrl.orEmpty() }
            }
        }
        val singleUrl = rememberOptimizedImageModel(
            illust = illust,
            pageIndex = 0,
            targetUrl = rawSingleUrl,
            originalUrl = illust.metaSinglePage?.originalImageUrl,
            customBasePath = settings.storePath,
            pictureSource = settings.pictureSource,
        )
        val thumbnailUrl = remember(illust, settings.feedPreviewQuality, changeVersion) {
            resolveIllustCoverUrl(illust.imageUrls, settings.feedPreviewQuality, allowSquare = false)
        }
        val imageModifier = Modifier
            .fillMaxWidth()
            .background(CouiTheme.colorScheme.container)
            .then(
                if (illustAspectRatio != null) {
                    Modifier.aspectRatio(illustAspectRatio)
                } else {
                    Modifier
                },
            )
            .illustDragAndDropSource(illust, pageIndex = 0)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onPageClick(0) }

        PixivAsyncImage(
            model = singleUrl,
            thumbnailUrl = thumbnailUrl,
            contentDescription = illust.title,
            contentScale = ContentScale.FillWidth,
            modifier = imageModifier,
        )
    }
}

/** 信息卡：属性徽章、大标题、指标、一键复制 ID、四合一快捷操作条、作者关注行、多页下载全部与系列入口。 */
@Composable
private fun CouiDetailInfoCard(
    illust: Illust,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    displayedBookmarks: Int,
    isDownloading: Boolean,
    showFollowButton: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    strings: AppStrings,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyIdClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    onFollowClick: () -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onDownloadAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val badges = remember(illust) { resolveCouiIllustBadges(illust) }
    val idInteraction = remember { MutableInteractionSource() }

    Spacer(modifier = Modifier.height(12.dp))
    CouiCard(modifier = modifier.padding(horizontal = CouiPageDefaults.horizontalMargin)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (badges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    badges.forEach { badge ->
                        CouiDetailBadgePill(badge = badge)
                    }
                }
            }

            BasicText(
                text = illust.title,
                style = TextStyle(
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiDetailMetric(icon = CouiIcons.Show, text = illust.totalView.toString())
                CouiDetailMetric(icon = CouiIcons.Heart, text = displayedBookmarks.toString())
                CouiDetailMetric(icon = CouiIcons.Recent, text = formatPixivDate(illust.createDate))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 作品 ID 与尺寸：点击一键复制 ID，减少进入二级菜单的步骤。
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .couiPressFeedback(idInteraction, list = false)
                    .clickable(
                        interactionSource = idInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onCopyIdClick,
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "ID: ${illust.id}  ·  ${illust.width}×${illust.height}",
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                )
                Image(
                    painter = rememberVectorPainter(CouiIcons.Copy),
                    contentDescription = strings.copy,
                    colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                    modifier = Modifier.size(13.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 四合一快捷操作栏（收藏 / 下载 / 评论 / 分享）
            CouiDetailQuickActionsBar(
                isBookmarked = isBookmarked,
                isBookmarkLoading = isBookmarkLoading,
                displayedBookmarks = displayedBookmarks,
                isDownloading = isDownloading,
                commentCount = illust.totalComments ?: 0,
                strings = strings,
                onBookmarkClick = onBookmarkClick,
                onDownloadClick = onDownloadClick,
                onCommentsClick = onCommentsClick,
                onShareClick = onShareClick,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 作者行（含快捷关注/已关注按钮）
            CouiDetailArtistRow(
                illust = illust,
                showFollowButton = showFollowButton,
                isFollowed = isFollowed,
                isFollowLoading = isFollowLoading,
                strings = strings,
                onUserClick = onUserClick,
                onFollowClick = onFollowClick,
            )
        }

        // 多页下载全部与系列入口位于卡片直属槽内，避免与上方 16dp padding 叠加导致双重缩进。
        if (illust.pageCount > 1) {
            CouiRowDivider()
            CouiValueRow(
                title = strings.downloadTaskFilterAll,
                value = "${illust.pageCount} P",
                enabled = !isDownloading,
                onClick = {
                    if (!isDownloading) onDownloadAll()
                },
            )
        }
        illust.series?.let { series ->
            CouiRowDivider()
            CouiValueRow(
                title = series.title.orEmpty(),
                value = strings.novelSeries,
                onClick = { onIllustSeriesClick(series.id) },
            )
        }
    }
}

@Composable
private fun CouiDetailBadgePill(badge: CouiIllustBadge) {
    val scheme = CouiTheme.colorScheme
    val destructive = CouiDialogDefaults.colors(CouiTheme.isDark).destructive
    val (bg, fg) = when (badge.kind) {
        CouiIllustBadgeKind.R18 -> destructive.copy(alpha = 0.14f) to destructive
        CouiIllustBadgeKind.AI -> scheme.accent.copy(alpha = 0.14f) to scheme.accent
        CouiIllustBadgeKind.UGOIRA -> scheme.accent.copy(alpha = 0.14f) to scheme.accent
        CouiIllustBadgeKind.MULTI_PAGE -> scheme.container to scheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(CouiCapsuleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        BasicText(
            text = badge.label,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg),
        )
    }
}

/** 四合一快捷操作条：高频动作收进拇指热区。 */
@Composable
private fun CouiDetailQuickActionsBar(
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    displayedBookmarks: Int,
    isDownloading: Boolean,
    commentCount: Int,
    strings: AppStrings,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CouiQuickActionTile(
            icon = if (isBookmarked) CouiIcons.HeartFill else CouiIcons.Heart,
            label = if (displayedBookmarks > 0) displayedBookmarks.toString() else strings.bookmark,
            active = isBookmarked,
            activeColor = CouiBookmarkActiveColor,
            enabled = !isBookmarkLoading,
            onClick = onBookmarkClick,
            modifier = Modifier.weight(1f),
        )
        CouiQuickActionTile(
            icon = CouiIcons.Download,
            label = strings.download,
            active = false,
            enabled = !isDownloading,
            onClick = onDownloadClick,
            modifier = Modifier.weight(1f),
        )
        CouiQuickActionTile(
            icon = CouiIcons.Messages,
            label = if (commentCount > 0) commentCount.toString() else strings.comments,
            active = false,
            enabled = true,
            onClick = onCommentsClick,
            modifier = Modifier.weight(1f),
        )
        CouiQuickActionTile(
            icon = CouiIcons.Share,
            label = strings.share,
            active = false,
            enabled = true,
            onClick = onShareClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CouiQuickActionTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = CouiTheme.colorScheme.accent,
) {
    val scheme = CouiTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val bgColor = if (active) activeColor.copy(alpha = 0.14f) else scheme.container
    val tintColor = when {
        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.45f)
        active -> activeColor
        else -> scheme.onSurface
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .couiPressFeedback(interaction, enabled = enabled, list = false)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(tintColor),
            modifier = Modifier.size(20.dp),
        )
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) activeColor else scheme.onSurfaceVariant,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CouiDetailMetric(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(CouiTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CouiDetailArtistRow(
    illust: Illust,
    showFollowButton: Boolean,
    isFollowed: Boolean,
    isFollowLoading: Boolean,
    strings: AppStrings,
    onUserClick: (Long) -> Unit,
    onFollowClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val rowInteraction = remember { MutableInteractionSource() }
    val followInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.container)
            .couiPressFeedback(rowInteraction)
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                role = Role.Button,
            ) { onUserClick(illust.user.id) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixivAsyncImage(
            model = illust.user.profileImageUrls.medium,
            contentDescription = illust.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = illust.user.name,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${illust.user.account}",
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showFollowButton) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CouiCapsuleShape)
                    .background(if (isFollowed) scheme.card else scheme.accent)
                    .couiPressFeedback(followInteraction, enabled = !isFollowLoading, list = false)
                    .clickable(
                        interactionSource = followInteraction,
                        indication = null,
                        enabled = !isFollowLoading,
                        role = Role.Button,
                        onClick = onFollowClick,
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isFollowed) strings.followed else strings.follow,
                    kind = CouiTextKind.ItemSummary,
                    color = if (isFollowed) scheme.onSurfaceVariant else scheme.onAccent,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 通用分区卡片：标题 + 内容，统一对齐 CouiPageDefaults.horizontalMargin。 */
@Composable
private fun CouiDetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    CouiCard(modifier = modifier.padding(horizontal = CouiPageDefaults.horizontalMargin)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiDetailTagFlow(
    tags: List<IllustTag>,
    onTagClick: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        tags.forEach { tag ->
            val tagText = buildString {
                append("#")
                append(tag.name)
                if (!tag.translatedName.isNullOrBlank() && tag.translatedName != tag.name) {
                    append(" ")
                    append(tag.translatedName)
                }
            }
            CouiAssistChip(
                text = tagText,
                selected = false,
                onClick = { onTagClick(tag.name) },
            )
        }
    }
}

/** 与 MIUIX 详情页同一套画质档位解析：漫画走独立档，其它走通用图片档。 */
private fun resolveCouiIllustQuality(illust: Illust, settings: SettingsRepository?): Int =
    if (illust.type == IllustType.MANGA) {
        settings?.mangaQuality ?: settings?.pictureQuality ?: 0
    } else {
        settings?.pictureQuality ?: 0
    }
