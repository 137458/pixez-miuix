package com.perol.pixez.shared.platform

import android.content.Context
import android.os.PowerManager
import io.github.aakira.napier.Napier
import java.util.concurrent.ConcurrentHashMap

actual object PlatformDownloadKeeper {
    /** 任务 ID 到嵌套持有次数的映射：同一作品并发下载（批量 + 单页）时会多次 acquire。 */
    private val activeTasks = ConcurrentHashMap<Long, Int>()
    private var wakeLock: PowerManager.WakeLock? = null
    private val lock = Any()

    actual fun acquire(taskId: Long) {
        val context = BrowserLauncherContext.applicationContext ?: return
        synchronized(lock) {
            activeTasks.merge(taskId, 1, Int::plus)
            try {
                if (wakeLock == null) {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = powerManager?.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        DownloadPlatformDefaults.WAKELOCK_TAG,
                    )?.apply {
                        setReferenceCounted(false)
                    }
                }
                // 每次有新任务注册或执行时动态刷新唤醒锁守护期，防止超长批量下载中途被系统休眠切断
                wakeLock?.acquire(DownloadPlatformDefaults.WAKELOCK_TIMEOUT_MS)
                Napier.d("下载保活守护器已激活/续期 (活跃任务数: ${activeTasks.size})", tag = "DownloadKeeper")
            } catch (e: Throwable) {
                Napier.w("申请或续期系统下载 WakeLock 失败", e, tag = "DownloadKeeper")
            }
        }
    }

    actual fun release(taskId: Long) {
        synchronized(lock) {
            val remaining = (activeTasks[taskId] ?: 0) - 1
            if (remaining > 0) {
                // 仍有外层/同作品任务持有保活，不能提前释放唤醒锁
                activeTasks[taskId] = remaining
                return
            }
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
