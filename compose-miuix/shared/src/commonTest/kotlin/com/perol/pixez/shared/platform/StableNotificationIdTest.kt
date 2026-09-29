package com.perol.pixez.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * 下载通知通知 ID 派生函数测试（D-12 前置）：
 * Android NotificationManager.notify(int, ...) 要求 Int 通知 ID，
 * 而下载任务 ID 将放宽为 Long（pixiv ID 全链路 Long 化）。
 * [stableNotificationId] 必须把任意 Long 稳定折叠为 Int，且同进程内配对一致。
 */
class StableNotificationIdTest {

    @Test
    fun `同一输入恒等派生`() {
        for (id in listOf(0L, 1L, 42L, 123456789L)) {
            assertEquals(stableNotificationId(id), stableNotificationId(id), "同输入必须产出同值: $id")
        }
    }

    @Test
    fun `小值 ID 派生值保持原值便于排查`() {
        // 低位 Long 直接落 Int 区间时保持原值，日志中可与任务 ID 对应
        assertEquals(42, stableNotificationId(42L))
        assertEquals(0, stableNotificationId(0L))
    }

    @Test
    fun `跨 2^31 的两个不同 ID 派生值不同`() {
        val low = stableNotificationId(2_000_000_000L)
        val high = stableNotificationId(6_000_000_000L)
        assertNotEquals(low, high, "两个不同任务 ID 不应折叠为同一通知 ID")
    }

    @Test
    fun `负数与 Long 边界值不抛异常且稳定`() {
        for (id in listOf(-1L, Long.MIN_VALUE, Long.MAX_VALUE)) {
            val derived = stableNotificationId(id)
            assertEquals(derived, stableNotificationId(id))
        }
    }
}
