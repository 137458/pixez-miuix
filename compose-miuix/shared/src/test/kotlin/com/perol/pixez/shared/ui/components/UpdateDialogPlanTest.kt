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

    private val asset = release.updateAsset()!!

    private val downloading = UpdateDownloadState.Downloading(
        downloaded = 512L,
        total = 1024L,
        speedBytesPerSec = 2048L,
        target = asset.identity,
    )

    @Test
    fun `标题下摘要只有一行版本跃迁与日期`() {
        assertEquals("当前 1.9.0 → 最新 2.0.0 · 2026-11-01", updateDialogSummary("1.9.0", release, strings))
    }

    @Test
    fun `进度详情按模板拼接已下载总量与速度`() {
        assertEquals("512 B / 1.0 KB · 2.0 KB/s", updateDialogProgressDetail(downloading, strings))
    }

    @Test
    fun `总长未知时进度详情只拼接已下载与速度`() {
        val unknownTotal = downloading.copy(total = 0L)
        assertEquals("512 B · 2.0 KB/s", updateDialogProgressDetail(unknownTotal, strings))
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
        assertEquals(UpdatePrimaryAction.Download, plan.primaryAction)
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
            downloadState = UpdateDownloadState.Failed("HTTP 500", asset.identity),
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals("重试", plan.primaryText)
        assertEquals(UpdatePrimaryAction.Download, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
        assertTrue(plan.showBrowserFallback)
        assertTrue(plan.showIgnore)
        assertEquals(strings.updateDownloadFailed, plan.errorText)
    }

    @Test
    fun `失败但没有安装包时不会出现假重试`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release.copy(downloadUrl = null, fileName = null),
            downloadState = UpdateDownloadState.Failed("HTTP 500", asset.identity),
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals(strings.updateDialogOpenRelease, plan.primaryText)
        assertEquals(UpdatePrimaryAction.OpenBrowser, plan.primaryAction)
        assertFalse(plan.showBrowserFallback)
    }

    @Test
    fun `下载完成时主按钮为立即安装`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.9.0",
            release = release,
            downloadState = UpdateDownloadState.Done("/cache/app.apk", asset.identity),
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )

        assertEquals(strings.updateInstallNow, plan.primaryText)
        assertEquals(UpdatePrimaryAction.Install, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
        assertEquals("/cache/app.apk", plan.installPath)
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
        assertEquals(UpdatePrimaryAction.OpenBrowser, plan.primaryAction)
        assertTrue(plan.primaryEnabled)
        // 主按钮已是 Releases 页，再从次要位置放一个同目标入口只会重复动作
        assertFalse(plan.showBrowserFallback)
    }

    @Test
    fun `不是新版本时文案与动作都必须走浏览器`() {
        val cta = updatePrimaryCta(
            hasNew = false,
            hasAsset = true,
            downloadState = UpdateDownloadState.Idle,
            strings = strings,
        )

        assertEquals(strings.updateOpenInBrowser, cta.text)
        assertEquals(UpdatePrimaryAction.OpenBrowser, cta.action)
        assertTrue(cta.enabled)
        assertFalse(cta.action == UpdatePrimaryAction.Download)
    }

    @Test
    fun `不是新版本即使已有失败包也不触发下载`() {
        val cta = updatePrimaryCta(
            hasNew = false,
            hasAsset = true,
            downloadState = UpdateDownloadState.Failed("HTTP 500", asset.identity),
            strings = strings,
        )

        assertEquals(UpdatePrimaryAction.OpenBrowser, cta.action)
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
    fun `截断落在代码块内时延伸到闭合围栏`() {
        // 第 10 行开启代码块，第 14 行闭合；预览上限 12 行时不能把围栏切成半截。
        val lines = buildList {
            repeat(9) { add("正文 $it") }
            add("```")
            add("code 1")
            add("code 2")
            add("code 3")
            add("```")
            repeat(5) { add("后续 $it") }
        }
        val markdown = lines.joinToString("\n")

        val state = updateDialogNotesState(markdown, expanded = false, previewLines = 12)
        val visible = state.visibleMarkdown.lines()
        assertEquals(14, visible.size)
        assertEquals("```", visible[9])
        assertEquals("```", visible[13])
        assertTrue(state.hiddenLineCount > 0)
    }

    @Test
    fun `截断落在半截链接时回退到安全行`() {
        val lines = buildList {
            repeat(10) { add("正文 $it") }
            add("详见 [更新")
            add("说明](https://example.com/release")
            add(")")
            add("尾行")
        }
        val markdown = lines.joinToString("\n")

        val state = updateDialogNotesState(markdown, expanded = false, previewLines = 12)
        assertEquals(10, state.visibleMarkdown.lines().size)
        assertFalse(state.visibleMarkdown.contains("详见 [更新"))
    }

    @Test
    fun `更新页长正文用独立的预览行数上限`() {
        val markdown = (1..30).joinToString("\n") { "第 $it 行" }
        val collapsed = updateDialogNotesState(markdown, expanded = false, previewLines = UPDATE_PAGE_PREVIEW_LINES)
        assertEquals(16, collapsed.visibleMarkdown.lines().size)
        val expanded = updateDialogNotesState(markdown, expanded = true, previewLines = UPDATE_PAGE_PREVIEW_LINES)
        assertEquals(30, expanded.visibleMarkdown.lines().size)
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
        assertEquals(release.tagName, asset?.releaseTag)

        assertNull(release.copy(downloadUrl = null).updateAsset())
        assertNull(release.copy(fileName = null).updateAsset())
    }
}
