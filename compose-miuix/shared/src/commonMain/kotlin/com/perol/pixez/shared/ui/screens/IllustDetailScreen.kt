package com.perol.pixez.shared.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
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
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
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
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品详情页：通过 illustId 从 Repository 查询真实作品数据。
 */
@Composable
fun IllustDetailScreen(
    illustId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<Illust>?>(
        initialValue = null,
        illustId,
        repository,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getIllustDetail(illustId) }
    }

    val result = state.value
    val illust = result?.getOrNull()
    var isBookmarked by rememberSaveable(illust) { mutableStateOf(illust?.isBookmarked ?: false) }
    var isBookmarkLoading by rememberSaveable { mutableStateOf(false) }
    var bookmarkError by rememberSaveable { mutableStateOf<String?>(null) }
    var isDownloading by rememberSaveable { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showActionMenu by rememberSaveable { mutableStateOf(false) }
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = illust?.title ?: "作品详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isBookmarkLoading) return@IconButton
                            illust?.let {
                                coroutineScope.launch {
                                    try {
                                        isBookmarkLoading = true
                                        bookmarkError = null
                                        runCatchingNonCancel {
                                            if (isBookmarked) {
                                                bookmarkRepository.deleteBookmark(it.id)
                                            } else {
                                                bookmarkRepository.addBookmark(it.id)
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
                        enabled = !isBookmarkLoading,
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isBookmarked) "已收藏" else "收藏",
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isDownloading || illust == null) return@IconButton
                            coroutineScope.launch {
                                try {
                                    isDownloading = true
                                    toastMessage = "下载中…"
                                    // downloadRepository 已捕获保存/网络异常并返回 Failed 状态，
                                    // 此处只需在 finally 中重置加载态，取消时也能保证状态恢复。
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
                        enabled = !isDownloading && illust != null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "下载",
                        )
                    }
                    IconButton(
                        onClick = { showActionMenu = true },
                        enabled = illust != null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
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
            Column(modifier = Modifier.fillMaxSize()) {
                bookmarkError?.let { error ->
                    Text(
                        text = error,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                when {
                    result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    result.isSuccess && illust != null -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item {
                                PixivAsyncImage(
                                    model = illust.imageUrls.large,
                                    contentDescription = illust.title,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            item {
                                IllustInfoSection(
                                    illust = illust,
                                    onUserClick = onUserClick,
                                    onCommentsClick = onCommentsClick,
                                    onRelatedIllustsClick = onRelatedIllustsClick,
                                    onIllustSeriesClick = onIllustSeriesClick,
                                    onDownloadAllPagesClick = if (illust.pageCount > 1) {
                                        {
                                            if (isDownloading) return@IllustInfoSection
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
                                        }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.padding(16.dp),
                                )
                            }

                            item {
                                SmallTitle(
                                    text = "标签",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }

                            items(illust.tags, key = { it.name }) { tag ->
                                Text(
                                    text = tag.translatedName ?: tag.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MiuixTheme.textStyles.body1,
                                )
                            }
                        }
                    }
                    else -> ErrorPlaceholder(
                        error = result.exceptionOrNull(),
                        onRetry = { retryCount++ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
            )
        }

        illust?.let {
            IllustActionMenu(
                show = showActionMenu,
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
            )
        }
    }
}

@Composable
private fun IllustInfoSection(
    illust: Illust,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
    onDownloadAllPagesClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = illust.title,
            style = MiuixTheme.textStyles.title1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = illust.caption,
            style = MiuixTheme.textStyles.body2,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUserClick(illust.user.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixivAsyncImage(
                model = illust.user.profileImageUrls.medium,
                contentDescription = illust.user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = illust.user.name,
                    style = MiuixTheme.textStyles.body1,
                )
                Text(
                    text = "@${illust.user.account}",
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(label = "浏览", value = illust.totalView.toString())
            StatItem(label = "收藏", value = illust.totalBookmarks.toString())
            StatItem(
                label = "评论",
                value = (illust.totalComments ?: 0).toString(),
                onClick = { onCommentsClick(illust.id) },
            )
            StatItem(label = "页数", value = illust.pageCount.toString())
        }

        onDownloadAllPagesClick?.let { onClick ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "下载全部页",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "相关作品",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRelatedIllustsClick(illust.id) }
                .padding(vertical = 8.dp),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.primary,
        )

        illust.series?.let { series ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "系列：${series.title.orEmpty()}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIllustSeriesClick(series.id) }
                    .padding(vertical = 8.dp),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}
