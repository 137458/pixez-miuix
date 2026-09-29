package com.perol.pixez.shared.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.perol.pixez.shared.R
import io.github.aakira.napier.Napier

/**
 * Android 平台实现：支持 Android 16 (API 36) Rich Ongoing Notifications 实时动态胶囊下载通知。
 */
actual class DownloadNotifier {
    private val channelId = "pixez_download_live_channel"

    private fun getNotificationManager(context: Context): NotificationManager? {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getNotificationManager(context) ?: return
            // 尝试移除旧版低优先级渠道，确保状态栏实时动态胶囊不受抑制
            try {
                manager.deleteNotificationChannel("pixez_download_channel")
            } catch (e: Throwable) {
                // 旧渠道可能本身不存在，删除失败不影响后续实时进度通知
                Napier.d("删除旧版下载通知渠道失败", e, tag = "DownloadNotifier")
            }

            val existing = manager.getNotificationChannel(channelId)
            val localized = context.localizedForAppLanguage()
            val channelName = localized.getString(R.string.download_channel_name)
            val channelDescription = localized.getString(R.string.download_channel_description)
            // 渠道名与描述允许原地更新，故应用内语言变更时也要重建，否则渠道一直停在旧语言
            if (existing == null || existing.importance < NotificationManager.IMPORTANCE_DEFAULT ||
                existing.name?.toString() != channelName
            ) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = channelDescription
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    actual fun notifyProgress(id: Long, title: String, current: Int, total: Int) {
        val context = BrowserLauncherContext.applicationContext ?: return
        createChannelIfNeeded(context)
        val manager = getNotificationManager(context) ?: return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pixez://downloads")).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            stableNotificationId(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val percent = if (total > 0) (current * 100 / total) else 0
        val localized = context.localizedForAppLanguage()
        val progressTitle = localized.getString(R.string.download_progress_title, title)
        val percentText = localized.getString(R.string.download_progress_percent, percent)
        val subText = if (total > 0) percentText else ""

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(progressTitle)
            .setContentText(localized.getString(R.string.download_progress_text, current, total, percent))
            .setSubText(subText)
            .setProgress(total, current, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColorized(true)
            .setColor(0xFF2196F3.toInt())
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .addExtras(android.os.Bundle().apply {
                // Android 16 (API 36) 原生实时动态状态栏胶囊 (Rich Ongoing Notifications / Live Status)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_LIVE_STATUS, true)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_LIVE, true)
                putString(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_SUBST_NAME, "PixEz")
                putCharSequence(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_LIVE_TITLE, progressTitle)
                putCharSequence(
                    com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_LIVE_TEXT,
                    localized.getString(R.string.download_live_text, current, total, percent),
                )
                putInt(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_LIVE_PROGRESS, percent)

                // Xiaomi HyperOS / MIUI 焦点通知与灵动胶囊协议支持
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_FOCUS, true)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_LIVE, true)
                putInt(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_LIVE_TYPE, 1)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_ENABLE_FLOAT, true)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_FLOAT, true)
                putString(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_CATEGORY, "download")
                putString(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_SUBTEXT, percentText)
                putInt(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_PROGRESS, percent)
                putInt(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_PROGRESS_MAX, 100)

                // OPLUS / ColorOS / OxygenOS 智慧胶囊与流体云协议支持
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_OPLUS_CAPSULE, true)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_OPLUS_CAPSULE_ONGOING, true)
                putString(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_OPLUS_CAPSULE_TITLE, progressTitle)
                putString(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_OPLUS_CAPSULE_TEXT, percentText)
                putInt(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_OPLUS_CAPSULE_PROGRESS, percent)
            })

        val notification = notificationBuilder.build()
        manager.notify(stableNotificationId(id), notification)
    }

    actual fun notifyFinished(id: Long, title: String, successCount: Int, failedCount: Int) {
        val context = BrowserLauncherContext.applicationContext ?: return
        createChannelIfNeeded(context)
        val manager = getNotificationManager(context) ?: return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pixez://downloads")).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            stableNotificationId(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val localized = context.localizedForAppLanguage()
        val summaryText = if (failedCount == 0) {
            localized.getString(R.string.download_finished_all, successCount)
        } else {
            localized.getString(R.string.download_finished_partial, successCount, failedCount)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(localized.getString(R.string.download_finished_title, title))
            .setContentText(summaryText)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(stableNotificationId(id), notification)
    }

    actual fun cancel(id: Long) {
        val context = BrowserLauncherContext.applicationContext ?: return
        val manager = getNotificationManager(context) ?: return
        manager.cancel(stableNotificationId(id))
    }
}
