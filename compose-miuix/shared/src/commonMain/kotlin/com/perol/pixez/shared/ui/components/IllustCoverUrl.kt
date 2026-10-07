package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.ImageUrls

/**
 * 按列表封面画质设置解析作品封面预览 URL。
 *
 * 列表卡片与详情页转场缩略图共用本函数，保证两侧取到的封面 URL 完全一致，
 * 卡片展开转场首帧 100% 命中 Coil 内存缓存，实现零白屏一镜到底。
 *
 * @param allowSquare 是否允许返回方图裁切档（squareMedium）。详情页大图容器按作品真实
 *   比例布局，方图占位在竖图容器中垂直留空会露出「正方形预览图」，详情页调用侧必须传
 *   false；列表卡片容器即方图比例，保持默认 true。
 */
fun resolveIllustCoverUrl(imageUrls: ImageUrls, feedPreviewQuality: Int?, allowSquare: Boolean = true): String {
    val preferred = when (feedPreviewQuality ?: 0) {
        0 -> imageUrls.medium
        1 -> imageUrls.large
        2 -> if (allowSquare) imageUrls.squareMedium else imageUrls.medium
        else -> imageUrls.medium
    }
    return preferred.ifBlank { imageUrls.medium.ifBlank { imageUrls.squareMedium } }
}
