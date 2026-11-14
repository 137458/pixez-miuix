package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AmWork
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.SpotlightDetail
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalPixivisionRepository
import com.perol.pixez.shared.ui.navigation.animation.illustTransitionBounds
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI Spotlight 特辑详情页：固定顶栏 + 阅读列。
 *
 * 数据加载、缓存初始化与手动刷新口径与 MIUIX 版一致；顶栏保留刷新与复制链接/分享/
 * 浏览器打开三个动作，正文按阅读列单列排布，作品卡片保留共享元素过渡。
 */
@Composable
internal fun CouiSpotlightDetailScreen(
    article: SpotlightArticle,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onArticleClick: (SpotlightArticle) -> Unit,
) {
    val repository = LocalPixivisionRepository.current
    val strings = LocalStrings.current
    var retryKey by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }
    val toastSink = LocalToastSink.current
    val showErrorToast: (String) -> Unit = { toastSink(ToastData(it, ToastType.Error)) }

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

    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }
    val menuAnchor = rememberCouiMenuAnchor()
    var menuExpanded by remember { mutableStateOf(false) }
    val scheme = CouiTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        CouiFixedTopBar(
            title = article.pureTitle.ifBlank { article.title },
            onBack = onBack,
            actionSpacing = 4.dp,
        ) {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = triggerManualRefresh,
            )
            Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                CouiActionButton(
                    icon = MiuixIcons.More,
                    contentDescription = strings.menuMoreActions,
                    onClick = { menuAnchor.open { menuExpanded = true } },
                )
                Menu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    anchor = menuAnchor,
                    items = listOf(
                        CouiMenuItem(value = "copy_link", text = strings.menuCopyLink),
                        CouiMenuItem(value = "share", text = strings.share),
                        CouiMenuItem(value = "browser", text = strings.openInBrowser),
                    ),
                    onItemClick = { item ->
                        menuExpanded = false
                        when (item.value) {
                            "copy_link" -> {
                                try {
                                    clipboard.copy(article.articleUrl)
                                    toastSink(ToastData(strings.copiedToClipboard, ToastType.Success))
                                } catch (_: Throwable) {
                                    toastSink(
                                        ToastData("${strings.copy}${strings.failedSuffix}", ToastType.Error),
                                    )
                                }
                            }
                            "share" -> {
                                try {
                                    share.share(article.articleUrl, article.pureTitle.ifBlank { article.title })
                                } catch (_: Throwable) {
                                    toastSink(
                                        ToastData("${strings.share}${strings.failedSuffix}", ToastType.Error),
                                    )
                                }
                            }
                            "browser" -> openSafeUrl(article.articleUrl, strings, onError = showErrorToast)
                        }
                    },
                )
            }
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            val result = state.value
            when {
                result == null -> CouiStatePlaceholder(message = strings.loading)

                result.isSuccess -> {
                    val detail = result.getOrNull() ?: SpotlightDetail(
                        title = article.title,
                        pureTitle = article.pureTitle,
                        coverUrl = article.thumbnail,
                        rawUrl = article.articleUrl,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = rememberCouiOverscrollEffect(),
                    ) {
                        item(key = "coui_spotlight_header", contentType = "header") {
                            CouiArticleHeader(article = article, detail = detail)
                        }

                        if (detail.description.isNotBlank()) {
                            item(key = "coui_spotlight_desc", contentType = "section") {
                                CouiCard {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = strings.spotlightLead,
                                            kind = CouiTextKind.ItemTitle,
                                            color = scheme.accent,
                                        )
                                        Text(
                                            text = detail.description,
                                            kind = CouiTextKind.ItemSummary,
                                            color = scheme.onSurface,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }
                        }

                        if (detail.subArticles.isNotEmpty()) {
                            item(key = "coui_spotlight_sub_header", contentType = "header") {
                                Text(
                                    text = "${strings.includedArticles} (${detail.subArticles.size})",
                                    kind = CouiTextKind.ItemTitle,
                                    color = scheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                )
                            }
                            items(
                                items = detail.subArticles,
                                key = { "sub_${it.id}" },
                                contentType = { "sub_article" },
                            ) { subArticle ->
                                CouiSpotlightArticleCard(
                                    article = subArticle,
                                    onClick = { onArticleClick(subArticle) },
                                )
                            }
                        }

                        if (detail.works.isNotEmpty()) {
                            item(key = "coui_spotlight_works_header", contentType = "header") {
                                Text(
                                    text = "${strings.includedWorks} (${detail.works.size})",
                                    kind = CouiTextKind.ItemTitle,
                                    color = scheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                )
                            }
                            items(
                                items = detail.works,
                                key = { it.arworkLink ?: it.showImage.orEmpty() },
                                contentType = { "work" },
                            ) { work ->
                                CouiSpotlightWorkCard(
                                    work = work,
                                    onIllustClick = { work.illustId?.let(onIllustClick) },
                                    onUserClick = { work.userId?.let(onUserClick) },
                                )
                            }
                        }

                        item(key = "coui_spotlight_browser", contentType = "section") {
                            CouiCard {
                                CouiNavigationRow(
                                    title = strings.viewOriginalArticle,
                                    summary = article.articleUrl,
                                    onClick = {
                                        openSafeUrl(article.articleUrl, strings, onError = showErrorToast)
                                    },
                                )
                            }
                        }
                    }
                }

                else -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CouiStatePlaceholder(
                        message = couiErrorMessage(result.exceptionOrNull()),
                        actionText = strings.retry,
                        onAction = triggerManualRefresh,
                        modifier = Modifier.weight(1f),
                    )
                    CouiTextAction(
                        text = strings.viewOriginalArticle,
                        onClick = {
                            openSafeUrl(article.articleUrl, strings, onError = showErrorToast)
                        },
                    )
                }
            }
        }
    }
}

/** 顶部封面与标题。 */
@Composable
private fun CouiArticleHeader(
    article: SpotlightArticle,
    detail: SpotlightDetail,
) {
    val scheme = CouiTheme.colorScheme
    CouiCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            PixivAsyncImage(
                model = detail.coverUrl ?: article.thumbnail,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = article.pureTitle.ifBlank { detail.pureTitle.ifBlank { article.title } },
                    kind = CouiTextKind.GroupTitle,
                    color = scheme.onSurface,
                )
                if (article.publishDate.isNotBlank()) {
                    Text(
                        text = article.publishDate.take(10),
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** 子特辑入口：缩略图 + 标题。 */
@Composable
private fun CouiSpotlightArticleCard(
    article: SpotlightArticle,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    CouiCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PixivAsyncImage(
                model = article.thumbnail,
                contentDescription = article.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = article.pureTitle.ifBlank { article.title },
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 收录画作卡片：画师行可进入画师主页，整卡进入作品详情。 */
@Composable
private fun CouiSpotlightWorkCard(
    work: AmWork,
    onIllustClick: () -> Unit,
    onUserClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    CouiCard(
        modifier = Modifier
            .illustTransitionBounds(
                illustId = work.illustId,
                cornerRadius = AppConstants.Layout.ILLUST_CARD_CORNER_RADIUS_DP.dp,
            )
            .clickable(enabled = work.illustId != null, onClick = onIllustClick),
    ) {
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
                    Box(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = work.user ?: strings.author,
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (work.userId != null) {
                    Text(
                        text = "›",
                        kind = CouiTextKind.GroupTitle,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!work.title.isNullOrBlank()) {
                Text(
                    text = work.title,
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (work.illustId != null) {
                Text(
                    text = "ID: ${work.illustId}",
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
