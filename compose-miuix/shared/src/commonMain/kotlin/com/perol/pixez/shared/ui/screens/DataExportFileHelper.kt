package com.perol.pixez.shared.ui.screens

/**
 * 将文本内容写入指定路径的导出文件。
 *
 * 该函数为平台相关函数：Android / Desktop 使用 Java [java.io.File]，
 * 其它平台在需要时再补充实现。
 *
 * 平台实现会校验 [path] 必须位于 [getExportBaseDirectory] 之下，
 * 防止 `../` 路径遍历导致写入应用私有目录之外。
 *
 * @param path 用户输入的目标文件路径，必须以 `.json` 结尾。
 * @param content 待写入的 JSON 文本。
 * @return [Result] 包装写入结果，失败时携带异常信息。
 */
internal expect fun writeExportFile(path: String, content: String): Result<Unit>

/**
 * 从指定路径读取导出文件的文本内容。
 *
 * 平台实现会校验 [path] 必须位于 [getExportBaseDirectory] 之下，
 * 防止 `../` 路径遍历导致读取应用私有目录之外的数据。
 *
 * @param path 用户输入的源文件路径，必须以 `.json` 结尾。
 * @return [Result] 包装读取到的文本，失败时携带异常信息。
 */
internal expect fun readExportFile(path: String): Result<String>

/**
 * 返回当前平台允许导出/导入的根目录。
 *
 * Android 使用应用外部私有目录下的 `export` 子目录；
 * Desktop 使用用户主目录下的 `PixEz/export` 子目录。
 */
internal expect fun getExportBaseDirectory(): String
