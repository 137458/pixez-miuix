package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.platform.AppInstaller
import com.perol.pixez.shared.platform.AppUpdateDownloader
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.ReleaseInfo
import com.perol.pixez.shared.ui.i18n.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 更新下载流式状态结构体，收敛零散可变状态。
 */
private data class DownloadUiState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedText: String = "",
    val filePath: String? = null,
    val error: String? = null,
)

/**
 * 官方 Miuix / HyperOS 规范版本更新弹窗：
 * 支持版本跃迁胶囊卡片、元数据展示、应用内流式测速下载、任务中断取消与无缝回退浏览器。
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
    val coroutineScope = rememberCoroutineScope()

    var downloadState by remember { mutableStateOf(DownloadUiState()) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadState = DownloadUiState()
    }

    fun startDownload() {
        val downloadUrl = releaseInfo.downloadUrl
        val fileName = releaseInfo.fileName
        if (downloadUrl.isNullOrBlank() || fileName.isNullOrBlank()) {
            onUpdate(releaseInfo.releaseUrl)
            onDismiss()
            return
        }

        downloadState = DownloadUiState(isDownloading = true)

        downloadJob = coroutineScope.launch {
            var lastTime = Clock.System.now().toEpochMilliseconds()
            var lastBytes = 0L

            val result = AppUpdateDownloader().download(
                downloadUrl = downloadUrl,
                fileName = fileName,
                onProgress = { progress, downloaded, total ->
                    val currentTime = Clock.System.now().toEpochMilliseconds()
                    val timeDiff = currentTime - lastTime
                    var speed = downloadState.speedText
                    if (timeDiff >= AppConstants.Update.SPEED_CALCULATION_INTERVAL_MS) {
                        val bytesDiff = downloaded - lastBytes
                        if (bytesDiff >= 0 && timeDiff > 0) {
                            val rate = (bytesDiff * 1000L) / timeDiff
                            speed = "${formatFileSize(rate)}/s"
                        }
                        lastTime = currentTime
                        lastBytes = downloaded
                    }
                    downloadState = downloadState.copy(
                        progress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        speedText = speed,
                    )
                },
            )

            downloadJob = null

            result
                .onSuccess { path ->
                    downloadState = downloadState.copy(
                        isDownloading = false,
                        filePath = path,
                    )
                    AppInstaller().install(path)
                }
                .onFailure { error ->
                    if (error !is CancellationException) {
                        downloadState = downloadState.copy(
                            isDownloading = false,
                            error = error.message ?: strings.updateDownloadFailed,
                        )
                    }
                }
        }
    }

    WindowDialog(
        show = show,
        title = strings.dialogNewVersionFound,
        onDismissRequest = {
            if (downloadState.isDownloading) {
                cancelDownload()
            }
            onDismiss()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 1. HyperOS 规范版本跃迁与元数据胶囊卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .squircleClip(AppConstants.Update.CARD_CORNER_RADIUS_DP.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 版本流动指示行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VersionBadge(
                            label = strings.updateCurrentVersion,
                            version = "v${AppInfo.VERSION_NAME}",
                            isHighlight = false,
                        )

                        Text(
                            text = "→",
                            style = MiuixTheme.textStyles.title4.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )

                        VersionBadge(
                            label = strings.updateLatestVersion,
                            version = "v${releaseInfo.versionName}",
                            isHighlight = true,
                        )
                    }

                    // 元数据标签（更新包大小与发布日期）
                    val metadataParts = buildList {
                        val size = releaseInfo.fileSize ?: downloadState.totalBytes.takeIf { it > 0 }
                        if (size != null && size > 0) {
                            add(formatFileSize(size))
                        }
                        val published = releaseInfo.publishedAt?.take(10)
                        if (!published.isNullOrBlank()) {
                            add(published)
                        }
                    }
                    if (metadataParts.isNotEmpty()) {
                        Text(
                            text = metadataParts.joinToString("  •  "),
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 更新日志卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = AppConstants.Update.CHANGELOG_MIN_HEIGHT_DP.dp,
                        max = AppConstants.Update.CHANGELOG_MAX_HEIGHT_DP.dp,
                    )
                    .squircleClip(AppConstants.Update.CARD_CORNER_RADIUS_DP.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (releaseInfo.changelog.isNotBlank()) {
                    MarkdownText(
                        markdown = releaseInfo.changelog,
                        modifier = Modifier.fillMaxWidth(),
                        baseFontSize = 13,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = strings.updateChangelogEmpty,
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            // 3. 下载进度状态与即时测速
            AnimatedVisibility(
                visible = downloadState.isDownloading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.updateDownloading,
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MiuixTheme.colorScheme.primary,
                        )

                        val percentText = if (downloadState.progress >= 0f) "${(downloadState.progress * 100).toInt()}%" else ""
                        val speedAndPercent = buildList {
                            if (percentText.isNotEmpty()) add(percentText)
                            if (downloadState.speedText.isNotEmpty()) add(downloadState.speedText)
                        }.joinToString("  •  ")

                        Text(
                            text = speedAndPercent,
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (downloadState.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = downloadState.progress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val sizeText = if (downloadState.totalBytes > 0) {
                        "${formatFileSize(downloadState.downloadedBytes)} / ${formatFileSize(downloadState.totalBytes)}"
                    } else {
                        formatFileSize(downloadState.downloadedBytes)
                    }

                    Text(
                        text = sizeText,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            // 4. 下载成功状态提示
            if (downloadState.filePath != null && !downloadState.isDownloading) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squircleClip(AppConstants.Update.BADGE_CORNER_RADIUS_DP.dp)
                        .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = strings.updateDownloadSuccess,
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // 5. 错误提示
            if (downloadState.error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squircleClip(AppConstants.Update.BADGE_CORNER_RADIUS_DP.dp)
                        .background(MiuixTheme.colorScheme.error.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = downloadState.error ?: "",
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. 底部操作按钮栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (downloadState.isDownloading) {
                    // 下载中状态：提供取消下载操作
                    TextButton(
                        text = strings.updateCancelDownload,
                        onClick = { cancelDownload() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (downloadState.filePath != null) {
                    // 下载完成：取消 / 立即安装（安全解包避免 !!）
                    TextButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = strings.updateInstallNow,
                        onClick = {
                            downloadState.filePath?.let { AppInstaller().install(it) }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                } else if (downloadState.error != null) {
                    // 下载失败：取消 / 浏览器下载 / 重试
                    TextButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.9f),
                    )
                    TextButton(
                        text = strings.updateOpenInBrowser,
                        onClick = {
                            onDismiss()
                            onUpdate(releaseInfo.releaseUrl)
                        },
                        modifier = Modifier.weight(1.2f),
                    )
                    TextButton(
                        text = strings.retry,
                        onClick = { startDownload() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                } else {
                    // 常规待更新状态
                    val hasDirectDownload = !releaseInfo.downloadUrl.isNullOrBlank() && !releaseInfo.fileName.isNullOrBlank()

                    if (onIgnore != null) {
                        TextButton(
                            text = strings.btnIgnore,
                            onClick = { onIgnore(releaseInfo.versionName) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    TextButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )

                    if (hasDirectDownload) {
                        TextButton(
                            text = strings.btnUpdate,
                            onClick = { startDownload() },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    } else {
                        // 无直链场景（如桌面端或其他架构），平滑提供浏览器下载
                        TextButton(
                            text = strings.updateOpenInBrowser,
                            onClick = {
                                onDismiss()
                                onUpdate(releaseInfo.releaseUrl)
                            },
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * HyperOS 风格版本徽标胶囊。
 */
@Composable
private fun VersionBadge(
    label: String,
    version: String,
    isHighlight: Boolean,
) {
    val bgColor = if (isHighlight) {
        MiuixTheme.colorScheme.primaryContainer
    } else {
        MiuixTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = if (isHighlight) {
        MiuixTheme.colorScheme.onPrimaryContainer
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .squircleClip(AppConstants.Update.BADGE_CORNER_RADIUS_DP.dp)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = textColor.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = version,
                style = MiuixTheme.textStyles.title4.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = textColor,
            )
        }
    }
}
