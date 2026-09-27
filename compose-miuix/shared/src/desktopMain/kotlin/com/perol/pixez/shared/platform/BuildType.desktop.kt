package com.perol.pixez.shared.platform

/** 桌面 JVM 无构建类型区分，按 debug 处理（保留 stdout 排障日志，错误响应体已统一截断）。 */
actual val isDebugBuild: Boolean
    get() = true
