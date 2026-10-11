package com.perol.pixez.shared.network

/**
 * GitHub Release 发布页与最新版 API 端点常量。
 *
 * 下层（data / network）所需成员收敛于此，避免反向依赖 ui 层 [com.perol.pixez.shared.ui.AppConstants]。
 */
object GitHubReleaseEndpoints {
    const val RELEASES_PAGE = "https://github.com/137458/pixez-miuix/releases"
    const val LATEST_API = "https://api.github.com/repos/137458/pixez-miuix/releases/latest"
}
