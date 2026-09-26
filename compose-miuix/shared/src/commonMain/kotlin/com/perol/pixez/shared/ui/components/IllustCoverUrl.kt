package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.ImageUrls

/**
 * 按列表封面画质设置解析作品封面预览 URL。
 *
 * 列表卡片与详情页转场缩略图共用本函数，保证两侧取到的封面 URL 完全一致，
 * 卡片展开转场首帧 100% 命中 Coil 内存缓存，实现零白屏一镜到底。
 */
fun resolveIllustCoverUrl(imageUrls: ImageUrls, feedPreviewQuality: Int?): String {
    val preferred = when (feedPreviewQuality ?: 0) {
        0 -> imageUrls.medium
        1 -> imageUrls.large
        2 -> imageUrls.squareMedium
        else -> imageUrls.medium
    }
    return preferred.ifBlank { imageUrls.medium.ifBlank { imageUrls.squareMedium } }
}
