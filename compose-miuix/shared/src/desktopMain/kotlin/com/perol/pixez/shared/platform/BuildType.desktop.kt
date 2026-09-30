package com.perol.pixez.shared.platform

/**
 * 桌面端以系统属性 pixez.debug 判定（-Dpixez.debug=true），默认视为发布构建：
 * 不安装全量日志，避免 jpackage 发布版向 stdout 泄露 URL/参数并拖累性能。
 * 开发运行可在 Gradle run 任务或 IDE 中加该属性开启。
 */
actual val isDebugBuild: Boolean
    get() = System.getProperty("pixez.debug")?.equals("true", ignoreCase = true) == true
