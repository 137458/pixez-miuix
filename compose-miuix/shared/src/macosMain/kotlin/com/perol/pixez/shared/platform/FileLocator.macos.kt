package com.perol.pixez.shared.platform

import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

/**
 * macOS 平台实现：在 Finder 中选中文件。
 */
actual class FileLocator {
    actual fun showInFileManager(filePath: String): Boolean {
        val url = NSURL.fileURLWithPath(filePath)
        return NSWorkspace.sharedWorkspace.activateFileViewerSelectingURLs(listOf(url))
    }
}
