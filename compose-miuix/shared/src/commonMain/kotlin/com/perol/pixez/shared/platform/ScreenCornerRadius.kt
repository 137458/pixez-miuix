package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * 获取当前设备屏幕硬件级的物理圆角半径（以 Dp 为单位）。
 * - Android 12+ (API 31+)：通过系统 WindowInsets 动态获取物理屏幕四个角落的真实弧度；
 * - 直角屏幕 / 小圆角设备 / 低于 Android 12 / 桌面端：返回实际检测到的硬件半径或 0.dp。
 */
@Composable
expect fun rememberScreenCornerRadius(): Dp
