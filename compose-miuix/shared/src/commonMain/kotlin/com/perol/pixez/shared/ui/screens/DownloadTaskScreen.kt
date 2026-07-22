package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 下载任务页：展示全部 / 运行中 / 完成 / 失败四类下载任务，
 * 支持点击跳转作品详情、重试失败任务、删除单条任务、清空已完成任务。
 *
 * 运行中任务列表每秒自动刷新，以跟踪下载状态变化。
 */
@Composable
fun DownloadTaskScreen(
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    downloadRepository: DownloadRepository,
    downloadHistoryRepository: DownloadHistoryRepository,
) {
    // 当前选中的筛选标签：0=全部，1=运行中，2=完成，3=失败。
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    // 用于触发列表重新加载的令牌；删除/重试/清空后自增。
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    // 初始加载失败时自增，触发 produceState 重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    // 是否显示批量操作底部菜单。
    var showBatchMenu by rememberSaveable { mutableStateOf(false) }
    // 是否显示清空已完成确认栏。
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    // Toast 提示文本。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    // 正在处理中的任务 ID 集合，用于禁用单条操作按钮防止重复提交。
    // 使用 remember 而非 rememberSaveable：进程恢复后协程不会恢复，避免标志位永久锁定。
    var processingTaskIds by remember { mutableStateOf(setOf<Long>()) }
    // 是否正在执行批量操作，用于禁用顶部菜单按钮。
    var isBatchProcessing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // 使用 produceState 从数据库加载任务列表；筛选、刷新令牌或重试计数变化时自动重新加载。
    val state = produceState<Result<List<DownloadTaskHistory>>?>(
        initialValue = null,
        downloadHistoryRepository,
        selectedFilter,
        refreshToken,
        retryCount,
    ) {
        value = runCatchingNonCancel {
            when (selectedFilter) {
                FILTER_ALL -> downloadHistoryRepository.getAllTasks()
                // 「运行中」同时包含正在下载与等待中的任务，避免 Pending 任务在分类视图中不可见。
                FILTER_RUNNING -> downloadHistoryRepository.getTasksByStatus(DownloadStatus.Downloading) +
                    downloadHistoryRepository.getTasksByStatus(DownloadStatus.Pending)
                FILTER_COMPLETED -> downloadHistoryRepository.getTasksByStatus(DownloadStatus.Success)
                FILTER_FAILED -> downloadHistoryRepository.getTasksByStatus(DownloadStatus.Failed)
                else -> downloadHistoryRepository.getAllTasks()
            }
        }
    }

    // 筛选为「运行中」时启动定时器，每秒刷新一次以跟踪下载进度与状态变化。
    LaunchedEffect(selectedFilter) {
        if (selectedFilter != FILTER_RUNNING) return@LaunchedEffect
        while (true) {
            delay(1000L)
            refreshToken++
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "下载任务",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBatchMenu = true },
                        enabled = !isBatchProcessing,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多操作",
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
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部筛选标签：全部 / 运行中 / 完成 / 失败。
                FilterTabRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                when (val result = state.value) {
                    null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    else -> when {
                        result.isSuccess -> {
                            val tasks = result.getOrNull().orEmpty()
                            if (tasks.isEmpty()) {
                                EmptyPlaceholder(
                                    message = emptyMessage(selectedFilter),
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
                                        DownloadTaskItem(
                                            task = task,
                                            isProcessing = processingTaskIds.contains(task.id) || isBatchProcessing,
                                            onClick = { onIllustClick(task.illustId) },
                                            onRetry = {
                                                // 在启动协程前设置标志位，降低快速双击导致重复提交的竞态风险。
                                                if (processingTaskIds.contains(task.id)) return@DownloadTaskItem
                                                processingTaskIds = processingTaskIds + task.id
                                                coroutineScope.launch {
                                                    try {
                                                        runCatchingNonCancel {
                                                            downloadRepository.retry(task)
                                                        }.onSuccess {
                                                            toastMessage = "重试成功"
                                                            refreshToken++
                                                        }.onFailure {
                                                            toastMessage = "重试失败: ${it.message}"
                                                        }
                                                    } finally {
                                                        processingTaskIds = processingTaskIds - task.id
                                                    }
                                                }
                                            },
                                            onDelete = {
                                                if (processingTaskIds.contains(task.id)) return@DownloadTaskItem
                                                processingTaskIds = processingTaskIds + task.id
                                                coroutineScope.launch {
                                                    try {
                                                        runCatchingNonCancel {
                                                            downloadHistoryRepository.deleteTask(task.id)
                                                        }.onSuccess {
                                                            refreshToken++
                                                        }.onFailure {
                                                            toastMessage = "删除失败: ${it.message}"
                                                        }
                                                    } finally {
                                                        processingTaskIds = processingTaskIds - task.id
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
            }

            // 批量操作底部菜单：重试失败任务、清空已完成任务。
            if (showBatchMenu) {
                BatchActionMenu(
                    onDismissRequest = { showBatchMenu = false },
                    onRetryFailed = {
                        showBatchMenu = false
                        if (isBatchProcessing) return@BatchActionMenu
                        isBatchProcessing = true
                        coroutineScope.launch {
                            try {
                                // 先查询所有失败任务，再逐个重试；单个失败不影响其他任务。
                                val failedTasks = runCatchingNonCancel {
                                    downloadHistoryRepository.getTasksByStatus(DownloadStatus.Failed)
                                }.getOrDefault(emptyList())
                                var successCount = 0
                                var failureCount = 0
                                failedTasks.forEach { task ->
                                    runCatchingNonCancel { downloadRepository.retry(task) }
                                        .onSuccess { successCount++ }
                                        .onFailure { failureCount++ }
                                }
                                toastMessage = when {
                                    failureCount == 0 -> "已批量重试失败任务"
                                    successCount == 0 -> "批量重试全部失败"
                                    else -> "批量重试完成：成功 $successCount 条，失败 $failureCount 条"
                                }
                                refreshToken++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                toastMessage = "批量重试失败: ${e.message}"
                            } finally {
                                isBatchProcessing = false
                            }
                        }
                    },
                    onClearCompleted = {
                        showBatchMenu = false
                        showClearConfirm = true
                    },
                    enabled = !isBatchProcessing,
                )
            }

            // 清空已完成确认栏。
            if (showClearConfirm) {
                ClearConfirmBar(
                    onConfirm = {
                        showClearConfirm = false
                        if (isBatchProcessing) return@ClearConfirmBar
                        isBatchProcessing = true
                        coroutineScope.launch {
                            try {
                                // 查询所有已完成任务并逐条删除；数据库暂无按状态删除接口。
                                val completedTasks = runCatchingNonCancel {
                                    downloadHistoryRepository.getTasksByStatus(DownloadStatus.Success)
                                }.getOrDefault(emptyList())
                                completedTasks.forEach { task ->
                                    runCatchingNonCancel { downloadHistoryRepository.deleteTask(task.id) }
                                }
                                toastMessage = "已清空已完成任务"
                                refreshToken++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                toastMessage = "清空失败: ${e.message}"
                            } finally {
                                isBatchProcessing = false
                            }
                        }
                    },
                    onCancel = { showClearConfirm = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Toast 提示层，覆盖在其他内容之上。
            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
            )
        }
    }
}

// 筛选标签常量，避免魔法数字。
private const val FILTER_ALL = 0
private const val FILTER_RUNNING = 1
private const val FILTER_COMPLETED = 2
private const val FILTER_FAILED = 3

/**
 * 根据当前筛选标签返回空态提示文案。
 */
private fun emptyMessage(filter: Int): String = when (filter) {
    FILTER_RUNNING -> "暂无运行中的任务"
    FILTER_COMPLETED -> "暂无已完成的任务"
    FILTER_FAILED -> "暂无失败的任务"
    else -> "暂无下载任务"
}

/**
 * 顶部筛选标签行：全部 / 运行中 / 完成 / 失败。
 */
@Composable
private fun FilterTabRow(
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 使用 remember 缓存标签列表，避免每次重组重新创建。
    val labels = remember { listOf("全部", "运行中", "完成", "失败") }

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            FilterTab(
                label = label,
                selected = selectedFilter == index,
                onClick = { onFilterSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 单个筛选标签：选中时高亮显示。
 */
@Composable
private fun FilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val textColor = if (selected) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

/**
 * 单条下载任务列表项：缩略图、标题、画师、状态图标、操作按钮，
 * 运行中任务底部显示进度条。
 */
@Composable
private fun DownloadTaskItem(
    task: DownloadTaskHistory,
    isProcessing: Boolean,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick, enabled = !isProcessing)
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
                    text = "${task.userName} · 第 ${task.pageIndex + 1} 页",
                    style = MiuixTheme.textStyles.footnote2,
                )
            }
            StatusIcon(
                status = task.status,
                modifier = Modifier.size(20.dp),
            )
            IconButton(
                onClick = onRetry,
                enabled = task.status == DownloadStatus.Failed && !isProcessing,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重试",
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = !isProcessing,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MiuixTheme.colorScheme.error,
                )
            }
        }

        // 运行中任务展示进度条；当前下载器未暴露字节级进度，使用不确定进度条表示正在进行。
        if (task.status == DownloadStatus.Downloading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                color = MiuixTheme.colorScheme.primary,
                trackColor = MiuixTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

/**
 * 状态图标映射：成功/失败/运行中/等待中。
 */
@Composable
private fun StatusIcon(
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        DownloadStatus.Success -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "完成",
            modifier = modifier,
            tint = MiuixTheme.colorScheme.primary,
        )

        DownloadStatus.Failed -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "失败",
            modifier = modifier,
            tint = MiuixTheme.colorScheme.error,
        )

        DownloadStatus.Downloading -> Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "下载中",
            modifier = modifier,
            tint = MiuixTheme.colorScheme.primary,
        )

        DownloadStatus.Pending -> Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = "等待中",
            modifier = modifier,
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 批量操作底部菜单：提供重试失败任务与清空已完成任务入口。
 */
@Composable
private fun BatchActionMenu(
    onDismissRequest: () -> Unit,
    onRetryFailed: () -> Unit,
    onClearCompleted: () -> Unit,
    enabled: Boolean,
) {
    SuperBottomSheet(
        show = true,
        title = "批量操作",
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            BasicComponent(
                title = "重试失败任务",
                onClick = { if (enabled) onRetryFailed() },
            )
            BasicComponent(
                title = "清空已完成任务",
                onClick = { if (enabled) onClearCompleted() },
            )
        }
    }
}

/**
 * 底部清空确认栏：询问用户是否确认删除全部已完成任务。
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
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "确定清空所有已完成任务？",
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        top.yukonga.miuix.kmp.basic.TextButton(
            text = "取消",
            onClick = onCancel,
        )
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onConfirm,
        ) {
            Text("确定")
        }
    }
}
