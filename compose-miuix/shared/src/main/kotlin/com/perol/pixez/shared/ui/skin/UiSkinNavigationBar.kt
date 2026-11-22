package com.perol.pixez.shared.ui.skin

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRail
import com.perol.pixez.shared.ui.coui.core.CouiBarAccessory
import com.perol.pixez.shared.ui.coui.core.CouiBarItem
import com.perol.pixez.shared.ui.coui.core.CouiBottomBar
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.navigation.RootComponent

/**
 * 按一级标签映射到 COUI 专属的 `(未选中图标, 选中实心图标)` 对。
 */
internal fun couiNavIconsForTab(
    tab: RootComponent.MainTab?,
    fallback: ImageVector,
): Pair<ImageVector, ImageVector> = when (tab) {
    RootComponent.MainTab.Hello -> CouiIcons.Grid to CouiIcons.GridFill
    RootComponent.MainTab.Search -> CouiIcons.Search to CouiIcons.Search
    RootComponent.MainTab.Ranking -> CouiIcons.Rank to CouiIcons.RankFill
    RootComponent.MainTab.New -> CouiIcons.Recent to CouiIcons.RecentFill
    RootComponent.MainTab.Spotlight -> CouiIcons.Compass to CouiIcons.CompassFill
    null -> fallback to fallback
}

private fun List<SkinNavItem>.toCouiBarItems(): List<CouiBarItem> =
    map { item ->
        val (icon, selectedIcon) = couiNavIconsForTab(item.tab, item.icon)
        CouiBarItem(
            title = item.title,
            icon = icon,
            selectedIcon = selectedIcon,
            showBadge = item.showBadge,
        )
    }

@Composable
internal fun CouiSkinNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    isFloating: Boolean,
    onSelect: (Int) -> Unit,
    onLeftAccessoryClick: (() -> Unit)?,
    onRightAccessoryClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val leftAccessory = onLeftAccessoryClick?.let { onClick ->
        CouiBarAccessory(
            icon = CouiIcons.Settings,
            description = strings.tabSettings,
            onClick = onClick,
        )
    }
    val rightAccessory = onRightAccessoryClick?.let { onClick ->
        CouiBarAccessory(
            icon = CouiIcons.Search,
            description = strings.tabSearch,
            onClick = onClick,
        )
    }
    CouiBottomBar(
        items = items.toCouiBarItems(),
        selectedIndex = selectedIndex,
        isFloating = isFloating,
        onSelect = onSelect,
        leftAccessory = leftAccessory,
        rightAccessory = rightAccessory,
        badgeOverlay = { _ -> CouiBottomBarBadgeLayer(items) },
        modifier = modifier,
    )
}

@Composable
internal fun CouiSkinNavigationRail(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onSearchClick: (() -> Unit)?,
    modifier: Modifier,
) {
    CouiNavigationRail(
        items = items.toCouiBarItems(),
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        onSearchClick = onSearchClick,
        modifier = modifier,
    )
}

/**
 * 未读红点叠层（仅 API 34+ 的官方浮动底栏需要）。
 *
 * 官方 `NavigationBar` 的条目只接受图标与标题、没有内容槽，未读红点只能由本层在底栏之上只读叠绘，
 * 不接管点击与无障碍语义；位置按 [CouiNavigationBarMetrics] 复算，几何公式由
 * `NavigationBarBadgeMetricsTest` 守护。
 */
@RequiresApi(34)
@Composable
private fun BoxScope.CouiBottomBarBadgeLayer(items: List<SkinNavItem>) {
    val scheme = CouiTheme.colorScheme
    val maxMenuWidth = CouiNavigationBarMetrics.maximumMenuWidth(items.size)
    val bottomMargin = couiBottomBarBadgeMargin()
    Box(
        modifier = Modifier
            .matchParentSize()
            .windowInsetsPadding(
                WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(
                start = CouiNavigationBarMetrics.contentPadding,
                end = CouiNavigationBarMetrics.contentPadding,
                top = CouiNavigationBarMetrics.contentPadding,
                bottom = CouiNavigationBarMetrics.contentPadding + bottomMargin,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = maxMenuWidth + CouiNavigationBarMetrics.innerExtraWidth)
                .fillMaxWidth(),
        ) {
            val menuWidth = CouiNavigationBarMetrics.menuWidth(maxWidth)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CouiNavigationBarMetrics.rowHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.width(CouiNavigationBarMetrics.sideButtonWidth))
                Row(
                    modifier = Modifier
                        .width(menuWidth)
                        .height(CouiNavigationBarMetrics.rowHeight)
                        .padding(horizontal = CouiNavigationBarMetrics.menuHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(CouiNavigationBarMetrics.itemSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(CouiNavigationBarMetrics.itemHeight),
                        ) {
                            if (item.showBadge) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        // 图标 24 居中、红点 7：红点右边贴图标右边，中心右移 12 - 3.5 = 8.5；
                                        // 竖向沿用条目内图标顶部 4 的留白。
                                        .offset(
                                            x = CouiNavigationBarMetrics.badgeOffsetX(),
                                            y = CouiNavigationBarMetrics.badgeOffsetY(),
                                        )
                                        .size(CouiNavigationBarMetrics.badgeSize)
                                        .clip(RoundedCornerShape(50))
                                        .background(scheme.accent),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(CouiNavigationBarMetrics.sideButtonWidth))
            }
        }
    }
}

/** 与官方底栏一致：系统底部可点击区存在时不再额外留白，避免红点层与底栏错位。 */
@Composable
private fun couiBottomBarBadgeMargin(): Dp {
    val density = LocalDensity.current
    return if (WindowInsets.tappableElement.getBottom(density) > 0) 0.dp else 8.dp
}
