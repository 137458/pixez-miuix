package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * Xiaomi HyperOS / MIUIX 桌面端与平板端侧边导航栏 (NavigationRail)。
 * 当屏幕宽度 >= 600dp (宽屏/平板/桌面) 时，自动将底栏转换为左侧原生 MIUIX 侧边栏。
 */
@Composable
fun MainNavigationRail(
    activeTab: RootComponent.MainTab,
    onTabSelected: (RootComponent.MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
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

    val settingsRepository = com.perol.pixez.shared.data.settings.LocalSettingsRepository.current
    val hasUnreadBadge = (settingsRepository?.hasUnreadFeedBadge == true) && activeTab != RootComponent.MainTab.New

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
    ) {
        mainTabs.forEach { (tab, pair) ->
            val (label, icon) = pair
            NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                icon = icon,
                label = label,
                badge = if (hasUnreadBadge && tab == RootComponent.MainTab.New) {
                    { top.yukonga.miuix.kmp.basic.Badge() }
                } else null,
            )
        }
    }
}
