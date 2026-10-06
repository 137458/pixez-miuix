package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.network.PixivApiException

/**
 * PixivApiException 的差异化错误文案语义键。
 * 各键对应 AppStrings 中 loadFailed 语义族的多语言键。
 */
enum class PixivApiErrorKey {
    /** 403：访问受限（风控 / IP 封禁）。 */
    FORBIDDEN,

    /** 429：请求过于频繁（限流）。 */
    RATE_LIMITED,

    /** 404：资源不存在或已被删除。 */
    NOT_FOUND,
}

/**
 * PixivApiException.statusCode → 差异化错误文案语义键 的映射。
 * 纯 Kotlin 无 Compose 依赖，供 ErrorPlaceholder 等错误占位复用；
 * 未收录的状态码与非 PixivApiException 一律回退 null，由调用方保持「加载失败」兜底文案。
 */
object PixivApiErrorFormatter {
    fun resolve(error: Throwable?): PixivApiErrorKey? = when {
        error !is PixivApiException -> null
        error.statusCode == 403 -> PixivApiErrorKey.FORBIDDEN
        error.statusCode == 429 -> PixivApiErrorKey.RATE_LIMITED
        error.statusCode == 404 -> PixivApiErrorKey.NOT_FOUND
        else -> null
    }
}
