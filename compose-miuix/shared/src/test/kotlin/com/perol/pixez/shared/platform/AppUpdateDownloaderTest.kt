package com.perol.pixez.shared.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// advanceUntilIdle 等测试调度器 API 仍标注 ExperimentalCoroutinesApi。
@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateDownloaderTest {

    /** 与 [asset] 摘要匹配的整包 SHA-256（小写 hex）。 */
    private val digestHex = "a".repeat(64)

    private val asset = UpdateAsset(
        downloadUrl = "https://example.com/app.apk",
        fileName = "app.apk",
        size = 1024L,
        sha256Digest = "sha256:$digestHex",
        releaseTag = "v2.0.0",
    )

    private val nextAsset = asset.copy(
        downloadUrl = "https://example.com/app-next.apk",
        fileName = "app-next.apk",
        releaseTag = "v2.1.0",
    )

    /** 远端未提供可用摘要的资产：无法校验完整性，一律拒绝自动安装（fail-closed）。 */
    private val untrustedAsset = asset.copy(sha256Digest = null)

    @Test
    fun `启动即进入下载态并在完成后进入 Done`() = runTest {
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, onProgress ->
                onProgress(512L, 1024L)
                Result.success("/cache/app.apk")
            },
            sha256OfFile = { "a".repeat(64) },
            deleteFile = { true },
        )

        downloader.start(asset)
        assertIs<UpdateDownloadState.Downloading>(downloader.state.value)

        advanceUntilIdle()
        assertEquals(UpdateDownloadState.Done("/cache/app.apk", asset.identity), downloader.state.value)
    }

    @Test
    fun `有摘要且不一致时删除安装包并进入 Failed`() = runTest {
        var deletedPath: String? = null
        val expected = "a".repeat(64)
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.success("/cache/app.apk") },
            sha256OfFile = { "b".repeat(64) },
            deleteFile = { path ->
                deletedPath = path
                true
            },
        )

        downloader.start(asset.copy(sha256Digest = "sha256:$expected"))
        advanceUntilIdle()

        assertEquals("/cache/app.apk", deletedPath)
        val failed = assertIs<UpdateDownloadState.Failed>(downloader.state.value)
        assertTrue(failed.message.contains("SHA-256"), "失败原因应说明摘要不匹配")
    }

    // 规格变更（R-05，决策 D1 = fail-closed）：原断言的"无摘要 → Done 并保留安装包"已被推翻，
    // 缺摘要时无法确认包完整性，必须删除已下载文件并拒绝自动安装。
    @Test
    fun `缺少摘要时删除安装包并拒绝自动安装`() = runTest {
        var digestCalculated = false
        val deletedPaths = mutableListOf<String>()
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.success("/cache/app.apk") },
            sha256OfFile = {
                digestCalculated = true
                null
            },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
        )

        downloader.start(untrustedAsset)
        advanceUntilIdle()

        val failed = assertIs<UpdateDownloadState.Failed>(downloader.state.value)
        assertTrue(failed.message.contains("摘要"), "失败原因应说明缺少摘要，实际: ${failed.message}")
        assertEquals(untrustedAsset.identity, failed.target)
        assertEquals(listOf("/cache/app.apk"), deletedPaths, "无法校验的安装包必须删除")
        assertNull(downloader.installPathFor(untrustedAsset))
        assertTrue(!digestCalculated, "无期望摘要时不应白算整包摘要")
    }

    @Test
    fun `传输失败进入 Failed 且可重试成功`() = runTest {
        var attempts = 0
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ ->
                attempts++
                if (attempts == 1) {
                    Result.failure(IllegalStateException("HTTP 500"))
                } else {
                    Result.success("/cache/app.apk")
                }
            },
            sha256OfFile = { "a".repeat(64) },
            deleteFile = { true },
        )

        downloader.start(asset)
        advanceUntilIdle()
        assertIs<UpdateDownloadState.Failed>(downloader.state.value)

        downloader.start(asset)
        advanceUntilIdle()
        assertEquals(UpdateDownloadState.Done("/cache/app.apk", asset.identity), downloader.state.value)
        assertEquals(2, attempts)
    }

    @Test
    fun `下载中重复启动不会新建任务`() = runTest {
        var started = 0
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ ->
                started++
                awaitCancellation()
            },
            sha256OfFile = { null },
            deleteFile = { true },
        )

        downloader.start(asset)
        runCurrent()
        downloader.start(asset)
        assertEquals(1, started)

        downloader.cancel()
        assertEquals(UpdateDownloadState.Idle, downloader.state.value)
    }

    @Test
    fun `取消后回到 Idle 且不再回写完成态`() = runTest {
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> awaitCancellation() },
            sha256OfFile = { null },
            deleteFile = { true },
        )

        downloader.start(asset)
        assertIs<UpdateDownloadState.Downloading>(downloader.state.value)
        downloader.cancel()
        advanceUntilIdle()

        assertEquals(UpdateDownloadState.Idle, downloader.state.value)
    }

    @Test
    fun `切换到新版本时清除旧包，安装目标只认当前版本`() = runTest {
        val deletedPaths = mutableListOf<String>()
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { url, _, _ ->
                Result.success(if (url == nextAsset.downloadUrl) "/cache/app-next.apk" else "/cache/app.apk")
            },
            sha256OfFile = { digestHex },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
            scanUpdateDir = { emptyList() },
        )

        downloader.start(asset)
        advanceUntilIdle()
        assertEquals("/cache/app.apk", downloader.installPathFor(asset))
        assertNull(downloader.installPathFor(nextAsset))

        // 释放更新信息变化：旧完成包必须被删除，状态回到 Idle。
        downloader.syncTarget(nextAsset)
        // 旧包删除在注入的 scope 上异步执行（不阻塞调用线程），等其跑完再断言。
        advanceUntilIdle()
        assertEquals(UpdateDownloadState.Idle, downloader.state.value)
        assertEquals(listOf("/cache/app.apk"), deletedPaths)
        assertNull(downloader.installPathFor(asset))
        downloader.start(nextAsset)
        advanceUntilIdle()
        assertEquals(UpdateDownloadState.Done("/cache/app-next.apk", nextAsset.identity), downloader.state.value)
        assertNull(downloader.installPathFor(asset))
        assertEquals("/cache/app-next.apk", downloader.installPathFor(nextAsset))
    }

    @Test
    fun `Done 后直接 start 新版本也不会沿用旧包`() = runTest {
        val deletedPaths = mutableListOf<String>()
        var requestedUrl: String? = null
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { url, _, _ ->
                requestedUrl = url
                Result.success(if (url == nextAsset.downloadUrl) "/cache/app-next.apk" else "/cache/app.apk")
            },
            sha256OfFile = { digestHex },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
        )

        downloader.start(asset)
        advanceUntilIdle()
        assertEquals(UpdateDownloadState.Done("/cache/app.apk", asset.identity), downloader.state.value)

        downloader.start(nextAsset)
        advanceUntilIdle()
        assertEquals(nextAsset.downloadUrl, requestedUrl)
        assertEquals(UpdateDownloadState.Done("/cache/app-next.apk", nextAsset.identity), downloader.state.value)
        assertEquals(listOf("/cache/app.apk"), deletedPaths)
    }

    @Test
    fun `安装失败后丢弃完成包并允许重新下载`() = runTest {
        var downloaded = 0
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ ->
                downloaded++
                Result.success("/cache/app.apk")
            },
            sha256OfFile = { digestHex },
            deleteFile = { true },
        )

        downloader.start(asset)
        advanceUntilIdle()
        assertEquals("/cache/app.apk", downloader.installPathFor(asset))

        downloader.discardCompletedAfterInstallFailure(asset)
        assertEquals(UpdateDownloadState.Idle, downloader.state.value)
        assertNull(downloader.installPathFor(asset))

        downloader.start(asset)
        advanceUntilIdle()
        assertEquals(2, downloaded)
        assertEquals("/cache/app.apk", downloader.installPathFor(asset))
    }

    @Test
    fun `进程重启后从磁盘恢复完成包并清理半成品`() = runTest {
        val deletedPaths = mutableListOf<String>()
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.failure(IllegalStateException("不应重新下载")) },
            sha256OfFile = { "a".repeat(64) },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
            scanUpdateDir = {
                listOf(
                    UpdateFileEntry("/cache/.app.apk.1.part", ".app.apk.1.part", 100L),
                    UpdateFileEntry("/cache/app.apk", "app.apk", 1024L),
                )
            },
        )

        downloader.syncTarget(asset)
        advanceUntilIdle()

        assertEquals(UpdateDownloadState.Done("/cache/app.apk", asset.identity), downloader.state.value)
        assertEquals(listOf("/cache/.app.apk.1.part"), deletedPaths)
        assertEquals("/cache/app.apk", downloader.installPathFor(asset))
    }

    @Test
    fun `恢复完成包时不得误删非当前更新包命名的其它隐藏 part 文件`() = runTest {
        val deletedPaths = mutableListOf<String>()
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.failure(IllegalStateException("不应重新下载")) },
            sha256OfFile = { "a".repeat(64) },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
            scanUpdateDir = {
                listOf(
                    UpdateFileEntry("/cache/.app.apk.1.part", ".app.apk.1.part", 100L),
                    UpdateFileEntry("/cache/.user-archive.zip.part", ".user-archive.zip.part", 200L),
                    UpdateFileEntry("/cache/app.apk", "app.apk", 1024L),
                )
            },
        )

        downloader.syncTarget(asset)
        advanceUntilIdle()

        assertEquals(listOf("/cache/.app.apk.1.part"), deletedPaths)
    }

    @Test
    fun `磁盘文件大小不符时不恢复完成态`() = runTest {
        val deletedPaths = mutableListOf<String>()
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.failure(IllegalStateException("不应重新下载")) },
            sha256OfFile = { null },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
            scanUpdateDir = {
                listOf(UpdateFileEntry("/cache/app.apk", "app.apk", 512L))
            },
        )

        downloader.syncTarget(asset)
        advanceUntilIdle()

        assertEquals(UpdateDownloadState.Idle, downloader.state.value)
        assertTrue(deletedPaths.isEmpty(), "大小不符的完成包不应被采用或删除")
    }

    @Test
    fun `磁盘文件摘要不符时删除且不恢复`() = runTest {
        val deletedPaths = mutableListOf<String>()
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.failure(IllegalStateException("不应重新下载")) },
            sha256OfFile = { "b".repeat(64) },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
            scanUpdateDir = {
                listOf(UpdateFileEntry("/cache/app.apk", "app.apk", 1024L))
            },
        )

        downloader.syncTarget(asset.copy(sha256Digest = "sha256:" + "a".repeat(64)))
        advanceUntilIdle()

        assertEquals(UpdateDownloadState.Idle, downloader.state.value)
        assertEquals(listOf("/cache/app.apk"), deletedPaths)
    }

    // 规格变更（R-05，决策 D1 = fail-closed）：恢复路径同样不得把无法校验的旧完成包接回 Done。
    @Test
    fun `恢复时缺少摘要不接回完成态并删除候选文件`() = runTest {
        val deletedPaths = mutableListOf<String>()
        var digestCalculated = false
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.failure(IllegalStateException("不应重新下载")) },
            sha256OfFile = {
                digestCalculated = true
                digestHex
            },
            deleteFile = { path ->
                deletedPaths += path
                true
            },
            scanUpdateDir = {
                listOf(UpdateFileEntry("/cache/app.apk", "app.apk", 1024L))
            },
        )

        downloader.syncTarget(untrustedAsset)
        advanceUntilIdle()

        assertEquals(UpdateDownloadState.Idle, downloader.state.value, "缺少摘要的完成包不得接回 Done")
        assertNull(downloader.installPathFor(untrustedAsset))
        assertEquals(listOf("/cache/app.apk"), deletedPaths, "无法校验的完成包应被删除")
        assertTrue(!digestCalculated, "无期望摘要时不应白算整包摘要")
    }
}
