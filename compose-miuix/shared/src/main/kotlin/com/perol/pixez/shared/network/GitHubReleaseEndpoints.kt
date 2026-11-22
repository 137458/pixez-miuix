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

    /** 官方发布仓库路径（owner/repo），asset 直链白名单校验用。 */
    const val OFFICIAL_REPO_PATH = OWNER + "/" + REPO_NAME

    const val RELEASES_PAGE = "https://github.com/" + OFFICIAL_REPO_PATH + "/releases"

    /** 最新发布的 REST API：按出口 IP 限流（未认证 60 次/小时），只作首选通道。 */
    const val LATEST_API = "https://api.github.com/repos/" + OFFICIAL_REPO_PATH + "/releases/latest"

    /** 发布 Atom Feed：不限流、无需鉴权，作为限流耗尽时的兜底通道。 */
    const val RELEASES_ATOM = "https://github.com/" + OFFICIAL_REPO_PATH + "/releases.atom"

    /** 发布资产的展开列表片段端点：只含资产名、直链与摘要，用于兜底解析安装包。 */
    fun expandedAssetsUrl(tagName: String): String =
        "https://github.com/" + OFFICIAL_REPO_PATH + "/releases/expanded_assets/" + tagName
}
