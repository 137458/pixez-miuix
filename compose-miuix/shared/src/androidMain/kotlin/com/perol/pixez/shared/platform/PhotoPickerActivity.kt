package com.perol.pixez.shared.platform

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal object PhotoPickerRegistry {
    var callback: ((byteArray: ByteArray?, fileName: String?) -> Unit)? = null
}

/**
 * 原生透明照片选择代理 Activity。
 *
 * 封装官方 ActivityResultContracts.PickVisualMedia，零存储权限选择相册图片，
 * 将图片数据流读取后通过回调传递给调用方，完成后自动 finish 自身。
 */
class PhotoPickerActivity : ComponentActivity() {

    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val cb = PhotoPickerRegistry.callback
        PhotoPickerRegistry.callback = null
        if (uri != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    var fileName: String? = null
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    cb?.invoke(bytes, fileName)
                } catch (e: Exception) {
                    Napier.e("Failed to read picked photo", e, tag = "PhotoPicker")
                    cb?.invoke(null, null)
                } finally {
                    finish()
                }
            }
        } else {
            cb?.invoke(null, null)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            Napier.e("Failed to launch PickVisualMedia", e, tag = "PhotoPicker")
            PhotoPickerRegistry.callback?.invoke(null, null)
            PhotoPickerRegistry.callback = null
            finish()
        }
    }
}
