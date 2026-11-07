package com.perol.pixez.shared.platform

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppUpdateDownloaderTest {

    private val asset = UpdateAsset(
        downloadUrl = "https://example.com/app.apk",
        fileName = "app.apk",
        size = 1024L,
        sha256Digest = null,
    )

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
        assertEquals(UpdateDownloadState.Done("/cache/app.apk"), downloader.state.value)
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

    @Test
    fun `没有摘要时跳过校验并保留安装包`() = runTest {
        var digestCalculated = false
        var deleted = false
        val downloader = AppUpdateDownloader(
            scope = this,
            downloadFile = { _, _, _ -> Result.success("/cache/app.apk") },
            sha256OfFile = {
                digestCalculated = true
                null
            },
            deleteFile = {
                deleted = true
                true
            },
        )

        downloader.start(asset.copy(sha256Digest = null))
        advanceUntilIdle()

        assertEquals(UpdateDownloadState.Done("/cache/app.apk"), downloader.state.value)
        assertTrue(!digestCalculated, "无摘要时不应白算整包摘要")
        assertTrue(!deleted)
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
        assertEquals(UpdateDownloadState.Done("/cache/app.apk"), downloader.state.value)
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
}
