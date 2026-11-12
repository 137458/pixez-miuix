package com.perol.pixez.shared.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.platform.forAsset
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalUpdateDownloader
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * HyperOS 规范版本更新弹窗。
 *
 * 口径与 jm3 整合版一致：标题下只有一行版本跃迁摘要；动作按下载状态推导；说明默认折叠
 * 12 行，整段说明只用一个滚动容器；忽略降级为说明下方的文字链接。
 *
 * 下载状态来自进程级 [Composable] 单例，弹窗只观察不持有任务，关闭弹窗不会取消下载。
 */
@Composable
fun UpdateDialog(
    show: Boolean,
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onUpdate: (url: String) -> Unit,
    onIgnore: ((version: String) -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val downloader = LocalUpdateDownloader.current
    val rawDownloadState by downloader.state.collectAsState()
    val asset = remember(releaseInfo) { releaseInfo.updateAsset() }
    // 渲染前与当前 release 对齐：归属不一致的旧进度/旧完成包一律按 Idle 呈现。
    val downloadState = remember(rawDownloadState, asset) { rawDownloadState.forAsset(asset) }
    LaunchedEffect(asset) { downloader.syncTarget(asset) }

    var notesExpanded by remember(show, releaseInfo.tagName) { mutableStateOf(false) }
    var installErrorText by remember(releaseInfo.tagName) { mutableStateOf<String?>(null) }
    val notesState = remember(releaseInfo.changelog, notesExpanded) {
        updateDialogNotesState(releaseInfo.changelog, notesExpanded)
    }
    val plan = remember(releaseInfo, downloadState, strings, onIgnore, asset) {
        buildUpdateDialogPlan(
            currentVersion = AppInfo.VERSION_NAME,
            release = releaseInfo,
            downloadState = downloadState,
            strings = strings,
            canIgnore = onIgnore != null,
            canCancelDownload = true,
        )
    }

    val scrollState = rememberScrollState()
    // 只给还有内容的方向加羽化：滚到顶/底时对应一侧恢复实心，避免“永远有雾”的错误暗示
    val hasContentAbove by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }
    val hasContentBelow by remember(scrollState) { derivedStateOf { scrollState.maxValue > scrollState.value } }

    WindowDialog(
        show = show,
        title = strings.dialogNewVersionFound,
        summary = plan.summary,
        onDismissRequest = onDismiss,
    ) {
        // 说明区是弹窗内唯一的滚动容器；羽化必须在 verticalScroll 外侧，
        // 在内侧时量到的是内容总高，渐变会落在滚出视口的位置，肉眼看不到羽化。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = UPDATE_NOTES_MAX_HEIGHT_DP.dp)
                .verticalEdgeFade(
                    top = hasContentAbove,
                    bottom = hasContentBelow,
                    fade = UPDATE_NOTES_FADE_DP.dp,
                )
                .verticalScroll(scrollState),
        ) {
            if (downloadState is UpdateDownloadState.Downloading) {
                UpdateDownloadProgressSection(
                    state = downloadState,
                    label = strings.updateDownloading,
                    strings = strings,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val errorText = installErrorText ?: plan.errorText
            if (errorText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squircleClip(AppConstants.Update.BADGE_CORNER_RADIUS_DP.dp)
                        .background(MiuixTheme.colorScheme.error.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = errorText,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (plan.hasNotes) {
                SmallTitle(text = strings.updateNotesTitle)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MarkdownText(
                            markdown = notesState.visibleMarkdown,
                            modifier = Modifier.fillMaxWidth(),
                            baseFontSize = 13,
                            onLinkClick = { url ->
                                // 说明来自远端，统一走 openSafeUrl 的 scheme 白名单。
                                openSafeUrl(url, strings, onError = {})
                            },
                        )
                        if (notesState.hiddenLineCount > 0 || notesExpanded) {
                            TextButton(
                                text = if (notesExpanded) strings.collapse else strings.expand,
                                onClick = { notesExpanded = !notesExpanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = strings.updateChangelogEmpty,
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            if (plan.showIgnore) {
                // 「忽略此版本」不再与主操作并列，降级为说明下方的一枚文字链接。
                TextButton(
                    text = strings.updateDialogIgnore,
                    onClick = { onIgnore?.invoke(releaseInfo.versionName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    // 返回 null 时同时清掉上一次的安装失败提示。
                    installErrorText = runUpdatePrimaryAction(
                        action = plan.primaryAction,
                        asset = asset,
                        downloader = downloader,
                        installPath = plan.installPath,
                        installFailedMessage = strings.updateInstallFailed,
                        openBrowser = { onUpdate(updateBrowserFallbackUrl(releaseInfo)) },
                    )
                },
                enabled = plan.primaryEnabled,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = plan.primaryText, style = MiuixTheme.textStyles.button)
            }

            if (plan.showBrowserFallback) {
                TextButton(
                    text = strings.updateOpenInBrowser,
                    onClick = { onUpdate(updateBrowserFallbackUrl(releaseInfo)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (plan.showCancelDownload) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        text = strings.updateCancelDownload,
                        onClick = downloader::cancel,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = plan.dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                TextButton(
                    text = plan.dismissText,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 边缘羽化底座：离屏合成后用 DstIn 遮罩把内容在指定边缘淡出。
 */
internal fun Modifier.edgeFadeMask(brushFactory: DrawScope.() -> Brush): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        if (size.width <= 0f || size.height <= 0f) return@drawWithContent
        drawRect(brush = brushFactory(), blendMode = BlendMode.DstIn)
    }

/**
 * 竖向边缘羽化：只在 [top] / [bottom] 为 true 的一侧铺渐变，滚到端点时对应侧完全实心。
 */
internal fun Modifier.verticalEdgeFade(
    top: Boolean,
    bottom: Boolean,
    fade: Dp = UPDATE_NOTES_FADE_DP.dp,
): Modifier {
    if (!top && !bottom) return this
    return edgeFadeMask {
        val fadePx = fade.toPx().coerceAtMost(size.height / 2f)
        val stop = (fadePx / size.height).coerceIn(0f, 0.5f)
        when {
            top && bottom -> Brush.verticalGradient(
                0f to Color.Transparent,
                stop to Color.Black,
                1f - stop to Color.Black,
                1f to Color.Transparent,
            )
            top -> Brush.verticalGradient(0f to Color.Transparent, stop to Color.Black)
            else -> Brush.verticalGradient(1f - stop to Color.Black, 1f to Color.Transparent)
        }
    }
}
