package com.perol.pixez.shared.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android 平台实现：使用系统 ClipboardManager 复制文本与图片。
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

    actual fun copyImage(imageBytes: ByteArray) {
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 ClipboardManager")
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cacheDir = File(context.cacheDir, "clipboard_images").apply { mkdirs() }
        val tempFile = File(cacheDir, "clip_${System.currentTimeMillis()}.png")
        tempFile.writeBytes(imageBytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        val clipData = ClipData.newUri(context.contentResolver, "Image", uri)
        clipboardManager.setPrimaryClip(clipData)
    }

    actual fun getText(): String? {
        val context = BrowserLauncherContext.applicationContext ?: return null
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboardManager.primaryClip ?: return null
        if (clip.itemCount > 0) {
            return clip.getItemAt(0).coerceToText(context)?.toString()
        }
        return null
    }
}
