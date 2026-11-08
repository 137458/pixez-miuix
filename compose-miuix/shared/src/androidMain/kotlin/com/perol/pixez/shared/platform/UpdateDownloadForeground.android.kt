package com.perol.pixez.shared.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.perol.pixez.shared.R
import io.github.aakira.napier.Napier

/**
 * 更新包下载前台服务：关闭弹窗/退到后台后仍保持下载，并在通知栏展示进度。
 *
 * 只做保活与进度呈现，真正的传输仍由公共状态机驱动；服务不持有下载器。
 */
class UpdateDownloadService : Service() {

    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                ensureChannel()
                startForegroundCompat(buildNotification(0L, 0L))
            }
            ACTION_UPDATE -> {
                val downloaded = intent.getLongExtra(EXTRA_DOWNLOADED, 0L)
                val total = intent.getLongExtra(EXTRA_TOTAL, 0L)
                val notification = buildNotification(downloaded, total)
                if (isForeground) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NOTIFICATION_ID, notification)
                } else {
                    // SERVICE 由 startForegroundService 拉起但 START 分支未走到（进程被回收后重建等）：
                    // 这里补一次 startForeground，满足 5 秒内必须进入前台的契约。
                    startForegroundCompat(notification)
                }
            }
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.download_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(downloaded: Long, total: Long): Notification {
        val percent = if (total > 0L) {
            ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.download_channel_name))
            .setContentText(getString(R.string.download_progress_text, downloaded, total, percent))
            .setProgress(
                if (total > 0L) total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else 0,
                downloaded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                total <= 0L,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForeground = true
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        isForeground = false
    }

    companion object {
        private const val CHANNEL_ID = "pixez_update_download_channel"
        private const val NOTIFICATION_ID = 0x00A1_0001
        private const val ACTION_START = "com.perol.pixez.shared.action.UPDATE_DOWNLOAD_START"
        private const val ACTION_UPDATE = "com.perol.pixez.shared.action.UPDATE_DOWNLOAD_UPDATE"
        private const val ACTION_STOP = "com.perol.pixez.shared.action.UPDATE_DOWNLOAD_STOP"
        private const val EXTRA_DOWNLOADED = "extra_downloaded"
        private const val EXTRA_TOTAL = "extra_total"

        fun start(context: Context) {
            dispatch(
                context,
                Intent(context, UpdateDownloadService::class.java).setAction(ACTION_START),
                foregroundService = true,
            )
        }

        fun update(context: Context, downloaded: Long, total: Long) {
            val intent = Intent(context, UpdateDownloadService::class.java)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_DOWNLOADED, downloaded)
                .putExtra(EXTRA_TOTAL, total)
            dispatch(context, intent, foregroundService = false)
        }

        fun stop(context: Context) {
            dispatch(
                context,
                Intent(context, UpdateDownloadService::class.java).setAction(ACTION_STOP),
                foregroundService = false,
            )
        }

        /**
         * 只有 ACTION_START 需要 startForegroundService；UPDATE/STOP 发给的是已在运行的服务，
         * 用 startForegroundService 下发会要求服务在 5 秒内 startForeground，否则抛
         * RemoteServiceException。
         */
        private fun dispatch(context: Context, intent: Intent, foregroundService: Boolean) {
            try {
                if (foregroundService) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Napier.w("更新下载前台服务调度失败", e, tag = "UpdateDownloadService")
            }
        }
    }
}

internal actual fun updateDownloadForegroundStart() {
    BrowserLauncherContext.applicationContext?.let(UpdateDownloadService::start)
}

internal actual fun updateDownloadForegroundProgress(downloaded: Long, total: Long) {
    BrowserLauncherContext.applicationContext?.let { UpdateDownloadService.update(it, downloaded, total) }
}

internal actual fun updateDownloadForegroundStop() {
    BrowserLauncherContext.applicationContext?.let(UpdateDownloadService::stop)
}
