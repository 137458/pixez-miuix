package com.perol.pixez.shared.ui.coui.page

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.platform.FileLocator
import com.perol.pixez.shared.platform.FileNamePolicy
import com.perol.pixez.shared.platform.getDefaultPictureDirectory
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalDownloadHistoryRepository
import com.perol.pixez.shared.ui.LocalDownloadRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.screens.emptyMessage
import com.perol.pixez.shared.ui.screens.label
import com.perol.pixez.shared.ui.screens.matches
import com.perol.pixez.shared.ui.screens.TaskFilter
import com.perol.pixez.shared.ui.screens.TaskFilterSaver
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 下载任务页：固定顶栏 + 全部/运行中/完成/失败胶囊筛选 + 任务卡片行。
 *
 * 数据编排与 MIUIX 版完全一致：按当前筛选查询一次，用 [derivedStateOf] 派生子列表；
 * 下载仓库任务事件流经 conflate 合并后触发刷新。差异：
 * 批量操作从底部弹层改为 COUI 锚点菜单，清空已完成从底部确认栏改为 COUI `Dialog`；
 * 运行中任务的不确定进度条按 COUI 令牌自绘，表头筛选与列表收敛到平板可读宽度。
 */
@Composable
internal fun CouiDownloadTaskScreen(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val downloadRepository = LocalDownloadRepository.current
    val downloadHistoryRepository = LocalDownloadHistoryRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by rememberSaveable(stateSaver = TaskFilterSaver) { mutableStateOf(TaskFilter.All) }
    // 删除/重试/清空后自增，触发列表重新加载。
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    // 初始加载失败时自增，触发 produceState 重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    // 进行中的单条任务 ID 与批量标志：进程恢复后协程不会恢复，故用 remember 防止永久锁定。
    var processingTaskIds by remember { mutableStateOf(setOf<Long>()) }
    var isBatchProcessing by remember { mutableStateOf(false) }

    val state = produceState<Result<List<DownloadTaskHistory>>?>(
        initialValue = null,
        downloadHistoryRepository,
        selectedFilter,
        refreshToken,
        retryCount,
    ) {
        // 在后台线程执行数据库查询，避免主线程被 SQLite 阻塞。
        value = suspendRunCatchingNonCancel {
            withContext(Dispatchers.Default) {
                if (selectedFilter == TaskFilter.Running) {
                    downloadHistoryRepository.getTasksByStatus(DownloadStatus.Downloading) +
                        downloadHistoryRepository.getTasksByStatus(DownloadStatus.Pending)
                } else {
                    downloadHistoryRepository.getAllTasks()
                }
            }
        }
    }

    val filteredTasks by remember(selectedFilter) {
        derivedStateOf { state.value?.getOrNull()?.filter(selectedFilter::matches) }
    }

    // 收集下载仓库的任务状态变更事件驱动刷新，替代秒级全表轮询。
    LaunchedEffect(downloadRepository) {
        downloadRepository.taskEventFlow.conflate().collect { refreshToken++ }
    }

    val filterChips = remember(strings) {
        TaskFilter.entries.map {
            CouiFilterChip(key = it.name, label = it.label(strings))
        }
    }

    /** 逐条重试失败任务：单个失败不影响其他任务，结束后给出汇总提示。 */
    fun retryAllFailed() {
        if (isBatchProcessing) return
        isBatchProcessing = true
        coroutineScope.launch {
            try {
                val failedTasks = suspendRunCatchingNonCancel {
                    withContext(Dispatchers.Default) {
                        downloadHistoryRepository.getTasksByStatus(DownloadStatus.Failed)
                    }
                }.getOrDefault(emptyList())
                if (failedTasks.isEmpty()) {
                    toastSink(ToastData(strings.downloadTaskEmptyFailed, ToastType.Normal))
                    return@launch
                }

                var successCount = 0
                var failureCount = 0
                failedTasks.forEach { task ->
                    suspendRunCatchingNonCancel { downloadRepository.retry(task) }
                        .onSuccess { successCount++ }
                        .onFailure { failureCount++ }
                }
                toastSink(
                    when {
                        failureCount == 0 ->
                            ToastData(strings.downloadTaskRetrySuccess, ToastType.Success)

                        successCount == 0 ->
                            ToastData(strings.downloadTaskRetryFailed.format("$failureCount"), ToastType.Error)

                        else ->
                            ToastData(
                                "${strings.downloadTaskRetrySuccess} ($successCount), " +
                                    strings.downloadTaskRetryFailed.format("$failureCount"),
                                ToastType.Normal,
                            )
                    },
                )
                refreshToken++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toastSink(
                    ToastData(
                        strings.downloadTaskRetryFailed.format(PixivApiErrorFormatter.format(e, strings)),
                        ToastType.Error,
                    ),
                )
            } finally {
                isBatchProcessing = false
            }
        }
    }

    /** 清空已完成任务：数据库暂无按状态删除接口，逐条在后台线程删除。 */
    fun clearCompleted() {
        if (isBatchProcessing) return
        isBatchProcessing = true
        coroutineScope.launch {
            try {
                val completedTasks = suspendRunCatchingNonCancel {
                    withContext(Dispatchers.Default) {
                        downloadHistoryRepository.getTasksByStatus(DownloadStatus.Success)
                    }
                }.getOrDefault(emptyList())
                if (completedTasks.isEmpty()) {
                    toastSink(ToastData(strings.downloadTaskEmptyCompleted, ToastType.Normal))
                    return@launch
                }

                completedTasks.forEach { task ->
                    suspendRunCatchingNonCancel {
                        withContext(Dispatchers.Default) {
                            downloadHistoryRepository.deleteTask(task.id)
                        }
                    }
                }
                toastSink(ToastData(strings.downloadTaskEmptyCompleted, ToastType.Success))
                refreshToken++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toastSink(
                    ToastData(
                        "${strings.btnDelete}: ${PixivApiErrorFormatter.format(e, strings)}",
                        ToastType.Error,
                    ),
                )
            } finally {
                isBatchProcessing = false
            }
        }
    }

    val menuAnchor = rememberCouiMenuAnchor()
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingDownloadTask,
            onBack = onBack,
        ) {
            Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                CouiActionButton(
                    icon = MiuixIcons.More,
                    contentDescription = strings.menuMoreActions,
                    onClick = { menuAnchor.open { menuExpanded = true } },
                    enabled = !isBatchProcessing,
                )
                Menu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    anchor = menuAnchor,
                    headerText = strings.batchActions,
                    items = listOf(
                        CouiMenuItem(value = "retry_failed", text = strings.retryFailedTasks),
                        CouiMenuItem(value = "clear_completed", text = strings.clearCompletedTasks),
                    ),
                    onItemClick = { item ->
                        menuExpanded = false
                        if (isBatchProcessing) return@Menu
                        when (item.value) {
                            "retry_failed" -> retryAllFailed()
                            "clear_completed" -> showClearConfirm = true
                        }
                    },
                )
            }
        }

        // 筛选与任务列表收敛到平板可读宽度，与 MIUIX 版一致。
        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            CouiFilterChipBar(
                chips = filterChips,
                selectedKey = selectedFilter.name,
                onSelect = { key ->
                    selectedFilter = TaskFilter.entries.firstOrNull { it.name == key } ?: TaskFilter.All
                },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val result = state.value) {
                    null -> CouiStatePlaceholder(
                        message = strings.loading,
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> when {
                        result.isSuccess -> {
                            val tasks = filteredTasks.orEmpty()
                            if (tasks.isEmpty()) {
                                CouiStatePlaceholder(
                                    message = selectedFilter.emptyMessage(),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                CouiDownloadTaskList(
                                    tasks = tasks,
                                    isBatchProcessing = isBatchProcessing,
                                    processingTaskIds = processingTaskIds,
                                    onClick = { onIllustClick(it.illustId) },
                                    onRetry = { task ->
                                        if (processingTaskIds.contains(task.id)) return@CouiDownloadTaskList
                                        processingTaskIds = processingTaskIds + task.id
                                        coroutineScope.launch {
                                            try {
                                                suspendRunCatchingNonCancel {
                                                    downloadRepository.retry(task)
                                                }.onSuccess {
                                                    toastSink(
                                                        ToastData(strings.downloadTaskRetrySuccess, ToastType.Success),
                                                    )
                                                    refreshToken++
                                                }.onFailure {
                                                    toastSink(
                                                        ToastData(
                                                            strings.downloadTaskRetryFailed.format(
                                                                PixivApiErrorFormatter.format(it, strings),
                                                            ),
                                                            ToastType.Error,
                                                        ),
                                                    )
                                                }
                                            } finally {
                                                processingTaskIds = processingTaskIds - task.id
                                            }
                                        }
                                    },
                                    onDelete = { task ->
                                        if (processingTaskIds.contains(task.id)) return@CouiDownloadTaskList
                                        processingTaskIds = processingTaskIds + task.id
                                        coroutineScope.launch {
                                            try {
                                                suspendRunCatchingNonCancel {
                                                    withContext(Dispatchers.Default) {
                                                        downloadHistoryRepository.deleteTask(task.id)
                                                    }
                                                }.onSuccess {
                                                    refreshToken++
                                                }.onFailure {
                                                    toastSink(
                                                        ToastData(
                                                            "${strings.btnDelete}${strings.failedSuffix}: " +
                                                                PixivApiErrorFormatter.format(it, strings),
                                                            ToastType.Error,
                                                        ),
                                                    )
                                                }
                                            } finally {
                                                processingTaskIds = processingTaskIds - task.id
                                            }
                                        }
                                    },
                                )
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
    }

    if (showClearConfirm) {
        val clearing = isBatchProcessing
        Dialog(
            onDismissRequest = { if (!clearing) showClearConfirm = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.clearCompletedTasks,
            message = strings.downloadTaskClearCompletedConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                enabled = !clearing,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    showClearConfirm = false
                    clearCompleted()
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { if (!isBatchProcessing) showClearConfirm = false },
            ),
        )
    }
}

@Composable
private fun CouiDownloadTaskList(
    tasks: List<DownloadTaskHistory>,
    isBatchProcessing: Boolean,
    processingTaskIds: Set<Long>,
    onClick: (DownloadTaskHistory) -> Unit,
    onRetry: (DownloadTaskHistory) -> Unit,
    onDelete: (DownloadTaskHistory) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        items(
            items = tasks,
            key = { it.id },
            contentType = { "coui_download_task" },
        ) { task ->
            CouiDownloadTaskRow(
                task = task,
                isProcessing = processingTaskIds.contains(task.id) || isBatchProcessing,
                onClick = { onClick(task) },
                onRetry = { onRetry(task) },
                onDelete = { onDelete(task) },
            )
        }
    }
}

/** 单条下载任务卡：缩略图、标题、状态、操作按钮，运行中任务底部带不确定进度条。 */
@Composable
private fun CouiDownloadTaskRow(
    task: DownloadTaskHistory,
    isProcessing: Boolean,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val destructive = CouiDialogDefaults.colors(CouiTheme.isDark).destructive
    val settingsRepository = LocalSettingsRepository.current
    val pressInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .couiPressFeedback(pressInteraction, enabled = !isProcessing)
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null,
                    enabled = !isProcessing,
                    onClick = onClick,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            }
            CouiDownloadStatusIcon(
                status = task.status,
                modifier = Modifier.size(20.dp),
            )
            if (task.status == DownloadStatus.Success) {
                CouiActionButton(
                    icon = MiuixIcons.Show,
                    contentDescription = strings.downloadTaskShowInFolder,
                    tint = scheme.accent,
                    onClick = {
                        val settings = settingsRepository
                        val storePath = settings.storePath
                        val basePath = storePath?.takeIf { it.isNotBlank() }
                            ?: getDefaultPictureDirectory()
                        val subDir = if (
                            settings.singleFolder == false &&
                            task.userName.isNotBlank() &&
                            task.userId > 0
                        ) {
                            "${FileNamePolicy.sanitizeSegment(task.userName)}_${task.userId}"
                        } else {
                            null
                        }
                        val fullDir = if (subDir != null) "$basePath/$subDir" else basePath
                        FileLocator().showInFileManager("$fullDir/${task.fileName}")
                    },
                )
            }
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.retry,
                enabled = task.status == DownloadStatus.Failed && !isProcessing,
                onClick = onRetry,
            )
            CouiActionButton(
                icon = MiuixIcons.Delete,
                contentDescription = strings.btnDelete,
                enabled = !isProcessing,
                tint = destructive,
                onClick = onDelete,
            )
        }

        // 下载器未暴露字节级进度，用不确定进度条表示正在进行。
        if (task.status == DownloadStatus.Downloading) {
            CouiIndeterminateProgressBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
            )
        }
    }
}

/** 状态图标映射：成功/失败/运行中/等待中。 */
@Composable
private fun CouiDownloadStatusIcon(
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val destructive = CouiDialogDefaults.colors(CouiTheme.isDark).destructive
    val icon = when (status) {
        DownloadStatus.Success -> MiuixIcons.Ok
        DownloadStatus.Failed -> MiuixIcons.Report
        DownloadStatus.Downloading -> MiuixIcons.Download
        DownloadStatus.Pending -> MiuixIcons.Stopwatch
    }
    val description = when (status) {
        DownloadStatus.Success -> strings.downloadStatusSuccess
        DownloadStatus.Failed -> strings.downloadStatusFailed
        DownloadStatus.Downloading -> strings.downloadStatusDownloading
        DownloadStatus.Pending -> strings.downloadStatusPending
    }
    val tint = when (status) {
        DownloadStatus.Success, DownloadStatus.Downloading -> scheme.accent
        DownloadStatus.Failed -> destructive
        DownloadStatus.Pending -> scheme.onSurfaceVariant
    }
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

/** COUI 令牌下的不确定进度条：容器轨道 + 循环推进的强调色片段。 */
@Composable
private fun CouiIndeterminateProgressBar(modifier: Modifier = Modifier) {
    val scheme = CouiTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "coui_download_progress")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
        ),
        label = "coui_download_progress_phase",
    )
    Canvas(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.container),
    ) {
        val segmentWidth = size.width * 0.35f
        val startX = (size.width + segmentWidth) * phase - segmentWidth
        drawRoundRect(
            color = scheme.accent,
            topLeft = Offset(startX, 0f),
            size = Size(segmentWidth, size.height),
            cornerRadius = CornerRadius(size.height / 2f),
        )
    }
}
