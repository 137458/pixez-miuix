package com.perol.pixez.shared.platform

import android.content.Intent
import io.github.aakira.napier.Napier

/**
 * Android 原生零权限照片选择器实现。
 *
 * 优先接入 Android 13+ (API 33+) / Google Play 模块化系统 Photo Picker 原生协议 (PickVisualMedia / ACTION_PICK_IMAGES)，
 * 在低于 Android 13 的设备上优雅降级至 ACTION_GET_CONTENT 开放选择沙盒，全程零存储权限声明。
 */
class PlatformPhotoPicker {
    /**
     * 选择一张图片并读取字节。
     *
     * @param title 系统对话框标题（调用方传本地化文案）；空白时用 [PlatformDialogDefaults.PICK_PHOTO_TITLE]。
     *   标题作为参数而非在实现内读 ui 单例，避免 platform 层反向依赖 ui（架构审查 DN15）。
     * @param onResult 选中结果回调；取消或失败时为 (null, null)。
     */
    fun pickPhoto(
        title: String = PlatformDialogDefaults.PICK_PHOTO_TITLE,
        onResult: (byteArray: ByteArray?, fileName: String?) -> Unit,
    ) {
        val context = BrowserLauncherContext.applicationContext ?: run {
            Napier.w("PlatformPhotoPicker failed: applicationContext is null", tag = "PhotoPicker")
            onResult(null, null)
            return
        }
        try {
            PhotoPickerRegistry.setCallback(onResult)
            val intent = Intent(context, PhotoPickerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Napier.i("PlatformPhotoPicker started PhotoPickerActivity", tag = "PhotoPicker")
        } catch (e: Exception) {
            Napier.e("Failed to launch PhotoPickerActivity", e, tag = "PhotoPicker")
            PhotoPickerRegistry.setCallback(null)
            onResult(null, null)
        }
    }
}
