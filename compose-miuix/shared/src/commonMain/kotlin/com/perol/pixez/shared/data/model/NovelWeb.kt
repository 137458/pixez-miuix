package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * /webview/v2/novel 内嵌 novel 数据（从 HTML script 提取的 JSON）。
 *
 * 字段名逐字段核对旧 Flutter 版 models/novel_web_response.dart：
 * - illusts 键为 "作品ID-页码"（如 "12345-1"），值形如 {"illust": {...缩略图 url...}}，可为 null；
 * - images 键为上传图 ID（如 "777"），urls 尺寸键为 wire 原始键（"128x128"/"1200x1200" 等，
 *   旧 Dart 模型因标识符不能以数字开头写作 the128X128，归档缺失 .g.dart，按真实 wire 键声明）。
 * 仅保留阅读器富文本渲染所需字段，未知字段由 commonJson 的 ignoreUnknownKeys 忽略。
 */
@Serializable
data class NovelWebResponse(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val illusts: Map<String, NovelWebIllustEntry?> = emptyMap(),
    val images: Map<String, NovelWebUploadedImage> = emptyMap(),
    val seriesNavigation: NovelWebSeriesNavigation? = null,
)

/** illusts 映射条目：{"illust": {...}}。 */
@Serializable
data class NovelWebIllustEntry(
    val illust: NovelWebIllust,
)

/** 内嵌插画：{"images": {"small"/"medium"/"original"}}。 */
@Serializable
data class NovelWebIllust(
    val images: NovelWebIllustImages,
)

/** 内嵌插画缩略图地址（均可空，渲染层按 medium → original → small 兜底）。 */
@Serializable
data class NovelWebIllustImages(
    val small: String? = null,
    val medium: String? = null,
    val original: String? = null,
)

/** images 映射条目（用户上传图）：{"novelImageId", "sl", "urls"}。 */
@Serializable
data class NovelWebUploadedImage(
    @SerialName("novelImageId") val novelImageId: String? = null,
    val sl: String? = null,
    val urls: NovelWebUploadedImageUrls,
)

/**
 * 上传图地址，键为 pixiv wire 原始键（"240mw"/"480mw"/"1200x1200"/"128x128"/"original"）。
 * 渲染层按 128x128 → 1200x1200 → original 兜底（对齐旧版 image_text.dart 取值顺序）。
 */
@Serializable
data class NovelWebUploadedImageUrls(
    @SerialName("240mw") val mw240: String? = null,
    @SerialName("480mw") val mw480: String? = null,
    @SerialName("1200x1200") val size1200: String? = null,
    @SerialName("128x128") val size128: String? = null,
    val original: String? = null,
)

/** 系列章节导航：{"nextNovel", "prevNovel"}。 */
@Serializable
data class NovelWebSeriesNavigation(
    val nextNovel: NovelWebSeriesNeighbor? = null,
    val prevNovel: NovelWebSeriesNeighbor? = null,
)

/** 系列相邻章节：{"id", "viewable", "contentOrder", "title", "coverUrl"}。 */
@Serializable
data class NovelWebSeriesNeighbor(
    val id: Long,
    val viewable: Boolean = false,
    @SerialName("contentOrder") val contentOrder: String = "",
    val title: String? = null,
    val coverUrl: String? = null,
)
