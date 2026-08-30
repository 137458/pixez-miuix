package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式毛玻璃模糊质感的大标题顶栏。
 *
 * 采用 92% 表面磨砂色与纯净 Backdrop 采样，消除滚动时的透明度动画抖动。
 * 当大标题未折叠 (collapsedFraction == 0) 时，大标题区域为纯净表面，不进行全高模糊采样，
 * 避免模糊滤镜边缘切割到下方的分类标题（SmallTitle）；
 * 仅在发生滚动折叠 (collapsedFraction > 0) 时才渐进激活毛玻璃模糊与底部分割线。
 */
@Composable
fun FrostedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backdrop: Backdrop? = null,
) {
    val colorScheme = MiuixTheme.colorScheme
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val isCollapsed = collapsedFraction > 0.01f
    val outlineAlpha = (collapsedFraction * 0.15f).coerceIn(0f, 0.15f)
    val outlineColor = colorScheme.outline

    val effectiveTintAlpha = if (scrollBehavior != null) {
        (0.92f * collapsedFraction).coerceIn(0f, 0.92f)
    } else {
        0.92f
    }

    val barModifier = if (backdrop != null && isCollapsed) {
        modifier
            .backdropBlur(
                backdrop = backdrop,
                tintColor = colorScheme.surface,
                tintAlpha = effectiveTintAlpha,
            )
            .drawBehind {
                if (outlineAlpha > 0.01f) {
                    drawLine(
                        color = outlineColor.copy(alpha = outlineAlpha),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                }
            }
    } else {
        modifier.drawBehind {
            if (outlineAlpha > 0.01f) {
                drawLine(
                    color = outlineColor.copy(alpha = outlineAlpha),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
        }
    }

    TopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = if (backdrop != null && isCollapsed) Color.Transparent else colorScheme.surface,
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = barModifier,
    )
}

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式毛玻璃模糊质感的紧凑型小顶栏。
 */
@Composable
fun FrostedSmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backdrop: Backdrop? = null,
) {
    val colorScheme = MiuixTheme.colorScheme
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val isScrolled = if (scrollBehavior != null) collapsedFraction > 0.01f else true
    val outlineAlpha = if (scrollBehavior != null) (collapsedFraction * 0.15f).coerceIn(0f, 0.15f) else 0.12f
    val outlineColor = colorScheme.outline

    val effectiveTintAlpha = if (scrollBehavior != null) {
        (0.92f * collapsedFraction).coerceIn(0f, 0.92f)
    } else {
        0.92f
    }

    val barModifier = if (backdrop != null && isScrolled) {
        modifier
            .backdropBlur(
                backdrop = backdrop,
                tintColor = colorScheme.surface,
                tintAlpha = effectiveTintAlpha,
            )
            .drawBehind {
                if (outlineAlpha > 0.01f) {
                    drawLine(
                        color = outlineColor.copy(alpha = outlineAlpha),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                }
            }
    } else {
        modifier.drawBehind {
            if (outlineAlpha > 0.01f) {
                drawLine(
                    color = outlineColor.copy(alpha = outlineAlpha),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
        }
    }

    SmallTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = if (backdrop != null && isScrolled) Color.Transparent else colorScheme.surface,
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = barModifier,
    )
}

