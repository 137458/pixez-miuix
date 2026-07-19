package com.perol.pixez.shared.platform

import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString

/**
 * macOS 平台实现：使用系统剪贴板复制文本。
 */
actual class IllustClipboard {
    actual fun copy(text: String) {
        val pasteboard = NSPasteboard.generalPasteboard
            ?: throw IllegalStateException("无法获取 NSPasteboard")

        pasteboard.clearContents()
        pasteboard.setString(text, forType = NSPasteboardTypeString)
    }
}
