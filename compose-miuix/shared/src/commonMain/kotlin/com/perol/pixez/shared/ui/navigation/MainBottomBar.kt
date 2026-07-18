package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem

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
            icon = Icons.Default.Home,
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.Search,
            activeTab = activeTab,
            label = "搜索",
            icon = Icons.Default.Search,
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.Ranking,
            activeTab = activeTab,
            label = "排行榜",
            icon = Icons.Outlined.Explore,
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.New,
            activeTab = activeTab,
            label = "最新",
            icon = Icons.Default.Newspaper,
            onClick = onTabSelected,
        )
        MainTabItem(
            tab = RootComponent.MainTab.Spotlight,
            activeTab = activeTab,
            label = "Spotlight",
            icon = Icons.Outlined.Lightbulb,
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
