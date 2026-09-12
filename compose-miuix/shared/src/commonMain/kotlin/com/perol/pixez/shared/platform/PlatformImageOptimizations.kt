package com.perol.pixez.shared.platform

import coil3.request.ImageRequest

/**
 * 跨平台图片解码与加载管线性能优化配置。
 */
expect fun ImageRequest.Builder.configurePlatformOptimizations(): ImageRequest.Builder
