package com.perol.pixez.shared.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * 系统返回事件处理器，桥接 [androidx.activity.compose.BackHandler]。
 */
@Composable
fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
