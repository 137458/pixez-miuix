package com.perol.pixez.shared.platform

import platform.AppKit.NSApplication
import platform.AppKit.NSSharingServicePicker

/**
 * macOS 平台实现：使用 `NSSharingServicePicker` 展示系统分享面板。
 *
 * 以当前主窗口的内容视图作为 anchor，将分享面板显示在窗口中央上方。
 */
actual class IllustShare {
    actual fun share(text: String, subject: String?) {
        val items = buildList<Any> {
            add(text)
            subject?.let { add(it) }
        }

        val sharingServicePicker = NSSharingServicePicker(items = items)

        val window = NSApplication.sharedApplication.mainWindow
            ?: NSApplication.sharedApplication.windows.firstOrNull()
            ?: throw IllegalStateException("无法获取 NSWindow，无法展示分享面板")

        val contentView = window.contentView
            ?: throw IllegalStateException("无法获取 NSWindow 的 contentView")

        sharingServicePicker.showRelativeToRect(
            rect = contentView.bounds,
            ofView = contentView,
            preferredEdge = platform.AppKit.NSRectEdgeMinY,
        )
    }
}
