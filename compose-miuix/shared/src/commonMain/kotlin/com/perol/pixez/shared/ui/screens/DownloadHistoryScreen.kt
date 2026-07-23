package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 下载历史列表页：展示本地 task 表中记录的下载任务，支持删除单条与清空全部。
 */
@Composable
fun DownloadHistoryScreen(
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: DownloadHistoryRepository,
) {
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
        value = runCatchingNonCancel { repository.getAllTasks() }
    }

    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "下载历史",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        enabled = state.value?.getOrNull()?.isNotEmpty() ?: false,
                    ) {
                        Text(
                            text = "清空",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val result = state.value) {
                null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                else -> when {
                    result.isSuccess -> {
                        val tasks = result.getOrNull().orEmpty()
                        if (tasks.isEmpty()) {
                            EmptyPlaceholder(
                                message = "暂无下载记录",
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                items(
                                    items = tasks,
                                    key = { it.id },
                                ) { task ->
                                    DownloadHistoryItem(
                                        task = task,
                                        onClick = { onIllustClick(task.illustId) },
                                        onDelete = {
                                            coroutineScope.launch {
                                                runCatchingNonCancel {
                                                    repository.deleteTask(task.id)
                                                }.onSuccess {
                                                    refreshToken++
                                                }
                                            }
                                        },
                                    )
                                }
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
                            runCatchingNonCancel { repository.clearAll() }
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
            text = statusLabel(task.status),
            style = MiuixTheme.textStyles.footnote2,
            color = statusColor(task.status),
        )
        IconButton(
            onClick = onDelete,
        ) {
            Icon(
                imageVector = MiuixIcons.Delete,
                contentDescription = "删除",
                tint = MiuixTheme.colorScheme.error,
            )
        }
    }
}

/**
 * 构建摘要文本：画师名、页码、文件名。
 */
private fun buildSummary(task: DownloadTaskHistory): String {
    return "${task.userName} · 第 ${task.pageIndex + 1} 页\n${task.fileName}"
}

/**
 * 状态文本映射。
 */
private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.Pending -> "等待中"
    DownloadStatus.Downloading -> "下载中"
    DownloadStatus.Success -> "成功"
    DownloadStatus.Failed -> "失败"
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "确定清空全部下载记录？",
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        TextButton(
            text = "取消",
            onClick = onCancel,
        )
        Button(
            onClick = onConfirm,
        ) {
            Text("确定")
        }
    }
}
