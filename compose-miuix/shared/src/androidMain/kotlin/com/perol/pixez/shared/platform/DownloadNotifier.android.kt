package com.perol.pixez.shared.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Android 平台实现：支持 Android 16 (API 36) Rich Ongoing Notifications 实时动态胶囊下载通知。
 */
actual class DownloadNotifier {
    private val channelId = "pixez_download_channel"
    private val channelName = "下载任务"

    private fun getNotificationManager(context: Context): NotificationManager? {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getNotificationManager(context) ?: return
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "显示插画与漫画下载进度"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    actual fun notifyProgress(id: Int, title: String, current: Int, total: Int) {
        val context = BrowserLauncherContext.applicationContext ?: return
        createChannelIfNeeded(context)
        val manager = getNotificationManager(context) ?: return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pixez://downloads")).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val percent = if (total > 0) (current * 100 / total) else 0
        val subText = if (total > 0) "$percent%" else ""

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载: $title")
            .setContentText("进度: $current / $total ($percent%)")
            .setSubText(subText)
            .setProgress(total, current, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addExtras(android.os.Bundle().apply {
                // Android 16 (API 36) 实时动态状态栏胶囊 (Rich Ongoing Notifications)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_ANDROID_LIVE_STATUS, true)
                // Xiaomi HyperOS / MIUI 焦点通知与灵动胶囊协议支持
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_FOCUS, true)
                putBoolean(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_LIVE, true)
                putString(com.perol.pixez.shared.ui.AppConstants.Download.EXTRA_MIUI_SUBTEXT, "$percent%")
            })
            .build()

        manager.notify(id, notification)
    }

    actual fun notifyFinished(id: Int, title: String, successCount: Int, failedCount: Int) {
        val context = BrowserLauncherContext.applicationContext ?: return
        createChannelIfNeeded(context)
        val manager = getNotificationManager(context) ?: return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pixez://downloads")).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val summaryText = if (failedCount == 0) {
            "全部 $successCount 张保存完成"
        } else {
            "完成 $successCount 张，失败 $failedCount 张"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("下载完成: $title")
            .setContentText(summaryText)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(id, notification)
    }

    actual fun cancel(id: Int) {
        val context = BrowserLauncherContext.applicationContext ?: return
        val manager = getNotificationManager(context) ?: return
        manager.cancel(id)
    }
}
