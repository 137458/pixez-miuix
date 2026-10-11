package com.perol.pixez.shared.platform

/** iOS 仅编译验证、当前未启用该目标，错误日志落盘不接入：目录返回 null、安装为空操作。 */
actual fun errorLogDirectory(): String? = null

actual fun installErrorLogAntilog() = Unit
