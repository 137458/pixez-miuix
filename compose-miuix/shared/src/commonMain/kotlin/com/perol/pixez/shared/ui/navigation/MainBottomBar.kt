package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 底部 5 标签导航栏，与原 Flutter 应用底部导航顺序一致。
 */
@Composable
fun MainBottomBar(
    activeTab: RootComponent.MainTab,
    onTabSelected: (RootComponent.MainTab) -> Unit,
) {
    NavigationBar {
        MainTabItem(
            tab = RootComponent.MainTab.Hello,
            activeTab = activeTab,
            label = "首页",
            icon = MiuixIcons.All, // 首页：MIUIX 无 Home，用 All（全部/汇总）语义最接近
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.Search,
            activeTab = activeTab,
            label = "搜索",
            icon = MiuixIcons.Search,
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.Ranking,
            activeTab = activeTab,
            label = "排行榜",
            icon = MiuixIcons.TopDownloads, // 排行榜/探索：用 TopDownloads（热门下载）语义最接近
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.New,
            activeTab = activeTab,
            label = "最新",
            icon = MiuixIcons.Recent, // 最新：用 Recent（最近）语义最接近
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.Spotlight,
            activeTab = activeTab,
            label = "Spotlight",
            icon = MiuixIcons.Promotions, // Spotlight：用 Promotions（推荐/精选）语义最接近
            onClick = onTabSelected,
        )
    }
}

@Composable
private fun RowScope.MainTabItem(
    tab: RootComponent.MainTab,
    activeTab: RootComponent.MainTab,
    label: String,
    icon: ImageVector,
    onClick: (RootComponent.MainTab) -> Unit,
) {
    NavigationBarItem(
        selected = tab == activeTab,
        onClick = { onClick(tab) },
        icon = icon,
        label = label,
    )
}
