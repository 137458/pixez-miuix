package com.perol.pixez.shared.ui.screens

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 下载历史列表页：展示本地 task 表中记录的下载任务，支持删除单条与清空全部。
 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun DownloadHistoryScreen(
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: DownloadHistoryRepository,
) {
    val listState = rememberLazyListState()
    // retryCount 用于触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    // 是否需要重新加载列表；删除/清空后自增。
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<List<DownloadTaskHistory>>?>(
        initialValue = null,
        repository,
        retryCount,
        refreshToken,
    ) {
        // 当前处于 produceState 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        value = suspendRunCatchingNonCancel { repository.getAllTasks() }
    }

    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = LocalBackdrop.current
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingDownloadHistory,
                scrollBehavior = scrollBehavior,
                color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                modifier = Modifier.topAppBarBlur(backdrop = backdrop, tintColor = colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        enabled = state.value?.getOrNull()?.isNotEmpty() ?: false,
                    ) {
                        Text(
                            text = strings.actionClear,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val result = state.value) {
                null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                else -> when {
                    result.isSuccess -> {
                        val tasks = result.getOrNull().orEmpty()
                        if (tasks.isEmpty()) {
                            EmptyPlaceholder(
                                message = strings.downloadHistoryEmpty,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().blurBackdropSource(backdrop).nestedScroll(scrollBehavior.nestedScrollConnection),
                                    contentPadding = paddingValues,
                                ) {
                                    items(
                                        items = tasks,
                                        key = { it.id },
                                        contentType = { "download_history_item" },
                                    ) { task ->
                                        DownloadHistoryItem(
                                            task = task,
                                            onClick = { onIllustClick(task.illustId) },
                                            onDelete = {
                                                coroutineScope.launch {
                                                    suspendRunCatchingNonCancel {
                                                        repository.deleteTask(task.id)
                                                    }.onSuccess {
                                                        refreshToken++
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                                VerticalScrollBar(
                                    adapter = rememberScrollBarAdapter(listState),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                    else -> ErrorPlaceholder(
                        error = result.exceptionOrNull(),
                        onRetry = { retryCount++ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 底部确认栏：避免引入 AlertDialog，保持依赖最小化。
            if (showClearConfirm) {
                ClearConfirmBar(
                    onConfirm = {
                        showClearConfirm = false
                        coroutineScope.launch {
                            suspendRunCatchingNonCancel { repository.clearAll() }
                                .onSuccess { refreshToken++ }
                        }
                    },
                    onCancel = { showClearConfirm = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * 单条下载历史列表项：缩略图、标题、页码、文件名与状态，右侧提供删除按钮。
 */
@Composable
private fun DownloadHistoryItem(
    task: DownloadTaskHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixivAsyncImage(
            model = task.medium,
            contentDescription = task.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = task.title,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = buildSummary(task),
                style = MiuixTheme.textStyles.footnote2,
            )
        }
        Text(
            text = statusLabel(task.status, strings),
            style = MiuixTheme.textStyles.footnote2,
            color = statusColor(task.status),
        )
        IconButton(
            onClick = onDelete,
        ) {
            Icon(
                imageVector = MiuixIcons.Delete,
                contentDescription = strings.btnDelete,
                tint = MiuixTheme.colorScheme.error,
            )
        }
    }
}

/**
 * 构建摘要文本：画师名、页码、文件名。
 */
private fun buildSummary(task: DownloadTaskHistory): String {
    return "${task.userName} · #${task.pageIndex + 1}\n${task.fileName}"
}

/**
 * 状态文本映射。
 */
private fun statusLabel(status: DownloadStatus, strings: com.perol.pixez.shared.ui.i18n.AppStrings): String = when (status) {
    DownloadStatus.Pending -> strings.downloadStatusPending
    DownloadStatus.Downloading -> strings.downloadStatusDownloading
    DownloadStatus.Success -> strings.downloadStatusSuccess
    DownloadStatus.Failed -> strings.downloadStatusFailed
}

/**
 * 状态颜色映射：成功主色、失败错误色、其他次景色。
 */
@Composable
private fun statusColor(status: DownloadStatus): Color {
    return when (status) {
        DownloadStatus.Success -> MiuixTheme.colorScheme.primary
        DownloadStatus.Failed -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onSurface
    }
}

/**
 * 底部清空确认栏：询问用户是否确认删除全部下载记录。
 */
@Composable
private fun ClearConfirmBar(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.downloadHistoryClearConfirm,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        TextButton(
            text = strings.cancel,
            onClick = onCancel,
        )
        Button(
            onClick = onConfirm,
        ) {
            Text(strings.confirm)
        }
    }
}
