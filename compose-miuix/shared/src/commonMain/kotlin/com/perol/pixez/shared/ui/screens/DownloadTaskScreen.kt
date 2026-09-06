package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.platform.FileLocator
import com.perol.pixez.shared.platform.FileNamePolicy
import com.perol.pixez.shared.platform.getDefaultPictureDirectory
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
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
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 下载任务页：展示全部 / 运行中 / 完成 / 失败四类下载任务，
 * 支持点击跳转作品详情、重试失败任务、删除单条任务、清空已完成任务。
 *
 * 运行中任务列表每秒自动刷新，以跟踪下载状态变化。
 * 为避免筛选切换或轮询导致全量重组，所有任务只加载一次，再通过 [derivedStateOf] 按筛选条件派生子列表。
 */
@Composable
fun DownloadTaskScreen(
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    downloadRepository: DownloadRepository,
    downloadHistoryRepository: DownloadHistoryRepository,
) {
    val strings = LocalStrings.current
    // 当前选中的筛选标签，使用 enum + Saver 保证类型安全与进程恢复。
    var selectedFilter by rememberSaveable(stateSaver = TaskFilterSaver) { mutableStateOf(TaskFilter.All) }
    // 用于触发列表重新加载的令牌；删除/重试/清空后自增。
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    // 初始加载失败时自增，触发 produceState 重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    // 是否显示批量操作底部菜单。
    var showBatchMenu by rememberSaveable { mutableStateOf(false) }
    // 是否显示清空已完成确认栏。
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    // Toast 提示文本与类型。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var toastType by rememberSaveable { mutableStateOf(ToastType.Normal) }
    // 正在处理中的任务 ID 集合，用于禁用单条操作按钮防止重复提交。
    // 使用 remember 而非 rememberSaveable：进程恢复后协程不会恢复，避免标志位永久锁定。
    var processingTaskIds by remember { mutableStateOf(setOf<Long>()) }
    // 是否正在执行批量操作，用于禁用顶部菜单按钮。
    var isBatchProcessing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // 一次性加载全部任务；运行中标签通过 refreshToken 轮询触发重新加载。
    val state = produceState<Result<List<DownloadTaskHistory>>?>(
        initialValue = null,
        downloadHistoryRepository,
        refreshToken,
        retryCount,
    ) {
        // 在 IO 线程执行数据库查询，避免主线程被 SQLite 阻塞。
        value = suspendRunCatchingNonCancel {
            withContext(Dispatchers.Default) { downloadHistoryRepository.getAllTasks() }
        }
    }

    // 通过 derivedStateOf 派生筛选后的列表：只有当底层数据或筛选条件真正改变时才会触发重组，
    // 避免运行中轮询每秒导致全量列表重组。
    val filteredTasks by remember { derivedStateOf { state.value?.getOrNull()?.filter(selectedFilter::matches) } }

    // 筛选为「运行中」时启动定时器，每秒刷新一次以跟踪下载进度与状态变化。
    LaunchedEffect(selectedFilter) {
        if (selectedFilter != TaskFilter.Running) return@LaunchedEffect
        while (true) {
            delay(1000L)
            refreshToken++
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingDownloadTask,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
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
                            onClick = { showBatchMenu = true },
                            enabled = !isBatchProcessing,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.More,
                                contentDescription = strings.menuMoreActions,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding()),
            ) {
                // 顶部筛选标签：全部 / 运行中 / 完成 / 失败。
                val filterTabs = remember(strings) {
                    TaskFilter.entries.map { it.label(strings) }
                }
                TabRow(
                    tabs = filterTabs,
                    selectedTabIndex = selectedFilter.ordinal,
                    onTabSelected = { index ->
                        selectedFilter = TaskFilter.entries.getOrElse(index) { TaskFilter.All }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                when (val result = state.value) {
                    null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    else -> when {
                        result.isSuccess -> {
                            val tasks = filteredTasks.orEmpty()
                            if (tasks.isEmpty()) {
                                EmptyPlaceholder(
                                    message = selectedFilter.emptyMessage(),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                ) {
                                    items(
                                        items = tasks,
                                        key = { it.id },
                                        contentType = { "download_task_item" },
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
                                                        suspendRunCatchingNonCancel {
                                                            downloadRepository.retry(task)
                                                        }.onSuccess {
                                                            toastMessage = strings.downloadTaskRetrySuccess
                                                            toastType = ToastType.Success
                                                            refreshToken++
                                                        }.onFailure {
                                                            toastMessage = strings.downloadTaskRetryFailed.format(it.message ?: "")
                                                            toastType = ToastType.Error
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
                                                        // 删除单条记录时显式切到后台，保持与仓库方法一致的线程语义。
                                                        suspendRunCatchingNonCancel {
                                                            withContext(Dispatchers.Default) {
                                                                downloadHistoryRepository.deleteTask(task.id)
                                                            }
                                                        }.onSuccess {
                                                            refreshToken++
                                                        }.onFailure {
                                                            toastMessage = "${strings.btnDelete}${strings.loadFailed}: ${it.message}"
                                                            toastType = ToastType.Error
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
                                // 在后台线程查询所有失败任务；没有失败任务时给出明确提示，避免误导性成功文案。
                                val failedTasks = suspendRunCatchingNonCancel {
                                    withContext(Dispatchers.Default) {
                                        downloadHistoryRepository.getTasksByStatus(DownloadStatus.Failed)
                                    }
                                }.getOrDefault(emptyList())
                                if (failedTasks.isEmpty()) {
                                    toastMessage = strings.downloadTaskEmptyFailed
                                    return@launch
                                }

                                // 逐个重试；单个失败不影响其他任务。
                                var successCount = 0
                                var failureCount = 0
                                failedTasks.forEach { task ->
                                    suspendRunCatchingNonCancel { downloadRepository.retry(task) }
                                        .onSuccess { successCount++ }
                                        .onFailure { failureCount++ }
                                }
                                toastMessage = when {
                                    failureCount == 0 -> {
                                        toastType = ToastType.Success
                                        strings.downloadTaskRetrySuccess
                                    }
                                    successCount == 0 -> {
                                        toastType = ToastType.Error
                                        strings.downloadTaskRetryFailed
                                    }
                                    else -> {
                                        toastType = ToastType.Normal
                                        "${strings.downloadTaskRetrySuccess} ($successCount), ${strings.downloadTaskRetryFailed} ($failureCount)"
                                    }
                                }
                                refreshToken++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                toastMessage = "${strings.downloadTaskRetryFailed}: ${e.message}"
                                toastType = ToastType.Error
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
                                // 在后台线程查询所有已完成任务；没有已完成任务时给出明确提示，避免误导性成功文案。
                                val completedTasks = suspendRunCatchingNonCancel {
                                    withContext(Dispatchers.Default) {
                                        downloadHistoryRepository.getTasksByStatus(DownloadStatus.Success)
                                    }
                                }.getOrDefault(emptyList())
                                if (completedTasks.isEmpty()) {
                                    toastMessage = strings.downloadTaskEmptyCompleted
                                    return@launch
                                }

                                // 数据库暂无按状态删除接口，逐条在后台线程删除。
                                completedTasks.forEach { task ->
                                    suspendRunCatchingNonCancel {
                                        withContext(Dispatchers.Default) {
                                            downloadHistoryRepository.deleteTask(task.id)
                                        }
                                    }
                                }
                                toastMessage = strings.downloadTaskEmptyCompleted
                                refreshToken++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                toastMessage = "${strings.btnDelete}: ${e.message}"
                                toastType = ToastType.Error
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
                type = toastType,
                onDismiss = { toastMessage = null },
            )
        }
    }
}

/**
 * 下载任务筛选条件，替代 Int 常量以提升类型安全。
 */
private enum class TaskFilter {
    All,
    Running,
    Completed,
    Failed,
}

private fun TaskFilter.label(strings: com.perol.pixez.shared.ui.i18n.AppStrings): String = when (this) {
    TaskFilter.All -> strings.downloadTaskFilterAll
    TaskFilter.Running -> strings.downloadTaskFilterRunning
    TaskFilter.Completed -> strings.downloadTaskFilterCompleted
    TaskFilter.Failed -> strings.downloadTaskFilterFailed
}

/**
 * 用于 [rememberSaveable] 保存/恢复 [TaskFilter] 的 [Saver]。
 */
private val TaskFilterSaver: Saver<TaskFilter, String> = Saver(
    save = { it.name },
    restore = { name -> TaskFilter.entries.find { it.name == name } ?: TaskFilter.All },
)

/**
 * 根据当前筛选条件返回空态提示文案（exhaustive when）。
 */
@Composable
private fun TaskFilter.emptyMessage(): String {
    val strings = LocalStrings.current
    return when (this) {
        TaskFilter.All -> strings.downloadTaskEmptyAll
        TaskFilter.Running -> strings.downloadTaskEmptyRunning
        TaskFilter.Completed -> strings.downloadTaskEmptyCompleted
        TaskFilter.Failed -> strings.downloadTaskEmptyFailed
    }
}

/**
 * 判断一条历史记录是否匹配当前筛选条件（exhaustive when）。
 *
 * 「运行中」同时包含正在下载与等待中的任务，避免 Pending 任务在分类视图中不可见。
 */
private fun TaskFilter.matches(task: DownloadTaskHistory): Boolean = when (this) {
    TaskFilter.All -> true
    TaskFilter.Running -> task.status == DownloadStatus.Downloading || task.status == DownloadStatus.Pending
    TaskFilter.Completed -> task.status == DownloadStatus.Success
    TaskFilter.Failed -> task.status == DownloadStatus.Failed
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val settingsRepository = com.perol.pixez.shared.data.settings.LocalSettingsRepository.current
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
                    text = "${task.userName} · #${task.pageIndex + 1}",
                    style = MiuixTheme.textStyles.footnote2,
                )
            }
            StatusIcon(
                status = task.status,
                modifier = Modifier.size(20.dp),
            )
            if (task.status == DownloadStatus.Success) {
                IconButton(
                    onClick = {
                        val settings = settingsRepository
                        val storePath = settings?.storePath
                        val basePath = storePath?.takeIf { it.isNotBlank() }
                            ?: getDefaultPictureDirectory()
                        val subDir = if (settings?.singleFolder == false && task.userName.isNotBlank() && task.userId > 0) {
                            "${FileNamePolicy.sanitizeSegment(task.userName)}_${task.userId}"
                        } else null
                        val fullDir = if (subDir != null) "$basePath/$subDir" else basePath
                        val filePath = "$fullDir/${task.fileName}"
                        FileLocator().showInFileManager(filePath)
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Show,
                        contentDescription = strings.downloadTaskShowInFolder,
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(
                onClick = onRetry,
                enabled = task.status == DownloadStatus.Failed && !isProcessing,
            ) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.retry,
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = !isProcessing,
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = strings.btnDelete,
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    when (status) {
        DownloadStatus.Success -> Icon(
            imageVector = MiuixIcons.Ok, // 完成：用 Ok 语义最接近
            contentDescription = strings.downloadStatusSuccess,
            modifier = modifier,
            tint = MiuixTheme.colorScheme.primary,
        )

        DownloadStatus.Failed -> Icon(
            imageVector = MiuixIcons.Report, // 错误：用 Report 语义最接近
            contentDescription = strings.downloadStatusFailed,
            modifier = modifier,
            tint = MiuixTheme.colorScheme.error,
        )

        DownloadStatus.Downloading -> Icon(
            imageVector = MiuixIcons.Download,
            contentDescription = strings.downloadStatusDownloading,
            modifier = modifier,
            tint = MiuixTheme.colorScheme.primary,
        )

        DownloadStatus.Pending -> Icon(
            imageVector = MiuixIcons.Stopwatch, // 等待中：用 Stopwatch 语义最接近
            contentDescription = strings.downloadStatusPending,
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
    enabled: Boolean = true,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    OverlayBottomSheet(
        show = true,
        title = strings.batchActions,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            BasicComponent(
                title = strings.retryFailedTasks,
                onClick = { if (enabled) onRetryFailed() },
            )
            BasicComponent(
                title = strings.clearCompletedTasks,
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.downloadTaskClearCompletedConfirm,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        top.yukonga.miuix.kmp.basic.TextButton(
            text = strings.cancel,
            onClick = onCancel,
        )
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onConfirm,
        ) {
            Text(strings.confirm)
        }
    }
}
