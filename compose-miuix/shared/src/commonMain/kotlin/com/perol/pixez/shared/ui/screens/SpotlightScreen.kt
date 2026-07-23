package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.UserPreviewsResponse
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Spotlight 发现页：顶部展示推荐用户入口（RecomUserRoad），下方展示官方 Spotlight 文章列表。
 */
@Composable
fun SpotlightScreen(
    onUserClick: (Int) -> Unit,
    onRecomUserListClick: () -> Unit,
    repository: IllustRepository,
    userRepository: UserRepository,
    accountRepository: AccountRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    // 推荐用户接口需要登录态，未登录时不请求以避免 401。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(accountRepository) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        isLoggedIn = suspendRunCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    val state = produceState<Result<List<SpotlightArticle>>?>(
        initialValue = null,
        repository,
        retryCount,
    ) {
        value = suspendRunCatchingNonCancel { repository.getSpotlightArticles() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = "Spotlight")
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // 推荐用户横向入口仅在有登录态时展示，避免未登录请求 401。
            if (isLoggedIn == true) {
                RecomUserRoad(
                    onUserClick = onUserClick,
                    onMoreClick = onRecomUserListClick,
                    repository = userRepository,
                )
            }

            val result = state.value
            when {
                result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                result.isSuccess -> {
                    val articles = result.getOrNull().orEmpty()
                    if (articles.isEmpty()) {
                        EmptyPlaceholder(
                            message = "暂无 Spotlight 内容",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp,
                            ),
                            verticalItemSpacing = 12.dp,
                        ) {
                            items(
                                items = articles,
                                key = { it.id },
                            ) { article ->
                                SpotlightArticleCard(
                                    article = article,
                                    onClick = { openBrowser(article.articleUrl) },
                                )
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
        }
    }
}

/**
 * 推荐用户横向入口：展示前 10 位推荐用户头像与名称，点击头像进入用户详情，
 * 点击「更多」进入完整推荐用户列表。
 */
@Composable
private fun RecomUserRoad(
    onUserClick: (Int) -> Unit,
    onMoreClick: () -> Unit,
    repository: UserRepository,
) {
    val state = produceState<Result<UserPreviewsResponse>?>(
        initialValue = null,
        repository,
    ) {
        value = suspendRunCatchingNonCancel { repository.getRecommendedUsers() }
    }

    val result = state.value
    if (result?.isSuccess != true) return

    val previews = result.getOrNull()?.userPreviews?.take(RECOMMENDED_USER_ROAD_COUNT).orEmpty()
    if (previews.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SmallTitle(
            text = "推荐用户",
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = previews,
                key = { it.user.id },
            ) { preview ->
                RecomUserRoadItem(
                    preview = preview,
                    onClick = { onUserClick(preview.user.id) },
                )
            }

            item(key = "more") {
                TextButton(
                    text = "更多",
                    onClick = onMoreClick,
                )
            }
        }
    }
}

/**
 * 单个推荐用户条目：圆形头像 + 名称。
 */
@Composable
private fun RecomUserRoadItem(
    preview: UserPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PixivAsyncImage(
            model = preview.user.profileImageUrls.medium,
            contentDescription = preview.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
        )
        Text(
            text = preview.user.name,
            style = MiuixTheme.textStyles.footnote2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SpotlightArticleCard(
    article: SpotlightArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column {
            PixivAsyncImage(
                model = article.thumbnail,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
            Text(
                text = article.title,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

/**
 * 推荐用户 Road 单次展示数量。
 */
private const val RECOMMENDED_USER_ROAD_COUNT = 10
