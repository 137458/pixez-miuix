package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式磨砂质感与动态滚动感知的大标题顶栏。
 */
@Composable
fun FrostedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colorScheme = MiuixTheme.colorScheme
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f

    val targetAlpha = remember(collapsedFraction) {
        if (collapsedFraction > 0.01f) {
            0.78f + (collapsedFraction * 0.12f).coerceAtMost(0.12f)
        } else {
            1.0f
        }
    }
    val animatedAlpha by animateFloatAsState(targetAlpha, label = "FrostedTopBarAlpha")
    val outlineAlpha = (collapsedFraction * 0.25f).coerceIn(0f, 0.25f)
    val outlineColor = colorScheme.outline

    TopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = colorScheme.surface.copy(alpha = animatedAlpha),
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = modifier.drawBehind {
            if (outlineAlpha > 0.01f) {
                drawLine(
                    color = outlineColor.copy(alpha = outlineAlpha),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
        },
    )
}

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式磨砂质感的紧凑型小顶栏。
 */
@Composable
fun FrostedSmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colorScheme = MiuixTheme.colorScheme
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f

    val targetAlpha = remember(collapsedFraction) {
        if (collapsedFraction > 0.01f) {
            0.82f
        } else {
            1.0f
        }
    }
    val animatedAlpha by animateFloatAsState(targetAlpha, label = "FrostedSmallTopBarAlpha")
    val outlineAlpha = (collapsedFraction * 0.25f).coerceIn(0f, 0.25f)
    val outlineColor = colorScheme.outline

    SmallTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        color = colorScheme.surface.copy(alpha = animatedAlpha),
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = modifier.drawBehind {
            if (outlineAlpha > 0.01f) {
                drawLine(
                    color = outlineColor.copy(alpha = outlineAlpha),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
        },
    )
}
