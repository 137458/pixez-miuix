package com.perol.pixez.shared.platform

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import java.io.File

/**
 * 将错误级日志 Antilog 安装到 Napier（架构审查 中-12，裁决 2026-10-11）。
 *
 * [logDirectory] 为 null（平台不可用）时跳过；由 androidMain/desktopMain 的
 * `installErrorLogAntilog` actual 薄壳委托调用，装配点集中在 [com.perol.pixez.shared.AppDependencies]。
 */
internal fun installReleaseErrorAntilog(logDirectory: String?) {
    if (logDirectory == null) return
    Napier.takeLogarithm(ReleaseErrorAntilog(File(logDirectory)))
}

/**
 * Release 错误级日志 Antilog（架构审查 中-12，裁决 2026-10-11）。
 *
 * 仅放行 W/E 级，经 [ErrorLogSanitizer] 红线脱敏后写入 [logDirectory] 下的滚动文件
 * app.log（单文件上限 [MAX_BYTES]，滚动保留 [MAX_FILES] 个），日志仅落应用私有目录、
 * 不自动外发，导出属用户主动行为；落盘失败静默降级，不影响主流程。
 */
internal class ReleaseErrorAntilog(
    private val logDirectory: File?,
    private val maxBytes: Long = MAX_BYTES,
    private val maxFiles: Int = MAX_FILES,
) : Antilog() {

    private val writeLock = Any()

    override fun isEnable(priority: LogLevel, tag: String?): Boolean =
        priority == LogLevel.WARNING || priority == LogLevel.ERROR

    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        val dir = logDirectory ?: return
        try {
            val line = buildString {
                append(Clock.System.now().toString())
                append(" [")
                append(priority.name.first())
                append('/')
                append(tag ?: "-")
                append("] ")
                appendLine(ErrorLogSanitizer.sanitize(message).orEmpty())
                if (throwable != null) {
                    appendLine(ErrorLogSanitizer.sanitize(throwable.stackTraceToString()).orEmpty())
                }
            }
            synchronized(writeLock) { appendWithRotation(dir, line) }
        } catch (_: Exception) {
            // 日志落盘失败属可容忍降级（磁盘满/目录不可写等），静默放弃，绝不影响主流程。
        }
    }

    private fun appendWithRotation(dir: File, line: String) {
        if (!dir.exists()) dir.mkdirs()
        val current = File(dir, LOG_FILE_NAME)
        if (current.exists() && current.length() + line.toByteArray(Charsets.UTF_8).size > maxBytes) {
            rotate(dir)
        }
        // 单条日志超过 maxBytes 时仍完整写入，保证单条现场不被截断丢失。
        current.appendText(line)
    }

    /** 轮转：app.log.2 删除，app.log.1→app.log.2，app.log→app.log.1，随后 app.log 重新开始。 */
    private fun rotate(dir: File) {
        File(dir, "$LOG_FILE_NAME.${maxFiles - 1}").delete()
        for (index in maxFiles - 2 downTo 1) {
            val from = File(dir, "$LOG_FILE_NAME.$index")
            if (from.exists()) from.renameTo(File(dir, "$LOG_FILE_NAME.${index + 1}"))
        }
        File(dir, LOG_FILE_NAME).renameTo(File(dir, "$LOG_FILE_NAME.1"))
    }

    companion object {
        /** 单个日志文件字节上限：5MB。 */
        const val MAX_BYTES: Long = 5L * 1024 * 1024

        /** 滚动保留的文件数（含当前 app.log）：共 3 个。 */
        const val MAX_FILES: Int = 3

        /** 当前活跃日志文件名，轮转后为 app.log.1、app.log.2。 */
        const val LOG_FILE_NAME: String = "app.log"
    }
}
