// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.libs.liquid.lens
import com.perol.pixez.shared.ui.libs.liquid.vibrancy
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DefaultBlurRadius = 20.dp
private val DefaultCornerRadius = 24.dp
private const val DefaultRefractionRatio = 0.12f
private const val DefaultChromaticAberration = 0.15f
private const val PressedScale = 0.96f
private val GlassBorderWidth = 0.5.dp

private val LiquidGlassHighlightLight = Highlight(
    width = 1.dp,
    alpha = 0.85f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.08f),
        innerBlurRadius = 2.8.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.2f, -0.15f),
            color = Color.White,
            intensity = 0.55f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.85f, -0.5f),
            color = Color.White,
            intensity = 0.3f,
        ),
        dualPeak = true,
    ),
)

private val LiquidGlassHighlightDark = Highlight(
    width = 0.8.dp,
    alpha = 0.9f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.15f, -0.1f),
            color = Color.White,
            intensity = 0.65f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.45f),
            color = Color.White,
            intensity = 0.25f,
        ),
        dualPeak = true,
    ),
)

/**
 * MIUIX Liquid Glass 物理液态玻璃容器。
 */
@Composable
fun LiquidGlass(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DefaultCornerRadius,
    blurRadius: Dp = DefaultBlurRadius,
    refractionRatio: Float = DefaultRefractionRatio,
    chromaticAberration: Float = DefaultChromaticAberration,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    if (!isRuntimeShaderSupported() || !enabled) {
        Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)), content = content)
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(isPressed) {
        pressScale.animateTo(
            targetValue = if (isPressed) PressedScale else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 400f,
            ),
        )
    }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val highlight = remember(isDark) {
        if (isDark) LiquidGlassHighlightDark else LiquidGlassHighlightLight
    }

    val density = LocalDensity.current
    val blurRadiusPx = with(density) { blurRadius.toPx() }
    val refractionAmountPx = blurRadiusPx * refractionRatio.coerceIn(0.08f, 0.15f)

    val borderColor = remember(isDark) {
        if (isDark) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.4f)
        }
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .squircleClip(cornerRadius)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    padding = maxOf(padding, 40.dp.toPx())
                    vibrancy()
                    blur(blurRadiusPx, blurRadiusPx)
                    lens(
                        refractionHeight = blurRadiusPx * 0.9f,
                        refractionAmount = refractionAmountPx,
                        depthEffect = true,
                        chromaticAberration = chromaticAberration,
                    )
                },
                highlight = { highlight },
                onDrawSurface = {
                    drawRect(
                        color = if (isDark) {
                            Color.White.copy(alpha = 0.03f)
                        } else {
                            Color.White.copy(alpha = 0.06f)
                        },
                    )
                },
            )
            .squircleBorder(
                width = GlassBorderWidth,
                color = borderColor,
                cornerRadius = cornerRadius,
            ),
        content = content,
    )
}

/**
 * 全局 Backdrop 采样源 CompositionLocal。
 */
val LocalBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * 纯毛玻璃模糊效果 Modifier，用于标准底栏、顶栏与浮层。
 */
fun Modifier.backdropBlur(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(0.dp),
    blurRadius: Dp = 25.dp,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.92f,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        blur(blurRadius.toPx(), blurRadius.toPx())
    },
    highlight = null,
    onDrawSurface = {
        val color = if (tintColor != Color.Unspecified) tintColor else Color.White
        drawRect(color.copy(alpha = tintAlpha))
    },
)

/**
 * 顶栏专用背景毛玻璃模糊 Modifier。
 *
 * @param backdrop 全局或页面层级的 Backdrop 采样源。若为 null 则保持原样。
 * @param tintColor 表面着色，默认取当前主题表面色。
 * @param tintAlpha 表面着色不透明度，默认 0.92f。
 */
fun Modifier.topAppBarBlur(
    backdrop: Backdrop?,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.92f,
): Modifier = if (backdrop != null) {
    this.backdropBlur(
        backdrop = backdrop,
        shape = RectangleShape,
        blurRadius = 25.dp,
        tintColor = tintColor,
        tintAlpha = tintAlpha,
    )
} else {
    this
}

/**
 * 安全挂载 layerBackdrop 采样源的扩展 Modifier。
 * 当 backdrop 为非 null 时注册为 Backdrop 图层采样源，为 null 时保持原样。
 */
fun Modifier.blurBackdropSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) this.layerBackdrop(backdrop) else this

/**
 * Liquid Glass Modifier using MIUIX Blur & Shader pipeline.
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
        blur(blurRadius.toPx(), blurRadius.toPx())
        lens(16.dp.toPx(), 20.dp.toPx())
    },
    highlight = {
        LiquidGlassHighlightLight
    },
    onDrawSurface = {
        val color = if (tintColor != Color.Unspecified) tintColor else Color.White
        drawRect(color.copy(alpha = tintAlpha))
    },
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
