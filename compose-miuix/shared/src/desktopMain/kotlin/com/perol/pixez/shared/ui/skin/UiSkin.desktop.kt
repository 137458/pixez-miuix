package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.state.MainFeedRestore

actual val isCouiSkinSupported: Boolean = false

@Composable
actual fun CouiSkinHost(enabled: Boolean, content: @Composable () -> Unit) {
    content()
}

@Composable
internal actual fun CouiMainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    // Desktop 不支持 COUI；调用方按 isCouiSkinSupported 保证不会进入此分支。
}

@Composable
internal actual fun CouiMainTabPage(
    tab: RootComponent.MainTab,
    component: RootComponent,
    feedRestore: MainFeedRestore,
    fallback: @Composable () -> Unit,
) {
    fallback()
}
