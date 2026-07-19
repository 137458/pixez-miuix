package com.perol.pixez.shared.platform

import platform.UIKit.UIPasteboard

/**
 * iOS 平台实现：使用系统剪贴板复制文本。
 */
actual class IllustClipboard {
    actual fun copy(text: String) {
        UIPasteboard.general.string = text
    }
}
