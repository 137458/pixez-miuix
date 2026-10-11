package com.perol.pixez.shared.platform

/**
 * 错误级日志目录（架构审查 中-12，裁决 2026-10-11）。
 *
 * Android 为 `context.filesDir/logs`，desktop 为 `~/.pixez/logs`；目录由写入方按需创建，
 * 仅落应用私有目录、不自动外发。返回 null 表示平台不可用，调用方跳过落盘。
 */
expect fun errorLogDirectory(): String?

/**
 * 安装错误级日志 Antilog（仅 W/E 级、红线脱敏、滚动 5MB×3）到 Napier；
 * 平台不可用（[errorLogDirectory] 返回 null）时为空操作。
 */
expect fun installErrorLogAntilog()
