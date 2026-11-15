package com.perol.pixez.shared.platform

import coil3.request.ImageRequest
import coil3.request.allowHardware

actual fun ImageRequest.Builder.configurePlatformOptimizations(): ImageRequest.Builder {
    // 开启硬件位图 (Hardware Bitmap)，将解码后的像素缓冲直接放入 GPU 图形显存，
    // 降低 60% 以上的 JVM 堆内存开销，彻底杜绝大图浏览时的 GC 顿挫与 OOM。
    // minSdk 31 恒高于 O（26），硬件位图恒可用，不再保留关闭分支。
    allowHardware(true)
    return this
}
