package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.UpdateAsset
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.platform.updateProgressFraction
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.format
import com.perol.pixez.shared.ui.i18n.formatFileSize

/** 折叠态只展示前 12 行，展开后整段说明放进弹窗自己的滚动区。 */
internal const val UPDATE_NOTES_PREVIEW_LINES = 12

/** 说明区可视高度上限：超长说明在弹窗内部滚动，主按钮始终留在可视区。 */
internal const val UPDATE_NOTES_MAX_HEIGHT_DP = 240

/** 说明区上下羽化带高度。 */
internal const val UPDATE_NOTES_FADE_DP = 40

/** 弹窗主按钮的落点：由版本资产与下载状态推导，UI 不再自行分支。 */
internal enum class UpdateDialogPrimaryAction { Download, Retry, Install, OpenRelease }

/**
 * 弹窗的一次快照：Miuix 组件只按这份数据铺像素，不自己判断版本或下载态。
 */
internal data class UpdateDialogPlan(
    val summary: String,
    val primaryText: String,
    val primaryAction: UpdateDialogPrimaryAction,
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

/** 折叠态取前 N 行；空白说明与短说明都不显示“展开”。 */
internal fun updateDialogNotesState(
    markdown: String,
    expanded: Boolean,
    previewLines: Int = UPDATE_NOTES_PREVIEW_LINES,
): UpdateDialogNotesState {
    if (markdown.isBlank() || expanded) return UpdateDialogNotesState(markdown, 0)
    val lines = markdown.lines()
    val limit = previewLines.coerceAtLeast(1)
    return UpdateDialogNotesState(
        visibleMarkdown = lines.take(limit).joinToString("\n"),
        hiddenLineCount = (lines.size - limit).coerceAtLeast(0),
    )
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

/** 主按钮文案：下载中把百分比放进按钮本身；无资产时主操作只能是打开 Releases 页。 */
internal fun updateDialogPrimaryText(
    hasAsset: Boolean,
    downloadState: UpdateDownloadState,
    strings: AppStrings,
): String = when {
    !hasAsset -> strings.updateDialogOpenRelease
    downloadState is UpdateDownloadState.Downloading -> {
        val percent = (downloadState.progress * 100).toInt()
        strings.updateDialogDownloadingPercent.format(percent)
    }
    downloadState is UpdateDownloadState.Failed -> strings.retry
    downloadState is UpdateDownloadState.Done -> strings.updateInstallNow
    else -> strings.updateDialogDownloadNow
}

internal fun updateDialogPrimaryAction(
    hasAsset: Boolean,
    downloadState: UpdateDownloadState,
): UpdateDialogPrimaryAction = when {
    !hasAsset -> UpdateDialogPrimaryAction.OpenRelease
    downloadState is UpdateDownloadState.Failed -> UpdateDialogPrimaryAction.Retry
    downloadState is UpdateDownloadState.Done -> UpdateDialogPrimaryAction.Install
    else -> UpdateDialogPrimaryAction.Download
}

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
    val hasAsset = release.updateAsset() != null
    val downloading = downloadState as? UpdateDownloadState.Downloading
    return UpdateDialogPlan(
        summary = updateDialogSummary(currentVersion, release, strings),
        primaryText = updateDialogPrimaryText(hasAsset, downloadState, strings),
        primaryAction = updateDialogPrimaryAction(hasAsset, downloadState),
        primaryEnabled = downloading == null,
        dismissText = updateDialogDismissText(downloadState, strings),
        showCancelDownload = downloading != null && canCancelDownload,
        showBrowserFallback = downloadState is UpdateDownloadState.Failed,
        showIgnore = canIgnore && downloading == null && downloadState !is UpdateDownloadState.Done,
        hasNotes = release.changelog.isNotBlank(),
        showProgress = downloading != null,
        progressFraction = downloading?.let { if (it.total > 0L) updateProgressFraction(it.downloaded, it.total) else null },
        progressDetail = downloading?.let(::updateDialogProgressDetail),
        errorText = if (downloadState is UpdateDownloadState.Failed) strings.updateDownloadFailed else null,
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
    )
}
