package com.perol.pixez.shared.platform

/**
 * 平台级长时/大批量下载任务保活守护器。
 * 在 Android 等移动平台申请系统唤醒锁与前台活跃保障，
 * 防止锁屏或退到后台时被系统休眠策略冻结 CPU 与网络套接字。
 */
expect object PlatformDownloadKeeper {
    fun acquire(taskId: Int)
    fun release(taskId: Int)
}
