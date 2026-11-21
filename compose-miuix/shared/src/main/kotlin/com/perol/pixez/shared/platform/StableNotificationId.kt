package com.perol.pixez.shared.platform

/**
 * 将 Long 下载任务 ID 稳定折叠为 Android 通知所需的 Int 通知 ID。
 *
 * 折叠规则：
 * 1. 任务 ID 在非负 Int 范围内（0..Int.MAX_VALUE）时原值保留，日志与通知 ID 一对一对应；
 * 2. 超过 Int.MAX_VALUE 的大数或负数时，对高 32 位与低 32 位进行混淆异或折叠，并通过掩码强制转为非负 Int，
 *    避免跨 2^32 周期由于低位符号为正直接截断导致的高位忽略与 100% 碰撞缺陷。
 */
fun stableNotificationId(taskId: Long): Int {
    if (taskId in 0L..Int.MAX_VALUE.toLong()) {
        return taskId.toInt()
    }
    val folded = ((taskId xor (taskId ushr 32)).toInt()) and 0x7FFFFFFF
    return if (folded == 0) 1 else folded
}

