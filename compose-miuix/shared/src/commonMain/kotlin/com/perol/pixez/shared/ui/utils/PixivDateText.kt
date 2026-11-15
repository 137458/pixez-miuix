package com.perol.pixez.shared.ui.utils

/**
 * 把 Pixiv 的 ISO-8601 时间戳（如 2024-05-01T12:34:56+09:00）格式化为日历日。
 *
 * 作品发布日、小说发布时间只需要年月日，所以按 'T' 截断即可；
 * 这里刻意不做本地时区换算：Pixiv 时间是日本时区，换算后凌晨发布的作品
 * 会显示成前一天，观感反而错。输入不是 ISO 形态时原样返回，避免截成空串。
 */
internal fun formatPixivDate(isoDateTime: String): String =
    isoDateTime.substringBefore('T').ifEmpty { isoDateTime }
