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
 * 采用静态 85% 表面磨砂色与纯净 Backdrop 采样，与主悬浮底栏完全对齐，
 * 消除滚动时的透明度动画抖动与抽动。
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
    val outlineAlpha = (collapsedFraction * 0.15f).coerceIn(0f, 0.15f)
    val outlineColor = colorScheme.outline

    val barModifier = modifier
        .topAppBarBlur(
            backdrop = backdrop,
            tintColor = colorScheme.surface,
            tintAlpha = 0.85f,
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

    TopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = if (backdrop != null) Color.Transparent else colorScheme.surface,
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
    val outlineAlpha = (collapsedFraction * 0.15f).coerceIn(0f, 0.15f)
    val outlineColor = colorScheme.outline

    val barModifier = modifier
        .topAppBarBlur(
            backdrop = backdrop,
            tintColor = colorScheme.surface,
            tintAlpha = 0.85f,
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

    SmallTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = if (backdrop != null) Color.Transparent else colorScheme.surface,
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = barModifier,
    )
}

