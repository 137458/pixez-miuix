package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.perol.pixez.shared.ui.components.IosLiquidGlassNavigationBar
import com.perol.pixez.shared.ui.components.backdropBlur
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBottomBarVisibility = compositionLocalOf { mutableStateOf(true) }

/**
 * 底部 5 标签导航栏。
 * - 悬浮模式 (isFloating = true)：与官方 compose-miuix-ui 示例 1:1 对齐的 IosLiquidGlassNavigationBar。
 * - 标准模式 (isFloating = false)：使用原生 MIUIX NavigationBar + 背景毛玻璃模糊。
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

    val mainTabs = remember(strings) {
        listOf(
            RootComponent.MainTab.Hello to (strings.tabRecommend to MiuixIcons.All),
            RootComponent.MainTab.Search to (strings.tabSearch to MiuixIcons.Search),
            RootComponent.MainTab.Ranking to (strings.tabRanking to MiuixIcons.TopDownloads),
            RootComponent.MainTab.New to (strings.tabNew to MiuixIcons.Recent),
            RootComponent.MainTab.Spotlight to (strings.tabSpotlight to MiuixIcons.Promotions),
        )
    }

    if (isFloating) {
        val currentPosition = mainTabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
        val navItems = remember(mainTabs) {
            mainTabs.map { (_, pair) ->
                val (label, icon) = pair
                NavigationItem(label = label, icon = icon)
            }
        }

        IosLiquidGlassNavigationBar(
            items = navItems,
            selectedIndex = currentPosition,
            onItemClick = { index ->
                mainTabs.getOrNull(index)?.let { (tab, _) ->
                    onTabSelected(tab)
                }
            },
            backdrop = backdrop,
            isBlurActive = backdrop != null,
            refractionLevel = refractionLevel,
            badge = { index ->
                if (mainTabs.getOrNull(index)?.first == RootComponent.MainTab.New) {
                    { Badge() }
                } else null
            },
            modifier = modifier,
        )

    } else {
        // 标准固定底栏：应用 Backdrop Blur 毛玻璃效果
        val bottomBarModifier = if (backdrop != null) {
            modifier
                .fillMaxWidth()
                .backdropBlur(
                    backdrop = backdrop,
                    tintColor = colorScheme.surface,
                    tintAlpha = 0.85f,
                )
        } else {
            modifier.fillMaxWidth()
        }

        val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
        NavigationBar(
            modifier = bottomBarModifier,
            color = if (backdrop != null) Color.Transparent else colorScheme.surface,
        ) {
            mainTabs.forEach { (tab, pair) ->
                val (label, icon) = pair
                NavigationBarItem(
                    selected = activeTab == tab,
                    onClick = {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onTabSelected(tab)
                    },
                    icon = icon,
                    label = label,
                    badge = if (tab == RootComponent.MainTab.New) {
                        { Badge() }
                    } else null,
                )
            }
        }
    }
}
