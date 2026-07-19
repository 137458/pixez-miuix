package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
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
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
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
                    IconButton(onClick = { /* M4: 更多菜单 */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
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
}
}

@Composable
private fun IllustInfoSection(
    illust: Illust,
    onUserClick: (Int) -> Unit,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
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
