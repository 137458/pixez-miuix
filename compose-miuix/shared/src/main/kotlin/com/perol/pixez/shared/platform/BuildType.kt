package com.perol.pixez.shared.platform

import android.content.pm.ApplicationInfo

/**
 * 当前构建是否为可调试构建（debug）。
 *
 * 以应用 debuggable 标志判定构建类型：debug 构建为 true，release（含 debug 签名）为 false。
 * 上下文未初始化时按 debug 处理，避免 release 日志误开（错误响应体已统一截断脱敏）。
 */
val isDebugBuild: Boolean
    get() = runCatching {
        val context = BrowserLauncherContext.applicationContext
        context == null || context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }.getOrDefault(true)
