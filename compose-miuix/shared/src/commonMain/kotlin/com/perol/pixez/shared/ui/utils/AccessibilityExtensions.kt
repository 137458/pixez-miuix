package com.perol.pixez.shared.ui.utils

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 确保交互控件的触摸热区满足最小无障碍尺寸（默认 48x48 dp）。
 *
 * 符合 WCAG 2.1 AAA 级与 Android 原生无障碍规范，在保持 UI 视觉轻巧的同时，
 * 为小图标或紧凑按钮提供足够大的点击判定范围，避免手抖或大拇指误触。
 */
fun Modifier.accessibleTouchTarget(minTouchSize: Dp = 48.dp): Modifier =
    this.defaultMinSize(minWidth = minTouchSize, minHeight = minTouchSize)
