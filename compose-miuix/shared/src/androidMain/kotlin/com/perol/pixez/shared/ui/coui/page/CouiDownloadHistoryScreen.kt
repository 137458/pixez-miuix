package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalDownloadHistoryRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 下载历史页：固定顶栏 + 已下载任务卡片列表。
 *
 * 数据加载、单条删除与清空全部与 MIUIX 版一致；差异只有皮肤层：
 * 清空确认改用 COUI `Dialog`，列表不再带 MIUIX 的纵向滚动条，
 * 三态占位与列表收敛到平板可读宽度。
 */
@Composable
internal fun CouiDownloadHistoryScreen(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val repository = LocalDownloadHistoryRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()

    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val state = produceState<Result<List<DownloadTaskHistory>>?>(
        initialValue = null,
        repository,
        retryCount,
        refreshToken,
    ) {
        value = suspendRunCatchingNonCancel { repository.getAllTasks() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingDownloadHistory,
            onBack = onBack,
        ) {
            CouiTextAction(
                text = strings.actionClear,
                enabled = state.value?.getOrNull()?.isNotEmpty() == true,
                color = if (state.value?.getOrNull()?.isNotEmpty() == true) {
                    CouiTheme.colorScheme.accent
                } else {
                    null
                },
                onClick = { showClearConfirm = true },
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            when (val result = state.value) {
                null -> CouiStatePlaceholder(
                    message = strings.loading,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> when {
                    result.isSuccess -> {
                        val tasks = result.getOrNull().orEmpty()
                        if (tasks.isEmpty()) {
                            CouiStatePlaceholder(
                                message = strings.downloadHistoryEmpty,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    top = 8.dp,
                                    end = 12.dp,
                                    bottom = LocalBottomBarContentPadding.current,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                overscrollEffect = rememberCouiOverscrollEffect(),
                            ) {
                                items(
                                    items = tasks,
                                    key = { it.id },
                                    contentType = { "coui_download_history" },
                                ) { task ->
                                    CouiDownloadHistoryRow(
                                        task = task,
                                        onClick = { onIllustClick(task.illustId) },
                                        onDelete = {
                                            coroutineScope.launch {
                                                suspendRunCatchingNonCancel {
                                                    repository.deleteTask(task.id)
                                                }.onSuccess { refreshToken++ }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    else -> CouiStatePlaceholder(
                        message = couiErrorMessage(result.exceptionOrNull()),
                        actionText = strings.retry,
                        onAction = { retryCount++ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        val clearing = isProcessing
        Dialog(
            onDismissRequest = { if (!clearing) showClearConfirm = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.actionClear,
            message = strings.downloadHistoryClearConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                enabled = !clearing,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    isProcessing = true
                    coroutineScope.launch {
                        try {
                            val result = suspendRunCatchingNonCancel { repository.clearAll() }
                            result.onSuccess { refreshToken++ }
                            // 成功后关闭；失败保持弹窗，交给用户在错误提示下重试或取消。
                            if (result.isSuccess) showClearConfirm = false
                        } finally {
                            isProcessing = false
                        }
                    }
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { if (!isProcessing) showClearConfirm = false },
            ),
        )
    }
}

/** 单条下载历史卡：缩略图、标题、画师/页码/文件名与状态，右侧删除按钮。 */
@Composable
private fun CouiDownloadHistoryRow(
    task: DownloadTaskHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    val statusColor = when (task.status) {
        DownloadStatus.Success -> scheme.accent
        DownloadStatus.Failed -> CouiDialogDefaults.colors(CouiTheme.isDark).destructive
        else -> scheme.onSurfaceVariant
    }
    val statusLabel = when (task.status) {
        DownloadStatus.Pending -> strings.downloadStatusPending
        DownloadStatus.Downloading -> strings.downloadStatusDownloading
        DownloadStatus.Success -> strings.downloadStatusSuccess
        DownloadStatus.Failed -> strings.downloadStatusFailed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card)
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixivAsyncImage(
            model = task.medium,
            contentDescription = task.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${task.userName} · #${task.pageIndex + 1}",
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = task.fileName,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = statusLabel,
            kind = CouiTextKind.ItemSummary,
            color = statusColor,
            maxLines = 1,
        )
        CouiActionButton(
            icon = MiuixIcons.Delete,
            contentDescription = strings.btnDelete,
            tint = CouiDialogDefaults.colors(CouiTheme.isDark).destructive,
            onClick = onDelete,
        )
    }
}
