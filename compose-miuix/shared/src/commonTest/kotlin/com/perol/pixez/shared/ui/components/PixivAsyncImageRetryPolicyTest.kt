package com.perol.pixez.shared.ui.components

import coil3.decode.DataSource
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 图片错误自愈裁决 [resolveSilentErrorDecision] 与异步加载后内存缓存同步重绑判定 [shouldCommitMemoryCacheRebind] 的验证。
 *
 * 取消类错误必须落到 [SilentErrorDecision.Ignore]：LazyColumn 回收节点与去重让位都会派发
 * `CancellationException`，若据此重建图片节点重试就会形成无限重启循环。
 */
class PixivAsyncImageRetryPolicyTest {

    @Test
    fun `取消类错误既不重试也不上报`() {
        val decision = resolveSilentErrorDecision(
            throwable = CancellationException("request cancelled"),
            onError = null,
            silentRetryCount = 0,
        )

        assertEquals(SilentErrorDecision.Ignore, decision)
    }

    @Test
    fun `调用方未接管错误且未达重试预算时静默重试`() {
        val decision = resolveSilentErrorDecision(
            throwable = RuntimeException("transient"),
            onError = null,
            silentRetryCount = 0,
        )

        assertEquals(SilentErrorDecision.Retry, decision)
    }

    @Test
    fun `重试预算耗尽后不再静默重试`() {
        val throwable = RuntimeException("still broken")
        val decision = resolveSilentErrorDecision(
            throwable = throwable,
            onError = null,
            silentRetryCount = SILENT_ERROR_MAX_RETRIES,
        )

        assertEquals(SilentErrorDecision.Report(throwable), decision)
    }

    @Test
    fun `调用方已接管错误回调时永不静默重试`() {
        val throwable = RuntimeException("caller handled")
        val decision = resolveSilentErrorDecision(
            throwable = throwable,
            onError = {},
            silentRetryCount = 0,
        )

        assertEquals(SilentErrorDecision.Report(throwable), decision)
    }

    @Test
    fun `首次从网络或磁盘异步加载成功时触发一次内存缓存同步重绑`() {
        assertTrue(
            shouldCommitMemoryCacheRebind(dataSource = DataSource.NETWORK, memoryCacheCommitCount = 0),
            "首次 NETWORK 异步完成必须触发一次 MEMORY_CACHE 同步重绑以修复入场窗口期失效丢失",
        )
        assertTrue(
            shouldCommitMemoryCacheRebind(dataSource = DataSource.DISK, memoryCacheCommitCount = 0),
            "首次 DISK 异步完成必须触发一次 MEMORY_CACHE 同步重绑以修复入场窗口期失效丢失",
        )
    }

    @Test
    fun `已命中内存缓存或已完成重绑时不再触发重绑避免无限循环`() {
        assertFalse(
            shouldCommitMemoryCacheRebind(dataSource = DataSource.MEMORY_CACHE, memoryCacheCommitCount = 0),
            "已从 MEMORY_CACHE 同步直出的结果在 onAttach 阶段即完成绑定，无需重绑",
        )
        assertFalse(
            shouldCommitMemoryCacheRebind(dataSource = DataSource.NETWORK, memoryCacheCommitCount = 1),
            "已完成过一次重绑后严禁再次触发，防止无限重建循环",
        )
        assertFalse(
            shouldCommitMemoryCacheRebind(dataSource = DataSource.DISK, memoryCacheCommitCount = 1),
            "已完成过一次重绑后严禁再次触发，防止无限重建循环",
        )
    }
}
