package com.perol.pixez.shared.platform

/**
 * 下载通知 extra 与 WakeLock 平台常量（架构审查 中-2 自 ui 层下沉）。
 *
 * 正名落位于 platform 包，Android actual（DownloadNotifier / PlatformDownloadKeeper）
 * 直引此处，不再反向依赖 ui 层 [com.perol.pixez.shared.ui.AppConstants]；
 * UI 侧经 `AppConstants.Download` 保留的委托别名访问，调用点不变，随全量移包清理。
 */
object DownloadPlatformDefaults {

    // Android 16 (API 36) 原生实时动态状态栏胶囊 (Rich Ongoing Notifications / Live Status)
    const val EXTRA_ANDROID_LIVE_STATUS = "android.requestLiveStatusNotification"
    const val EXTRA_ANDROID_LIVE = "android.liveStatus"
    const val EXTRA_ANDROID_LIVE_TITLE = "android.liveStatusTitle"
    const val EXTRA_ANDROID_LIVE_TEXT = "android.liveStatusText"
    const val EXTRA_ANDROID_LIVE_PROGRESS = "android.liveStatusProgress"
    const val EXTRA_ANDROID_SUBST_NAME = "android.substName"

    // Xiaomi HyperOS / MIUI 焦点通知与灵动胶囊协议支持
    const val EXTRA_MIUI_FOCUS = "miui.focus.notification"
    const val EXTRA_MIUI_LIVE = "miui.live.notification"
    const val EXTRA_MIUI_SUBTEXT = "miui.subtext"
    const val EXTRA_MIUI_LIVE_TYPE = "miui.live_type"
    const val EXTRA_MIUI_ENABLE_FLOAT = "miui.enable_float"
    const val EXTRA_MIUI_FLOAT = "miui.float"
    const val EXTRA_MIUI_CATEGORY = "miui.category"
    const val EXTRA_MIUI_PROGRESS = "miui.progress"
    const val EXTRA_MIUI_PROGRESS_MAX = "miui.progress_max"

    // OPLUS / ColorOS / OxygenOS 智慧胶囊与流体云协议支持
    const val EXTRA_OPLUS_CAPSULE = "oplus.capsule.notification"
    const val EXTRA_OPLUS_CAPSULE_ONGOING = "oplus.capsule.ongoing"
    const val EXTRA_OPLUS_CAPSULE_TITLE = "oplus.capsule.title"
    const val EXTRA_OPLUS_CAPSULE_TEXT = "oplus.capsule.text"
    const val EXTRA_OPLUS_CAPSULE_PROGRESS = "oplus.capsule.progress"

    const val WAKELOCK_TAG = "PixEz:DownloadWakeLock"
    const val WAKELOCK_TIMEOUT_MS = 15 * 60 * 1000L
}
