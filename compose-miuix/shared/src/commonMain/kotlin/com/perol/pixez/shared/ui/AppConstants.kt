package com.perol.pixez.shared.ui

import com.perol.pixez.shared.network.GitHubReleaseEndpoints
import com.perol.pixez.shared.network.HttpEngineDefaults
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.platform.TokenCipherDefaults

/**
 * 应用级全局常量定义，集中管理外部 URL、常用占位符模板与预设档位，消除散落的魔法值。
 */
object AppConstants {

    /**
     * 网络与图片源域名
     */
    object Network {
        // 过渡委托：正名已外迁至 network/PixivHosts.kt（UI 与测试仍经由处引用），随全量移包清理。
        const val HOST_PXIMG = PixivHosts.HOST_PXIMG
        const val HOST_PIXIV_RE = PixivHosts.HOST_PIXIV_RE

        // 过渡委托：正名已外迁至 network/HttpEngineDefaults.kt（network 层直引，UI 与测试仍经由处引用），随全量移包清理。
        const val MODE_STANDARD = HttpEngineDefaults.MODE_STANDARD
        const val HTTP_POOL_MAX_IDLE_CONNECTIONS = HttpEngineDefaults.HTTP_POOL_MAX_IDLE_CONNECTIONS
        const val HTTP_POOL_KEEP_ALIVE_DURATION_MINUTES = HttpEngineDefaults.HTTP_POOL_KEEP_ALIVE_DURATION_MINUTES
        const val HTTP_DISPATCHER_MAX_REQUESTS = HttpEngineDefaults.HTTP_DISPATCHER_MAX_REQUESTS
        const val HTTP_DISPATCHER_MAX_REQUESTS_PER_HOST = HttpEngineDefaults.HTTP_DISPATCHER_MAX_REQUESTS_PER_HOST

        const val IMAGE_MAX_DECODE_DIMENSION = 4096

        /** 普通浏览场景（列表 / 详情页）的解码边长上限（px）。 */
        const val IMAGE_STANDARD_DECODE_DIMENSION = 2048

        /**
         * Pixivision 页面 Referer（Pixivision 域名下的图片同样启用防盗链校验）。
         */
        const val REFERER_PIXIVISION = "https://www.pixivision.net/"

        // 过渡委托：正名已外迁至 network/HttpEngineDefaults.kt（network 层直引，UI 仍经由处引用），随全量移包清理。
        const val IMAGE_REQUEST_USER_AGENT = HttpEngineDefaults.IMAGE_REQUEST_USER_AGENT

        /**
         * 相邻页预加载的解码边长上限（px）。
         *
         * 预加载只为写入 Coil 磁盘缓存，解码结果随即丢弃，因此按最小尺寸解码，
         * 避免原图全尺寸位图（数十 MB）在堆内存中短暂驻留。
         */
        const val IMAGE_PRELOAD_DECODE_DIMENSION = 192

        // 过渡委托：正名已外迁至 network/HttpEngineDefaults.kt（network 层直引，UI 与测试仍经由处引用），随全量移包清理。
        const val DOH_URL = HttpEngineDefaults.DOH_URL
        const val DOH_TIMEOUT_SECONDS = HttpEngineDefaults.DOH_TIMEOUT_SECONDS
        val DOH_BOOTSTRAP_HOSTS = HttpEngineDefaults.DOH_BOOTSTRAP_HOSTS
    }

    /**
     * 账号与凭据安全常量
     */
    object Auth {
        // 过渡委托：正名已外迁至 platform/TokenCipherDefaults.kt（platform 层直引，测试仍经由处引用），随全量移包清理。
        const val TOKEN_ENCRYPTION_PREFIX = TokenCipherDefaults.TOKEN_ENCRYPTION_PREFIX
        const val KEYSTORE_ALIAS = TokenCipherDefaults.KEYSTORE_ALIAS
    }

    /**
     * 深度链接与快捷跳转协议
     */
    object Scheme {
        const val URI_QUICK_SEARCH = "pixez://search"
    }

    /**
     * 分享与复制信息模板
     */
    object Share {
        const val ARTWORK_URL_PLACEHOLDER = "https://www.pixiv.net/artworks/{illust_id}"
        const val USER_URL_PLACEHOLDER = "https://www.pixiv.net/users/{user_id}"
    }

    /**
     * 外部链接与社群地址
     */
    object Urls {
        // 过渡委托：正名已外迁至 network/PixivHosts.kt（UI 与测试仍经由处引用），随全量移包清理。
        const val PIXIV_APP_API = PixivHosts.PIXIV_APP_API
        const val PIXIV_USER_PREFIX = "https://www.pixiv.net/users/"
        fun pixivArtworkUrl(id: Long): String = PixivHosts.PIXIV_ARTWORK_PREFIX + id
        fun pixivUserUrl(userId: Long): String = "$PIXIV_USER_PREFIX$userId"
        fun pixivUserUrl(userId: String): String = "$PIXIV_USER_PREFIX$userId"
        const val PIXIV_NOVEL_PREFIX = "https://www.pixiv.net/novel/show.php?id="
        fun pixivNovelUrl(id: Long): String = "$PIXIV_NOVEL_PREFIX$id"
        fun pixivNovelUrl(id: Int): String = "$PIXIV_NOVEL_PREFIX$id"
        const val GITHUB_REPO = "https://github.com/137458/pixez-miuix"

        // 过渡委托：正名已外迁至 network/GitHubReleaseEndpoints.kt（data 层 UpdateRepository 直引），随全量移包清理。
        const val GITHUB_RELEASES = GitHubReleaseEndpoints.RELEASES_PAGE
        const val GITHUB_RELEASES_LATEST_API = GitHubReleaseEndpoints.LATEST_API
        const val PIXIV_PRIVACY = "https://policies.pixiv.net/privacy.html"
        const val PIXIV_TERMS = "https://policies.pixiv.net/terms.html"
        const val PIXIV_LEAVE_ACCOUNT = "https://www.pixiv.net/leave_pixiv.php"
        const val SAUCE_NAO_SEARCH = "https://saucenao.com/search.php"
        const val AUTHOR_NOTSFSSSF = "https://github.com/Notsfsssf"
        const val AUTHOR_ROSEMARY = "https://github.com/137458"
        const val FEEDBACK_EMAIL = "PxezFeedBack@outlook.com"
        const val FEEDBACK_MAILTO = "mailto:PxezFeedBack@outlook.com"
    }

    /**
     * 下载相关预设与占位符。
     *
     * 通知 extra / WakeLock 常量已统一放在
     * [com.perol.pixez.shared.platform.DownloadPlatformDefaults]，此处不再保留重复字面量副本。
     */
    object Download {
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

        /** 宽屏断点（dp）：容器达到该宽度即视为大屏，详情页图片收进内容列并限制显示高度。 */
        const val WIDE_SCREEN_MIN_WIDTH_DP = 600

        /** 详情页大图高度占视口比例上限：竖图在大屏上按该比例收缩宽度，避免纵向占满整屏。 */
        const val DETAIL_IMAGE_MAX_HEIGHT_FRACTION = 0.8f

        /** 作品卡片视觉圆角（dp），与 MIUIX Card 默认圆角一致，卡片展开转场按登记值对齐收回终点。 */
        const val ILLUST_CARD_CORNER_RADIUS_DP = 16

        /** 浏览历史卡片视觉圆角（dp），HistoryScreen 手动 clip 的视觉圆角。 */
        const val HISTORY_CARD_CORNER_RADIUS_DP = 12

        /** 列表触底预加载阈值：最后可见项距列表末尾不足该数量时触发加载更多。 */
        const val PRELOAD_THRESHOLD_ITEMS = 4

        /** 瀑布流双列场景的触底预加载阈值：单屏条目少，需更早触发（IllustStaggeredGrid）。 */
        const val PRELOAD_THRESHOLD_STAGGERED_ITEMS = 6

        /** 自适应网格最小卡宽上限（dp）：布局设置给的偏好宽度超过该值时收敛（IllustStaggeredGrid）。 */
        const val GRID_ADAPTIVE_MAX_WIDTH_DP = 1000

        /** 网格固定列数上限（IllustStaggeredGrid）。 */
        const val GRID_MAX_FIXED_COLUMNS = 8

        /** 特辑卡片网格最小卡宽换算倍率：特辑卡片更宽，按其偏好宽度放大后再作自适应最小宽。 */
        const val SPOTLIGHT_CARD_WIDTH_SCALE = 1.2f

        /** 特辑卡片自适应最小宽下限（dp）。 */
        const val SPOTLIGHT_CARD_MIN_WIDTH_DP = 160

        /** 特辑卡片自适应最小宽上限（dp）。 */
        const val SPOTLIGHT_CARD_MAX_WIDTH_DP = 600

        /** 特辑卡片在「固定 2 列」时改走自适应所用卡宽（dp）。 */
        const val SPOTLIGHT_CARD_ADAPTIVE_WIDTH_DP = 240

        /** 特辑卡片网格固定列数上限。 */
        const val SPOTLIGHT_GRID_MAX_COLUMNS = 4

        /** 折叠手势上滑阈值（px）：手指上推位移超过该值才协同折叠大标题（SearchScreen）。 */
        const val COLLAPSE_SCROLL_THRESHOLD_PX = 8f

        /** 折叠手势回顶过冲阈值（px）：列表触顶后仍继续下拉超过该值才展开大标题（SearchScreen）。 */
        const val EXPAND_OVERSCROLL_THRESHOLD_PX = 4f

        /** 详情页大图折叠进度累计位移阈值（dp）：上滑累计到该位移即视为完全折叠（IllustDetailScreen）。 */
        const val DETAIL_COLLAPSE_THRESHOLD_DP = 72
    }

    /**
     * 小说阅读器常量预设
     */
    object Novel {
        const val DEFAULT_FONT_SIZE_SP = 16f
        const val MIN_FONT_SIZE_SP = 12f
        const val MAX_FONT_SIZE_SP = 28f

        /** [newpage] 分页符渲染留白高度（dp）。 */
        const val NEW_PAGE_SPACING_DP = 48

        /** 内嵌插画 / 用户上传图在正文中的渲染高度（dp）。 */
        const val EMBEDDED_IMAGE_HEIGHT_DP = 220

        /** 章节标题相对正文的字号增量（sp）。 */
        const val CHAPTER_TITLE_FONT_SIZE_DELTA_SP = 4f
    }

    /**
     * 应用更新与弹窗常量预设（单位：dp / ms）
     */
    object Update {
        /** 更新日志卡片可视区域最大高度（dp），扩容以展示更充分的版本说明，消除视口狭窄感。 */
        const val CHANGELOG_MAX_HEIGHT_DP = 380

        /** 更新日志卡片可视区域最小高度（dp）。 */
        const val CHANGELOG_MIN_HEIGHT_DP = 80

        /** 更新日志滚动渐变遮罩边缘高度（dp），用于顶部卷出和底部未完时的平滑淡出。 */
        const val CHANGELOG_FADE_EDGE_DP = 28

        /** 滚动条指示器宽度（dp）。 */
        const val SCROLLBAR_WIDTH_DP = 3

        const val SPEED_CALCULATION_INTERVAL_MS = 500L
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

