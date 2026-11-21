package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.LocalCouiContentColor
import com.itos.coui.topappbar.CouiTopAppBarButtonDefaults

/** 动作按钮族：行内按钮、浮层按钮与文字按钮共用 40dp 触控面积与 COUI 按压反馈，页面只挑形态。 */

/**
 * 顶栏动作图标：官方 TopAppBarButton 的图标形态（40dp 触区、circleShape、iconSize 24dp），
 * 不依赖原厂按钮的 API 34 窗口特性。图标色跟随外层内容色，缺省回落页面主文字色；
 * 按压反馈走库的 couiButtonFeedback：hover/focus/press 蒙层与顶栏按钮同口径，
 * 点击指示器置空，避免在圆形实底上再叠一层涟漪。
 */
@Composable
internal fun CouiActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    val pageColors = CouiPageDefaults.themeColors()
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint = when {
        !enabled -> pageColors.disabled
        tint != null -> tint
        else -> LocalCouiContentColor.current.takeOrElse { pageColors.title }
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CouiTopAppBarButtonDefaults.circleShape)
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier.size(CouiPageDefaults.iconSize),
        )
    }
}

/**
 * COUI 浮层圆形动作按钮：半透明黑底保证图片上的图标对比度。
 * 官方无浮层遮罩常量，蒙层与禁用透明度沿用原值，仅形状与图标尺寸对齐官方小圆角口径。
 */
@Composable
internal fun CouiOverlayActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CouiTopAppBarButtonDefaults.circleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (enabled) tint else tint.copy(alpha = 0.5f)),
            modifier = Modifier.size(CouiPageDefaults.iconSize),
        )
    }
}

/**
 * 顶栏文本动作：官方 TopAppBarButton 的文字形态（高 40dp、胶囊形状、水平内边距 12dp），
 * 无图标容器，只有按压反馈与文字，供「清空」「追更」这类低频动作使用。
 */
@Composable
internal fun CouiTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
) {
    val pageColors = CouiPageDefaults.themeColors()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CouiCapsuleShape)
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = when {
                color != null -> color
                !enabled -> pageColors.disabled
                else -> LocalCouiContentColor.current.takeOrElse { pageColors.title }
            },
            maxLines = 1,
        )
    }
}

/** COUI 内联文本按钮：官方胶囊形状与 40dp 高触区，无底色，按下走库的按压蒙层，用于重试与占位动作。 */
@Composable
internal fun CouiTextActionButton(text: String, onClick: () -> Unit) {
    val pageColors = CouiPageDefaults.themeColors()
    val pressInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(CouiCapsuleShape)
            .couiPressFeedback(pressInteraction, list = false)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = pageColors.accent,
        )
    }
}
