package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem

/**
 * Xiaomi HyperOS / MIUIX 桌面端与平板端侧边导航栏 (NavigationRail)。
 * 当屏幕宽度 >= 600dp (宽屏/平板/桌面) 时，自动将底栏转换为左侧原生 MIUIX 侧边栏。
 *
 * 标签文案、图标与徽标条件统一读 [MAIN_TAB_DESCRIPTORS]（nav13），与底栏 [MainBottomBar]
 * 及 Pager 同源。
 */
@Composable
fun MainNavigationRail(
    activeTab: RootComponent.MainTab,
    onTabSelected: (RootComponent.MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    val descriptors = MAIN_TAB_DESCRIPTORS

    val settingsRepository = com.perol.pixez.shared.ui.LocalSettingsRepository.current
    val hasUnreadFeedBadge = settingsRepository.hasUnreadFeedBadge

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
    ) {
        descriptors.forEach { descriptor ->
            NavigationRailItem(
                selected = activeTab == descriptor.tab,
                onClick = { onTabSelected(descriptor.tab) },
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
