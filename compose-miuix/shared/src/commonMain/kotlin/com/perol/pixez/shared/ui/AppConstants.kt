package com.perol.pixez.shared.ui

/**
 * 应用级全局常量定义，集中管理外部 URL、常用占位符模板与预设档位，消除散落的魔法值。
 */
object AppConstants {

    /**
     * 外部链接与社群地址
     */
    object Urls {
        const val PIXIV_APP_API = "https://app-api.pixiv.net/"
        const val PIXIV_OAUTH = "https://oauth.secure.pixiv.net/"
        const val PIXIV_FAVICON = "https://i.pximg.net/favicon.ico"
        const val GITHUB_REPO = "https://github.com/137458/pixez-miuix"
        const val GITHUB_RELEASES = "https://github.com/137458/pixez-miuix/releases"
        const val GITHUB_ISSUES = "https://github.com/137458/pixez-miuix/issues"
        const val TELEGRAM_GROUP = "https://t.me/pixez_group"
        const val AFDIAN = "https://afdian.com/a/perol"
        const val PIXIV_PRIVACY = "https://policies.pixiv.net/privacy.html"
        const val PIXIV_TERMS = "https://policies.pixiv.net/terms.html"
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

    /**
     * 小说阅读器常量预设
     */
    object Novel {
        const val DEFAULT_FONT_SIZE_SP = 16f
        const val MIN_FONT_SIZE_SP = 12f
        const val MAX_FONT_SIZE_SP = 28f
    }
}

