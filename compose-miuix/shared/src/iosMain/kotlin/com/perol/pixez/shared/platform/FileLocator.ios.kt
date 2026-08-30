package com.perol.pixez.shared.platform

/**
 * iOS 平台实现：文件定位安全回退。
 */
actual class FileLocator {
    actual fun showInFileManager(filePath: String): Boolean = false
}
