package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 标准毛玻璃顶栏页面骨架：收敛各页手抄的 Scaffold + BlurredBar + TopAppBar + 返回键 + 透明色判定样板（架构审查 中-10）。
 *
 * - [backdrop] 非空时 TopAppBar 背景透明，由 [BlurredBar] 按滚动偏移淡入毛玻璃；为 null（平台不支持模糊）时自动降级 surface 实色；
 * - [onBack] 非空时在导航槽注入返回键；为 null（如首页等根页面）时导航槽留空，与省略 navigationIcon 参数的布局度量一致；
 * - 内容区滚动联动（nestedScroll / PullToRefresh 的 topAppBarScrollBehavior）由调用方持有并传入同一 [scrollBehavior] 实例。
 */
@Composable
fun BlurTopBarScaffold(
    title: String,
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val strings = LocalStrings.current
    Scaffold(
        modifier = modifier,
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = title,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                    navigationIcon = {
                        onBack?.let { back ->
                            IconButton(onClick = back) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = strings.back,
                                )
                            }
                        }
                    },
                    actions = actions,
                )
            }
        },
        bottomBar = bottomBar,
        content = content,
    )
}
