package com.perol.pixez.shared.platform

import java.io.File

/**
 * 错误日志文件工具：崩溃兜底落盘（架构审查 中-12，对齐 desktop main.kt 的 crash-*.log 行为）。
 */
object ErrorLogFiles {

    /**
     * 将未捕获异常写入 [logDirectory] 下的 crash-<时间戳>.log，返回落盘文件；失败返回 null。
     * 崩溃堆栈同样经 [ErrorLogSanitizer] 红线脱敏。
     */
    fun writeCrashLog(logDirectory: File, threadName: String, throwable: Throwable): File? = try {
        logDirectory.mkdirs()
        val file = File(logDirectory, "crash-${System.currentTimeMillis()}.log")
        file.writeText(
            buildString {
                appendLine("Thread: $threadName")
                append(ErrorLogSanitizer.sanitize(throwable.stackTraceToString()).orEmpty())
            },
        )
        file
    } catch (_: Exception) {
        // 崩溃兜底已是最后一道防线，落盘失败只能放弃，不得干扰原有崩溃处理链。
        null
    }
}
