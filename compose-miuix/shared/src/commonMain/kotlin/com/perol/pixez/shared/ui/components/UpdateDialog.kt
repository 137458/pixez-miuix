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
import top.yukonga.miuix.kmp.squircle.squircleClip
import com.perol.pixez.shared.platform.AppInstaller
import com.perol.pixez.shared.platform.AppUpdateDownloader
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.screens.ReleaseInfo
import com.perol.pixez.shared.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 官方 Miuix / HyperOS 规范版本更新弹窗：支持应用内流式下载安装包与一键自动调起安装。
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

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    fun startDownload() {
        val downloadUrl = releaseInfo.downloadUrl
        val fileName = releaseInfo.fileName
        if (downloadUrl == null || fileName == null) {
            downloadError = strings.updateDownloadFailed
            return
        }

        isDownloading = true
        downloadError = null
        downloadProgress = 0f

        coroutineScope.launch {
            val result = AppUpdateDownloader().download(
                downloadUrl = downloadUrl,
                fileName = fileName,
                onProgress = { progress, downloaded, total ->
                    downloadProgress = progress
                    downloadedBytes = downloaded
                    totalBytes = total
                },
            )
            isDownloading = false
            result
                .onSuccess { path ->
                    downloadedFilePath = path
                    AppInstaller().install(path)
                }
                .onFailure { error ->
                    downloadError = error.message ?: strings.updateDownloadFailed
                }
        }
    }

    WindowDialog(
        show = show,
        title = strings.dialogNewVersionFound,
        summary = "v${AppInfo.VERSION_NAME} → v${releaseInfo.versionName}",
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 更新日志卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .squircleClip(12.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (releaseInfo.changelog.isNotBlank()) {
                    MarkdownText(
                        markdown = releaseInfo.changelog,
                        modifier = Modifier.fillMaxWidth(),
                        baseFontSize = 13,
                    )
                } else {
                    Text(
                        text = strings.updateChangelogTitle,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            // 下载进度状态条
            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = strings.updateDownloading,
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.primary,
                        )
                        val percent = if (downloadProgress >= 0f) "${(downloadProgress * 100).toInt()}%" else ""
                        val sizeText = if (totalBytes > 0) {
                            "${formatSize(downloadedBytes)} / ${formatSize(totalBytes)}"
                        } else {
                            formatSize(downloadedBytes)
                        }
                        Text(
                            text = if (percent.isNotEmpty()) "$sizeText ($percent)" else sizeText,
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (downloadProgress >= 0f) {
                        LinearProgressIndicator(
                            progress = downloadProgress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 错误提示
            if (downloadError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = downloadError ?: "",
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部操作按钮
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (onIgnore != null && !isDownloading && downloadedFilePath == null) {
                    TextButton(
                        text = strings.btnIgnore,
                        onClick = { onIgnore(releaseInfo.versionName) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                }

                if (!isDownloading) {
                    TextButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                }

                if (downloadedFilePath != null) {
                    // 已下载完成，提供重新安装 / 打开安装包
                    TextButton(
                        text = strings.updateInstallNow,
                        onClick = {
                            AppInstaller().install(downloadedFilePath!!)
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                } else if (!isDownloading) {
                    TextButton(
                        text = if (downloadError != null) strings.retry else strings.btnUpdate,
                        onClick = {
                            startDownload()
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${(gb * 10).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10).toInt() / 10.0} MB"
        kb >= 1.0 -> "${(kb * 10).toInt() / 10.0} KB"
        else -> "$bytes B"
    }
}
