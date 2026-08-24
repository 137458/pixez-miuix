package com.perol.pixez.shared.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import io.github.aakira.napier.Napier

actual class PlatformPhotoPicker {
    actual fun pickPhoto(onResult: (byteArray: ByteArray?, fileName: String?) -> Unit) {
        val context = BrowserLauncherContext.applicationContext ?: return
        // 跨平台回调入口，实际在 Compose Activity 中通过 rememberLauncherForActivityResult(PickVisualMedia()) 挂接
        Napier.i("PlatformPhotoPicker ready on Android with zero storage permissions", tag = "PhotoPicker")
    }
}
