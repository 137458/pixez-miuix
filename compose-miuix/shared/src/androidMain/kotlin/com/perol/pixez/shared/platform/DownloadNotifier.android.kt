package com.perol.pixez.shared.platform

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
        }
        ensureDownloadNotificationChannel(
            context = context,
            channelId = channelId,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            nameRes = R.string.download_channel_name,
            descriptionRes = R.string.download_channel_description,
        )
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

        val localized = context.localizedForAppLanguage()
        val progressTitle = localized.getString(R.string.download_progress_title, title)
        val percent = if (total > 0) (current * 100 / total) else 0
        val notification = buildDownloadProgressNotification(
            context = context,
            channelId = channelId,
            title = progressTitle,
            current = current,
            total = total,
            contentIntent = pendingIntent,
            includeLiveStatus = true,
            indeterminate = false,
            liveText = localized.getString(R.string.download_live_text, current, total, percent),
        )
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
