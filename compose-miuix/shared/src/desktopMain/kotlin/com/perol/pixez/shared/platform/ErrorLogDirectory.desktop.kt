package com.perol.pixez.shared.platform

import java.io.File

actual fun errorLogDirectory(): String? =
    File(System.getProperty("user.home"), ".pixez/logs").absolutePath

/**
 * desktop 的 isDebugBuild 由系统属性 pixez.debug 运行期判定（见 [isDebugBuild]，默认 release 态）：
 * 默认 release 态不装 DebugAntilog、发布 EXE 无控制台，落盘为本通道唯一可观测出口（报告 中-12 :557）；
 * debug 态（-Dpixez.debug=true）与 DebugAntilog 双通道并存。故本 actual 不做 isDebugBuild 判断。
 */
actual fun installErrorLogAntilog() {
    installReleaseErrorAntilog(errorLogDirectory())
}
