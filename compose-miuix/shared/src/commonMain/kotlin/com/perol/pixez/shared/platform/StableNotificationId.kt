package com.perol.pixez.shared.platform

/**
 * 将 Long 下载任务 ID 稳定折叠为 Android 通知所需的 Int 通知 ID。
 *
 * 折叠规则：低 32 位若为非负则原值保留（日志可与任务 ID 直接对应），
 * 否则折叠高位异或低位（同值稳定、同进程内配对 notify/cancel 一致）。
 * 注意折叠理论上存在碰撞可能，但同进程同时活跃的下载任务数量极小，实际不会相遇。
 */
fun stableNotificationId(taskId: Long): Int {
    val low = taskId.toInt()
    return if (low >= 0) low else (taskId xor (taskId ushr 32)).toInt()
}
