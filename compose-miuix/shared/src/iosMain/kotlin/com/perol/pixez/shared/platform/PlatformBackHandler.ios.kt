package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS 平台手势与返回通过系统原生或 UI 按钮处理。
}
