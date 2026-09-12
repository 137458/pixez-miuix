package com.perol.pixez.shared.platform

import android.content.Context
import android.os.PowerManager
import io.github.aakira.napier.Napier
import java.util.concurrent.ConcurrentHashMap

actual object PlatformDownloadKeeper {
    private val activeTasks = ConcurrentHashMap.newKeySet<Int>()
    private var wakeLock: PowerManager.WakeLock? = null
    private val lock = Any()

    actual fun acquire(taskId: Int, title: String) {
        val context = BrowserLauncherContext.applicationContext ?: return
        synchronized(lock) {
            activeTasks.add(taskId)
            if (wakeLock == null) {
                try {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = powerManager?.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "PixEz:DownloadWakeLock",
                    )?.apply {
                        setReferenceCounted(false)
                        // 单次最长保持 15 分钟唤醒守护，避免极端情况下永久耗电
                        acquire(15 * 60 * 1000L)
                    }
                    Napier.d("下载保活守护器已激活 (任务数: ${activeTasks.size})", tag = "DownloadKeeper")
                } catch (e: Throwable) {
                    Napier.w("申请系统下载 WakeLock 失败", e)
                }
            }
        }
    }

    actual fun release(taskId: Int) {
        synchronized(lock) {
            activeTasks.remove(taskId)
            if (activeTasks.isEmpty()) {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                        Napier.d("下载任务全部完成，保活守护器已释放", tag = "DownloadKeeper")
                    }
                } catch (e: Throwable) {
                    Napier.w("释放系统下载 WakeLock 失败", e)
                } finally {
                    wakeLock = null
                }
            }
        }
    }
}
