/**
 * [IllustDetailTopBar] 的顶栏主体行与右侧操作按钮组（自 IllustDetailTopBar.kt 拆分而来，纯代码搬运）。
 *
 * [IllustDetailTopBarHeader] 负责返回按钮、动态标题与右侧操作组的整体布局；
 * [IllustDetailTopBarActions] 负责收藏、下载、更多操作三个液态玻璃圆形按钮的渲染。
 * 折叠进度、图标着色、微气泡透明度等参数均由入口函数通过参数传入。
 */

package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏主体内容行：返回按钮 + 动态标题 + 右侧操作组。
 *
 * @param collapseProgress 折叠进度（0f 展开，1f 完全折叠）
 * @param illust 作品数据，为 null 时隐藏标题与部分操作
 */
@Composable
internal fun IllustDetailTopBarHeader(
    collapseProgress: Float,
    illust: Illust?,
    detailBackdrop: Backdrop?,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    bookmarkHeartScale: Animatable<Float, AnimationVector1D>,
    onBookmarkClick: () -> Unit,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bubbleAlpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
        val dynamicIconTint = lerp(
            Color.White,
            MiuixTheme.colorScheme.onSurface,
            collapseProgress,
        )

        // 返回按钮
        LiquidCircleActionButton(
            tooltip = strings.back,
            onClick = onBack,
            bubbleAlpha = bubbleAlpha,
            detailBackdrop = detailBackdrop,
        ) {
            Icon(
                imageVector = MiuixIcons.Back,
                contentDescription = strings.back,
                tint = dynamicIconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        // 中间标题区（随上滑折叠平滑淡入）
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .graphicsLayer {
                    alpha = ((collapseProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = illust?.title.orEmpty(),
                style = MiuixTheme.textStyles.title3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        // 右侧操作按钮组（收藏、下载、更多）
        IllustDetailTopBarActions(
            illust = illust,
            collapseProgress = collapseProgress,
            dynamicIconTint = dynamicIconTint,
            bubbleAlpha = bubbleAlpha,
            detailBackdrop = detailBackdrop,
            isBookmarked = isBookmarked,
            isBookmarkLoading = isBookmarkLoading,
            bookmarkHeartScale = bookmarkHeartScale,
            onBookmarkClick = onBookmarkClick,
            isDownloading = isDownloading,
            onDownloadClick = onDownloadClick,
            onMoreClick = onMoreClick,
        )
    }
}

/**
 * 右侧操作按钮组：收藏、下载、更多菜单。
 */
@Composable
internal fun IllustDetailTopBarActions(
    illust: Illust?,
    collapseProgress: Float,
    dynamicIconTint: Color,
    bubbleAlpha: Float,
    detailBackdrop: Backdrop?,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    bookmarkHeartScale: Animatable<Float, AnimationVector1D>,
    onBookmarkClick: () -> Unit,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 收藏按钮
        LiquidCircleActionButton(
            tooltip = if (isBookmarked) strings.bookmarked else strings.bookmark,
            onClick = onBookmarkClick,
            bubbleAlpha = bubbleAlpha,
            detailBackdrop = detailBackdrop,
            enabled = !isBookmarkLoading && illust != null,
        ) {
            Icon(
                imageVector = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                contentDescription = if (isBookmarked) strings.bookmarked else strings.bookmark,
                tint = if (isBookmarked) Color(0xFFFF4D6A) else dynamicIconTint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = bookmarkHeartScale.value
                        scaleY = bookmarkHeartScale.value
                    },
            )
        }

        // 2. 下载按钮
        LiquidCircleActionButton(
            tooltip = strings.download,
            onClick = onDownloadClick,
            bubbleAlpha = bubbleAlpha,
            detailBackdrop = detailBackdrop,
            enabled = !isDownloading && illust != null,
        ) {
            if (isDownloading) {
                InfiniteProgressIndicator(
                    color = if (collapseProgress > 0.5f) MiuixTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    imageVector = MiuixIcons.Download,
                    contentDescription = strings.download,
                    tint = dynamicIconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // 3. 更多菜单按钮
        if (illust != null) {
            LiquidCircleActionButton(
                tooltip = strings.menuMoreActions,
                onClick = onMoreClick,
                bubbleAlpha = bubbleAlpha,
                detailBackdrop = detailBackdrop,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = strings.menuMoreActions,
                    tint = dynamicIconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}