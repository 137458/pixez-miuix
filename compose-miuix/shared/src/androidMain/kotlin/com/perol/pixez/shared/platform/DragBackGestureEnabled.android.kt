package com.perol.pixez.shared.platform

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Settings.Secure 的 NAVIGATION_MODE / NAVIGATION_MODE_THREE_BUTTON 常量是 hidden API，
// 公共 SDK 用字符串 key 读取：0 = 三键导航，1 = 两键（pill），2 = 手势导航。
private const val KEY_NAVIGATION_MODE = "navigation_mode"
private const val NAV_MODE_THREE_BUTTON = 0

// API < 28 无此设置项：其时不存在手势导航，视为可用。
private const val NAV_MODE_NOT_PRESENT = -2

@Composable
actual fun rememberDragBackGestureEnabled(): Boolean {
    val context = LocalContext.current
    // 手势导航（含两键 pill）的边缘横拖由系统 predictive back 接管，
    // 应用内拖拽仅在三键导航（或无此设置的旧系统）启用，避免双重响应。
    return remember {
        runCatching {
            val mode = Settings.Secure.getInt(context.contentResolver, KEY_NAVIGATION_MODE, NAV_MODE_NOT_PRESENT)
            mode == NAV_MODE_THREE_BUTTON || mode == NAV_MODE_NOT_PRESENT
        }.getOrDefault(false)
    }
}
