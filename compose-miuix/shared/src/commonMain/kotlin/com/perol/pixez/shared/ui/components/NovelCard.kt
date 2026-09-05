package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 小说卡片组件：
 * 遵循 Xiaomi HyperOS / MIUIX 规范，以横向排版卡片展示小说封面、标题、系列、作者、字数统计与收藏信息。
 */
@Composable
fun NovelCard(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val isAI = remember(novel.novelAIType) { novel.novelAIType == 2 }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 封面缩略图（标准书籍比例 3:4）
            Box(
                modifier = Modifier
                    .width(84.dp)
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                PixivAsyncImage(
                    model = novel.imageUrls.medium.ifBlank { novel.imageUrls.squareMedium },
                    contentDescription = novel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )

                // AI 生成标识
                if (isAI) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "AI",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // 文本信息区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    // 系列标识（若存在）
                    val seriesTitle = novel.series?.title
                    if (!seriesTitle.isNullOrBlank()) {
                        Text(
                            text = seriesTitle,
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // 标题
                    Text(
                        text = novel.title,
                        style = MiuixTheme.textStyles.title3,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }

                Column {
                    // 作者信息行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PixivAsyncImage(
                            model = novel.user.profileImageUrls.medium,
                            contentDescription = novel.user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape),
                        )
                        Text(
                            text = novel.user.name,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 统计行：字数与收藏数
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = strings.formatNovelWordCount(novel.textLength),
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Favorites,
                                contentDescription = strings.bookmark,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "${novel.totalBookmarks}",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }
    }
}
