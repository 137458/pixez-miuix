package com.perol.pixez.shared.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.perol.pixez.shared.R
import io.github.aakira.napier.Napier

/**
 * 图片下载与应用更新下载共用的通知构建逻辑。
 *
 * 两者共用同一个通知渠道（[IMAGE_DOWNLOAD_CHANNEL_ID]），渠道创建、进度条与状态栏胶囊 extras
 * 只维护这一份，防止两边继续分叉；通知 ID 各自隔离。
 */
internal fun ensureDownloadNotificationChannel(
    context: Context,
    channelId: String,
    importance: Int,
    nameRes: Int,
    descriptionRes: Int,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    // 渠道名与描述允许原地更新，故应用内语言变更时也要重建，否则渠道一直停在旧语言。
    val localized = context.localizedForAppLanguage()
    val name = localized.getString(nameRes)
    val existing = manager.getNotificationChannel(channelId)
    if (existing != null && existing.importance >= importance && existing.name?.toString() == name) return
    manager.createNotificationChannel(
        NotificationChannel(channelId, name, importance).apply {
            description = localized.getString(descriptionRes)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        },
    )
}

/**
 * 删除旧版低优先级下载渠道（一次性迁移）。
 *
 * 在 Application.onCreate 调用一次：旧实现在每次进度通知前都尝试删除，
 * 等于每 500ms 调一次系统接口，既无意义也会与新建渠道竞争。
 */
fun migrateLegacyDownloadChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    listOf(LEGACY_DOWNLOAD_CHANNEL_ID, LEGACY_UPDATE_DOWNLOAD_CHANNEL_ID).forEach { channelId ->
        runCatching { manager.deleteNotificationChannel(channelId) }
            .onFailure { Napier.d("删除旧版下载通知渠道失败", it, tag = "DownloadNotifier") }
    }
}

/** 已废弃的旧渠道 ID，仅用于启动时清理。 */
private const val LEGACY_DOWNLOAD_CHANNEL_ID = "pixez_download_channel"

/** 更新下载并入图片下载渠道后废弃的旧渠道 ID，仅用于启动时清理。 */
private const val LEGACY_UPDATE_DOWNLOAD_CHANNEL_ID = "pixez_update_download_channel"

/** 图片下载与更新下载共用的进行中通知渠道。 */
internal const val IMAGE_DOWNLOAD_CHANNEL_ID = "pixez_download_live_channel"

/**
 * 构建「下载中」进度通知。
 *
 * [includeLiveStatus] 为 true 时附带 Android 16 Live Status、MIUI/OPLUS 胶囊 extras；
 * 图片下载与更新下载均开启。
 */
internal fun buildDownloadProgressNotification(
    context: Context,
    channelId: String,
    title: CharSequence,
    current: Int,
    total: Int,
    contentIntent: PendingIntent?,
    includeLiveStatus: Boolean,
    indeterminate: Boolean = total <= 0,
    liveText: CharSequence? = null,
): Notification {
    val localized = context.localizedForAppLanguage()
    val percent = if (total > 0) (current.toLong() * 100L / total).toInt().coerceIn(0, 100) else 0
    val percentText = localized.getString(R.string.download_progress_percent, percent)

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setContentText(localized.getString(R.string.download_progress_text, current, total, percent))
        .setSubText(if (total > 0) percentText else "")
        .setProgress(total, current, indeterminate)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setOnlyAlertOnce(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setShowWhen(true)
        .apply {
            if (contentIntent != null) setContentIntent(contentIntent)
            if (includeLiveStatus) {
                setColorized(true)
                setColor(0xFF2196F3.toInt())
                addExtras(downloadLiveStatusExtras(localized, title, percent, percentText, liveText ?: percentText))
            }
        }
    return builder.build()
}

private fun downloadLiveStatusExtras(
    localized: Context,
    title: CharSequence,
    percent: Int,
    percentText: String,
    liveText: CharSequence,
): Bundle = Bundle().apply {
    // Android 16 (API 36) 原生实时动态状态栏胶囊 (Rich Ongoing Notifications / Live Status)
    putBoolean(DownloadPlatformDefaults.EXTRA_ANDROID_LIVE_STATUS, true)
    putBoolean(DownloadPlatformDefaults.EXTRA_ANDROID_LIVE, true)
    putString(DownloadPlatformDefaults.EXTRA_ANDROID_SUBST_NAME, "PixEz")
    putCharSequence(DownloadPlatformDefaults.EXTRA_ANDROID_LIVE_TITLE, title)
    putCharSequence(DownloadPlatformDefaults.EXTRA_ANDROID_LIVE_TEXT, liveText)
    putInt(DownloadPlatformDefaults.EXTRA_ANDROID_LIVE_PROGRESS, percent)

    // Xiaomi HyperOS / MIUI 焦点通知与灵动胶囊协议支持
    putBoolean(DownloadPlatformDefaults.EXTRA_MIUI_FOCUS, true)
    putBoolean(DownloadPlatformDefaults.EXTRA_MIUI_LIVE, true)
    putInt(DownloadPlatformDefaults.EXTRA_MIUI_LIVE_TYPE, 1)
    putBoolean(DownloadPlatformDefaults.EXTRA_MIUI_ENABLE_FLOAT, true)
    putBoolean(DownloadPlatformDefaults.EXTRA_MIUI_FLOAT, true)
    putString(DownloadPlatformDefaults.EXTRA_MIUI_CATEGORY, "download")
    putString(DownloadPlatformDefaults.EXTRA_MIUI_SUBTEXT, percentText)
    putInt(DownloadPlatformDefaults.EXTRA_MIUI_PROGRESS, percent)
    putInt(DownloadPlatformDefaults.EXTRA_MIUI_PROGRESS_MAX, 100)

    // OPLUS / ColorOS / OxygenOS 智慧胶囊与流体云协议支持
    putBoolean(DownloadPlatformDefaults.EXTRA_OPLUS_CAPSULE, true)
    putBoolean(DownloadPlatformDefaults.EXTRA_OPLUS_CAPSULE_ONGOING, true)
    putString(DownloadPlatformDefaults.EXTRA_OPLUS_CAPSULE_TITLE, title.toString())
    putString(DownloadPlatformDefaults.EXTRA_OPLUS_CAPSULE_TEXT, percentText)
    putInt(DownloadPlatformDefaults.EXTRA_OPLUS_CAPSULE_PROGRESS, percent)
}
