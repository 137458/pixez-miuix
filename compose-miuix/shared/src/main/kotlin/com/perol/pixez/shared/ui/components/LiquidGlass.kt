// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.libs.liquid.lens
import com.perol.pixez.shared.ui.libs.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.material.EdgeLightSource
import top.yukonga.miuix.kmp.material.EdgePeak
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.drawEdgeStroke

private val LiquidGlassHighlightLight = EdgeStroke(
    width = 1.dp,
    bevel = 2.8.dp,
    color = Color.White.copy(alpha = 0.08f),
    primary = EdgeLightSource(
        x = 0.5f,
        y = -0.2f,
        z = -0.15f,
        color = Color.White,
        intensity = 0.55f,
    ),
    secondary = EdgeLightSource(
        x = 0.5f,
        y = 0.85f,
        z = -0.5f,
        color = Color.White,
        intensity = 0.3f,
    ),
    peak = EdgePeak.Axial,
)

/**
 * 全局 Backdrop 采样源 CompositionLocal。
 */
val LocalBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * 纯毛玻璃模糊效果 Modifier，用于标准底栏、顶栏与浮层。
 */
fun Modifier.backdropBlur(
    backdrop: Backdrop?,
    shape: Shape = RoundedCornerShape(0.dp),
    blurRadius: Dp = 20.dp,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.96f,
): Modifier = if (backdrop != null) {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            blur(blurRadius.toPx(), blurRadius.toPx())
        },
        onDrawFront = null,
        onDrawSurface = {
            val color = if (tintColor != Color.Unspecified) tintColor else Color.White
            drawRect(color.copy(alpha = tintAlpha))
        },
    )
} else {
    val color = if (tintColor != Color.Unspecified) tintColor else Color.White
    this.background(color.copy(alpha = tintAlpha), shape)
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
    backdrop: Backdrop?,
    shape: Shape = RoundedCornerShape(32.dp),
    blurRadius: Dp = 20.dp,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = 0.45f,
): Modifier = if (backdrop != null) {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx(), blurRadius.toPx())
            lens(16.dp.toPx(), 20.dp.toPx())
        },
        onDrawFront = {
            drawEdgeStroke(
                shape = shape,
                stroke = LiquidGlassHighlightLight,
                alpha = 0.85f,
            )
        },
        onDrawSurface = {
            val color = if (tintColor != Color.Unspecified) tintColor else Color.White
            drawRect(color.copy(alpha = tintAlpha))
        },
    )
} else {
    val color = if (tintColor != Color.Unspecified) tintColor else Color.White
    this.background(color.copy(alpha = tintAlpha), shape)
}
