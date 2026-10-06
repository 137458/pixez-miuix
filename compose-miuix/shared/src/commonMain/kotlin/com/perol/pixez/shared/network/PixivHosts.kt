package com.perol.pixez.shared.network

/**
 * Pixiv 图片源域名与外部跳转地址常量。
 *
 * 下层（network / data）所需成员收敛于此，避免反向依赖 ui 层 [com.perol.pixez.shared.ui.AppConstants]。
 */
object PixivHosts {
    const val HOST_PXIMG = "i.pximg.net"
    const val HOST_PIXIV_RE = "i.pixiv.re"
    const val PIXIV_APP_API = "https://app-api.pixiv.net/"

    /** Pixivision 中文站点 Referer：作品图源抓取走 /zh/ 路径以匹配文章语言。 */
    const val REFERER_PIXIVISION_ZH = "https://www.pixivision.net/zh/"

    const val PIXIV_ARTWORK_PREFIX = "https://www.pixiv.net/artworks/"
}
