package com.perol.pixez.shared.ui.screens

/**
 * 跨平台 String.format 扩展，支持 %s, %d, %f, %1$s, %1$d, %2$s 等标准占位符，
 * 兼容 JVM、Android、iOS、macOS 等全部 Kotlin Multiplatform 目标。
 */
fun String.format(vararg args: Any?): String {
    if (args.isEmpty()) return this
    var result = this
    // 1. 处理位置占位符 %1$s, %2$d, %1$s 等
    for (i in args.indices) {
        val argStr = args[i]?.toString() ?: "null"
        val posPattern = Regex("%" + (i + 1) + "\\$[a-zA-Z]")
        result = result.replace(posPattern, argStr)
    }
    // 2. 处理顺序占位符 %s, %d, %f 等
    val simplePattern = Regex("%[a-zA-Z]")
    for (arg in args) {
        val match = simplePattern.find(result) ?: break
        val argStr = arg?.toString() ?: "null"
        result = result.replaceRange(match.range, argStr)
    }
    return result
}
