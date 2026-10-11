package com.perol.pixez.shared.network

/**
 * HTTP 引擎层默认参数收敛点：连接池 / 并发上限 / DoH / 网络模式取值 / 图片请求 UA（架构审查 中-2）。
 *
 * 正名落位于 network 包，network / platform 层直引此处，不再反向依赖
 * ui 层 [com.perol.pixez.shared.ui.AppConstants]；UI 侧经 `AppConstants.Network`
 * 保留的委托别名访问，调用点不变，随全量移包清理。
 */
object HttpEngineDefaults {

    /** 网络模式「标准直连」取值：modeProvider 返回该值时跳过 DoH 走系统 DNS（RobustDohDns 契约值）。 */
    const val MODE_STANDARD = "standard"

    const val HTTP_POOL_MAX_IDLE_CONNECTIONS = 32
    const val HTTP_POOL_KEEP_ALIVE_DURATION_MINUTES = 5L
    const val HTTP_DISPATCHER_MAX_REQUESTS = 128
    const val HTTP_DISPATCHER_MAX_REQUESTS_PER_HOST = 32

    /**
     * 图片请求统一使用的浏览器 User-Agent。
     */
    const val IMAGE_REQUEST_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val DOH_URL = "https://cloudflare-dns.com/dns-query"
    const val DOH_TIMEOUT_SECONDS = 5L
    val DOH_BOOTSTRAP_HOSTS = listOf("1.1.1.1", "1.0.0.1")
}
