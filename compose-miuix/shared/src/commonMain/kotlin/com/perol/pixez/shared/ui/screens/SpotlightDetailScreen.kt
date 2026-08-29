package com.perol.pixez.shared.ui.screens

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AmWork
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.SpotlightDetail
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * Pixivision Spotlight 原生特辑阅读详情页。
 * 采用 MIUIX 标准 TopAppBar 与可折叠滑动标题栏，
 * 支持普通插画特辑与大合辑（Feature Collection）嵌套浏览。
 */
@Composable
fun SpotlightDetailScreen(
    article: SpotlightArticle,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    onArticleClick: (SpotlightArticle) -> Unit,
    repository: IllustRepository,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var retryKey by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    // 使用已有的内存缓存初始化，避免从画作详情返回时触发页面闪烁与重新请求
    val state = produceState<Result<SpotlightDetail>?>(
        initialValue = repository.getCachedSpotlightDetail(article.articleUrl)?.let { Result.success(it) },
        article.articleUrl,
        retryKey,
    ) {
        val result = suspendRunCatchingNonCancel {
            repository.getSpotlightArticleDetail(article.articleUrl, forceRefresh = isManualRefreshing)
        }
        isManualRefreshing = false
        value = result
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryKey++
    }

    val settings = LocalSettingsRepository.current
    val effectiveColumns = remember(settings?.crossAdapt, settings?.crossAdapterWidth, settings?.crossCount, settings?.changeVersion) {
        if (settings?.crossAdapt == true) {
            val minWidth = (settings.crossAdapterWidth * 1.2f).toInt().coerceIn(160, 600)
            StaggeredGridCells.Adaptive(minWidth.dp)
        } else {
            val configuredCols = settings?.crossCount ?: 2
            if (configuredCols == 2) {
                StaggeredGridCells.Adaptive(240.dp)
            } else {
                StaggeredGridCells.Fixed(configuredCols.coerceIn(1, 4))
            }
        }
    }


    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = article.pureTitle.ifBlank { article.title },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { openBrowser(article.articleUrl) }) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = strings.openInBrowser,
                        )
                    }
                    IconButton(onClick = triggerManualRefresh) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = strings.refresh,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val result = state.value
        when {
            result == null -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            result.isSuccess -> {
                val detail = result.getOrNull() ?: SpotlightDetail(
                    title = article.title,
                    pureTitle = article.pureTitle,
                    coverUrl = article.thumbnail,
                    rawUrl = article.articleUrl,
                )

                PullToRefresh(
                    isRefreshing = isManualRefreshing,
                    onRefresh = triggerManualRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding()),
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = effectiveColumns,
                        modifier = Modifier
                            .fillMaxSize()
                            
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = 48.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                    ) {
                        // 顶部文章封面与标题
                        item(span = StaggeredGridItemSpan.FullLine) {
                            ArticleHeaderCard(
                                article = article,
                                detail = detail,
                            )
                        }

                        // 导语简介
                        if (detail.description.isNotBlank()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Text(
                                            text = strings.spotlightLead,
                                            style = MiuixTheme.textStyles.title4,
                                            fontWeight = FontWeight.Bold,
                                            color = MiuixTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = detail.description,
                                            style = MiuixTheme.textStyles.body1,
                                            color = MiuixTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        // 特辑合集（子特辑列表）
                        if (detail.subArticles.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SmallTitle(
                                    text = "${strings.includedArticles} (${detail.subArticles.size})",
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            items(
                                items = detail.subArticles,
                                key = { "sub_${it.id}" },
                            ) { subArticle ->
                                SpotlightArticleCard(
                                    article = subArticle,
                                    onClick = { onArticleClick(subArticle) },
                                )
                            }
                        }

                        // 收录画作分区列表
                        if (detail.works.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SmallTitle(
                                    text = "${strings.includedWorks} (${detail.works.size})",
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            // 作品瀑布流卡片列表
                            items(
                                items = detail.works,
                                key = { it.arworkLink ?: it.showImage.orEmpty() },
                            ) { work ->
                                SpotlightWorkCard(
                                    work = work,
                                    onIllustClick = {
                                        work.illustId?.let(onIllustClick)
                                    },
                                    onUserClick = {
                                        work.userId?.let(onUserClick)
                                    },
                                )
                            }
                        }

                        // 底部在浏览器查看原文卡片
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openBrowser(article.articleUrl) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.viewOriginalArticle,
                                            style = MiuixTheme.textStyles.headline2,
                                            color = MiuixTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = article.articleUrl,
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = MiuixIcons.More,
                                        contentDescription = strings.openInBrowser,
                                        tint = MiuixTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ErrorPlaceholder(
                        error = result.exceptionOrNull(),
                        onRetry = triggerManualRefresh,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    Button(
                        onClick = { openBrowser(article.articleUrl) },
                        modifier = Modifier.padding(horizontal = 32.dp),
                    ) {
                        Text(strings.viewOriginalArticle)
                    }
                }
            }
        }
    }
}

/**
 * 特辑顶部封面大图与标题卡片。
 */
@Composable
private fun ArticleHeaderCard(
    article: SpotlightArticle,
    detail: SpotlightDetail,
    modifier: Modifier = Modifier,
) {
    val coverUrl = detail.coverUrl ?: article.thumbnail

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                PixivAsyncImage(
                    model = coverUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = article.pureTitle.ifBlank { detail.pureTitle.ifBlank { article.title } },
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                if (article.publishDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.publishDate.take(10),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

/**
 * 特辑收录的单个画作与画师卡片：直接点击卡片/图片进入作品详情，点击画师行进入画师主页。
 */
@Composable
private fun SpotlightWorkCard(
    work: AmWork,
    onIllustClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = work.illustId != null, onClick = onIllustClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 画师信息栏（点击可直接前往画师个人主页）
            if (work.user != null || work.userImage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = work.userId != null, onClick = onUserClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (work.userImage != null) {
                        PixivAsyncImage(
                            model = work.userImage,
                            contentDescription = work.user,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = work.user ?: strings.author,
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (work.userId != null) {
                        Text(
                            text = "›",
                            style = MiuixTheme.textStyles.headline2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            // 画作大图 / 预览图
            if (work.showImage != null) {
                PixivAsyncImage(
                    model = work.showImage,
                    contentDescription = work.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop,
                )
            }

            // 画作标题与插画 ID 说明
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!work.title.isNullOrBlank()) {
                    Text(
                        text = work.title,
                        style = MiuixTheme.textStyles.headline2,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (work.illustId != null) {
                    Text(
                        text = "ID: ${work.illustId}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
