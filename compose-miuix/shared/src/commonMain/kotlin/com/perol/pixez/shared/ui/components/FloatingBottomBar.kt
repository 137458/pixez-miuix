// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0)
// and compose-miuix-ui official example (component.liquid.LiquidGlassNavigationBar).

package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.animation.DampedDragAnimation
import com.perol.pixez.shared.ui.animation.InteractiveHighlight
import kotlinx.coroutines.launch

import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    blurRadius = 2.dp,
    alpha = 0.75f,
    style = HighlightStyle.Default,
)

private data class RefractionConfig(
    val base: Dp,
    val indicator: Dp,
    val highlightAlpha: Float,
    val blur: Dp,
)

/**
 * 官方 compose-miuix-ui 规范的 iOS Liquid Glass 悬浮导航栏。
 *
 * 架构原理：
 * 1. 统一连续液态玻璃底层：在整个药丸外框上应用单层 drawBackdrop（模糊 + 折射 + 高光 + 深度阴影），彻底杜绝多层 Backdrop 引起的分割线；
 * 2. 物理平滑小药丸指示器：随 DampedDragAnimation 物理阻尼与弹性插值平滑滑动；
 * 3. 标签内容层：高亮选中项文字/图标，保证无重影与无缝毛玻璃视觉。
 */
@Composable
fun IosLiquidGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    backdrop: Backdrop?,
    isBlurActive: Boolean = true,
    refractionLevel: Int = 2,
    modifier: Modifier = Modifier,
    badge: (Int) -> (@Composable () -> Unit)? = { null },
) {
    val pillShape = remember { CircleShape }
    val accentColor = MiuixTheme.colorScheme.primary
    val tabContentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = if (isBlurActive && backdrop != null) surfaceContainer.copy(alpha = 0.4f) else surfaceContainer

    val (baseRefractionDp, _, highlightAlpha, lensBlurDp) = remember(refractionLevel) {
        when (refractionLevel) {
            0 -> RefractionConfig(base = 12.dp, indicator = 8.dp, highlightAlpha = 0.40f, blur = 3.dp)
            1 -> RefractionConfig(base = 20.dp, indicator = 12.dp, highlightAlpha = 0.60f, blur = 4.dp)
            2 -> RefractionConfig(base = 28.dp, indicator = 16.dp, highlightAlpha = 0.75f, blur = 4.dp)
            3 -> RefractionConfig(base = 38.dp, indicator = 22.dp, highlightAlpha = 0.88f, blur = 5.dp)
            4 -> RefractionConfig(base = 50.dp, indicator = 30.dp, highlightAlpha = 1.00f, blur = 6.dp)
            else -> RefractionConfig(base = 28.dp, indicator = 16.dp, highlightAlpha = 0.75f, blur = 4.dp)
        }
    }

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size


    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onItemClickUpdated by rememberUpdatedState(onItemClick)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position ->
                position.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onItemClickUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDrag.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (currentIndex != index) {
            currentIndex = index
            onItemClickUpdated(index)
        }
        dampedDrag.animateToValue(index.toFloat())
    }

    val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { layerSize, _ ->
                Offset(
                    x = if (isLtr) {
                        (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        layerSize.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = layerSize.height / 2f,
                )
            },
        )
    }

    val baseHighlight = remember { iosIndicatorSpecular.copy(alpha = 0.75f) }

    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = if (navBarBottomPadding != 0.dp) 8.dp + navBarBottomPadding else 20.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPaddingValue, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(
                    min = AppConstants.Layout.FLOATING_BAR_MIN_WIDTH_DP.dp,
                    max = AppConstants.Layout.FLOATING_BAR_MAX_WIDTH_DP.dp,
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            // ── 1. 统一连续液态玻璃底层（消除多层 Backdrop 引起的区域分割线） ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .onSizeChanged { coords ->
                        totalWidthPx = coords.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                        tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                    }
                    .graphicsLayer { translationX = panelOffset }
                    .shadow(
                        elevation = 10.dp,
                        shape = pillShape,
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.20f),
                    )
                    .then(
                        if (isBlurActive && backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    vibrancy()
                                    blur(lensBlurDp.toPx())
                                    lens(
                                        refractionHeight = baseRefractionDp.toPx(),
                                        refractionAmount = baseRefractionDp.toPx(),
                                    )
                                },
                                highlight = { baseHighlight.copy(alpha = highlightAlpha) },
                                layerBlock = {
                                    val width = size.width.coerceAtLeast(1f)
                                    val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDrag.pressProgress)
                                    scaleX = s
                                    scaleY = s
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                        } else {
                            Modifier.background(containerColor, pillShape)
                        },
                    )
                    .then(
                        if (isBlurActive && backdrop != null) {
                            interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                        } else {
                            Modifier
                        },
                    )
                    .then(dampedDrag.modifier),
            )

            // ── 2. 平滑滑动选中小胶囊指示器 ──
            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDrag.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                        }
                        .clip(pillShape)
                        .background(accentColor.copy(alpha = 0.15f), pillShape)
                        .height(56.dp)
                        .width(tabWidthDp),
                )
            }

            // ── 3. 标签内容层（文字与图标） ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(4.dp)
                    .graphicsLayer { translationX = panelOffset }
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == currentIndex
                    val itemColor = if (isSelected) accentColor else tabContentColor

                    Column(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {
                                selected = isSelected
                                role = Role.Tab
                                onClick {
                                    activateTab(index)
                                    true
                                }
                            }
                            .onKeyEvent { event ->
                                val isActivationKey = event.key == Key.Enter ||
                                    event.key == Key.NumPadEnter ||
                                    event.key == Key.Spacebar
                                if (isActivationKey) {
                                    if (event.type == KeyEventType.KeyUp) activateTab(index)
                                    true
                                } else {
                                    false
                                }
                            }
                            .focusable()
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                        horizontalAlignment = CenterHorizontally,
                    ) {
                        val currentBadge = badge(index)
                        if (currentBadge != null) {
                            Box {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = itemColor,
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                ) {
                                    currentBadge()
                                }
                            }
                        } else {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = itemColor,
                            )
                        }
                        Text(
                            text = item.label,
                            color = itemColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

