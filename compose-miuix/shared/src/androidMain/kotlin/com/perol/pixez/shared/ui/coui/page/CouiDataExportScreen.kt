package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalHistoryRepository
import com.perol.pixez.shared.ui.LocalMuteRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.screens.DataExportErrorCode
import com.perol.pixez.shared.ui.screens.DataExportException
import com.perol.pixez.shared.ui.screens.DataType
import com.perol.pixez.shared.ui.screens.performExport
import com.perol.pixez.shared.ui.screens.performImport
import com.perol.pixez.shared.ui.screens.pickExportFilePath
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 数据导入导出页：固定顶栏 + 单张数据类型卡片。
 *
 * 导入导出实现、文件选择器与错误码本地化完全复用 MIUIX 版；
 * 路径输入兜底改用 COUI 弹窗与原语输入框。
 */
@Composable
internal fun CouiDataExportScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val historyRepository = LocalHistoryRepository.current
    val novelHistoryRepository = LocalNovelHistoryRepository.current
    val muteRepository = LocalMuteRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()

    // 导入侧须宽松解析：含未知字段的新版本/手工编辑导出文件不应导入失败。
    val json = remember {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    }

    var pendingOperation by remember { mutableStateOf<CouiPendingOperation?>(null) }
    var showPathDialog by remember { mutableStateOf(false) }
    // 每次打开重置输入内容，避免同一操作复用旧路径。
    var dialogKey by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    suspend fun performOperation(operation: CouiPendingOperation, path: String) {
        isProcessing = true
        try {
            val result = if (operation.action == CouiExportAction.Export) {
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
            val actionStr = if (operation.action == CouiExportAction.Export) {
                strings.dataExportActionExport
            } else {
                strings.dataExportActionImport
            }
            val typeStr = operation.type.couiTitle(strings)
            if (result.isSuccess) {
                toastSink(ToastData(strings.dataExportSuccess.format(typeStr, actionStr), ToastType.Success))
            } else {
                val cause = result.exceptionOrNull()?.couiReason(strings) ?: strings.loadFailed
                toastSink(ToastData(strings.dataExportFailed.format(typeStr, actionStr, cause), ToastType.Error))
            }
        } finally {
            isProcessing = false
        }
    }

    fun startOperation(type: DataType, action: CouiExportAction) {
        dialogKey++
        pendingOperation = CouiPendingOperation(type, action)
        coroutineScope.launch {
            // 原生保存对话框优先；取消或平台未实现时回退手输路径。
            val picked = pickExportFilePath(type.couiDefaultFileName(), forSave = action == CouiExportAction.Export)
            if (picked != null) {
                performOperation(CouiPendingOperation(type, action), picked)
            } else {
                showPathDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingDataExport,
            onBack = onBack,
        )

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_data_export_list", contentType = "section") {
                    CouiSettingSection(title = strings.settingDataExport) {
                        DataType.entries.forEach { type ->
                            CouiDataExportRow(
                                type = type,
                                enabled = !isProcessing,
                                onExportClick = { startOperation(type, CouiExportAction.Export) },
                                onImportClick = { startOperation(type, CouiExportAction.Import) },
                            )
                        }
                    }
                }
            }
        }
    }

    val operation = pendingOperation
    if (showPathDialog && operation != null) {
        CouiPathInputDialog(
            operation = operation,
            dialogKey = dialogKey,
            isProcessing = isProcessing,
            onDismiss = { showPathDialog = false },
            onConfirm = { path ->
                showPathDialog = false
                coroutineScope.launch { performOperation(operation, path) }
            },
        )
    }
}

/** 一行数据操作入口：标题/说明 + 导出与导入文本动作。 */
@Composable
private fun CouiDataExportRow(
    type: DataType,
    enabled: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.couiTitle(strings),
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = type.couiSummary(strings),
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CouiTextAction(
            text = strings.dataExportActionExport,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 36.dp),
            onClick = onExportClick,
        )
        CouiTextAction(
            text = strings.dataExportActionImport,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 36.dp),
            onClick = onImportClick,
        )
    }
}

/** 路径输入弹窗：COUI 无文本输入原语，使用主题化 BasicTextField。 */
@Composable
private fun CouiPathInputDialog(
    operation: CouiPendingOperation,
    dialogKey: Int,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val actionStr = if (operation.action == CouiExportAction.Export) {
        strings.dataExportActionExport
    } else {
        strings.dataExportActionImport
    }
    val typeStr = operation.type.couiTitle(strings)
    var path by remember(dialogKey) { mutableStateOf(operation.type.couiDefaultFileName()) }

    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = "$typeStr - $actionStr",
        message = strings.dataExportPathDialogSummary.format(actionStr),
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = if (isProcessing) strings.loading else strings.confirm,
            enabled = path.isNotBlank() && !isProcessing,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = { onConfirm(path) },
        ),
        dismissAction = CouiDialogAction(
            text = strings.cancel,
            onClick = onDismiss,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                text = strings.dataExportFilePath,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
            )
            BasicTextField(
                value = path,
                onValueChange = { path = it },
                textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.accent),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

private enum class CouiExportAction {
    Export,
    Import,
}

private data class CouiPendingOperation(
    val type: DataType,
    val action: CouiExportAction,
)

private fun DataType.couiTitle(strings: AppStrings): String = when (this) {
    DataType.SearchTagHistory -> strings.dataExportTypeSearchHistory
    DataType.BookTags -> strings.dataExportTypeBookTags
    DataType.IllustHistory -> strings.dataExportTypeIllustHistory
    DataType.NovelHistory -> strings.dataExportTypeNovelHistory
    DataType.MuteData -> strings.dataExportTypeMuteData
}

private fun DataType.couiSummary(strings: AppStrings): String = when (this) {
    DataType.SearchTagHistory -> strings.dataExportTypeSearchHistorySummary
    DataType.BookTags -> strings.dataExportTypeBookTagsSummary
    DataType.IllustHistory -> strings.dataExportTypeIllustHistorySummary
    DataType.NovelHistory -> strings.dataExportTypeNovelHistorySummary
    DataType.MuteData -> strings.dataExportTypeMuteDataSummary
}

private fun DataType.couiDefaultFileName(): String = when (this) {
    DataType.SearchTagHistory -> "pixez_search_tags.json"
    DataType.BookTags -> "pixez_book_tags.json"
    DataType.IllustHistory -> "pixez_illust_history.json"
    DataType.NovelHistory -> "pixez_novel_history.json"
    DataType.MuteData -> "pixez_mute_data.json"
}

private fun Throwable.couiReason(strings: AppStrings): String = when (this) {
    is DataExportException -> code.couiLocalized(strings)
    else -> message ?: strings.loadFailed
}

private fun DataExportErrorCode.couiLocalized(strings: AppStrings): String = when (this) {
    DataExportErrorCode.PathBlank -> strings.dataExportErrorPathBlank
    DataExportErrorCode.PathUnsupportedExtension -> strings.dataExportErrorPathExtension
    DataExportErrorCode.PathOutsideExportDir -> strings.dataExportErrorPathOutside
    DataExportErrorCode.BaseDirUnavailable -> strings.dataExportErrorBaseDirUnavailable
    DataExportErrorCode.ImportFileTooLarge -> strings.dataExportErrorFileTooLarge
    DataExportErrorCode.ImportItemCountExceeded -> strings.dataExportErrorItemCountExceeded
    DataExportErrorCode.ImportFieldTooLong,
    DataExportErrorCode.ImportIllegalCharacter,
    DataExportErrorCode.ImportNegativeValue -> strings.dataExportErrorFieldInvalid
    DataExportErrorCode.ImportMalformedContent -> strings.dataExportErrorMalformedContent
    DataExportErrorCode.IoFailure -> strings.dataExportErrorIoFailed
}
