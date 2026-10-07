package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.platform.BrowserLauncherContext
import com.perol.pixez.shared.utils.runCatchingNonCancel
import java.io.File
import java.io.IOException

/**
 * Android 平台实现：使用 [java.io.File] 写入 UTF-8 文本。
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
 * Android 平台实现：使用 [java.io.File] 读取 UTF-8 文本。
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
 * Android 平台导出根目录：优先使用应用外部私有目录下的 `export` 子目录。
 */
internal actual fun getExportBaseDirectory(): String {
    val context = BrowserLauncherContext.applicationContext
        ?: throw DataExportException(DataExportErrorCode.BaseDirUnavailable, "BrowserLauncherContext")
    return context.getExternalFilesDir("export")?.absolutePath
        ?: File(context.filesDir, "export").absolutePath
}

/**
 * 校验导出/导入路径是否位于允许的基础目录内。
 *
 * 使用 [File.getCanonicalPath] 解析 `../` 等路径，确保目标文件不会穿越到应用目录之外。
 */
private fun validateExportPath(path: String): String {
    if (path.isBlank()) throw DataExportException(DataExportErrorCode.PathBlank)
    if (!path.endsWith(".json", ignoreCase = true)) {
        throw DataExportException(DataExportErrorCode.PathUnsupportedExtension, path)
    }
    val baseDir = File(getExportBaseDirectory()).canonicalPath
    val targetFile = File(baseDir, path).canonicalFile
    val targetPath = targetFile.canonicalPath
    if (!(targetPath.startsWith(baseDir + File.separator) || targetPath == baseDir)) {
        throw DataExportException(DataExportErrorCode.PathOutsideExportDir, path)
    }
    return targetPath
}

/**
 * Android 无桌面式文件选择器接入（SAF 另行排期）：返回 null 保持现有手输路径流程。
 */
internal actual suspend fun pickExportFilePath(suggestedName: String, forSave: Boolean): String? = null
