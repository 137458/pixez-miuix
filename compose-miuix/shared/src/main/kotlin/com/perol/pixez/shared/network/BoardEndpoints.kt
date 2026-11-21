package com.perol.pixez.shared.network

/**
 * 公告板数据源地址：GitHub Raw 主源 + jsdelivr CDN 双镜像，按序重试。
 *
 * 与本仓库的其他托管资源（[GitHubReleaseEndpoints]）同处 network 层，
 * 下层仓库直引此处，不再在 data 层散落硬编码 URL。
 */
object BoardEndpoints {
    private const val REPO_PATH = GitHubReleaseEndpoints.OFFICIAL_REPO_PATH
    private const val BOARD_PATH = ".github/board/android.json"

    const val RAW_MASTER = "https://raw.githubusercontent.com/$REPO_PATH/refs/heads/master/$BOARD_PATH"
    const val FASTLY_CDN = "https://fastly.jsdelivr.net/gh/$REPO_PATH@master/$BOARD_PATH"
    const val CDN = "https://cdn.jsdelivr.net/gh/$REPO_PATH@master/$BOARD_PATH"

    /** 按优先级排列的拉取顺序：主源失败后依次回退到镜像。 */
    val BOARD_URLS = listOf(RAW_MASTER, FASTLY_CDN, CDN)
}
