package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRail
import com.perol.pixez.shared.ui.coui.core.CouiBarItem
import com.perol.pixez.shared.ui.coui.core.CouiBottomBar
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * 导航栏适配层（底栏 + 宽屏侧栏）。
 *
 * 将宿主传入的 5 个一级标签按顺序映射为 COUI 专属线性图标 [CouiIcons]，
 * 避免底栏与侧栏在 ColorOS 皮肤下继续呈现 MIUIX 图标形态。
 */
private fun couiNavIconForIndex(index: Int, fallback: ImageVector): ImageVector = when (index) {
    0 -> CouiIcons.Grid
    1 -> CouiIcons.Search
    2 -> CouiIcons.Rank
    3 -> CouiIcons.Recent
    4 -> CouiIcons.Compass
    else -> fallback
}

private fun List<SkinNavItem>.toCouiBarItems(): List<CouiBarItem> =
    mapIndexed { index, item ->
        CouiBarItem(
            title = item.title,
            icon = couiNavIconForIndex(index, item.icon),
            showBadge = item.showBadge,
        )
    }

@Composable
internal fun CouiSkinNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    isFloating: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    CouiBottomBar(
        items = items.toCouiBarItems(),
        selectedIndex = selectedIndex,
        isFloating = isFloating,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
internal fun CouiSkinNavigationRail(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    CouiNavigationRail(
        items = items.toCouiBarItems(),
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
    )
}
