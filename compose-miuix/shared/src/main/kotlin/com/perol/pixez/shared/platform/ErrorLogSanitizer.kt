package com.perol.pixez.shared.platform

/**
 * 错误日志脱敏器（架构审查 中-12）。
 *
 * 隐私红线（裁决 2026-10-11）：进入错误日志的文本不得包含
 * Authorization 凭证、token 值、完整 URL（查询串承载 user_id 等用户标识，统一剥离查询串，
 * 保留接口路径以保留排障价值）。
 */
internal object ErrorLogSanitizer {

    private const val REDACTED = "[REDACTED]"

    /** token 类赋值：access_token=xxx / refresh_token: xxx / Bearer xxx（值 ≥8 位字母数字）。 */
    private val TOKEN_PATTERN = Regex(
        """(?i)\b((?:access|refresh|id)_token|bearer)\s*[:=]?\s*["']?[A-Za-z0-9._\-]{8,}["']?""",
    )

    /** Authorization 请求头：`Authorization: <值>` / `authorization=<值>`。 */
    private val AUTHORIZATION_PATTERN = Regex("""(?i)(authorization\s*[:=]\s*)(\S+)""")

    /** URL 查询串：从 `?` 剥到空白/引号/括号为止（查询串承载 user_id 等用户标识）。 */
    private val URL_QUERY_PATTERN = Regex("""\?[^\s"'<>()（）]*""")

    /**
     * 对即将落盘的日志文本执行红线脱敏；[message] 为 null 或空时原样返回。
     */
    fun sanitize(message: String?): String? {
        if (message.isNullOrEmpty()) return message
        // 先脱 token 值（覆盖 "Authorization: Bearer <token>" 的 Bearer 形态），再兜住裸 Authorization 头。
        var result = TOKEN_PATTERN.replace(message, REDACTED)
        result = AUTHORIZATION_PATTERN.replace(result) { match -> "${match.groupValues[1]}$REDACTED" }
        result = URL_QUERY_PATTERN.replace(result, "")
        return result
    }
}
