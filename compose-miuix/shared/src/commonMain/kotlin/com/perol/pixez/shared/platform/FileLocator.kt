package com.perol.pixez.shared.platform

/**
 * 跨平台文件定位与管理器打开工具。
 */
expect class FileLocator() {
    /**
     * 在系统文件管理器（Windows 资源管理器 / macOS Finder / 平台文件管理器）中打开并高亮选中 [filePath] 文件。
     *
     * @param filePath 文件的绝对路径
     * @return 是否成功唤起系统文件管理器
     */
    fun showInFileManager(filePath: String): Boolean
}
