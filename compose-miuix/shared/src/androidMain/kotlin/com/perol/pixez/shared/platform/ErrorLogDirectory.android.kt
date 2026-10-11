package com.perol.pixez.shared.platform

import java.io.File

actual fun errorLogDirectory(): String? =
    BrowserLauncherContext.applicationContext
        ?.let { context -> File(context.filesDir, "logs").absolutePath }

/**
 * Android 按 debug/release 二选一装配（架构审查 中-12 实施步骤 2）：
 * debug 保持全量 DebugAntilog、不落盘；release 安装错误级 Antilog（W/E 级、脱敏、滚动 5MB×3）。
 */
actual fun installErrorLogAntilog() {
    if (isDebugBuild) return
    installReleaseErrorAntilog(errorLogDirectory())
}
