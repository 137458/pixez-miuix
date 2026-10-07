// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// Adapted from InstallerX-Revived (com.rosan.installer.ui.library.FloatingBottomBar)
// and compose-miuix-ui official example (component.liquid.LiquidGlassNavigationBar)
// with core physics and shader pipeline from Kyant0/AndroidLiquidGlass (Apache 2.0).

/**
 * [FloatingBottomBar] 的三层渲染子组件（自 FloatingBottomBar.kt 拆分而来，纯代码搬运）：
 *
 * 1. [BottomBarBaseLayer]：未选中状态底层外壳，承载毛玻璃 / 实色 / 液态玻璃折射底衬与点击容器；
 * 2. [BottomBarActiveTabsLayer]：激活状态隐藏层，供 tabsBackdrop 录制高亮状态；
 * 3. [BottomBarIndicatorLayer]：双重背景采样透镜折射滑块，含 Blur / None 降级滑块分支。
 *
 * 液态玻璃绘制链与阻尼拖动动画参数整体原样搬运，不做拆解或优化，避免影响渲染行为。
 * 所有 `remember` / `derivedStateOf` / 动画状态仍保留在 [FloatingBottomBar] 作用域内，通过参数传入。
 */

package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.perol.pixez.shared.ui.animation.DampedDragAnimation
import com.perol.pixez.shared.ui.animation.InteractiveHighlight
import com.perol.pixez.shared.ui.libs.liquid.InnerShadow
import com.perol.pixez.shared.ui.libs.liquid.innerShadow
import com.perol.pixez.shared.ui.libs.liquid.lens
import com.perol.pixez.shared.ui.libs.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * Layer 1：底层外壳。
 *
 * 负责记录尺寸、按压缩放、拖拽面板位移、阴影，以及液态玻璃 / 毛玻璃 / 实色三种底衬降级。
 */
@Composable
internal fun BottomBarBaseLayer(
    colors: FloatingBottomBarColors,
    pillShape: Shape,
    panelOffsetState: State<Float>,
    isDark: Boolean,
    isLiquidGlassMode: Boolean,
    isBlurMode: Boolean,
    containerColor: Color,
    backdrop: Backdrop?,
    dampedDragAnimation: DampedDragAnimation,
    baseHighlight: State<Highlight>,
    interactiveHighlight: InteractiveHighlight?,
    onSizeChanged: (IntSize) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalFloatingBottomBarContentColor provides colors.contentColor) {
        Row(
            modifier = Modifier
                .selectableGroup()
                .onSizeChanged(onSizeChanged)
                .graphicsLayer { translationX = panelOffsetState.value }
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(
                        radius = 10.dp,
                        color = Color.Black,
                        alpha = if (isDark) 0.2f else 0.1f,
                    ),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .then(
                    if (backdrop != null && isLiquidGlassMode) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                padding = maxOf(padding, 40.dp.toPx())
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(
                                    refractionHeight = 24.dp.toPx(),
                                    refractionAmount = 24.dp.toPx(),
                                )
                            },
                            highlight = { baseHighlight.value.copy(alpha = 0.75f) },
                            layerBlock = {
                                val width = size.width.coerceAtLeast(1f)
                                val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDragAnimation.pressProgress)
                                scaleX = s
                                scaleY = s
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else if (backdrop != null && isBlurMode) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                blur(25.dp.toPx(), 25.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(containerColor.copy(alpha = 0.65f))
                            },
                        )
                    } else {
                        Modifier.background(containerColor, pillShape)
                    },
                )
                .then(if (isLiquidGlassMode && interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Layer 2：激活状态隐藏层。
 *
 * 仅液态玻璃模式下存在，以 alpha=0 隐藏渲染，供 tabsBackdrop 录制选中态内容。
 */
@Composable
internal fun BottomBarActiveTabsLayer(
    isLiquidGlassMode: Boolean,
    backdrop: Backdrop?,
    pillShape: Shape,
    panelOffsetState: State<Float>,
    dampedDragAnimation: DampedDragAnimation,
    colors: FloatingBottomBarColors,
    containerColor: Color,
    interactiveHighlight: InteractiveHighlight?,
    tabsBackdrop: LayerBackdrop,
    content: @Composable RowScope.() -> Unit,
) {
    if (backdrop != null && isLiquidGlassMode) {
        CompositionLocalProvider(
            LocalFloatingBottomBarTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            },
            LocalFloatingBottomBarContentColor provides colors.activeContentColor,
        ) {
            Row(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffsetState.value }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx(), 4.dp.toPx())
                            lens(
                                refractionHeight = 24.dp.toPx(),
                                refractionAmount = 24.dp.toPx(),
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveHighlight?.modifier ?: Modifier)
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * Layer 3：指示器滑块。
 *
 * 液态玻璃模式下使用双重背景采样透镜折射；否则降级为普通着色滑块 + 横向滚动内容。
 */
@Composable
internal fun BottomBarIndicatorLayer(
    tabWidthPx: Float,
    totalWidthPx: Float,
    density: Density,
    isLiquidGlassMode: Boolean,
    combinedBackdrop: Backdrop?,
    dampedDragAnimation: DampedDragAnimation,
    panelOffsetState: State<Float>,
    interactiveHighlight: InteractiveHighlight?,
    pillHighlight: State<Highlight>,
    pillShape: Shape,
    colors: FloatingBottomBarColors,
    isDark: Boolean,
    isLtr: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    if (tabWidthPx > 0f) {
        val tabWidthDp = with(density) { tabWidthPx.toDp() }
        if (isLiquidGlassMode && combinedBackdrop != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val progressOffset = dampedDragAnimation.value * tabWidthPx
                        translationX = if (isLtr) progressOffset + panelOffsetState.value else -progressOffset + panelOffsetState.value
                    }
                    .then(interactiveHighlight?.gestureModifier ?: Modifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = combinedBackdrop,
                        shape = { pillShape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(
                                refractionHeight = 10.dp.toPx() * progress,
                                refractionAmount = 14.dp.toPx() * progress,
                                depthEffect = true,
                                chromaticAberration = 0.5f,
                            )
                        },
                        highlight = { pillHighlight.value.copy(alpha = dampedDragAnimation.pressProgress) },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                alpha = 1f - progress,
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        },
                    )
                    .innerShadow(shape = pillShape) {
                        InnerShadow(
                            radius = 8.dp * dampedDragAnimation.pressProgress,
                            color = Color.Black.copy(alpha = 0.15f),
                            alpha = dampedDragAnimation.pressProgress,
                        )
                    }
                    .height(56.dp)
                    .width(tabWidthDp),
            )
        } else {
            // Blur / None 降级模式滑块
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val progressOffset = dampedDragAnimation.value * tabWidthPx
                        translationX = if (isLtr) progressOffset + panelOffsetState.value else -progressOffset + panelOffsetState.value
                    }
                    .then(dampedDragAnimation.modifier)
                    .clip(pillShape)
                    .background(colors.indicatorColor.copy(alpha = 0.15f), pillShape)
                    .height(56.dp)
                    .width(tabWidthDp),
                contentAlignment = Alignment.CenterStart,
            ) {
                CompositionLocalProvider(LocalFloatingBottomBarContentColor provides colors.activeContentColor) {
                    Row(
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .wrapContentWidth(align = Alignment.Start, unbounded = true)
                            .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                            .height(56.dp)
                            .graphicsLayer {
                                val progressOffset = dampedDragAnimation.value * tabWidthPx
                                translationX = if (isLtr) -progressOffset else progressOffset
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                    )
                }
            }
        }
    }
}