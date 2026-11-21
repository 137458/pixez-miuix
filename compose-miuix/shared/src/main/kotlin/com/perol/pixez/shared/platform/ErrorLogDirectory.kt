package com.perol.pixez.shared.platform

import java.io.File

/**
 * 错误级日志目录（架构审查 中-12，裁决 2026-10-11）。
 *
 * Android 为 `context.filesDir/logs`；目录由写入方按需创建，
 * 仅落应用私有目录、不自动外发。返回 null 表示不可用，调用方跳过落盘。
 */
fun errorLogDirectory(): String? =
    BrowserLauncherContext.applicationContext
        ?.let { context -> File(context.filesDir, "logs").absolutePath }

/**
 * 安装错误级日志 Antilog（仅 W/E 级、红线脱敏、滚动 5MB×3）到 Napier；
 * 不可用（[errorLogDirectory] 返回 null）时为空操作。
 *
 * Android 按 debug/release 二选一装配（架构审查 中-12 实施步骤 2）：
 * debug 保持全量 DebugAntilog、不落盘；release 安装错误级 Antilog（W/E 级、脱敏、滚动 5MB×3）。
 */
fun installErrorLogAntilog() {
    if (isDebugBuild) return
    installReleaseErrorAntilog(errorLogDirectory())
}
