package com.perol.pixez.shared.network

/**
 * Pixiv 图片源域名与外部跳转地址常量。
 *
 * 下层（network / data）所需成员收敛于此，避免反向依赖 ui 层 [com.perol.pixez.shared.ui.AppConstants]。
 */
object PixivHosts {
    const val HOST_PXIMG = "i.pximg.net"

    /** 插画静态资源域，SauceNAO 缩略图与旧版接口会返回该域链接。 */
    const val HOST_PXIMG_STATIC = "s.pximg.net"

    const val HOST_PIXIV_RE = "i.pixiv.re"

    /** Pixiv 裸域：DeepLink 与可信重定向按裸域及其子域一并放行。 */
    const val HOST_PIXIV_NET = "pixiv.net"

    /** Pixiv 网页主域。 */
    const val HOST_WWW_PIXIV = "www.pixiv.net"

    /** Pixiv 短链域（pixiv.me/<name>）。 */
    const val HOST_PIXIV_ME = "pixiv.me"

    /** Pixiv 开放接口域，同时是 OAuth 回调所在域。 */
    const val HOST_PIXIV_APP = "app-api.pixiv.net"

    const val PIXIV_APP_API = "https://$HOST_PIXIV_APP/"

    /** OAuth 认证域：token 申请与刷新只走该域。 */
    const val HOST_OAUTH = "oauth.secure.pixiv.net"

    /** 账号管理域：账号资料与设备管理接口走该域。 */
    const val HOST_ACCOUNTS = "accounts.pixiv.net"

    /** Pixivision 站点 host，供可信 URL 白名单按 host 比对。 */
    const val PIXIVISION_HOST = "www.pixivision.net"

    /** Pixivision 裸域：白名单需同时放行裸域与子域。 */
    const val PIXIVISION_BARE_HOST = "pixivision.net"

    /** Pixivision 作品嵌入域，特辑详情里的作品预览与兜底封面走该域。 */
    const val PIXIVISION_EMBED_HOST = "embed.pixiv.net"

    /** Pixivision 站点源，供网页抓取与文章链接拼接复用。 */
    const val PIXIVISION_ORIGIN = "https://$PIXIVISION_HOST"

    /** Pixivision 图片 Referer（站点根，防盗链校验按根域放行）。 */
    const val REFERER_PIXIVISION = "$PIXIVISION_ORIGIN/"

    /** Pixivision 中文站点 Referer：作品图源抓取走 /zh/ 路径以匹配文章语言。 */
    const val REFERER_PIXIVISION_ZH = "$PIXIVISION_ORIGIN/zh/"

    /** Pixiv 网页站源：作品页、用户页、政策页与退出账号页的链接一律由此派生。 */
    const val WEB_ORIGIN = "https://$HOST_WWW_PIXIV"

    const val PIXIV_ARTWORK_PREFIX = "$WEB_ORIGIN/artworks/"
}

/**
 * 第三方直连接口地址（非 Pixiv 域）。
 *
 * 与 [PixivHosts] 同处 network 层：data 层直引此处，不再反向 import ui.AppConstants（架构审查 DN15）。
 */
object SauceNaoEndpoints {
    /** SauceNAO 主域：搜图表单与缩略图白名单同源。 */
    const val HOST = "saucenao.com"

    /** SauceNAO 以图搜图表单提交地址。 */
    const val SEARCH = "https://$HOST/search.php"
}
