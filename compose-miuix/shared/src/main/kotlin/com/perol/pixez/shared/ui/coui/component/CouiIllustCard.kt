package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.itos.coui.menu.CouiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.platform.illustDragAndDropSource
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.rememberIllustQuickActions
import com.perol.pixez.shared.ui.components.resolveIllustCoverUrl
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.animation.illustTransitionBounds

/** 作品卡片与长按动作菜单：封面比例、AI/R-18 徽章、无障碍语义与动作枚举收口在此，网格只管布局。 */

/** COUI 卡片圆角：沿用 COUI 视觉的 18dp，共享边界动画必须传同一个值，否则动画圆角与静态圆角会跳变。 */
private val CouiIllustCardCornerRadius = 18.dp

/**
 * 长按菜单动作：菜单项与派发分支原先靠字符串字面量对应，改名或拼错都不会报错，
 * 换成枚举后每一项都有编译器保证的对应分支。
 */
private enum class CouiIllustAction {
    Download,
    CopyInfo,
    CopyImage,
    CopyLink,
    Share,
    Ban,
}

/** COUI 插画卡片：封面 + 标题/作者，支持点击进入详情与长按操作菜单。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CouiIllustCard(
    illust: Illust,
    changeVersion: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val hapticFeedback = LocalHapticFeedback.current
    val pressInteraction = remember { MutableInteractionSource() }
    val settings = LocalSettingsRepository.current
    val previewUrl = remember(illust, settings.feedPreviewQuality, changeVersion) {
        resolveIllustCoverUrl(illust.imageUrls, settings.feedPreviewQuality)
    }
    val isAI = remember(illust.illustAIType) { illust.illustAIType == 2 }
    val showAIBadge = remember(isAI, settings.feedAIBadge) { settings.feedAIBadge && isAI }
    val isNsfw = remember(illust, settings.nsfwMask, changeVersion) {
        settings.nsfwMask && illust.isR18()
    }
    // 语义串与 MIUIX 卡片同口径：标题、作者、页数、AI、R-18 一起朗读，
    // 读屏用户听到的就是卡片上能看到的全部信息，不必先点进详情才知道。
    val a11yDescription = remember(
        illust.title,
        illust.user.name,
        illust.pageCount,
        showAIBadge,
        isNsfw,
        strings,
    ) {
        buildString {
            append(illust.title)
            append(", ")
            append(strings.author)
            append(": ")
            append(illust.user.name)
            if (illust.pageCount > 1) {
                append(", ${illust.pageCount}P")
            }
            if (showAIBadge) {
                append(", ${strings.filterAi}")
            }
            if (isNsfw) {
                append(", R-18")
            }
        }
    }
    val ratio = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.5f, 2.0f)
        } else {
            1.0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .illustDragAndDropSource(illust, 0)
            .illustTransitionBounds(illust.id, CouiIllustCardCornerRadius)
            .clip(RoundedCornerShape(CouiIllustCardCornerRadius))
            .background(CouiTheme.colorScheme.card)
            .couiPressFeedback(pressInteraction)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = a11yDescription
                onClick(label = strings.viewArtworkDetail) {
                    onClick()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(label = strings.menuMoreActions) {
                        onLongClick()
                        true
                    },
                )
            }
            .combinedClickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
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
                        .background(CouiTheme.colorScheme.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "R-18",
                        kind = CouiTextKind.ItemTitle,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                    )
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
                CouiCardBadge(
                    text = "${illust.pageCount}P",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
            if (!isNsfw && showAIBadge) {
                CouiCardBadge(
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
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = illust.user.name,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CouiCardBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, kind = CouiTextKind.ItemSummary, color = Color.White, maxLines = 1)
    }
}

/**
 * COUI 插画长按菜单：官方锚点菜单贴着卡片弹出，动作编排复用
 * [rememberIllustQuickActions]，与 MIUIX 卡片保持同一套行为与反馈。
 */
@Composable
internal fun CouiIllustActionMenu(
    illust: Illust,
    expanded: Boolean,
    anchor: CouiMenuAnchor,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val actions = rememberIllustQuickActions(illust, onDismiss)
    Menu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        anchor = anchor,
        headerText = strings.menuMoreActions,
        items = listOf(
            CouiMenuItem(value = CouiIllustAction.Download, text = strings.download),
            CouiMenuItem(value = CouiIllustAction.CopyInfo, text = strings.menuCopyInfo),
            CouiMenuItem(value = CouiIllustAction.CopyImage, text = strings.menuCopyImage),
            CouiMenuItem(value = CouiIllustAction.CopyLink, text = strings.menuCopyLink),
            CouiMenuItem(value = CouiIllustAction.Share, text = strings.menuShareLink),
            CouiMenuItem(value = CouiIllustAction.Ban, text = strings.menuBanWork, dividerBefore = true),
        ),
        onItemClick = { item ->
            onDismiss()
            when (item.value) {
                CouiIllustAction.Download -> actions.onDownload()
                CouiIllustAction.CopyInfo -> actions.onCopyInfo()
                CouiIllustAction.CopyImage -> actions.onCopyImage()
                CouiIllustAction.CopyLink -> actions.onCopyLink()
                CouiIllustAction.Share -> actions.onShareLink()
                CouiIllustAction.Ban -> actions.onBan()
            }
        },
    )
}
