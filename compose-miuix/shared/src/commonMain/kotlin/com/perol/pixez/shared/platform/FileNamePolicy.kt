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

    /**
     * 清洗文件名片段中的非法字符（如 \\ / : * ? " < > | 及控制字符），替换为下划线。
     *
     * 控制字符覆盖 C0（U+0000–U+001F）与 C1（U+007F–U+009F）两段，与 [requireSafeBaseName]
     * 的 `Char.isISOControl` 判定面对齐：否则清洗会放过 DEL/C1，却被保存前的安全校验拒绝，
     * 最终表现为下载失败而非"剔除后照常保存"。连续点同理折叠为下划线，避免命中路径遍历校验。
     */
    fun sanitizeSegment(raw: String): String {
        val illegalChars = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F-\u009F]""")
        val clean = illegalChars.replace(raw, "_").trim()
        if (clean.isEmpty() || clean == "." || clean == "..") {
            return "untitled"
        }
        return clean.replace(Regex("\\.{2,}"), "_")
    }
}
