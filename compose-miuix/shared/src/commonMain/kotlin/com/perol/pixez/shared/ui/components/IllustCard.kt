package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil3.compose.LocalPlatformContext
import coil3.SingletonImageLoader
import com.perol.pixez.shared.LocalDownloadRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.mapToPictureSource
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.ui.navigation.animation.illustTransitionBounds
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 插画卡片：等比例展示封面缩略图，并在下方显示标题与作者。
 *
 * 使用 MIUIX Card 容器与设计语义色，支持画质选择、AI 标识与 NSFW 遮罩。
 */
@Composable
fun IllustCard(
    illust: Illust,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettingsRepository.current
    val previewUrl = remember(illust, settings?.feedPreviewQuality, settings?.changeVersion) {
        resolveIllustCoverUrl(illust.imageUrls, settings?.feedPreviewQuality)
    }

    val isAI = remember(illust.illustAIType) { illust.illustAIType == 2 }
    val showAIBadge = remember(isAI, settings?.feedAIBadge) { (settings?.feedAIBadge != false) && isAI }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val isNsfw = remember(illust, settings?.nsfwMask, settings?.changeVersion) {
        (settings?.nsfwMask == true) && illust.isR18()
    }

    val ratio = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.5f, 2.0f)
        } else {
            1.0f
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    var showActionMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    val downloadRepository = LocalDownloadRepository.current

    if (showActionMenu) {
        IllustActionMenu(
            show = showActionMenu,
            showBan = true,
            onDismissRequest = { showActionMenu = false },
            onDownload = if (downloadRepository != null) {
                {
                    showActionMenu = false
                    coroutineScope.launch {
                        suspendRunCatchingNonCancel {
                            downloadRepository.download(illust, pageIndex = 0)
                        }
                    }
                }
            } else null,
            onCopyInfo = {
                showActionMenu = false
                runCatching { IllustClipboard().copy(buildIllustCopyInfo(illust)) }
            },
            onCopyImage = {
                showActionMenu = false
                coroutineScope.launch {
                    runCatching {
                        val candidateUrls = listOf(illust.imageUrls.large, illust.imageUrls.medium, illust.imageUrls.squareMedium)
                        val transformedUrls = candidateUrls.map { it.mapToPictureSource(settings?.pictureSource) }
                        withContext(Dispatchers.IO) {
                            val bytes = extractCachedImageBytes(context, transformedUrls)
                            bytes?.let { IllustClipboard().copyImage(it) }
                        }
                    }
                }
            },
            onCopyLink = {
                showActionMenu = false
                runCatching { IllustClipboard().copy(buildIllustShareLink(illust)) }
            },
            onShareLink = {
                showActionMenu = false
                runCatching { IllustShare().share(buildIllustShareLink(illust), illust.title) }
            },
            onBan = {
                showActionMenu = false
            },
        )
    }

    val illustA11yDescription = remember(illust.title, illust.user.name, illust.pageCount, showAIBadge, isNsfw) {
        buildString {
            append(illust.title)
            append(", ")
            append(strings.author)
            append(": ")
            append(illust.user.name)
            if (illust.pageCount > 1) append(", ${illust.pageCount}P")
            if (showAIBadge) append(", ${strings.filterAi}")
            if (isNsfw) append(", R-18")
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .illustDragAndDropSource(illust, 0)
            .illustTransitionBounds(illust.id, AppConstants.Layout.ILLUST_CARD_CORNER_RADIUS_DP.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = illustA11yDescription
                onClick(label = strings.viewArtworkDetail) {
                    onClick()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(label = strings.menuMoreActions) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showActionMenu = true
                        true
                    }
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showActionMenu = true
                },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio),
            ) {
                if (isNsfw) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .squircleClip(8.dp)
                                    .background(
                                        color = MiuixTheme.colorScheme.error.copy(alpha = 0.15f),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "R-18",
                                    style = MiuixTheme.textStyles.title3,
                                    color = MiuixTheme.colorScheme.error,
                                )
                            }
                            Text(
                                text = strings.nsfwMaskSummaryOn,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    PixivAsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (!isNsfw && illust.pageCount > 1) {
                    MicroGlassBadge(
                        text = "${illust.pageCount}P",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clearAndSetSemantics {},
                    )
                }

                if (!isNsfw && showAIBadge) {
                    MicroGlassBadge(
                        text = "AI",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clearAndSetSemantics {},
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = illust.title,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = illust.user.name,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * 瀑布流卡片微型液态磨砂玻璃徽标。
 */
@Composable
private fun MicroGlassBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .squircleClip(6.dp)
            .background(Color.Black.copy(alpha = 0.50f))
            .squircleBorder(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.30f),
                cornerRadius = 6.dp,
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = Color.White,
        )
    }
}

