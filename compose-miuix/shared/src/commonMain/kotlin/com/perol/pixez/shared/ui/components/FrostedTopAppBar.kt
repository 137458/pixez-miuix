package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式毛玻璃模糊质感的大标题顶栏。
 *
 * 毛玻璃模糊仅作用于顶部固定高度标头区 (statusBars + 56.dp)，
 * 绝不下潜侵入下方的大标题与列表内容区，彻底消除模糊滤镜边缘对下方分类标题 (SmallTitle) 的切割与光晕块。
 * 采用 96% 高密度表面磨砂层，将滑入顶栏后方的文字充分柔化散射，杜绝暗色文字形成可见脏块。
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
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pinnedHeaderHeight = statusBarTop + 56.dp
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f

    // 表面不透明度与模糊随滚动折叠平滑过渡，未滚动折叠时保持透明，滚动折叠时渐入为 0.68f 通透毛玻璃
    val effectiveTintAlpha = if (scrollBehavior != null) {
        (collapsedFraction * 0.68f).coerceIn(0f, 0.68f)
    } else {
        0.68f
    }
    val effectiveBlurRadius = if (scrollBehavior != null) {
        (collapsedFraction * 25f).coerceIn(0f, 25f).dp
    } else {
        25.dp
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (backdrop != null && (scrollBehavior == null || collapsedFraction > 0.01f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pinnedHeaderHeight)
                    .backdropBlur(
                        backdrop = backdrop,
                        tintColor = colorScheme.surface,
                        tintAlpha = effectiveTintAlpha,
                        blurRadius = effectiveBlurRadius,
                    ),
            )
        }

        TopAppBar(
            title = title,
            scrollBehavior = scrollBehavior,
            color = if (backdrop != null) Color.Transparent else colorScheme.surface,
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pinnedHeaderHeight = statusBarTop + 56.dp
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f

    val effectiveTintAlpha = if (scrollBehavior != null) {
        (0.35f + collapsedFraction * 0.33f).coerceIn(0.35f, 0.68f)
    } else {
        0.68f
    }
    val effectiveBlurRadius = 25.dp

    Box(modifier = modifier.fillMaxWidth()) {
        if (backdrop != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pinnedHeaderHeight)
                    .backdropBlur(
                        backdrop = backdrop,
                        tintColor = colorScheme.surface,
                        tintAlpha = effectiveTintAlpha,
                        blurRadius = effectiveBlurRadius,
                    ),
            )
        }

        SmallTopAppBar(
            title = title,
            scrollBehavior = scrollBehavior,
            color = if (backdrop != null) Color.Transparent else colorScheme.surface,
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

