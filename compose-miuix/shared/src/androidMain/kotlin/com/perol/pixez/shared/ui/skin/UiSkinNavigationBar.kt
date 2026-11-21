package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.coui.core.CouiBarItem
import com.perol.pixez.shared.ui.coui.core.CouiBottomBar

/**
 * 底栏适配层。
 *
 * 版式、材质、未读红点、系统栏内边距与 API 降级全部由 [CouiBottomBar] 负责，
 * 这里只把宿主侧的 [SkinNavItem] 翻译成基础层条目。
 */
@Composable
internal fun CouiSkinNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    CouiBottomBar(
        items = items.map { CouiBarItem(title = it.title, icon = it.icon, showBadge = it.showBadge) },
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
    )
}
