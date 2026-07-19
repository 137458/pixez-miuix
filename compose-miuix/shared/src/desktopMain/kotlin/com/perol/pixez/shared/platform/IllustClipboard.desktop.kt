package com.perol.pixez.shared.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop(JVM) 平台实现：使用 AWT 系统剪贴板复制文本。
 */
actual class IllustClipboard {
    actual fun copy(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}
