package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

/**
 * Desktop 平台图片文件选择器（用于 SauceNAO 以图搜图等场景）。
 * 采用系统原生 [FileDialog] 选择图片并读取字节内容。
 */
actual class PlatformPhotoPicker {
    actual fun pickPhoto(onResult: (byteArray: ByteArray?, fileName: String?) -> Unit) {
        Thread {
            try {
                var selectedFile: File? = null
                EventQueue.invokeAndWait {
                    val activeFrame = Frame.getFrames().firstOrNull { it.isVisible }
                    val dialog = FileDialog(activeFrame, "Select Image", FileDialog.LOAD).apply {
                        filenameFilter = FilenameFilter { _, name ->
                            val lower = name.lowercase()
                            IMAGE_EXTENSIONS.any { lower.endsWith(it) }
                        }
                        isVisible = true
                    }
                    val dir = dialog.directory
                    val file = dialog.file
                    if (!dir.isNullOrBlank() && !file.isNullOrBlank()) {
                        selectedFile = File(dir, file)
                    }
                }
                val target = selectedFile
                if (target != null && target.isFile && target.canRead() && target.length() <= MAX_PHOTO_BYTES) {
                    val bytes = target.readBytes()
                    EventQueue.invokeLater {
                        onResult(bytes, target.name)
                    }
                } else {
                    EventQueue.invokeLater {
                        onResult(null, null)
                    }
                }
            } catch (e: Exception) {
                Napier.e("Desktop 选择图片文件失败", e)
                EventQueue.invokeLater {
                    onResult(null, null)
                }
            }
        }.apply {
            isDaemon = true
            name = "PixEz-PhotoPicker"
            start()
        }
    }

    private companion object {
        const val MAX_PHOTO_BYTES = 32L * 1024L * 1024L
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    }
}
