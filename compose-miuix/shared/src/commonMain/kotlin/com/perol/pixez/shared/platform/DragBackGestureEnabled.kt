package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable

/**
 * 应用内拖拽返回手势是否可用（平台开关）。
 *
 * desktop 恒可用（无系统手势竞争）；Android 仅三键导航机型可用——
 * 手势导航机型的边缘拖拽返回由系统预测性返回承担，应用内再监听会双重响应；
 * iOS 目标仅编译验证，不启用。
 */
@Composable
expect fun rememberDragBackGestureEnabled(): Boolean
