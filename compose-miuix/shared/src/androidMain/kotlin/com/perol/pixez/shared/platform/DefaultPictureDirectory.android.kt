package com.perol.pixez.shared.platform

import android.os.Environment
import java.io.File

actual fun getDefaultPictureDirectory(): String =
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PixEz").absolutePath
