package com.perol.pixez.shared.ui.i18n

/**
 * 跨平台 String.format 扩展，支持 %s, %d, %f, %1$s, %1$d, %2$s 等标准占位符，
 * 兼容 JVM、Android、iOS、macOS 等全部 Kotlin Multiplatform 目标。
 *
 * 实现为一次从左到右的扫描，按 token 位置重建结果：
 * - 参数值只做追加，绝不参与正则替换（旧实现把参数当替换串交给 Regex.replace，
 *   参数里的 $ 会被当成捕获组引用而抛异常、反斜杠会被当转义吞掉）；
 * - %% 输出单个百分号且不消耗参数；
 * - 占位符多于参数时保留原样（不抛异常），参数多于占位符时忽略多余参数。
 */
fun String.format(vararg args: Any?): String {
    if (isEmpty() || indexOf('%') < 0) return this
    val out = StringBuilder(length + 16)
    var cursor = 0
    var sequentialArgument = 0
    while (cursor < length) {
        val current = this[cursor]
        if (current != '%') {
            out.append(current)
            cursor++
            continue
        }
        // 百分号字面量：%% -> %
        if (cursor + 1 < length && this[cursor + 1] == '%') {
            out.append('%')
            cursor += 2
            continue
        }
        val positional = parsePositionalPlaceholder(cursor)
        if (positional != null) {
            val argumentIndex = positional.index - 1
            if (argumentIndex in args.indices) {
                out.append(args[argumentIndex]?.toString() ?: "null")
            } else {
                out.append(positional.raw)
            }
            cursor = positional.endIndex
            continue
        }
        if (cursor + 1 < length && this[cursor + 1].isAsciiLetter()) {
            if (sequentialArgument < args.size) {
                out.append(args[sequentialArgument]?.toString() ?: "null")
            } else {
                // 参数不足：保留占位符原文，便于调用方发现漏传而不是抛异常中断渲染
                out.append('%').append(this[cursor + 1])
            }
            sequentialArgument++
            cursor += 2
            continue
        }
        out.append(current)
        cursor++
    }
    return out.toString()
}

/** %<n>$<letter> 形式的定位占位符；n 超出参数个数时按原文保留。 */
private class PositionalPlaceholder(val index: Int, val endIndex: Int, val raw: String)

private const val MaxPositionalDigits = 9

private fun String.parsePositionalPlaceholder(start: Int): PositionalPlaceholder? {
    var cursor = start + 1
    var digits = 0
    var index = 0
    while (cursor < length && this[cursor] in '0'..'9') {
        if (++digits > MaxPositionalDigits) return null
        index = index * 10 + (this[cursor] - '0')
        cursor++
    }
    if (digits == 0 || cursor >= length || this[cursor] != '$') return null
    cursor++
    if (cursor >= length || !this[cursor].isAsciiLetter() || index <= 0) return null
    return PositionalPlaceholder(index, cursor + 1, substring(start, cursor + 1))
}

private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

/**
 * 存储占用展示（B / KB / MB / GB）：KB、MB 保留一位小数，GB 保留两位小数。
 * 语义与 ui/screens 中长期并存的私有副本一致，供其接线后删除私有副本。
 */
fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${((kb * 10).toInt() / 10.0)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${((mb * 10).toInt() / 10.0)} MB"
    val gb = mb / 1024.0
    return "${((gb * 100).toInt() / 100.0)} GB"
}

/**
 * 跨平台文件字节大小格式化工具（支持 B / KB / MB / GB）。
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${(gb * 10).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10).toInt() / 10.0} MB"
        kb >= 1.0 -> "${(kb * 10).toInt() / 10.0} KB"
        else -> "$bytes B"
    }
}
