package com.perol.pixez.shared.platform

import coil3.decode.Decoder

/**
 * 获取当前平台支持的动图（GIF / Animated WebP / APNG）解码器工厂列表。
 */
expect fun getPlatformGifDecoderFactories(): List<Decoder.Factory>
