package com.perol.pixez.shared.network

/**
 * 应用层稳定错误码。
 *
 * 数据层把失败原因归一为本枚举的 [code] 持久化（如 DownloadTask.error 写入本地历史库），
 * UI 层通过 [com.perol.pixez.shared.ui.components.PixivApiErrorFormatter] 把 code 翻译为
 * LocalStrings 本地化文案，保证错误语义单点维护，且不把内部异常消息直透给用户。
 *
 * 稳定性约定：[code] 会持久化在本地数据库中，一经发布只增不改。
 */
enum class AppErrorKind(val code: String) {
    /** 403：访问受限（风控 / IP 封禁）。 */
    FORBIDDEN("forbidden"),

    /** 429：请求过于频繁（限流）。 */
    RATE_LIMITED("rate_limited"),

    /** 404：资源不存在或已被删除。 */
    NOT_FOUND("not_found"),

    /** 其他非 2xx API 响应（含 401 未授权、5xx 服务端错误等）。 */
    API_ERROR("api_error"),

    /** 下载失败：原图字节下载或落盘失败。 */
    DOWNLOAD_FAILED("download_failed"),

    /** 动图 ZIP 保存失败。 */
    UGOIRA_SAVE_FAILED("ugoira_save_failed"),

    /** 未知错误：非本体系异常或无错误信息。 */
    UNKNOWN("unknown"),
    ;

    companion object {
        private val byCode = entries.associateBy { it.code }

        /** 持久化的错误码字符串反查枚举；未知内容（历史遗留文本）返回 null，由调用方兜底。 */
        fun fromCode(code: String): AppErrorKind? = byCode[code]

        /** HTTP 状态码到错误类型的稳定映射，供 [PixivApiException.errorKind] 使用。 */
        fun fromStatusCode(statusCode: Int): AppErrorKind = when (statusCode) {
            403 -> FORBIDDEN
            429 -> RATE_LIMITED
            404 -> NOT_FOUND
            else -> API_ERROR
        }

        /** 把任意异常归一为错误类型：PixivApiException 按状态码分类，其余回退 [UNKNOWN]。 */
        fun forThrowable(error: Throwable?): AppErrorKind = when (error) {
            null -> UNKNOWN
            is PixivApiException -> error.errorKind
            else -> UNKNOWN
        }
    }
}
