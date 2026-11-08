package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.UpdateAsset
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.platform.forAsset
import com.perol.pixez.shared.platform.installPathFor
import com.perol.pixez.shared.platform.updateProgressFraction
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.format
import com.perol.pixez.shared.ui.i18n.formatFileSize

/** 弹窗折叠态只展示前 12 行，展开后整段说明放进弹窗自己的滚动区。 */
internal const val UPDATE_NOTES_PREVIEW_LINES = 12

/** 更新页长正文的预览行数上限；超出后折叠并提供「展开」。 */
internal const val UPDATE_PAGE_PREVIEW_LINES = 16

/** 说明区可视高度上限：超长说明在弹窗内部滚动，主按钮始终留在可视区。 */
internal const val UPDATE_NOTES_MAX_HEIGHT_DP = 240

/** 说明区上下羽化带高度。 */
internal const val UPDATE_NOTES_FADE_DP = 40

/** 主按钮的唯一动作类型：文案与动作必须由同一次推导产出。 */
internal enum class UpdatePrimaryAction { Download, Install, OpenBrowser }

/** 主按钮的完整描述：文案、动作、可用性三位一体，UI 不再自行分支。 */
internal data class UpdatePrimaryCta(
    val text: String,
    val action: UpdatePrimaryAction,
    val enabled: Boolean,
)

/**
 * 弹窗与更新页共用的主按钮推导。
 *
 * 口径：`hasNew=false` 一律走浏览器打开；失败态有资产才是真重试（真的重新下载），
 * 没有资产则只能打开 Releases 页，绝不出现「文案说重试、点击却不下载」的假重试。
 */
internal fun updatePrimaryCta(
    hasNew: Boolean,
    hasAsset: Boolean,
    downloadState: UpdateDownloadState,
    strings: AppStrings,
): UpdatePrimaryCta {
    if (!hasNew) {
        return UpdatePrimaryCta(strings.updateOpenInBrowser, UpdatePrimaryAction.OpenBrowser, true)
    }
    return when (downloadState) {
        is UpdateDownloadState.Downloading -> UpdatePrimaryCta(
            text = strings.updateDialogDownloadingPercent.format((downloadState.progress * 100).toInt()),
            action = UpdatePrimaryAction.Download,
            enabled = false,
        )
        is UpdateDownloadState.Done -> if (hasAsset) {
            UpdatePrimaryCta(strings.updateInstallNow, UpdatePrimaryAction.Install, true)
        } else {
            UpdatePrimaryCta(strings.updateDialogOpenRelease, UpdatePrimaryAction.OpenBrowser, true)
        }
        is UpdateDownloadState.Failed -> if (hasAsset) {
            // 有资产时「重试」就是重新下载，不是打开浏览器。
            UpdatePrimaryCta(strings.retry, UpdatePrimaryAction.Download, true)
        } else {
            UpdatePrimaryCta(strings.updateDialogOpenRelease, UpdatePrimaryAction.OpenBrowser, true)
        }
        UpdateDownloadState.Idle -> if (hasAsset) {
            UpdatePrimaryCta(strings.updateDialogDownloadNow, UpdatePrimaryAction.Download, true)
        } else {
            UpdatePrimaryCta(strings.updateDialogOpenRelease, UpdatePrimaryAction.OpenBrowser, true)
        }
    }
}

/**
 * 弹窗的一次快照：Miuix 组件只按这份数据铺像素，不自己判断版本或下载态。
 */
internal data class UpdateDialogPlan(
    val summary: String,
    val primaryText: String,
    val primaryAction: UpdatePrimaryAction,
    val primaryEnabled: Boolean,
    val dismissText: String,
    val showCancelDownload: Boolean,
    val showBrowserFallback: Boolean,
    val showIgnore: Boolean,
    val hasNotes: Boolean,
    val showProgress: Boolean,
    val progressFraction: Float?,
    val progressDetail: String?,
    val errorText: String?,
    val installPath: String?,
)

internal data class UpdateDialogNotesState(val visibleMarkdown: String, val hiddenLineCount: Int)

/** 顶部唯一的一行版本信息：当前 → 最新，附发布日期。 */
internal fun updateDialogSummary(
    currentVersion: String,
    release: ReleaseInfo,
    strings: AppStrings,
): String {
    val jump = "${strings.updateDialogCurrentLabel} $currentVersion → " +
        "${strings.updateDialogLatestLabel} ${release.versionName}"
    val published = release.publishedAt?.take(10).orEmpty()
    return if (published.isBlank()) jump else "$jump · $published"
}

/**
 * 折叠态取前 N 行；空白说明与短说明都不显示“展开”。
 *
 * 截断必须落在安全行边界：不切断 ``` 代码块，也不留下半截 `[链接]`，
 * 否则折叠态会渲染出残缺标记或悬空链接注解。
 */
internal fun updateDialogNotesState(
    markdown: String,
    expanded: Boolean,
    previewLines: Int = UPDATE_NOTES_PREVIEW_LINES,
): UpdateDialogNotesState {
    if (markdown.isBlank() || expanded) return UpdateDialogNotesState(markdown, 0)
    val lines = markdown.lines()
    val visible = safeMarkdownPreviewLines(lines, previewLines.coerceAtLeast(1))
    return UpdateDialogNotesState(
        visibleMarkdown = visible.joinToString("\n"),
        hiddenLineCount = (lines.size - visible.size).coerceAtLeast(0),
    )
}

/**
 * 取前 [limit] 行的安全版本。
 *
 * 1) 截断点落在未闭合的 ``` 围栏内：延伸到闭合围栏；没有闭合围栏则回退到开围栏之前。
 * 2) 截断后仍存在未闭合的 `[`（半截链接）：逐行回退到上一个安全行。
 */
internal fun safeMarkdownPreviewLines(lines: List<String>, limit: Int): List<String> {
    if (lines.isEmpty()) return emptyList()
    var end = limit.coerceIn(1, lines.size)

    var insideFence = false
    var fenceStart = 0
    for (i in 0 until end) {
        if (lines[i].trimStart().startsWith("```")) {
            if (insideFence) {
                insideFence = false
            } else {
                insideFence = true
                fenceStart = i
            }
        }
    }
    if (insideFence) {
        val closing = (end until lines.size).firstOrNull { lines[it].trimStart().startsWith("```") }
        end = if (closing != null) closing + 1 else fenceStart.coerceAtLeast(1)
    }

    while (end > 1 && !hasBalancedLinkBrackets(lines.take(end))) {
        end--
    }

    while (end > 0 && lines[end - 1].isBlank()) {
        end--
    }
    return lines.take(end)
}

/** 粗判链接是否完整：未闭合的 `[` 或 `](` 后缺少 `)` 都说明截断切在了链接中间。 */
private fun hasBalancedLinkBrackets(lines: List<String>): Boolean {
    var openBracket = 0
    for (line in lines) {
        for (ch in line) {
            when (ch) {
                '[' -> openBracket++
                ']' -> if (openBracket > 0) openBracket--
            }
        }
    }
    if (openBracket != 0) return false
    val text = lines.joinToString("\n")
    val lastTarget = text.lastIndexOf("](")
    return lastTarget < 0 || text.indexOf(")", lastTarget + 2) >= 0
}

/** 下载中退场按钮写「后台下载」而不是「稍后」：关掉弹窗不会中断任务。 */
internal fun updateDialogDismissText(
    downloadState: UpdateDownloadState,
    strings: AppStrings,
): String = if (downloadState is UpdateDownloadState.Downloading) {
    strings.updateDialogBackgroundDownload
} else {
    strings.updateDialogLater
}

/** 下载失败时的浏览器兜底：优先安装包直链，没有资产才退回 Releases 页。 */
internal fun updateBrowserFallbackUrl(release: ReleaseInfo): String =
    release.downloadUrl?.takeIf { it.isNotBlank() } ?: release.releaseUrl

internal fun updateDialogProgressDetail(
    state: UpdateDownloadState.Downloading,
): String {
    val downloaded = formatFileSize(state.downloaded)
    val speed = "${formatFileSize(state.speedBytesPerSec)}/s"
    return if (state.total > 0L) {
        "$downloaded / ${formatFileSize(state.total)} · $speed"
    } else {
        "$downloaded · $speed"
    }
}

internal fun buildUpdateDialogPlan(
    currentVersion: String,
    release: ReleaseInfo,
    downloadState: UpdateDownloadState,
    strings: AppStrings,
    canIgnore: Boolean,
    canCancelDownload: Boolean,
): UpdateDialogPlan {
    val asset = release.updateAsset()
    // 渲染前先与当前 release 对齐：旧版本的进度/完成态一律按 Idle 呈现。
    val state = downloadState.forAsset(asset)
    val hasAsset = asset != null
    val cta = updatePrimaryCta(release.isNew, hasAsset, state, strings)
    val downloading = state as? UpdateDownloadState.Downloading
    return UpdateDialogPlan(
        summary = updateDialogSummary(currentVersion, release, strings),
        primaryText = cta.text,
        primaryAction = cta.action,
        primaryEnabled = cta.enabled,
        dismissText = updateDialogDismissText(state, strings),
        showCancelDownload = downloading != null && canCancelDownload,
        showBrowserFallback = state is UpdateDownloadState.Failed && hasAsset,
        showIgnore = canIgnore && downloading == null && state !is UpdateDownloadState.Done,
        hasNotes = release.changelog.isNotBlank(),
        showProgress = downloading != null,
        progressFraction = downloading?.let { if (it.total > 0L) updateProgressFraction(it.downloaded, it.total) else null },
        progressDetail = downloading?.let(::updateDialogProgressDetail),
        errorText = if (state is UpdateDownloadState.Failed) strings.updateDownloadFailed else null,
        installPath = state.installPathFor(asset),
    )
}

/** Release 信息转下载资产；直链或文件名缺失时返回 null（主操作回退 Releases 页）。 */
internal fun ReleaseInfo.updateAsset(): UpdateAsset? {
    val url = downloadUrl?.takeIf { it.isNotBlank() } ?: return null
    val name = fileName?.takeIf { it.isNotBlank() } ?: return null
    return UpdateAsset(
        downloadUrl = url,
        fileName = name,
        size = fileSize ?: 0L,
        sha256Digest = digest,
        releaseTag = tagName,
    )
}
