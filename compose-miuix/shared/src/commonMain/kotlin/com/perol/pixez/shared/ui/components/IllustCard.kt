package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.perol.pixez.shared.platform.IllustClipboard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.squircle.squircleBorder
import com.perol.pixez.shared.data.model.Illust
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
        when (settings?.feedPreviewQuality ?: 0) {
            1 -> illust.imageUrls.large
            2 -> illust.imageUrls.squareMedium
            else -> illust.imageUrls.medium
        }
    }

    val isAI = remember(illust.illustAIType) { illust.illustAIType == 2 }
    val showAIBadge = remember(isAI, settings?.feedAIBadge) { (settings?.feedAIBadge != false) && isAI }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val isNsfw = remember(illust.sanityLevel, illust.xRestrict, illust.tags, settings?.nsfwMask) {
        (settings?.nsfwMask == true) && (
            illust.sanityLevel > 4 ||
                illust.xRestrict > 0 ||
                illust.tags.any { it.name.contains("R-18", ignoreCase = true) || it.name.contains("R18", ignoreCase = true) }
        )
    }

    val ratio = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.5f, 2.0f)
        } else {
            1.0f
        }
    }

    val hapticFeedback = LocalHapticFeedback.current

    @OptIn(ExperimentalFoundationApi::class)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    runCatching {
                        IllustClipboard().copy("https://www.pixiv.net/artworks/${illust.id}")
                    }
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
                                    .background(
                                        color = MiuixTheme.colorScheme.error.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
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
                        contentDescription = illust.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (!isNsfw && illust.pageCount > 1) {
                    MicroGlassBadge(
                        text = "${illust.pageCount}P",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }

                if (!isNsfw && showAIBadge) {
                    MicroGlassBadge(
                        text = "AI",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
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
            .clip(RoundedCornerShape(6.dp))
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

