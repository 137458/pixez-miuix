package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable

/**
 * 跨平台系统返回事件处理器。
 *
 * Android 上桥接 [androidx.activity.compose.BackHandler]，其余平台为空实现。
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
