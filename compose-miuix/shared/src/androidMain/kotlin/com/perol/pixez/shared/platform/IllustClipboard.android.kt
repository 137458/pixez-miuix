package com.perol.pixez.shared.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Android 平台实现：使用系统 ClipboardManager 复制文本。
 *
 * 复用 [BrowserLauncherContext] 获取应用 Context，与 [IllustSaver] 保持一致。
 */
actual class IllustClipboard {
    actual fun copy(text: String) {
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 ClipboardManager")

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("PixEz", text)
        clipboardManager.setPrimaryClip(clipData)
    }
}
