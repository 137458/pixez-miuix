package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.platform.AppInstaller
import com.perol.pixez.shared.platform.AppUpdateDownloader
import com.perol.pixez.shared.platform.UpdateAsset
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.ui.i18n.AppStrings
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 更新下载进度的唯一渲染：左侧状态文案 + 右侧「已下载 / 总量 · 速度」+ 进度条。
 *
 * 更新弹窗与更新设置页共用，避免两处各自维护行布局、字号与进度条分支。
 */
@Composable
internal fun UpdateDownloadProgressSection(
    state: UpdateDownloadState.Downloading,
    label: String,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
            color = MiuixTheme.colorScheme.primary,
        )
        Text(
            text = updateDialogProgressDetail(state, strings),
            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    if (state.total > 0L) {
        LinearProgressIndicator(
            progress = state.progress,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * 更新主操作的唯一实现：下载 / 安装 / 浏览器兜底。
 *
 * 返回需要展示的安装失败文案；null 表示成功或已交给系统安装器。
 * 弹窗把它渲染成内联错误，设置页把它转成 Toast —— 差异只在展示层。
 */
internal fun runUpdatePrimaryAction(
    action: UpdatePrimaryAction,
    asset: UpdateAsset?,
    downloader: AppUpdateDownloader,
    installPath: String?,
    installFailedMessage: String,
    openBrowser: () -> Unit,
): String? = when (action) {
    UpdatePrimaryAction.Download -> {
        asset?.let(downloader::start)
        null
    }
    UpdatePrimaryAction.Install -> {
        if (installPath != null && AppInstaller().install(installPath)) {
            null
        } else {
            // 安装包缺失/为空或系统安装器无法调起：清理旧包以允许重新下载。
            downloader.discardCompletedAfterInstallFailure(asset)
            installFailedMessage
        }
    }
    UpdatePrimaryAction.OpenBrowser -> {
        openBrowser()
        null
    }
}
