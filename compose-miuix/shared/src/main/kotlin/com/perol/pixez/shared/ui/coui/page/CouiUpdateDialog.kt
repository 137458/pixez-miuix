package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.CouiContourStyle
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.shape.CouiShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.platform.forAsset
import com.perol.pixez.shared.ui.LocalUpdateDownloader
import com.perol.pixez.shared.ui.components.COUI_UPDATE_NOTES_PREVIEW_LINES
import com.perol.pixez.shared.ui.components.FoldableNotes
import com.perol.pixez.shared.ui.components.MarkdownText
import com.perol.pixez.shared.ui.components.NotesDialogViewport
import com.perol.pixez.shared.ui.components.buildUpdateDialogPlan
import com.perol.pixez.shared.ui.components.runUpdatePrimaryAction
import com.perol.pixez.shared.ui.components.updateAsset
import com.perol.pixez.shared.ui.components.updateBrowserFallbackUrl
import com.perol.pixez.shared.ui.components.updateDialogNotesState
import com.perol.pixez.shared.ui.components.updateNotesFold
import com.perol.pixez.shared.ui.coui.component.CouiDialogSecondaryButton
import com.perol.pixez.shared.ui.coui.component.CouiLinearProgressBar
import com.perol.pixez.shared.ui.coui.component.couiContourSurface
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiDialogPressable
import com.perol.pixez.shared.ui.coui.component.couiDialogSurfaceColor
import com.perol.pixez.shared.ui.coui.component.couiMarkdownColors
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl

/**
 * COUI 版本更新弹窗：对齐 `jm3` 整合版设计与 COUI 原生对话框规范。
 *
 * - 内容水平内边距对齐 `CouiDialogDefaults.contentPadding`，与标题、摘要左缘对齐；
 * - 下载中将「取消下载」挂载到 Dialog 原生 `neutralAction` 按钮栏，避免随内容滚走；
 * - 更新说明包裹在半透明凝光轮廓卡内，经 `FoldableNotes` 限定滚动视口与边缘羽化，
 *   折叠行固定在视口底缘外侧，展开长说明也能随时一键收起；
 * - 浏览器兜底与「忽略此版本」使用 `CouiDialogSecondaryButton` 次级胶囊按钮，不与主操作并列。
 */
@Composable
internal fun CouiUpdateDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onUpdate: (url: String) -> Unit,
    onIgnore: ((String) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val dialogMaterial = couiDialogMaterialOrNull()
    val hasMaterial = dialogMaterial != null
    val downloader = LocalUpdateDownloader.current
    val rawDownloadState by downloader.state.collectAsState()
    val asset = remember(releaseInfo) { releaseInfo.updateAsset() }
    val downloadState = remember(rawDownloadState, asset) { rawDownloadState.forAsset(asset) }
    LaunchedEffect(asset) { downloader.syncTarget(asset) }

    var notesExpanded by remember(releaseInfo.tagName) { mutableStateOf(false) }
    var installErrorText by remember(releaseInfo.tagName) { mutableStateOf<String?>(null) }
    val notesState = remember(releaseInfo.changelog, notesExpanded) {
        updateDialogNotesState(
            markdown = releaseInfo.changelog,
            expanded = notesExpanded,
            previewLines = COUI_UPDATE_NOTES_PREVIEW_LINES,
        )
    }
    val plan = remember(releaseInfo, downloadState, strings, onIgnore) {
        buildUpdateDialogPlan(
            currentVersion = AppInfo.VERSION_NAME,
            release = releaseInfo,
            downloadState = downloadState,
            strings = strings,
            canIgnore = onIgnore != null,
            canCancelDownload = true,
        )
    }
    val notesCardShape = remember { CouiShape(18.dp, 3f) }
    val errorCardShape = remember { CouiShape(14.dp, 3f) }
    val destructiveColor = CouiDialogDefaults.themeColors(dark).destructive

    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = dialogMaterial,
        title = strings.dialogNewVersionFound,
        message = plan.summary.takeIf { it.isNotBlank() },
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = plan.primaryText,
            enabled = plan.primaryEnabled,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = {
                installErrorText = runUpdatePrimaryAction(
                    action = plan.primaryAction,
                    asset = asset,
                    downloader = downloader,
                    installPath = plan.installPath,
                    installFailedMessage = strings.updateInstallFailed,
                    openBrowser = { onUpdate(updateBrowserFallbackUrl(releaseInfo)) },
                )
            },
        ),
        neutralAction = if (plan.showCancelDownload) {
            CouiDialogAction(
                text = strings.updateCancelDownload,
                onClick = downloader::cancel,
            )
        } else {
            null
        },
        dismissAction = CouiDialogAction(
            text = plan.dismissText,
            onClick = onDismiss,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiDialogDefaults.contentPadding),
        ) {
            if (plan.showProgress) {
                Text(
                    text = plan.progressDetail.orEmpty(),
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.height(6.dp))
                CouiLinearProgressBar(progress = plan.progressFraction)
                Spacer(modifier = Modifier.height(12.dp))
            }

            val errorText = installErrorText ?: plan.errorText
            if (errorText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(errorCardShape)
                        .background(destructiveColor.copy(alpha = if (dark) 0.16f else 0.10f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = errorText,
                        kind = CouiTextKind.ItemSummary,
                        color = destructiveColor,
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (plan.hasNotes) {
                Text(
                    text = strings.updateNotesTitle,
                    kind = CouiTextKind.GroupTitle,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                val notesFold = updateNotesFold(
                    expanded = notesExpanded,
                    hiddenLineCount = notesState.hiddenLineCount,
                    strings = strings,
                    onToggle = { notesExpanded = !notesExpanded },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .couiContourSurface(
                            shape = notesCardShape,
                            surfaceColor = couiDialogSurfaceColor(),
                            cornerRadius = 18.dp,
                            cornerWeight = 3f,
                            style = CouiContourStyle.Dialog,
                            dark = dark,
                            emphasized = false,
                            fallbackBorderColor = scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.08f),
                        ),
                ) {
                    FoldableNotes(
                        fold = notesFold,
                        spec = NotesDialogViewport,
                        content = {
                            MarkdownText(
                                markdown = notesState.visibleMarkdown,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                baseFontSize = 13,
                                colors = couiMarkdownColors(),
                                onLinkClick = { url -> openSafeUrl(url, strings, onError = {}) },
                            )
                        },
                        footer = {
                            notesFold?.let { fold ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(scheme.outlineVariant.copy(alpha = 0.55f)),
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp)
                                            .couiDialogPressable(
                                                spotlight = SpotlightPreset.ChipTab,
                                                onClick = fold.onToggle,
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = fold.label,
                                            kind = CouiTextKind.ItemValue,
                                            color = scheme.accent,
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Image(
                                            painter = rememberVectorPainter(
                                                if (fold.expanded) CouiIcons.ExpandLess else CouiIcons.ExpandMore,
                                            ),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(scheme.accent),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            } else {
                Text(
                    text = strings.updateChangelogEmpty,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }

            if (plan.showBrowserFallback) {
                Spacer(modifier = Modifier.height(12.dp))
                CouiDialogSecondaryButton(
                    text = strings.updateOpenInBrowser,
                    contentColor = scheme.accent,
                    hasMaterial = hasMaterial,
                    onClick = { onUpdate(updateBrowserFallbackUrl(releaseInfo)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (plan.showIgnore && onIgnore != null) {
                Spacer(modifier = Modifier.height(12.dp))
                CouiDialogSecondaryButton(
                    text = strings.updateDialogIgnore,
                    contentColor = destructiveColor,
                    hasMaterial = hasMaterial,
                    onClick = { onIgnore(releaseInfo.versionName) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
