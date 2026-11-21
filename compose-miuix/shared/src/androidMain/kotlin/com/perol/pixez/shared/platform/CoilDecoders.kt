package com.perol.pixez.shared.platform

import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder

/**
 * 获取当前平台支持的动图（GIF / Animated WebP / APNG）解码器工厂列表。
 */
fun getPlatformGifDecoderFactories(): List<Decoder.Factory> {
    // minSdk 31 恒高于 P（28），动图解码统一走 ImageDecoder 实现，GifDecoder 回退已无意义。
    return listOf(AnimatedImageDecoder.Factory())
}
