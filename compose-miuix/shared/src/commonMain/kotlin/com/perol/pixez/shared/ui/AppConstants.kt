package com.perol.pixez.shared.ui

/**
 * 应用级全局常量定义，集中管理外部 URL、常用占位符模板与预设档位，消除散落的魔法值。
 */
object AppConstants {

    /**
     * 网络与图片源域名
     */
    object Network {
        const val HOST_PXIMG = "i.pximg.net"
        const val HOST_PIXIV_RE = "i.pixiv.re"
        const val URL_PXIMG_FAVICON = "https://$HOST_PXIMG/favicon.ico"

        const val HTTP_POOL_MAX_IDLE_CONNECTIONS = 32
        const val HTTP_POOL_KEEP_ALIVE_DURATION_MINUTES = 5L
        const val HTTP_DISPATCHER_MAX_REQUESTS = 128
        const val HTTP_DISPATCHER_MAX_REQUESTS_PER_HOST = 32

        const val IMAGE_MAX_DECODE_DIMENSION = 4096

        /**
         * Pixivision 页面 Referer（Pixivision 域名下的图片同样启用防盗链校验）。
         */
        const val REFERER_PIXIVISION = "https://www.pixivision.net/"

        /**
         * 图片请求统一使用的浏览器 User-Agent。
         */
        const val IMAGE_REQUEST_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        /**
         * 相邻页预加载的解码边长上限（px）。
         *
         * 预加载只为写入 Coil 磁盘缓存，解码结果随即丢弃，因此按最小尺寸解码，
         * 避免原图全尺寸位图（数十 MB）在堆内存中短暂驻留。
         */
        const val IMAGE_PRELOAD_DECODE_DIMENSION = 192

        const val DOH_URL = "https://cloudflare-dns.com/dns-query"
        const val DOH_TIMEOUT_SECONDS = 5L
        val DOH_BOOTSTRAP_HOSTS = listOf("1.1.1.1", "1.0.0.1")
    }

    /**
     * 账号与凭据安全常量
     */
    object Auth {
        const val DEFAULT_PASSWORD_PLACEHOLDER = "no more"
        const val TOKEN_ENCRYPTION_PREFIX = "enc_v1:"
        const val KEYSTORE_ALIAS = "PixEzTokenMasterKey"
    }

    /**
     * 深度链接与快捷跳转协议
     */
    object Scheme {
        const val SCHEME_PIXEZ = "pixez"
        const val URI_QUICK_SEARCH = "pixez://search"
    }

    /**
     * 分享与复制信息模板
     */
    object Share {
        const val DEFAULT_COPY_TEXT_FORMAT = "title:{title}\npainter:{user_name}\nillust id:{illust_id}"
        const val ARTWORK_URL_PLACEHOLDER = "https://www.pixiv.net/artworks/{illust_id}"
        const val USER_URL_PLACEHOLDER = "https://www.pixiv.net/users/{user_id}"
    }

    /**
     * 外部链接与社群地址
     */
    object Urls {
        const val PIXIV_APP_API = "https://app-api.pixiv.net/"
        const val PIXIV_OAUTH = "https://oauth.secure.pixiv.net/"
        const val PIXIV_FAVICON = Network.URL_PXIMG_FAVICON
        const val PIXIV_ARTWORK_PREFIX = "https://www.pixiv.net/artworks/"
        fun pixivArtworkUrl(id: Long): String = "$PIXIV_ARTWORK_PREFIX$id"
        fun pixivArtworkUrl(id: Int): String = "$PIXIV_ARTWORK_PREFIX$id"
        const val PIXIV_USER_PREFIX = "https://www.pixiv.net/users/"
        fun pixivUserUrl(userId: Long): String = "$PIXIV_USER_PREFIX$userId"
        fun pixivUserUrl(userId: Int): String = "$PIXIV_USER_PREFIX$userId"
        fun pixivUserUrl(userId: String): String = "$PIXIV_USER_PREFIX$userId"
        const val PIXIV_NOVEL_PREFIX = "https://www.pixiv.net/novel/show.php?id="
        fun pixivNovelUrl(id: Long): String = "$PIXIV_NOVEL_PREFIX$id"
        fun pixivNovelUrl(id: Int): String = "$PIXIV_NOVEL_PREFIX$id"
        const val GITHUB_REPO = "https://github.com/137458/pixez-miuix"
        const val GITHUB_RELEASES = "https://github.com/137458/pixez-miuix/releases"
        const val GITHUB_ISSUES = "https://github.com/137458/pixez-miuix/issues"
        const val TELEGRAM_GROUP = "https://t.me/pixez_group"
        const val AFDIAN = "https://afdian.com/a/perol"
        const val PIXIV_PRIVACY = "https://policies.pixiv.net/privacy.html"
        const val PIXIV_TERMS = "https://policies.pixiv.net/terms.html"
        const val PIXIV_LEAVE_ACCOUNT = "https://www.pixiv.net/leave_pixiv.php"
        const val SAUCE_NAO = "https://saucenao.com"
        const val SAUCE_NAO_SEARCH = "https://saucenao.com/search.php"
        const val AUTHOR_NOTSFSSSF = "https://github.com/Notsfsssf"
        const val AUTHOR_ROSEMARY = "https://github.com/137458"
        const val FEEDBACK_EMAIL = "PxezFeedBack@outlook.com"
        const val FEEDBACK_MAILTO = "mailto:PxezFeedBack@outlook.com"
        val BOARD_URLS = listOf(
            "https://raw.githubusercontent.com/137458/pixez-miuix/refs/heads/master/.github/board/android.json",
            "https://fastly.jsdelivr.net/gh/137458/pixez-miuix@master/.github/board/android.json",
            "https://cdn.jsdelivr.net/gh/137458/pixez-miuix@master/.github/board/android.json",
        )
    }

    /**
     * 下载相关预设与占位符
     */
    object Download {
        const val DEFAULT_NAME_FORMAT = "{illust_id}_p{part}"

        val FORMAT_PLACEHOLDERS = listOf(
            "{illust_id}",
            "{user_id}",
            "{title}",
            "{author}",
            "{part}",
            "{create_date}",
            "{width}",
            "{height}",
            "{width}x{height}",
        )

        val MAX_TASK_OPTIONS = listOf(1, 2, 3, 5, 8, 10)
        const val TASK_COUNT_MIN = 1
        const val TASK_COUNT_MAX = 10
        val TASK_COUNT_RANGE = TASK_COUNT_MIN..TASK_COUNT_MAX

        const val EXTRA_ANDROID_LIVE_STATUS = "android.requestLiveStatusNotification"
        const val EXTRA_ANDROID_LIVE = "android.liveStatus"
        const val EXTRA_ANDROID_LIVE_TITLE = "android.liveStatusTitle"
        const val EXTRA_ANDROID_LIVE_TEXT = "android.liveStatusText"
        const val EXTRA_ANDROID_LIVE_PROGRESS = "android.liveStatusProgress"
        const val EXTRA_ANDROID_SUBST_NAME = "android.substName"

        const val EXTRA_MIUI_FOCUS = "miui.focus.notification"
        const val EXTRA_MIUI_LIVE = "miui.live.notification"
        const val EXTRA_MIUI_SUBTEXT = "miui.subtext"
        const val EXTRA_MIUI_LIVE_TYPE = "miui.live_type"
        const val EXTRA_MIUI_ENABLE_FLOAT = "miui.enable_float"
        const val EXTRA_MIUI_FLOAT = "miui.float"
        const val EXTRA_MIUI_CATEGORY = "miui.category"
        const val EXTRA_MIUI_PROGRESS = "miui.progress"
        const val EXTRA_MIUI_PROGRESS_MAX = "miui.progress_max"

        const val EXTRA_OPLUS_CAPSULE = "oplus.capsule.notification"
        const val EXTRA_OPLUS_CAPSULE_ONGOING = "oplus.capsule.ongoing"
        const val EXTRA_OPLUS_CAPSULE_TITLE = "oplus.capsule.title"
        const val EXTRA_OPLUS_CAPSULE_TEXT = "oplus.capsule.text"
        const val EXTRA_OPLUS_CAPSULE_PROGRESS = "oplus.capsule.progress"

        const val WAKELOCK_TAG = "PixEz:DownloadWakeLock"
        const val WAKELOCK_TIMEOUT_MS = 15 * 60 * 1000L
    }

    /**
     * 搜索筛选预设
     */
    object Search {
        val BOOKMARK_THRESHOLDS = listOf(0, 100, 250, 500, 1000, 5000, 10000, 20000, 50000, 100000)
    }


    /**
     * 跨适配网格预设
     */
    object CrossAdapter {
        const val WIDTH_DEFAULT = 180
        const val WIDTH_MIN = 50
        const val WIDTH_MAX = 2160
        const val PREVIEW_ITEM_COUNT = 20
    }

    /**
     * 布局与大屏自适应常量（单位：dp）
     */
    object Layout {
        const val FLOATING_BAR_MAX_WIDTH_DP = 540
        const val FLOATING_BAR_MIN_WIDTH_DP = 320
        const val TABLET_CONTENT_MAX_WIDTH_DP = 760
        const val GRID_CARD_MIN_WIDTH_DP = 180
    }

    /**
     * 小说阅读器常量预设
     */
    object Novel {
        const val DEFAULT_FONT_SIZE_SP = 16f
        const val MIN_FONT_SIZE_SP = 12f
        const val MAX_FONT_SIZE_SP = 28f
    }

    /**
     * 应用更新与弹窗常量预设（单位：dp / ms）
     */
    object Update {
        const val CHANGELOG_MAX_HEIGHT_DP = 220
        const val CHANGELOG_MIN_HEIGHT_DP = 80
        const val SPEED_CALCULATION_INTERVAL_MS = 500L
        const val CARD_CORNER_RADIUS_DP = 16
        const val BADGE_CORNER_RADIUS_DP = 8
    }

    object Ugoira {
        val PLAY_SPEEDS = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    }

    /**
     * 作品类型标识（对应 [com.perol.pixez.shared.data.model.Illust.type] 的接口取值）。
     *
     * 收敛集中，避免该领域词在界面层散落为字面量后各处判断口径漂移。
     */
    object IllustType {
        const val ILLUST = "illust"
        const val MANGA = "manga"
        const val UGOIRA = "ugoira"

        /** 判断该作品类型是否为 Ugoira 动图。 */
        fun isUgoira(type: String): Boolean = type == UGOIRA
    }
}

