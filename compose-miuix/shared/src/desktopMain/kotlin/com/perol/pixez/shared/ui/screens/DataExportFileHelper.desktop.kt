package com.perol.pixez.shared.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.perol.pixez.shared.utils.runCatchingNonCancel
import java.io.File
import java.io.IOException

/**
 * Desktop(JVM) 平台实现：使用 [java.io.File] 写入 UTF-8 文本。
 *
 * 写入前校验路径必须位于 [getExportBaseDirectory] 之下，防止路径遍历。
 * 失败只抛带错误码的 [DataExportException]，面向用户的文案由界面层按错误码本地化。
 */
internal actual fun writeExportFile(path: String, content: String): Result<Unit> =
    runCatchingNonCancel {
        val safePath = validateExportPath(path)
        try {
            File(safePath).apply { parentFile?.mkdirs() }.writeText(content, Charsets.UTF_8)
        } catch (e: IOException) {
            throw DataExportException(DataExportErrorCode.IoFailure, "path=$safePath", e)
        }
    }

/**
 * Desktop(JVM) 平台实现：使用 [java.io.File] 读取 UTF-8 文本。
 *
 * 读取前校验路径必须位于 [getExportBaseDirectory] 之下，防止路径遍历。
 */
internal actual fun readExportFile(path: String): Result<String> = runCatchingNonCancel {
    val safePath = validateExportPath(path)
    val file = File(safePath)
    val size = file.length()
    if (size > MAX_IMPORT_FILE_BYTES) {
        throw DataExportException(
            DataExportErrorCode.ImportFileTooLarge,
            "size=$size limit=$MAX_IMPORT_FILE_BYTES",
        )
    }
    try {
        file.readText(Charsets.UTF_8)
    } catch (e: IOException) {
        throw DataExportException(DataExportErrorCode.IoFailure, "path=$safePath", e)
    }
}

/**
 * Desktop 平台导出根目录：用户主目录下的 `PixEz/export` 子目录。
 */
internal actual fun getExportBaseDirectory(): String {
    val userHome = System.getProperty("user.home")
        ?: throw DataExportException(DataExportErrorCode.BaseDirUnavailable, "user.home")
    return File(userHome, "PixEz/export").absolutePath
}

/**
 * 校验导出/导入路径是否位于允许的基础目录内。
 *
 * 使用 [File.getCanonicalPath] 解析 `../` 等路径，确保目标文件不会穿越到应用目录之外。
 */
private val approvedPickerPaths: MutableSet<String> =
    java.util.Collections.synchronizedSet(mutableSetOf())

internal fun approveExportPickerPath(path: String): String {
    val canonical = File(path).canonicalPath
    approvedPickerPaths.add(canonical)
    return canonical
}

private fun validateExportPath(path: String): String {
    if (path.isBlank()) throw DataExportException(DataExportErrorCode.PathBlank)
    if (!path.endsWith(".json", ignoreCase = true)) {
        throw DataExportException(DataExportErrorCode.PathUnsupportedExtension, path)
    }
    val raw = File(path)
    val baseDir = File(getExportBaseDirectory()).canonicalFile
    val targetFile = if (raw.isAbsolute) raw.canonicalFile else File(baseDir, path).canonicalFile
    val isInsideBase = targetFile.toPath().normalize().startsWith(baseDir.toPath().normalize())
    if (isInsideBase) return targetFile.path
    // 仅允许经由原生文件选择器明确选中的导出目录外绝对路径，阻断手输/注入的任意绝对路径遍历
    if (raw.isAbsolute && targetFile.path in approvedPickerPaths) {
        return targetFile.path
    }
    throw DataExportException(DataExportErrorCode.PathOutsideExportDir, path)
}

/**
 * 桌面端原生文件选择器：导出用 AWT FileDialog(SAVE)，导入用 FileDialog(LOAD)。
 * 选择器在 IO 线程经 invokeAndWait 阻塞展示（模态），选择结果经 CompletableFuture 交还调用方协程。
 */
internal actual suspend fun pickExportFilePath(suggestedName: String, forSave: Boolean): String? =
    withContext(Dispatchers.IO) {
        val future = java.util.concurrent.CompletableFuture<String?>()
        java.awt.EventQueue.invokeAndWait {
            runCatching {
                val frame = java.awt.Frame.getFrames().firstOrNull { it.isVisible }
                val dialog = java.awt.FileDialog(
                    frame,
                    if (forSave) "选择导出位置" else "选择要导入的文件",
                    if (forSave) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD,
                ).apply {
                    directory = getExportBaseDirectory() + File.separator
                    if (forSave) file = suggestedName
                }
                dialog.isVisible = true
                val chosen = dialog.file?.let { file ->
                    val dir = dialog.directory.orEmpty()
                    val full = if (dir.endsWith(File.separator)) dir + file else dir + File.separator + file
                    approveExportPickerPath(full)
                }
                future.complete(chosen)
            }.onFailure { future.complete(null) }
        }
        future.get()
    }
