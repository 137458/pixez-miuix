package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.data.repository.MuteRepository
import com.perol.pixez.shared.data.repository.NovelHistoryRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/**
 * 应用数据导入导出页：为搜索标签历史、收藏标签、插画历史、小说历史、屏蔽数据
 * 提供导出/导入入口，操作结果通过 [ToastMessage] 提示。
 *
 * @param onBack 返回上一级页面。
 * @param settingsRepository 设置仓库，用于读写搜索历史与收藏标签。
 * @param historyRepository 插画浏览历史仓库。
 * @param novelHistoryRepository 小说浏览历史仓库。
 * @param muteRepository 屏蔽数据仓库。
 */
@Composable
fun DataExportScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    novelHistoryRepository: NovelHistoryRepository,
    muteRepository: MuteRepository,
) {
    val coroutineScope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true } }

    // 轻量提示文本；页面重建后恢复，避免用户错过结果。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // 当前弹出的导出/导入对话框；切换后重置，避免旧状态残留。
    var pendingOperation by remember { mutableStateOf<PendingOperation?>(null) }

    // 用于强制路径输入对话框每次打开都重置输入内容，避免相同 type/action 时 data class 相等导致 remember 不重置。
    var dialogKey by remember { mutableIntStateOf(0) }

    // 文件读写或仓库处理期间禁用确定按钮，防止重复提交。
    var isProcessing by remember { mutableStateOf(false) }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingDataExport,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
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
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.settingDataExport)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        DataType.entries.forEach { type ->
                            DataExportRow(
                                type = type,
                                onExportClick = {
                                    dialogKey++
                                    pendingOperation = PendingOperation(type, Action.Export)
                                },
                                onImportClick = {
                                    dialogKey++
                                    pendingOperation = PendingOperation(type, Action.Import)
                                },
                            )
                        }
                    }
                }
            }
        }

        // 路径输入对话框：平台文件选择器未就绪时，先以文本路径作为兜底方案。
        pendingOperation?.let { operation ->
            PathInputDialog(
                operation = operation,
                dialogKey = dialogKey,
                isProcessing = isProcessing,
                onDismiss = { pendingOperation = null },
                onConfirm = { path ->
                    coroutineScope.launch {
                        isProcessing = true
                        try {
                            // 文件读写与仓库操作属于阻塞或数据库操作，切到 IO 调度器执行，
                            // 结果回到主线程更新 UI 状态。
                            val result = withContext(Dispatchers.Default) {
                                if (operation.action == Action.Export) {
                                    performExport(
                                        operation.type,
                                        path,
                                        settingsRepository,
                                        historyRepository,
                                        novelHistoryRepository,
                                        muteRepository,
                                        json,
                                    )
                                } else {
                                    performImport(
                                        operation.type,
                                        path,
                                        settingsRepository,
                                        historyRepository,
                                        novelHistoryRepository,
                                        muteRepository,
                                        json,
                                    )
                                }
                            }
                            val actionStr = if (operation.action == Action.Export) strings.dataExportActionExport else strings.dataExportActionImport
                            val typeStr = operation.type.title(strings)
                            toastMessage = if (result.isSuccess) {
                                strings.dataExportSuccess.format(typeStr, actionStr)
                            } else {
                                val cause = result.exceptionOrNull()?.message ?: strings.loadFailed
                                strings.dataExportFailed.format(typeStr, actionStr, cause)
                            }
                        } finally {
                            // 页面退出或协程取消时也必须重置状态，避免对话框/按钮永久禁用。
                            isProcessing = false
                            pendingOperation = null
                        }
                    }
                },
            )
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

/**
 * 一行数据操作入口：标题 + 说明 + 导出/导入两个文本按钮。
 */
@Composable
private fun DataExportRow(
    type: DataType,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    BasicComponent(
        title = type.title(strings),
        summary = type.summary(strings),
        endActions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = strings.dataExportActionExport,
                    onClick = onExportClick,
                )
                TextButton(
                    text = strings.dataExportActionImport,
                    onClick = onImportClick,
                )
            }
        },
    )
}

/**
 * 路径输入对话框：用户输入导出/导入目标文件路径，后续可替换为系统文件选择器。
 */
@Composable
private fun PathInputDialog(
    operation: PendingOperation,
    dialogKey: Int,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val actionStr = if (operation.action == Action.Export) strings.dataExportActionExport else strings.dataExportActionImport
    val typeStr = operation.type.title(strings)

    // 对话框重新打开时重置输入内容，避免上一次的路径干扰新操作。
    var path by remember(dialogKey, operation.type, operation.action) { mutableStateOf("") }

    OverlayDialog(
        title = "$typeStr - $actionStr",
        summary = strings.dataExportPathDialogSummary.format(actionStr),
        show = true,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = path,
            onValueChange = { path = it },
            label = strings.dataExportFilePath,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = strings.cancel,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onConfirm(path) },
                enabled = path.isNotBlank() && !isProcessing,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = if (isProcessing) strings.loading else strings.confirm)
            }
        }
    }
}

private fun DataType.title(strings: com.perol.pixez.shared.ui.i18n.AppStrings): String {
    return when (this) {
        DataType.SearchTagHistory -> strings.dataExportTypeSearchHistory
        DataType.BookTags -> strings.dataExportTypeBookTags
        DataType.IllustHistory -> strings.dataExportTypeIllustHistory
        DataType.NovelHistory -> strings.dataExportTypeNovelHistory
        DataType.MuteData -> strings.dataExportTypeMuteData
    }
}

private fun DataType.summary(strings: com.perol.pixez.shared.ui.i18n.AppStrings): String {
    return when (this) {
        DataType.SearchTagHistory -> strings.dataExportTypeSearchHistorySummary
        DataType.BookTags -> strings.dataExportTypeBookTagsSummary
        DataType.IllustHistory -> strings.dataExportTypeIllustHistorySummary
        DataType.NovelHistory -> strings.dataExportTypeNovelHistorySummary
        DataType.MuteData -> strings.dataExportTypeMuteDataSummary
    }
}

/**
 * 对某类数据执行的导入或导出动作。
 */
private enum class Action {
    Export,
    Import,
}

/**
 * 待执行的导出/导入操作，用于触发路径输入对话框。
 */
private data class PendingOperation(
    val type: DataType,
    val action: Action,
)
