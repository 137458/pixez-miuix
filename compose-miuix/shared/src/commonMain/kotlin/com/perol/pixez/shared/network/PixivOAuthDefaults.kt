package com.perol.pixez.shared.network

/**
 * Pixiv OAuth 应用常量：客户端凭据与应用内登录回调地址。
 *
 * 客户端凭据属于网络协议参数而非本地加密密钥，因此与 [PixivHosts] 同处 network 层，
 * 由 [OAuthClient] 与 DeepLink 回调校验共同引用，避免同一回调地址在两层各写一份字面量。
 */
object PixivOAuthDefaults {
    const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"

    /** 回调地址的路径部分，DeepLink 按 host + path 校验剪贴板里的 OAuth 链接。 */
    const val REDIRECT_PATH = "/web/v1/users/auth/pixiv/callback"

    /** OAuth 回调地址，授权码交换 token 时作为 redirect_uri 提交。 */
    const val REDIRECT_URI = "https://${PixivHosts.HOST_PIXIV_APP}$REDIRECT_PATH"
}
