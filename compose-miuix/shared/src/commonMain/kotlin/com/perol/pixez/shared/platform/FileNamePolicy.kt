package com.perol.pixez.shared.platform

/** Validates names before they are used as a child of an application-owned directory. */
object FileNamePolicy {
    fun requireSafeBaseName(raw: String): String {
        val name = raw.trim()
        require(name.isNotEmpty()) { "文件名不能为空" }
        require(name != "." && name != "..") { "文件名无效" }
        require('/' !in name && '\\' !in name) { "文件名不得包含目录分隔符" }
        require(".." !in name) { "文件名不得包含路径遍历片段" }
        require(name.none { it == '\u0000' || it.isISOControl() }) { "文件名包含非法控制字符" }
        return name
    }
}
