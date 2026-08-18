package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.perol.pixez.shared.ui.AppConstants
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.components.liquidGlass
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustActionMenu
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.buildIllustCopyInfo
import com.perol.pixez.shared.ui.components.buildIllustShareLink
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.perol.pixez.shared.data.settings.LocalSettingsRepository

/**
 * 作品详情页：沉浸式大图展示、单一大标题、高对比度 MIUIX Card 容器与胶囊标签（Capsule Chips）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustDetailScreen(
    illustId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
    onTagClick: (String) -> Unit,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    banRepository: BanRepository,
) {
    val settings = LocalSettingsRepository.current

    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<Illust>?>(
        initialValue = null,
        illustId,
        repository,
        retryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.getIllustDetail(illustId) }
    }

    val result = state.value
    val illust = result?.getOrNull()
    var isBookmarked by rememberSaveable(illust) { mutableStateOf(illust?.isBookmarked ?: false) }
    var isBookmarkLoading by rememberSaveable { mutableStateOf(false) }
    var bookmarkError by rememberSaveable { mutableStateOf<String?>(null) }
    var isDownloading by rememberSaveable { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showActionMenu by rememberSaveable { mutableStateOf(false) }
    var isBanned by rememberSaveable(illustId) { mutableStateOf(false) }
    var isTempView by rememberSaveable(illustId) { mutableStateOf(false) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()

    // 页面进入或作品 ID 变化时，查询本地屏蔽记录；数据库异常时保持未屏蔽，避免崩溃。
    LaunchedEffect(illustId) {
        suspendRunCatchingNonCancel { banRepository.isBanIllust(illustId) }
            .onSuccess { isBanned = it }
    }

    val detailBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            result.isSuccess && illust != null -> when {
                isBanned && !isTempView -> BanPage(
                    name = illust.title,
                    onView = { isTempView = true },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(detailBackdrop),
                    ) {
                        // 1. 沉浸式顶部大图（从屏幕最顶端开始渲染，消除生硬的一刀切顶栏）
                        if (illust.metaPages.isNotEmpty()) {
                            items(
                                count = illust.metaPages.size,
                                key = { "page_$it" },
                            ) { pageIndex ->
                                val page = illust.metaPages[pageIndex]
                                val pageUrl = remember(page, settings?.pictureQuality, settings?.changeVersion) {
                                    when (settings?.pictureQuality ?: 0) {
                                        1 -> page.imageUrls?.original ?: page.imageUrls?.large.orEmpty()
                                        2 -> page.imageUrls?.medium ?: page.imageUrls?.large.orEmpty()
                                        else -> page.imageUrls?.large.orEmpty()
                                    }
                                }
                                PixivAsyncImage(
                                    model = pageUrl,
                                    contentDescription = "${illust.title} ($pageIndex)",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (pageIndex < illust.metaPages.lastIndex) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        } else {
                            item(key = "single_page") {
                                val singleUrl = remember(illust, settings?.pictureQuality, settings?.changeVersion) {
                                    when (settings?.pictureQuality ?: 0) {
                                        1 -> illust.metaSinglePage?.originalImageUrl ?: illust.imageUrls.large
                                        2 -> illust.imageUrls.medium
                                        else -> illust.imageUrls.large
                                    }
                                }
                                PixivAsyncImage(
                                    model = singleUrl,
                                    contentDescription = illust.title,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // 2. 作品信息与画师卡片（单一清晰大标题、数据指标、画师头像名称与下载全部/系列入口）
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                    .padding(horizontal = 12.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    // 唯一大标题
                                    Text(
                                        text = illust.title,
                                        style = MiuixTheme.textStyles.title2,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 浏览、收藏、日期指标
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Show,
                                                contentDescription = "浏览量",
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = illust.totalView.toString(),
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Favorites,
                                                contentDescription = "收藏量",
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = illust.totalBookmarks.toString(),
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Recent,
                                                contentDescription = "发布时间",
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = illust.createDate.take(10),
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ID: ${illust.id}   分辨率: ${illust.width}x${illust.height}",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 画师信息栏
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                            .clickable { onUserClick(illust.user.id) }
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
                                                style = MiuixTheme.textStyles.title4,
                                            )
                                            Text(
                                                text = "@${illust.user.account}",
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                        Icon(
                                            imageVector = MiuixIcons.Search,
                                            contentDescription = "查看画师主页",
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    // 多页作品下载全部入口
                                    if (illust.pageCount > 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        BasicComponent(
                                            title = "下载全部页",
                                            summary = "共 ${illust.pageCount} 页",
                                            onClick = {
                                                if (isDownloading) return@BasicComponent
                                                coroutineScope.launch {
                                                    try {
                                                        isDownloading = true
                                                        val tasks = downloadRepository.downloadAllPages(
                                                            illust,
                                                            onProgress = { completed, total ->
                                                                toastMessage = "下载中 $completed/$total"
                                                            },
                                                        )
                                                        val successCount = tasks.count { it.status == DownloadStatus.Success }
                                                        val failedCount = tasks.count { it.status == DownloadStatus.Failed }
                                                        toastMessage = when {
                                                             failedCount == 0 -> "全部下载成功: $successCount/${tasks.size}"
                                                             successCount == 0 -> "全部下载失败"
                                                             else -> "下载完成: 成功 $successCount, 失败 $failedCount"
                                                        }
                                                    } finally {
                                                        isDownloading = false
                                                    }
                                                }
                                            },
                                        )
                                    }

                                    // 系列入口
                                    illust.series?.let { series ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        BasicComponent(
                                            title = "所属系列",
                                            summary = series.title.orEmpty(),
                                            onClick = { onIllustSeriesClick(series.id) },
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 简介与文案卡片
                        if (illust.caption.isNotBlank()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                        .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                        .padding(horizontal = 12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Text(
                                            text = "简介",
                                            style = MiuixTheme.textStyles.title4,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = illust.caption,
                                            style = MiuixTheme.textStyles.body2,
                                        )
                                    }
                                }
                            }
                        }

                        // 4. 标签卡片与胶囊包裹（Capsule Chips）
                        if (illust.tags.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                        .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                        .padding(horizontal = 12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Text(
                                            text = "标签",
                                            style = MiuixTheme.textStyles.title4,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            illust.tags.forEach { tag ->
                                                TagCapsuleChip(
                                                    tag = tag,
                                                    onClick = { onTagClick(tag.name) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. 互动操作卡片（评论与相关作品）
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                                    .padding(horizontal = 12.dp),
                            ) {
                                BasicComponent(
                                    title = "查看评论",
                                    summary = "${illust.totalComments ?: 0} 条评论",
                                    onClick = { onCommentsClick(illust.id) },
                                )
                                BasicComponent(
                                    title = "相关作品",
                                    summary = "查看同类推荐与关联画作",
                                    onClick = { onRelatedIllustsClick(illust.id) },
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                    }
                }
            }
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 浮动悬浮顶部操作栏（半透明暗色胶囊/圆形按钮，平滑渐变，不遮挡大图，在任何明暗底色下清晰可见）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 返回按钮
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .liquidGlass(
                        backdrop = detailBackdrop,
                        shape = CircleShape,
                        blurRadius = 16.dp,
                        tintColor = Color.Black,
                        tintAlpha = 0.35f,
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                    tint = Color.White,
                )
            }

            // 右侧操作按钮组（收藏、下载、更多）
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 收藏按钮
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            backdrop = detailBackdrop,
                            shape = CircleShape,
                            blurRadius = 16.dp,
                            tintColor = Color.Black,
                            tintAlpha = 0.35f,
                        )
                        .clip(CircleShape)
                        .clickable(
                            enabled = !isBookmarkLoading && illust != null,
                            onClick = {
                                illust?.let {
                                    coroutineScope.launch {
                                        try {
                                            isBookmarkLoading = true
                                            bookmarkError = null
                                            suspendRunCatchingNonCancel {
                                                if (isBookmarked) {
                                                    bookmarkRepository.deleteBookmark(it.id)
                                                } else {
                                                    bookmarkRepository.addBookmark(
                                                        illustId = it.id,
                                                        isPrivate = settings?.defaultPrivateLike ?: false,
                                                    )
                                                }
                                            }.onSuccess {
                                                isBookmarked = !isBookmarked
                                            }.onFailure { e ->
                                                bookmarkError = e.message ?: "收藏操作失败"
                                            }
                                        } finally {
                                            isBookmarkLoading = false
                                        }
                                    }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                        contentDescription = if (isBookmarked) "已收藏" else "收藏",
                        tint = if (isBookmarked) Color(0xFFFF4D6A) else Color.White,
                    )
                }

                // 下载按钮
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            backdrop = detailBackdrop,
                            shape = CircleShape,
                            blurRadius = 16.dp,
                            tintColor = Color.Black,
                            tintAlpha = 0.35f,
                        )
                        .clip(CircleShape)
                        .clickable(
                            enabled = !isDownloading && illust != null,
                            onClick = {
                                if (isDownloading || illust == null) return@clickable
                                coroutineScope.launch {
                                    try {
                                        isDownloading = true
                                        toastMessage = "下载中…"
                                        val task = downloadRepository.download(illust, pageIndex = 0)
                                        toastMessage = when (task.status) {
                                            DownloadStatus.Success -> "下载成功"
                                            DownloadStatus.Failed -> "下载失败: ${task.error ?: "未知错误"}"
                                            else -> null
                                        }
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Download,
                        contentDescription = "下载",
                        tint = Color.White,
                    )
                }

                // 更多菜单按钮
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            backdrop = detailBackdrop,
                            shape = CircleShape,
                            blurRadius = 16.dp,
                            tintColor = Color.Black,
                            tintAlpha = 0.35f,
                        )
                        .clip(CircleShape)
                        .clickable(
                            enabled = illust != null,
                            onClick = { showActionMenu = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.More,
                        contentDescription = "更多",
                        tint = Color.White,
                    )
                }
            }
        }

        ToastMessage(
            message = toastMessage ?: bookmarkError,
            onDismiss = {
                toastMessage = null
                bookmarkError = null
            },
        )

        illust?.let {
            IllustActionMenu(
                show = showActionMenu,
                showBan = !isBanned,
                onDismissRequest = { showActionMenu = false },
                onCopyInfo = {
                    showActionMenu = false
                    val text = buildIllustCopyInfo(it)
                    runCatching { clipboard.copy(text) }.fold(
                        onSuccess = { toastMessage = "已复制到剪贴板" },
                        onFailure = { e -> toastMessage = "复制失败: ${e.message}" },
                    )
                },
                onCopyLink = {
                    showActionMenu = false
                    val link = buildIllustShareLink(it)
                    runCatching { clipboard.copy(link) }.fold(
                        onSuccess = { toastMessage = "链接已复制" },
                        onFailure = { e -> toastMessage = "复制失败: ${e.message}" },
                    )
                },
                onShareLink = {
                    showActionMenu = false
                    val link = buildIllustShareLink(it)
                    runCatching { share.share(link, it.title) }.fold(
                        onSuccess = { toastMessage = "已分享" },
                        onFailure = { e -> toastMessage = "分享失败: ${e.message}" },
                    )
                },
                onBan = {
                    showActionMenu = false
                    coroutineScope.launch {
                        suspendRunCatchingNonCancel {
                            banRepository.insertBanIllust(it.id, it.title)
                        }.fold(
                            onSuccess = {
                                isBanned = true
                                toastMessage = "已屏蔽"
                            },
                            onFailure = { e -> toastMessage = "屏蔽失败: ${e.message}" },
                        )
                    }
                },
            )
        }
    }
}

/**
 * 胶囊标签 Chip：圆角胶囊背景包裹，清晰展示标签与翻译名称。
 */
@Composable
private fun TagCapsuleChip(
    tag: IllustTag,
    onClick: () -> Unit,
) {
    val tagText = buildString {
        append("#")
        append(tag.name)
        if (!tag.translatedName.isNullOrBlank() && tag.translatedName != tag.name) {
            append(" ")
            append(tag.translatedName)
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tagText,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

/**
 * 屏蔽占位页。
 */
@Composable
private fun BanPage(
    name: String,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "作品\n$name\n",
            style = MiuixTheme.textStyles.title2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onView) {
            Text(text = "查看")
        }
    }
}
