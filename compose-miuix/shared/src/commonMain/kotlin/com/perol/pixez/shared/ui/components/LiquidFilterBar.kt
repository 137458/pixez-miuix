package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用液态磨砂玻璃筛选栏组件 (Liquid Filter Bar)。
 *
 * 规范与特性：
 * 1. 材质与光影：基于 [liquidGlass] 实现物理模糊、色散与高光，未选中态使用 [MiuixTheme.colorScheme.surfaceContainer]
 *    自适应浅色、深色及 AMOLED 纯黑模式，彻底解决硬编码色值在纯黑背景下的泛白与割裂。
 * 2. 几何连续曲率：全链路采用 [squircleClip] 与 [squircleBorder]，杜绝 [RoundedCornerShape] 裁切与 Squircle 边框叠加
 *    导致的抗锯齿裂缝与曲率不匹配问题。
 * 3. 交互反馈：具备物理弹簧按下缩放动效（0.93x）与系统触感振动反馈。
 * 4. 模式支持：支持固定均分模式 ([isScrollable] = false，使用权重均分) 与横向滚动模式 ([isScrollable] = true)。
 */
@Composable
fun <T> LiquidFilterBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    backdrop: Backdrop? = null,
    isScrollable: Boolean = false,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
) {
    if (isScrollable) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = items,
                key = { it.hashCode() },
                contentType = { "liquid_filter_chip" },
            ) { item ->
                val isSelected = item == selectedItem
                LiquidFilterChip(
                    text = labelProvider(item),
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            onItemSelected(item)
                        }
                    },
                    backdrop = backdrop,
                    cornerRadius = cornerRadius,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                LiquidFilterChip(
                    text = labelProvider(item),
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            onItemSelected(item)
                        }
                    },
                    backdrop = backdrop,
                    cornerRadius = cornerRadius,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 单个液态磨砂玻璃筛选芯片（快捷按钮）。
 *
 * 可在 [LiquidFilterBar] 外部作为独立的筛选/排序芯片使用（如搜索结果页面的排序与条件芯片）。
 */
@Composable
fun LiquidFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    cornerRadius: Dp = 16.dp,
    textStyle: TextStyle = MiuixTheme.textStyles.body2,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
) {
    LiquidFilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
    ) {
        Text(
            text = text,
            style = textStyle,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * 具有自定义内容的液态磨砂玻璃筛选芯片基组件。
 */
@Composable
fun LiquidFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        pressScale.animateTo(
            targetValue = if (isPressed) 0.93f else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 500f,
            ),
        )
    }

    val itemBackground = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val tintAlpha = if (selected) 0.88f else (if (isDark) 0.45f else 0.60f)

    val itemBorderColor = if (selected) {
        Color.White.copy(alpha = 0.35f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.65f)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .squircleClip(cornerRadius)
            .liquidGlass(
                backdrop = backdrop,
                shape = SquircleShape(cornerRadius),
                blurRadius = 16.dp,
                tintColor = itemBackground,
                tintAlpha = tintAlpha,
            )
            .squircleBorder(
                width = 0.5.dp,
                color = itemBorderColor,
                cornerRadius = cornerRadius,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
