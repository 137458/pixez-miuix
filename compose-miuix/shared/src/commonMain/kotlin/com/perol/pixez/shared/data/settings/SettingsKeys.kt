package com.perol.pixez.shared.data.settings

/**
 * 旧 Flutter 应用 SharedPreferences / NSUserDefaults 的键名。
 * 键名必须与 lib/store/user_setting.dart 保持一致，以实现设置迁移。
 */
@Suppress("unused")
internal object SettingsKeys {
    const val ZOOM_QUALITY = "zoom_quality"
    const val FEED_PREVIEW_QUALITY = "feed_preview_quality"
    const val SINGLE_FOLDER = "single_folder"
    const val SAVE_FORMAT = "save_format"
    const val LANGUAGE_NUM = "language_num"
    const val LEGACY_WELCOME_PAGE_NUM = "welcome_page_num"
    const val WELCOME_PAGE_TYPE = "welcome_page_type"
    const val WELCOME_PAGE_NUM_IOS_MIGRATION = "welcome_page_num_ios_migration_v2"
    const val CROSS_COUNT = "cross_count"
    const val H_CROSS_COUNT = "h_cross_count"
    const val PICTURE_QUALITY = "picture_quality"
    const val MANGA_QUALITY = "manga_quality"
    const val IS_BANGS = "is_bangs"
    const val IS_AMOLED = "is_amoled"
    const val IS_TOP_MODE = "is_top_mode"
    const val STORE_PATH = "save_store"
    const val PICTURE_SOURCE = "picture_source"
    const val NETWORK_MODE = "network_mode"
    const val API_NETWORK_MODE = "network_mode_app_api_pixiv_net"
    const val OAUTH_NETWORK_MODE = "network_mode_oauth_secure_pixiv_net"
    const val LEGACY_DISABLE_BYPASS_SNI = "disable_bypass_sni"
    const val IS_HELPLESS_WAY = "is_helplessway"
    const val THEME_MODE = "theme_mode"
    const val SAVE_MODE = "save_mode"
    const val NOVEL_FONT_SIZE = "novel_font_size"
    const val IS_RETURN_AGAIN_TO_EXIT = "return_again_to_exit"
    const val IS_CLEAR_OLD_FORMAT_FILE = "is_clear_old_format_file"
    const val IS_FOLLOW_AFTER_STAR = "is_follow_after_star"
    const val IS_OVER_SANITY_LEVEL_FOLDER = "is_over_sanity_level_folder"
    const val MAX_RUNNING_TASK = "max_running_task"
    const val NSFW_MASK = "nsfw_mask"
    const val SAVE_AFTER_STAR = "save_after_star"
    const val STAR_AFTER_SAVE = "star_after_save"
    const val SAVE_EFFECT = "save_effect"
    const val SAVE_EFFECT_ENABLE = "save_effect_enable"
    const val PAD_MODE = "pad_mode"
    const val COPY_INFO_TEXT = "copy_info_text"
    const val NAME_EVAL = "name_eval"
    const val CROSS_ADAPT = "cross_adapt"
    const val CROSS_ADAPT_WIDTH = "cross_adapt_width"
    // 旧版横屏与竖屏共用同一组键，使用显式别名避免维护时误改。
    const val H_CROSS_ADAPT = CROSS_ADAPT
    const val H_CROSS_ADAPT_WIDTH = CROSS_ADAPT_WIDTH
    const val DEFAULT_PRIVATE_LIKE = "default_private_like"
    const val IMAGE_PICKER_TYPE = "image_picker_type_renew"
    const val LONG_PRESS_SAVE_CONFIRM = "long_press_save_confirm"
    const val USE_DYNAMIC_COLOR = "use_dynamic_color"
    const val SEED_COLOR = "seed_color"
    const val SWIPE_CHANGE_ARTWORK = "swipe_change_artwork"
    const val USE_SAUNCE_NAO_WEBVIEW = "use_sauce_nao_webview"
    const val FEED_AI_BADGE = "feed_ai_badge"
    const val IGNORE_UPDATE_VERSION = "ignore_update_version"
    const val ILLUST_DETAIL_SAVE_SKIP_LONG_PRESS = "illust_detail_save_skip_long_press"
    const val DRAG_START_X = "drag_start_x"
    const val AUTO_TAG_WHEN_STAR = "auto_tag_when_star"
    const val SEARCH_HISTORY = "search_history"
    const val SEARCH_SORT = "search_sort"
    const val SEARCH_TARGET = "search_target"
    const val SEARCH_AI_TYPE = "search_ai_type"
    const val SEARCH_BOOKMARK_THRESHOLD = "search_bookmark_threshold"

    // 旧版遗留键，用于迁移兼容
    const val FILE_NAME_EVAL_LEGACY = "file_name_eval"
    const val H_IS_NOT_ALLOW_LEGACY = "h_is_not_allow"
    const val DISPLAY_MODE_LEGACY = "display_mode"
}
