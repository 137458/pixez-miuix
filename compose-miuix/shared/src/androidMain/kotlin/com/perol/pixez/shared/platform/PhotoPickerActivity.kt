package com.perol.pixez.shared.platform

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * 跨 Activity 传递的照片选择回调注册表。
 *
 * 采用原子「领取即清空」语义：结果回调与启动失败路径并发触发时，
 * 不会重复回调或互相覆盖（回调仅被领取一次）。
 */
internal object PhotoPickerRegistry {
    private val callbackRef = AtomicReference<((byteArray: ByteArray?, fileName: String?) -> Unit)?>(null)

    fun setCallback(callback: ((ByteArray?, String?) -> Unit)?) {
        callbackRef.set(callback)
    }

    /** 原子领取当前回调，领取后注册表内不再持有。 */
    fun claimCallback(): ((ByteArray?, String?) -> Unit)? = callbackRef.getAndSet(null)
}

/**
 * 原生透明照片选择代理 Activity。
 *
 * 封装官方 ActivityResultContracts.PickVisualMedia，零存储权限选择相册图片，
 * 将图片数据流读取后通过回调传递给调用方，完成后自动 finish 自身。
 */
class PhotoPickerActivity : ComponentActivity() {

    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val cb = PhotoPickerRegistry.claimCallback()
        if (uri != null) {
            // 绑定 Activity 生命周期的作用域替代裸 CoroutineScope；回调与 finish 统一回主线程。
            lifecycleScope.launch {
                var bytes: ByteArray? = null
                var fileName: String? = null
                try {
                    withContext(Dispatchers.IO) {
                        bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0 && cursor.moveToFirst()) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to read picked photo", e, tag = "PhotoPicker")
                } finally {
                    cb?.invoke(bytes, fileName)
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
            PhotoPickerRegistry.claimCallback()?.invoke(null, null)
            finish()
        }
    }
}
