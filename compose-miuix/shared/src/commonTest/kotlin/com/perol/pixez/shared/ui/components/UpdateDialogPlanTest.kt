package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.ui.i18n.ZhCnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateDialogPlanTest {

    private val strings = ZhCnStrings

    private val release = ReleaseInfo(
        tagName = "v2.0.0",
        versionName = "2.0.0",
        title = "PixEz MIUIX v2.0.0",
        changelog = "## 更新说明\n- 修复下载\n- 优化弹窗",
        releaseUrl = "https://github.com/perol/pixez/releases/tag/v2.0.0",
        publishedAt = "2026-11-01T12:00:00Z",
        isNew = true,
        downloadUrl = "https://github.com/perol/pixez/releases/download/v2.0.0/app.apk",
        fileName = "app.apk",
        fileSize = 1024L,
        digest = "sha256:${"a".repeat(64)}",
    )

    private val downloading = UpdateDownloadState.Downloading(
        downloaded = 512L,
        total = 1024L,
        speedBytesPerSec = 2048L,
    )

    @Test
    fun `标题下摘要只有一行版本跃迁与日期`() {
        assertEquals("当前 1.9.0 → 最新 2.0.0 · 2026-11-01", updateDialogSummary("1.9.0", release, strings))
    }

    @Test
    fun `有新版本且有安装包时主按钮立即下载`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release,
            downloadState = UpdateDownloadState.Idle,
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals("立即下载", plan.primaryText)
        assertEquals(UpdateDialogPrimaryAction.Download, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
        assertEquals("稍后", plan.dismissText)
        assertTrue(plan.showIgnore)
        assertFalse(plan.showBrowserFallback)
        assertFalse(plan.showProgress)
        assertNull(plan.errorText)
        assertTrue(plan.hasNotes)
    }

    @Test
    fun `下载中主按钮禁用并显示百分比，另给取消与后台下载`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release,
            downloadState = downloading,
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals("下载中 50%", plan.primaryText)
        assertFalse(plan.primaryEnabled)
        assertEquals("后台下载", plan.dismissText)
        assertTrue(plan.showCancelDownload)
        assertFalse(plan.showIgnore)
        assertFalse(plan.showBrowserFallback)
        assertTrue(plan.showProgress)
        assertEquals(0.5f, plan.progressFraction)
        assertTrue(plan.progressDetail.orEmpty().contains("512 B"))
    }

    @Test
    fun `下载中未授权取消时不显示取消入口`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release,
            downloadState = downloading,
            strings = strings,
            canIgnore = true,
            canCancelDownload = false,
        )

        assertFalse(plan.showCancelDownload)
    }

    @Test
    fun `失败时保留重试与浏览器下载`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release,
            downloadState = UpdateDownloadState.Failed("HTTP 500"),
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals("重试", plan.primaryText)
        assertEquals(UpdateDialogPrimaryAction.Retry, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
        assertTrue(plan.showBrowserFallback)
        assertTrue(plan.showIgnore)
        assertEquals(strings.updateDownloadFailed, plan.errorText)
    }

    @Test
    fun `下载完成时主按钮为立即安装`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release,
            downloadState = UpdateDownloadState.Done("/cache/app.apk"),
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals(strings.updateInstallNow, plan.primaryText)
        assertEquals(UpdateDialogPrimaryAction.Install, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
    }

    @Test
    fun `没有安装包时主操作退回 Releases 页`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release.copy(downloadUrl = null, fileName = null),
            downloadState = UpdateDownloadState.Idle,
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals("前往 Releases 页面", plan.primaryText)
        assertEquals(UpdateDialogPrimaryAction.OpenRelease, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
        // 主按钮已是 Releases 页，再从次要位置放一个同目标入口只会重复动作
        assertFalse(plan.showBrowserFallback)
    }

    @Test
    fun `浏览器兜底优先安装包直链，没有资产才回 Releases 页`() {
        assertEquals(release.downloadUrl, updateBrowserFallbackUrl(release))
        assertEquals(release.releaseUrl, updateBrowserFallbackUrl(release.copy(downloadUrl = null)))
    }

    @Test
    fun `说明默认折叠到十二行，展开显示全文`() {
        val markdown = (1..20).joinToString("\n") { "第 $it 行" }

        val collapsed = updateDialogNotesState(markdown, expanded = false)
        assertEquals(12, collapsed.visibleMarkdown.lines().size)
        assertEquals(8, collapsed.hiddenLineCount)

        val expanded = updateDialogNotesState(markdown, expanded = true)
        assertEquals(20, expanded.visibleMarkdown.lines().size)
        assertEquals(0, expanded.hiddenLineCount)
        assertEquals(markdown, expanded.visibleMarkdown)
    }

    @Test
    fun `短说明没有折叠提示`() {
        val state = updateDialogNotesState("只有一行", expanded = false)
        assertEquals(0, state.hiddenLineCount)
        assertEquals(1, state.visibleMarkdown.lines().size)
    }

    @Test
    fun `说明区高度与羽化尺寸符合统一口径`() {
        assertEquals(12, UPDATE_NOTES_PREVIEW_LINES)
        assertEquals(240, UPDATE_NOTES_MAX_HEIGHT_DP)
        assertEquals(40, UPDATE_NOTES_FADE_DP)
    }

    @Test
    fun `安装包资产仅在直链与文件名齐全时可用`() {
        val asset = release.updateAsset()
        assertEquals(release.downloadUrl, asset?.downloadUrl)
        assertEquals("app.apk", asset?.fileName)
        assertEquals(1024L, asset?.size)
        assertEquals(release.digest, asset?.sha256Digest)

        assertNull(release.copy(downloadUrl = null).updateAsset())
        assertNull(release.copy(fileName = null).updateAsset())
    }
}
