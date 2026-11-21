package com.perol.pixez.shared.ui

import com.perol.pixez.shared.network.GitHubReleaseEndpoints
import com.perol.pixez.shared.network.PixivHosts

/**
 * UI 层全局常量：图片解码档位、布局与断点、分享模板、外部链接模板与预置档位，
 * 集中管理以消除散落的魔法值。
 *
 * 网络 / 主机 / 引擎常量归属 [com.perol.pixez.shared.network.PixivHosts]、
 * [com.perol.pixez.shared.network.HttpEngineDefaults] 与
 * [com.perol.pixez.shared.network.GitHubReleaseEndpoints]，凭据常量归属
 * [com.perol.pixez.shared.platform.TokenCipherDefaults]；调用方直接引用正名，此处不再保留委托别名。
 */
object AppConstants {

    /**
     * 图片解码档位（Coil 请求侧边长上限）
     */
    object Image {
        /** 原图 / 详情页全屏浏览的解码边长上限（px）。 */
        const val MAX_DECODE_DIMENSION = 4096

        /** 普通浏览场景（列表 / 详情页）的解码边长上限（px）。 */
        const val STANDARD_DECODE_DIMENSION = 2048

        /**
         * 相邻页预加载的解码边长上限（px）。
         *
         * 预加载只为写入 Coil 磁盘缓存，解码结果随即丢弃，因此按最小尺寸解码，
         * 避免原图全尺寸位图（数十 MB）在堆内存中短暂驻留。
         */
        const val PRELOAD_DECODE_DIMENSION = 192
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
        /** 复制文案里的作品地址模板：与真实地址同一前缀，仅把 id 段替换为占位符。 */
        val ARTWORK_URL_PLACEHOLDER = PixivHosts.PIXIV_ARTWORK_PREFIX + "{illust_id}"

        /** 复制文案里的用户主页模板。 */
        val USER_URL_PLACEHOLDER = Urls.PIXIV_USER_PREFIX + "{user_id}"
    }

    /**
     * 站外链接与社群地址。
     *
     * 主机字面量一律由 [PixivHosts.WEB_ORIGIN] 与
     * [com.perol.pixez.shared.network.GitHubReleaseEndpoints.OFFICIAL_REPO_PATH] 派生，
     * 避免同一域名在本文件与 network 层各存一份。
     */
    object Urls {
        const val PIXIV_USER_PREFIX = PixivHosts.WEB_ORIGIN + "/users/"

        /** 小说阅读页直链前缀。 */
        const val PIXIV_NOVEL_PREFIX = PixivHosts.WEB_ORIGIN + "/novel/show.php?id="

        fun pixivArtworkUrl(id: Long): String = PixivHosts.PIXIV_ARTWORK_PREFIX + id

        fun pixivUserUrl(userId: Long): String = PIXIV_USER_PREFIX + userId

        fun pixivNovelUrl(id: Long): String = PIXIV_NOVEL_PREFIX + id

        /** 小说 id 在部分调用点以 Int 传入（如对旧版接口字段的解包结果），保留重载避免调用方手写转换。 */
        fun pixivNovelUrl(id: Int): String = PIXIV_NOVEL_PREFIX + id

        const val GITHUB_REPO = "https://github.com/" + GitHubReleaseEndpoints.OFFICIAL_REPO_PATH

        const val PIXIV_PRIVACY = PixivHosts.WEB_ORIGIN + "/policies/privacy.html"

        const val PIXIV_TERMS = PixivHosts.WEB_ORIGIN + "/policies/terms.html"

        const val PIXIV_LEAVE_ACCOUNT = PixivHosts.WEB_ORIGIN + "/leave_pixiv.php"

        const val AUTHOR_NOTSFSSSF = "https://github.com/Notsfsssf"

        const val AUTHOR_ROSEMARY = "https://github.com/" + GitHubReleaseEndpoints.OWNER

        const val FEEDBACK_EMAIL = "PxezFeedBack@outlook.com"

        const val FEEDBACK_MAILTO = "mailto:" + FEEDBACK_EMAIL
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
     * 应用更新与弹窗常量预设（单位：dp）
     */
    object Update {
        /** 更新日志卡片可视区域最大高度（dp），扩容以展示更充分的版本说明，消除视口狭窄感。 */
        const val CHANGELOG_MAX_HEIGHT_DP = 380

        /** 更新日志滚动渐变遮罩边缘高度（dp），用于顶部卷出和底部未完时的平滑淡出。 */
        const val CHANGELOG_FADE_EDGE_DP = 28

        /** 滚动条指示器宽度（dp）。 */
        const val SCROLLBAR_WIDTH_DP = 3

        const val BADGE_CORNER_RADIUS_DP = 8
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