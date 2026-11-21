package com.perol.pixez.shared.platform

import android.os.Environment
import java.io.File

/** Default platform directory used for saved pictures when no custom path is configured. */
fun getDefaultPictureDirectory(): String =
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PixEz").absolutePath
