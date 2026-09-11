package com.perol.pixez.shared.platform

import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.perol.pixez.shared.data.model.Illust

/**
 * 跨平台插画本地文件寻址器：
 * 检索系统相册、公共目录或用户自定义路径中是否已存在指定插画的已下载原图文件。
 */
expect object LocalIllustResolver {
    /**
     * 查找本地已下载的原图文件。
     *
     * @param illust 目标作品
     * @param pageIndex 页码（0-indexed）
     * @param customBasePath 可选的用户自定义存储基准目录
     * @return 存在且非空时返回标准 file: URI 字符串（例如 "file:///..."），未下载或不存在时返回 null
     */
    fun findDownloadedFileUri(
        illust: Illust,
        pageIndex: Int,
        customBasePath: String? = null,
    ): String?
}

/**
 * 检查指定 URL 是否已存在于 Coil 磁盘缓存中。
 * 同时检测原始 URL 以及经过图源替换（如 i.pixiv.re）后的变体 URL。
 */
fun isUrlInCoilDiskCache(
    context: PlatformContext,
    url: String?,
    pictureSource: String? = null,
): Boolean {
    if (url.isNullOrBlank()) return false
    val diskCache = SingletonImageLoader.get(context).diskCache ?: return false
    val candidateKeys = buildList {
        add(url)
        if (!pictureSource.isNullOrBlank() && pictureSource != "i.pximg.net") {
            add(url.replace("://i.pximg.net", "://$pictureSource"))
        }
    }
    for (key in candidateKeys) {
        val snapshot = diskCache.openSnapshot(key)
        if (snapshot != null) {
            snapshot.close()
            return true
        }
    }
    return false
}

/**
 * 智能解析插画页面的最优显示图片模型：
 * 1. 优先使用本地相册已下载原图文件（file: URI，0 网络开销，完整原图分辨率）；
 * 2. 检查 Coil 磁盘缓存中是否已有 original 原图缓存，若已有则优先复用原图（无需重新下载 Large 图）；
 * 3. 降级使用目标画质 URL。
 */
fun resolveOptimizedImageModel(
    context: PlatformContext,
    illust: Illust,
    pageIndex: Int,
    targetUrl: String,
    originalUrl: String?,
    customBasePath: String? = null,
    pictureSource: String? = null,
): String {
    // 1. 本地已下载原图文件优先（0 网络请求）
    val localUri = LocalIllustResolver.findDownloadedFileUri(illust, pageIndex, customBasePath)
    if (!localUri.isNullOrBlank()) {
        return localUri
    }

    // 2. Coil 磁盘缓存中的原图优先（最高清就地复用）
    if (!originalUrl.isNullOrBlank() && isUrlInCoilDiskCache(context, originalUrl, pictureSource)) {
        return originalUrl
    }

    // 3. 降级使用目标画质 URL
    return targetUrl
}
