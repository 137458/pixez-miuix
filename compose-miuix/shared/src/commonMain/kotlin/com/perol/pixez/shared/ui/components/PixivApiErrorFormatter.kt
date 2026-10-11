package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.network.PixivApiException
import com.perol.pixez.shared.ui.i18n.AppStrings

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

    /**
     * 将任意错误归一为可直接展示的本地化错误文案：
     * PixivApiException 的 403/429/404 走差异化语义文案（与 ErrorPlaceholder 同口径），
     * 未收录状态码、网络错误、解析失败、取消等其余情况一律回退 strings.loadFailed。
     * 原始异常消息（含内部技术细节）不进入返回值，仅供日志使用。
     */
    fun format(error: Throwable?, strings: AppStrings): String = when (resolve(error)) {
        PixivApiErrorKey.FORBIDDEN -> strings.loadFailedForbidden
        PixivApiErrorKey.RATE_LIMITED -> strings.loadFailedRateLimited
        PixivApiErrorKey.NOT_FOUND -> strings.loadFailedNotFound
        null -> strings.loadFailed
    }
}
