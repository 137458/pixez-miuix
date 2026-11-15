package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** 动作按钮族：行内按钮、浮层按钮与文字按钮共用 40dp 触控面积与 COUI 按压反馈，页面只挑形态。 */

/**
 * 顶栏动作图标：40dp 圆形触区，不依赖 COUI 原厂按钮的 API 34 窗口特性。
 * 按压反馈走库的 [couiButtonFeedback]：hover/focus/press 蒙层与顶栏按钮同口径，
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
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint = when {
        !enabled -> CouiTheme.colorScheme.onSurfaceVariant
        tint != null -> tint
        else -> CouiTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .couiButtonFeedback(
                dark = CouiTheme.isDark,
                interactionSource = interactionSource,
                enabled = enabled,
                list = false,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * COUI 浮层圆形动作按钮：半透明黑底保证图片上的图标对比度。
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
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (enabled) tint else tint.copy(alpha = 0.5f)),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 顶栏文本动作：无图标容器，只有按压反馈与文字，供「清空」「追更」这类低频动作使用。
 */
@Composable
internal fun CouiTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = color ?: if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** COUI 内联文本按钮：无底色，按下走库的按压蒙层，用于重试与占位动作。 */
@Composable
internal fun CouiTextActionButton(text: String, onClick: () -> Unit) {
    val pressInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = CouiTheme.colorScheme.accent,
        )
    }
}
