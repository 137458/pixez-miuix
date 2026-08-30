package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式毛玻璃模糊质感的大标题顶栏。
 *
 * 底层已重构接入官方规范的 [BlurredBar] 渐进式纹理毛玻璃实现，
 * 彻底消除 56.dp 硬编码物理截断、大标题展开镂空以及文字边缘黑斑问题。
 * 本组件作为平滑过渡兼容层保留，新页面推荐直接使用 [BlurredBar] + 原生 [TopAppBar]。
 */
@Deprecated(
    message = "Use BlurredBar with native TopAppBar instead.",
)
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
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    BlurredBar(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        blurEnabled = blurActive,
        scrollBehavior = scrollBehavior,
    ) {
        TopAppBar(
            title = title,
            scrollBehavior = scrollBehavior,
            color = barColor,
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 带有 Xiaomi HyperOS / MIUIX 沉浸式毛玻璃模糊质感的紧凑型小顶栏。
 */
@Deprecated(
    message = "Use BlurredBar with native SmallTopAppBar instead.",
)
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
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    BlurredBar(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        blurEnabled = blurActive,
        scrollBehavior = scrollBehavior,
    ) {
        SmallTopAppBar(
            title = title,
            scrollBehavior = scrollBehavior,
            color = barColor,
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
