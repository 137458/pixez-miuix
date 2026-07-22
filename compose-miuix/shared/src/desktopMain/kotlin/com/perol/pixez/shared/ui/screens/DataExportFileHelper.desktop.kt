package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import java.io.File

/**
 * Desktop(JVM) 平台实现：使用 [java.io.File] 写入 UTF-8 文本。
 *
 * 写入前校验路径必须位于 [getExportBaseDirectory] 之下，防止路径遍历。
 */
internal actual fun writeExportFile(path: String, content: String): Result<Unit> =
    runCatchingNonCancel {
        val safePath = validateExportPath(path)
        File(safePath).apply { parentFile?.mkdirs() }.writeText(content, Charsets.UTF_8)
    }

/**
 * Desktop(JVM) 平台实现：使用 [java.io.File] 读取 UTF-8 文本。
 *
 * 读取前校验路径必须位于 [getExportBaseDirectory] 之下，防止路径遍历。
 */
internal actual fun readExportFile(path: String): Result<String> = runCatchingNonCancel {
    val safePath = validateExportPath(path)
    File(safePath).readText(Charsets.UTF_8)
}

/**
 * Desktop 平台导出根目录：用户主目录下的 `PixEz/export` 子目录。
 */
internal actual fun getExportBaseDirectory(): String {
    val userHome = System.getProperty("user.home")
        ?: throw IllegalStateException("无法获取用户主目录")
    return File(userHome, "PixEz/export").absolutePath
}

/**
 * 校验导出/导入路径是否位于允许的基础目录内。
 *
 * 使用 [File.getCanonicalPath] 解析 `../` 等路径，确保目标文件不会穿越到应用目录之外。
 */
private fun validateExportPath(path: String): String {
    require(path.isNotBlank()) { "文件路径不能为空" }
    require(path.endsWith(".json", ignoreCase = true)) { "仅支持 .json 文件" }
    val baseDir = File(getExportBaseDirectory()).canonicalPath
    val targetFile = File(baseDir, path).canonicalFile
    val targetPath = targetFile.canonicalPath
    require(targetPath.startsWith(baseDir + File.separator) || targetPath == baseDir) {
        "文件路径必须在应用导出目录内"
    }
    return targetPath
}
