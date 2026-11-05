package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.components.IosLiquidGlassNavigationBar
import com.perol.pixez.shared.ui.components.backdropBlur
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBottomBarVisibility = compositionLocalOf { mutableStateOf(true) }

/**
 * 主页 Pager 手势翻页期间的连续标签位置（currentPage + currentPageOffsetFraction）；
 * 非手势滚动时为 null。底栏滑块在 draw 阶段读取：非 null 时直驱滑块实现零延迟跟手，
 * 点击切换仍走 DampedDragAnimation 的 spring 动画，两者互不打断。
 */
val LocalTabGesturePosition = compositionLocalOf { mutableStateOf<Float?>(null) }

/**
 * 底部 5 标签导航栏。
 * - 悬浮模式 (isFloating = true)：与 InstallerX-Revived / compose-miuix-ui 对齐的 Liquid Glass 液态玻璃悬浮导航栏。
 * - 标准模式 (isFloating = false)：使用原生 MIUIX NavigationBar + 背景毛玻璃模糊。
 *
 * 标签文案、图标与徽标条件统一读 [MAIN_TAB_DESCRIPTORS]（nav13），与侧栏 [MainNavigationRail]
 * 及 Pager（MainContent）同源，本文件不再维护第二份标签表。
 */
@Composable
fun MainBottomBar(
    activeTab: RootComponent.MainTab,
    onTabSelected: (RootComponent.MainTab) -> Unit,
    modifier: Modifier = Modifier,
    isFloating: Boolean = true,
    refractionLevel: Int = 2,
    backdrop: Backdrop? = null,
) {
    val colorScheme = MiuixTheme.colorScheme
    val strings = LocalStrings.current

    val descriptors = MAIN_TAB_DESCRIPTORS

    val settingsRepository = com.perol.pixez.shared.ui.LocalSettingsRepository.current
    val hasUnreadFeedBadge = settingsRepository.hasUnreadFeedBadge

    if (isFloating) {
        val navItems = remember(strings) {
            descriptors.map { descriptor ->
                NavigationItem(label = descriptor.label(strings), icon = descriptor.icon)
            }
        }
        val selectedIndex = descriptors.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)

        IosLiquidGlassNavigationBar(
            items = navItems,
            selectedIndex = selectedIndex,
            onItemClick = { index ->
                val target = descriptors.getOrNull(index) ?: return@IosLiquidGlassNavigationBar
                onTabSelected(target.tab)
            },
            backdrop = backdrop,
            modifier = modifier,
            badge = { index ->
                val descriptor = descriptors.getOrNull(index)
                if (descriptor != null &&
                    shouldShowUnreadFeedBadge(descriptor, activeTab, hasUnreadFeedBadge)
                ) {
                    { Badge() }
                } else {
                    null
                }
            },
        )
    } else {
        // 标准固定底栏：应用 Backdrop Blur 96% 高密度毛玻璃与顶部分割线
        val bottomBarModifier = if (backdrop != null) {
            modifier
                .fillMaxWidth()
                .backdropBlur(
                    backdrop = backdrop,
                    tintColor = colorScheme.surface,
                    tintAlpha = 0.96f,
                    blurRadius = 20.dp,
                )
                .drawBehind {
                    drawLine(
                        color = colorScheme.outline.copy(alpha = 0.12f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f,
                    )
                }
        } else {
            modifier.fillMaxWidth()
        }

        val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
        NavigationBar(
            modifier = bottomBarModifier,
            color = if (backdrop != null) Color.Transparent else colorScheme.surface,
        ) {
            descriptors.forEach { descriptor ->
                NavigationBarItem(
                    selected = activeTab == descriptor.tab,
                    onClick = {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onTabSelected(descriptor.tab)
                    },
                    icon = descriptor.icon,
                    label = descriptor.label(strings),
                    badge = if (shouldShowUnreadFeedBadge(descriptor, activeTab, hasUnreadFeedBadge)) {
                        { Badge() }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
