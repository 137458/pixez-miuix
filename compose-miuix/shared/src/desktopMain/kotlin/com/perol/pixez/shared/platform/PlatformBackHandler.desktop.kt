package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop 平台无物理返回键，通过 UI 上的返回按钮操作。
}
