package com.perol.pixez.shared.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * PHP 接口在空字典时常输出 `[]`（JsonArray）而非 `{}`（JsonObject）；
 * 反序列化遇到 `JsonArray` 时归一为空对象，其余保持原始结构。
 */
private open class PhpMapSerializer<V>(
    valueSerializer: KSerializer<V>,
) : JsonTransformingSerializer<Map<String, V>>(
    MapSerializer(String.serializer(), valueSerializer),
) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray) JsonObject(emptyMap()) else element
}

private object NullableIllustMapSerializer :
    PhpMapSerializer<NovelWebIllustEntry?>(NovelWebIllustEntry.serializer().nullable)

private object UploadedImageMapSerializer :
    PhpMapSerializer<NovelWebUploadedImage>(NovelWebUploadedImage.serializer())

/**
 * /webview/v2/novel 内嵌 novel 数据（从 HTML script 提取的 JSON）。
 *
 * 字段名逐字段核对旧 Flutter 版 models/novel_web_response.dart：
 * - illusts 键为 "作品ID-页码"（如 "12345-1"），值形如 {"illust": {...缩略图 url...}}，可为 null，
 *   且内嵌插画被删/不可见时可能为 `{"illust": null}`，空集合时 PHP 可能返回 `[]`；
 * - images 键为上传图 ID（如 "777"），urls 尺寸键为 wire 原始键（"128x128"/"1200x1200" 等，
 *   空集合时 PHP 可能返回 `[]`）；
 * 仅保留阅读器富文本渲染所需字段，未知字段由 commonJson 的 ignoreUnknownKeys 忽略。
 */
@Serializable
data class NovelWebResponse(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    @Serializable(with = NullableIllustMapSerializer::class)
    val illusts: Map<String, NovelWebIllustEntry?> = emptyMap(),
    @Serializable(with = UploadedImageMapSerializer::class)
    val images: Map<String, NovelWebUploadedImage> = emptyMap(),
    val seriesNavigation: NovelWebSeriesNavigation? = null,
)

/** illusts 映射条目：{"illust": {...}}；作品已删或不可见时 `illust` 为 null。 */
@Serializable
data class NovelWebIllustEntry(
    val illust: NovelWebIllust? = null,
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
