package com.perol.pixez.shared.platform

import java.io.File

/**
 * 错误日志文件工具：崩溃兜底落盘（架构审查 中-12，对齐 desktop main.kt 的 crash-*.log 行为）。
 */
object ErrorLogFiles {

    /** 崩溃日志保留份数上限：与 [ReleaseErrorAntilog] 错误日志的滚动保留份数一致，共 3 份。 */
    internal const val MAX_CRASH_FILES: Int = 3

    private const val CRASH_LOG_PREFIX = "crash-"
    private const val CRASH_LOG_SUFFIX = ".log"

    /**
     * 将未捕获异常写入 [logDirectory] 下的 crash-<时间戳>.log，返回落盘文件；失败返回 null。
     * 崩溃堆栈同样经 [ErrorLogSanitizer] 红线脱敏。
     * 保留策略与错误日志滚动一致：仅保留最新 [MAX_CRASH_FILES] 份，超出删除最旧，防止无限累积。
     */
    fun writeCrashLog(logDirectory: File, threadName: String, throwable: Throwable): File? = try {
        logDirectory.mkdirs()
        val file = File(logDirectory, "$CRASH_LOG_PREFIX${System.currentTimeMillis()}$CRASH_LOG_SUFFIX")
        file.writeText(
            buildString {
                appendLine("Thread: $threadName")
                append(ErrorLogSanitizer.sanitize(throwable.stackTraceToString()).orEmpty())
            },
        )
        retainNewestCrashLogs(logDirectory)
        file
    } catch (_: Exception) {
        // 崩溃兜底已是最后一道防线，落盘失败只能放弃，不得干扰原有崩溃处理链。
        null
    }

    /**
     * 仅保留目录下最新 [MAX_CRASH_FILES] 份 crash-*.log，删除更早的；文件名内时间戳为定长
     * 毫秒值，按名称降序即按时间降序。清理在写盘成功后执行，失败静默降级，不干扰崩溃处理链。
     */
    private fun retainNewestCrashLogs(logDirectory: File) {
        logDirectory
            .listFiles { file -> file.name.startsWith(CRASH_LOG_PREFIX) && file.name.endsWith(CRASH_LOG_SUFFIX) }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_CRASH_FILES)
            ?.forEach { it.delete() }
    }
}
