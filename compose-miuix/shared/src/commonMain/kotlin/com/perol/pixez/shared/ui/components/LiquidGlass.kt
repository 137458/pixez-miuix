package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Liquid Glass Modifier using Kyant0/AndroidLiquidGlass (Backdrop 2.0).
 * Provides real GPU shader-based blur, lens refraction, highlights, and depth shadows.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(32.dp),
    blurRadius: Dp = 20.dp,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.45f,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(16.dp.toPx(), 20.dp.toPx())
    },
    highlight = {
        Highlight(
            width = 1.dp,
            blurRadius = 1.dp,
            alpha = 0.35f,
            style = HighlightStyle.Default,
        )
    },
    shadow = {
        Shadow(
            radius = 8.dp,
            offset = DpOffset(0.dp, 4.dp),
            color = Color.Black,
            alpha = 0.15f,
        )
    },
    innerShadow = {
        InnerShadow(
            radius = 2.dp,
            offset = DpOffset(0.dp, 1.dp),
            color = Color.White,
            alpha = 0.25f,
        )
    },
    onDrawSurface = {
        val color = if (tintColor != Color.Unspecified) tintColor else Color.White
        drawRect(color.copy(alpha = tintAlpha))
    }
)

/**
 * 液态玻璃效果 Modifier，针对悬浮底栏优化。
 * 使用轻模糊 + 强折射/色散 + 高光 + 外阴影模拟 iOS Liquid Glass 药丸底栏。
 */
fun Modifier.liquidGlassBar(
    backdrop: Backdrop,
    shape: Shape = CircleShape,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.40f,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(4.dp.toPx())
        lens(24.dp.toPx(), 24.dp.toPx())
    },
    highlight = {
        Highlight(
            width = 1.dp,
            blurRadius = 2.dp,
            alpha = 0.75f,
            style = HighlightStyle.Default,
        )
    },
    shadow = {
        Shadow(
            radius = 10.dp,
            offset = DpOffset(0.dp, 4.dp),
            color = Color.Black,
            alpha = 0.12f,
        )
    },
    innerShadow = {
        InnerShadow(
            radius = 3.dp,
            offset = DpOffset(0.dp, 1.dp),
            color = Color.White,
            alpha = 0.20f,
        )
    },
    onDrawSurface = {
        val color = if (tintColor != Color.Unspecified) tintColor else Color.White
        drawRect(color.copy(alpha = tintAlpha))
    }
)

/**
 * 纯毛玻璃模糊效果 Modifier，用于顶栏和标准底栏。
 * 纯高斯模糊 + 半透明色调覆盖层，显式禁用任何高光边框、折射或外阴影，
 * 呈现平滑自然的 MIUIX / iOS 标准磨砂质感。
 */
fun Modifier.backdropBlur(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(0.dp),
    blurRadius: Dp = 20.dp,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.85f,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        blur(blurRadius.toPx())
    },
    highlight = null,
    shadow = null,
    innerShadow = null,
    onDrawSurface = {
        val color = if (tintColor != Color.Unspecified) tintColor else Color.White
        drawRect(color.copy(alpha = tintAlpha))
    }
)

/**
 * Liquid Glass Icon Button with real backdrop blur.
 */
@Composable
fun LiquidGlassIconButton(
    backdrop: Backdrop,
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    tint: Color = MiuixTheme.colorScheme.onSurface,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                tintColor = surfaceColor,
                tintAlpha = 0.50f,
            )
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
