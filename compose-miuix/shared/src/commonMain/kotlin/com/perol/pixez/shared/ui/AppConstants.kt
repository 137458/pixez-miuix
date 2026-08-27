package com.perol.pixez.shared.ui

/**
 * 应用级全局常量定义，集中管理外部 URL、常用占位符模板与预设档位，消除散落的魔法值。
 */
object AppConstants {

    /**
     * 外部链接与社群地址
     */
    object Urls {
        const val GITHUB_REPO = "https://github.com/137458/pixez-miuix"
        const val GITHUB_RELEASES = "https://github.com/137458/pixez-miuix/releases"
        const val GITHUB_ISSUES = "https://github.com/137458/pixez-miuix/issues"
        const val TELEGRAM_GROUP = "https://t.me/pixez_group"
        const val AFDIAN = "https://afdian.com/a/perol"
        const val PIXIV_PRIVACY = "https://policies.pixiv.net/privacy.html"
        const val PIXIV_TERMS = "https://policies.pixiv.net/terms.html"
        const val SAUCE_NAO = "https://saucenao.com"
    }

    /**
     * 下载相关预设与占位符
     */
    object Download {
        val FORMAT_PLACEHOLDERS = listOf(
            "{illust_id}",
            "{user_id}",
            "{title}",
            "{author}",
            "{part}",
        )

        val MAX_TASK_OPTIONS = listOf(1, 2, 3, 5, 8, 10)
        const val TASK_COUNT_MIN = 1
        const val TASK_COUNT_MAX = 10
        val TASK_COUNT_RANGE = TASK_COUNT_MIN..TASK_COUNT_MAX
    }

    /**
     * 搜索筛选预设
     */
    object Search {
        val BOOKMARK_THRESHOLDS = listOf(0, 100, 500, 1000, 5000, 10000, 20000, 50000)
    }

    /**
     * 跨适配网格预设
     */
    object CrossAdapter {
        const val WIDTH_MIN = 100
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
}

