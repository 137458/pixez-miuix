package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.state.MainFeedRestore

/** 底栏单项：文案与图标由一级标签目录求值后传入，避免皮肤层反向依赖导航目录。 */
internal data class SkinNavItem(
    val title: String,
    val icon: ImageVector,
    val showBadge: Boolean,
)

/**
 * COUI 底栏。Android 在 API 34+ 使用 COUI 官方 NavigationBar，API 31-33 用自绘胶囊平替。
 */
@Composable
internal expect fun CouiMainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
)

/**
 * 一级标签页的 COUI 宿主：COUI 实现不可用时（或标签尚未迁移）执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiMainTabPage(
    tab: RootComponent.MainTab,
    component: RootComponent,
    feedRestore: MainFeedRestore,
    fallback: @Composable () -> Unit,
)
