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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品详情页的信息卡片区块：作品信息卡、简介卡、标签卡与互动卡，以及标签胶囊 [TagCapsuleChip]。
 *
 * 全部为纯展示组件，`remember` 状态与业务操作均留在 [IllustDetailSingleContent]，
 * 通过参数与回调下发给这些子组件。
 */

/**
 * 作品信息卡：唯一大标题、浏览/收藏/日期指标、画师信息栏、下载全部与系列入口。
 */
@Composable
internal fun IllustDetailInfoCard(
    illust: Illust,
    isDownloading: Boolean,
    isBookmarked: Boolean,
    settings: SettingsRepository?,
    strings: AppStrings,
    downloadRepository: DownloadRepository,
    bookmarkRepository: BookmarkRepository,
    coroutineScope: CoroutineScope,
    onToast: (String?) -> Unit,
    onDownloadingChange: (Boolean) -> Unit,
    onBookmarkedChange: (Boolean) -> Unit,
    onUserClick: (Int) -> Unit,
    onIllustSeriesClick: (Int) -> Unit,
) {
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
            IllustDetailMetricsRow(
                illust = illust,
                strings = strings,
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ID: ${illust.id}   ${illust.width}x${illust.height}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 画师信息栏
            IllustDetailArtistRow(
                illust = illust,
                strings = strings,
                onUserClick = onUserClick,
            )

            // 多页作品下载全部入口
            if (illust.pageCount > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                BasicComponent(
                    title = strings.downloadTaskFilterAll,
                    summary = "${illust.pageCount} P",
                    onClick = {
                        if (isDownloading) return@BasicComponent
                        coroutineScope.launch {
                            try {
                                onDownloadingChange(true)
                                val tasks = downloadRepository.downloadAllPages(
                                    illust = illust,
                                    onProgress = { completed, total ->
                                        onToast("${strings.downloadStatusDownloading} $completed/$total")
                                    },
                                    maxConcurrency = settings?.maxRunningTask ?: 3,
                                )
                                val successCount = tasks.count { it.status == DownloadStatus.Success }
                                val failedCount = tasks.count { it.status == DownloadStatus.Failed }
                                if (successCount > 0 && settings?.starAfterSave == true && !isBookmarked) {
                                    coroutineScope.launch {
                                        suspendRunCatchingNonCancel {
                                            bookmarkRepository.addBookmark(
                                                illustId = illust.id,
                                                isPrivate = settings.defaultPrivateLike,
                                            )
                                        }.onSuccess {
                                            onBookmarkedChange(true)
                                        }
                                    }
                                }
                                onToast(
                                    when {
                                        failedCount == 0 -> "${strings.downloadStatusSuccess}: $successCount/${tasks.size}"
                                        successCount == 0 -> strings.downloadStatusFailed
                                        else -> "${strings.downloadStatusSuccess}: $successCount, ${strings.downloadStatusFailed} $failedCount"
                                    },
                                )
                            } finally {
                                onDownloadingChange(false)
                            }
                        }
                    },
                )
            }

            // 系列入口
            illust.series?.let { series ->
                Spacer(modifier = Modifier.height(6.dp))
                BasicComponent(
                    title = series.title.orEmpty(),
                    summary = "",
                    onClick = { onIllustSeriesClick(series.id) },
                )
            }
        }
    }
}

/**
 * 作品指标行：浏览数、收藏数与发布日期。
 */
@Composable
private fun IllustDetailMetricsRow(
    illust: Illust,
    strings: AppStrings,
) {
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
                contentDescription = strings.views,
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
                contentDescription = strings.bookmarks,
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
                contentDescription = strings.publishDate,
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
}

/**
 * 画师信息栏：头像、名称/账号与跳转入口。
 */
@Composable
private fun IllustDetailArtistRow(
    illust: Illust,
    strings: AppStrings,
    onUserClick: (Int) -> Unit,
) {
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
            contentDescription = strings.author,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 简介卡片：展示作品 caption（支持超链接解析）。
 */
@Composable
internal fun IllustDetailCaptionCard(
    illust: Illust,
    strings: AppStrings,
    onUserClick: (Int) -> Unit,
    onIllustClick: ((Int) -> Unit)?,
    onIllustSeriesClick: (Int) -> Unit,
    onNovelClick: ((Int) -> Unit)?,
    onTagClick: (String) -> Unit,
) {
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
                text = strings.searchTargetTitleCaption,
                style = MiuixTheme.textStyles.title4,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HtmlCaptionText(
                html = illust.caption,
                onUserClick = onUserClick,
                onIllustClick = onIllustClick,
                onIllustSeriesClick = onIllustSeriesClick,
                onNovelClick = onNovelClick,
                onTagClick = onTagClick,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

/**
 * 标签卡片：胶囊包裹（Capsule Chips）展示作品标签。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IllustDetailTagsCard(
    tags: List<IllustTag>,
    strings: AppStrings,
    onTagClick: (String) -> Unit,
) {
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
                text = strings.tags,
                style = MiuixTheme.textStyles.title4,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                tags.forEach { tag ->
                    TagCapsuleChip(
                        tag = tag,
                        onClick = { onTagClick(tag.name) },
                    )
                }
            }
        }
    }
}

/**
 * 互动操作卡片：评论与相关作品入口。
 */
@Composable
internal fun IllustDetailInteractionCard(
    illust: Illust,
    strings: AppStrings,
    onCommentsClick: (Int) -> Unit,
    onRelatedIllustsClick: (Int) -> Unit,
) {
    Spacer(modifier = Modifier.height(10.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
            .padding(horizontal = 12.dp),
    ) {
        BasicComponent(
            title = strings.commentsTitle,
            summary = "${illust.totalComments ?: 0}",
            onClick = { onCommentsClick(illust.id) },
        )
        BasicComponent(
            title = strings.relatedIllusts,
            summary = "",
            onClick = { onRelatedIllustsClick(illust.id) },
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
}

/**
 * 胶囊标签 Chip：圆角胶囊背景包裹，清晰展示标签与翻译名称。
 */
@Composable
internal fun TagCapsuleChip(
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