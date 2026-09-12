package com.perol.pixez.shared.platform

import android.content.Intent
import io.github.aakira.napier.Napier

/**
 * Android 原生零权限照片选择器实现。
 *
 * 优先接入 Android 13+ (API 33+) / Google Play 模块化系统 Photo Picker 原生协议 (PickVisualMedia / ACTION_PICK_IMAGES)，
 * 在低于 Android 13 的设备上优雅降级至 ACTION_GET_CONTENT 开放选择沙盒，全程零存储权限声明。
 */
actual class PlatformPhotoPicker {
    actual fun pickPhoto(onResult: (byteArray: ByteArray?, fileName: String?) -> Unit) {
        val context = BrowserLauncherContext.applicationContext ?: run {
            Napier.w("PlatformPhotoPicker failed: applicationContext is null", tag = "PhotoPicker")
            onResult(null, null)
            return
        }
        try {
            PhotoPickerRegistry.callback = onResult
            val intent = Intent(context, PhotoPickerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Napier.i("PlatformPhotoPicker started PhotoPickerActivity", tag = "PhotoPicker")
        } catch (e: Exception) {
            Napier.e("Failed to launch PhotoPickerActivity", e, tag = "PhotoPicker")
            PhotoPickerRegistry.callback = null
            onResult(null, null)
        }
    }
}
