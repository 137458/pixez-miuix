package com.perol.pixez.shared.network

/**
 * GitHub Release 发布页与最新版 API 端点常量。
 *
 * 下层（data / network）所需成员收敛于此，避免反向依赖 ui 层 [com.perol.pixez.shared.ui.AppConstants]。
 */
object GitHubReleaseEndpoints {
    /** 发布仓库 owner 账号，同时是项目作者主页所在账号。 */
    const val OWNER = "137458"

    /** 发布仓库名。 */
    const val REPO_NAME = "pixez-miuix"

    /** GitHub 网页主站来源。 */
    const val WEB_ORIGIN = "https://github.com"

    /** 官方发布仓库路径（owner/repo），asset 直链白名单校验用。 */
    const val OFFICIAL_REPO_PATH = OWNER + "/" + REPO_NAME

    const val RELEASES_PAGE = "$WEB_ORIGIN/$OFFICIAL_REPO_PATH/releases"

    /** 最新发布的 REST API：按出口 IP 限流（未认证 60 次/小时），只作首选通道。 */
    const val LATEST_API = "https://api.github.com/repos/$OFFICIAL_REPO_PATH/releases/latest"

    /** 发布 Atom Feed：不限流、无需鉴权，作为限流耗尽时的兜底通道。 */
    const val RELEASES_ATOM = "$WEB_ORIGIN/$OFFICIAL_REPO_PATH/releases.atom"

    /** 构造指定发布标签的官方 Release 页面 URL。 */
    fun releaseTagUrl(tagName: String): String {
        val safeTag = requireSafeTagSegment(tagName)
        return "$WEB_ORIGIN/$OFFICIAL_REPO_PATH/releases/tag/$safeTag"
    }

    /** 发布资产的展开列表片段端点：只含资产名、直链与摘要，用于兜底解析安装包。 */
    fun expandedAssetsUrl(tagName: String): String {
        val safeTag = requireSafeTagSegment(tagName)
        return "$WEB_ORIGIN/$OFFICIAL_REPO_PATH/releases/expanded_assets/$safeTag"
    }

    /** 校验发布标签为合法单路径段，阻断路径遍历与查询参数注入。 */
    internal fun requireSafeTagSegment(tagName: String): String {
        val trimmed = tagName.trim()
        val valid = trimmed.isNotEmpty() &&
            trimmed != "." &&
            trimmed != ".." &&
            !trimmed.contains('/') &&
            !trimmed.contains('\\') &&
            !trimmed.contains('?') &&
            !trimmed.contains('#') &&
            trimmed.none { it.isWhitespace() || it.isISOControl() }
        require(valid) { "非法的发布标签路径段: $tagName" }
        return trimmed
    }
}
