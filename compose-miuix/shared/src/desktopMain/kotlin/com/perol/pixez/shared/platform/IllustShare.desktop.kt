package com.perol.pixez.shared.platform

/**
 * Desktop(JVM) 平台实现：系统原生分享面板不可用，回退到剪贴板复制。
 *
 * 调用 [IllustClipboard.copy] 将 [text] 写入系统剪贴板，由调用方通过 Toast 提示用户。
 */
actual class IllustShare {
    actual fun share(text: String, subject: String?) {
        IllustClipboard().copy(text)
    }
}
