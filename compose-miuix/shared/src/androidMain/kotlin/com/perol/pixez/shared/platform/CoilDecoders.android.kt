package com.perol.pixez.shared.platform

import android.os.Build
import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

actual fun getPlatformGifDecoderFactories(): List<Decoder.Factory> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        listOf(AnimatedImageDecoder.Factory())
    } else {
        listOf(GifDecoder.Factory())
    }
}
