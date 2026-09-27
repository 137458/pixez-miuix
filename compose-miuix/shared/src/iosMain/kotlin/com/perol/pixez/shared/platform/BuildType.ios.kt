package com.perol.pixez.shared.platform

/** iOS 无 debug/release 构建类型区分，按 debug 处理。 */
actual val isDebugBuild: Boolean
    get() = true
