package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * 下载设置页：管理保存路径、保存模式、保存格式、脚本文件名、
 * Sanity 文件夹、同时下载任务数、单文件夹模式。
 *
 * @param settingsRepository 设置仓库，用于读写下载相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun DownloadSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current

    // 页面状态：从 SettingsRepository 读取当前各项下载设置。
    var storePath by remember { mutableStateOf(settingsRepository.storePath.orEmpty()) }
    var saveMode by remember { mutableIntStateOf(settingsRepository.saveMode) }
    var format by remember { mutableStateOf(settingsRepository.format) }
    var fileNameEval by remember { mutableStateOf(settingsRepository.fileNameEval) }
    var overSanityLevelFolder by remember { mutableStateOf(settingsRepository.overSanityLevelFolder) }
    var maxRunningTask by remember { mutableIntStateOf(settingsRepository.maxRunningTask) }
    var singleFolder by remember { mutableStateOf(settingsRepository.singleFolder) }

    // 对话框显隐状态。
    var showPathDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveModeDialog by rememberSaveable { mutableStateOf(false) }
    var showFormatDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }

    // 保存路径输入框状态，打开对话框时与当前设置同步。
    var pathInput by remember(showPathDialog) { mutableStateOf(storePath) }

    // 保存格式输入框状态，使用 TextFieldValue 以便跟踪光标/选区。
    var formatFieldValue by remember(showFormatDialog) {
        mutableStateOf(
            TextFieldValue(
                text = format,
                selection = TextRange(format.length),
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingDownload,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = strings.dialogSavePath)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.dialogSavePath,
                        summary = storePath.ifEmpty { strings.noData },
                        onClick = {
                            pathInput = storePath
                            showPathDialog = true
                        },
                    )
                }
            }

            item {
                SmallTitle(text = strings.dialogSaveMode)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.dialogSaveMode,
                        summary = saveMode.toSaveModeLabel(),
                        onClick = { showSaveModeDialog = true },
                    )
                }
            }

            item {
                SmallTitle(text = strings.dialogSaveFormat)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.dialogSaveFormat,
                        summary = if (fileNameEval) strings.settingShareFormat else format,
                        onClick = { showFormatDialog = true },
                    )
                }
            }

            item {
                SmallTitle(text = strings.settingDownloadTask)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.dialogTaskCount,
                        summary = maxRunningTask.toString(),
                        onClick = { showTaskDialog = true },
                    )
                }
            }

            item {
                SmallTitle(text = strings.settingSectionStorage)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.downloadSingleFolder,
                        summary = strings.downloadSingleFolderSummary,
                        endActions = {
                            Switch(
                                checked = singleFolder,
                                onCheckedChange = { checked ->
                                    singleFolder = checked
                                    settingsRepository.singleFolder = checked
                                },
                            )
                        },
                    )
                    BasicComponent(
                        title = strings.downloadSanityFolder,
                        summary = strings.downloadSanityFolderSummary,
                        endActions = {
                            Switch(
                                checked = overSanityLevelFolder,
                                onCheckedChange = { checked ->
                                    overSanityLevelFolder = checked
                                    settingsRepository.overSanityLevelFolder = checked
                                },
                            )
                        },
                    )
                }
            }
        }

        val saveModeOptions = listOf(
            SaveModeOption(0, "Media", strings.downloadSaveModeMedia),
            SaveModeOption(1, "SAF", strings.downloadSaveModeSaf),
            SaveModeOption(2, strings.qualityMedium, strings.downloadSaveModeLegacy),
        )

        val formatPlaceholderChips = listOf(
            FormatPlaceholderChip(label = strings.copyTextChipIllustId, text = "{illust_id}"),
            FormatPlaceholderChip(label = "part", text = "{part}"),
            FormatPlaceholderChip(label = strings.copyTextChipTitle, text = "{title}"),
            FormatPlaceholderChip(label = strings.copyTextChipUserId, text = "{user_id}"),
            FormatPlaceholderChip(label = strings.copyTextChipUserName, text = "{user_name}"),
        )

        // 保存路径编辑对话框。
        OverlayDialog(
            title = strings.dialogSavePath,
            show = showPathDialog,
            onDismissRequest = { showPathDialog = false },
        ) {
            TextField(
                value = pathInput,
                onValueChange = { pathInput = it },
                label = strings.dialogSavePath,
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
                    onClick = { showPathDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = strings.confirm,
                    onClick = {
                        val trimmed = pathInput.trim()
                        storePath = trimmed
                        settingsRepository.storePath = trimmed.takeIf { it.isNotEmpty() }
                        showPathDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        // 保存模式三选一对话框。
        OverlayDialog(
            title = strings.dialogSaveMode,
            show = showSaveModeDialog,
            onDismissRequest = { showSaveModeDialog = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                saveModeOptions.forEach { option ->
                    BasicComponent(
                        title = option.label,
                        summary = option.description,
                        onClick = {
                            saveMode = option.value
                            settingsRepository.saveMode = option.value
                            showSaveModeDialog = false
                        },
                        endActions = {
                            CheckIndicator(selected = saveMode == option.value)
                        },
                    )
                }
            }
        }

        // 保存格式编辑对话框，支持变量占位符快捷插入。
        OverlayDialog(
            title = strings.dialogSaveFormat,
            show = showFormatDialog,
            onDismissRequest = { showFormatDialog = false },
        ) {
            TextField(
                value = formatFieldValue,
                onValueChange = { formatFieldValue = it },
                label = strings.dialogSaveFormat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                formatPlaceholderChips.forEach { chip ->
                    FormatInsertChip(
                        label = chip.label,
                        onClick = {
                            formatFieldValue = insertTextAtSelection(
                                formatFieldValue,
                                chip.text,
                            )
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = strings.cancel,
                    onClick = { showFormatDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = strings.confirm,
                    onClick = {
                        val trimmed = formatFieldValue.text.trim()
                        format = trimmed
                        settingsRepository.format = trimmed
                        showFormatDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        // 同时下载任务数选择对话框。
        OverlayDialog(
            title = strings.dialogTaskCount,
            show = showTaskDialog,
            onDismissRequest = { showTaskDialog = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppConstants.Download.MAX_TASK_OPTIONS.forEach { value ->
                    BasicComponent(
                        title = value.toString(),
                        onClick = {
                            maxRunningTask = value
                            settingsRepository.maxRunningTask = value
                            showTaskDialog = false
                        },
                        endActions = {
                            CheckIndicator(selected = maxRunningTask == value)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 占位符 chip 数据：展示文案与插入文本。
 */
private data class FormatPlaceholderChip(
    val label: String,
    val text: String,
)

/**
 * 占位符 chip：点击后在文本框当前光标/选区处插入对应文本。
 */
@Composable
private fun FormatInsertChip(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(),
        minHeight = 32.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
        )
    }
}

/**
 * 在 TextFieldValue 的当前选区位置插入文本。
 * 若存在选区则替换选区内容，否则在光标处插入。
 * 插入完成后将光标置于插入文本末尾。
 */
private fun insertTextAtSelection(
    textFieldValue: TextFieldValue,
    insertText: String,
): TextFieldValue {
    val selection = textFieldValue.selection
    val newText = textFieldValue.text.replaceRange(
        selection.start,
        selection.end,
        insertText,
    )
    val newCursor = selection.start + insertText.length
    return textFieldValue.copy(
        text = newText,
        selection = TextRange(newCursor, newCursor),
    )
}

/**
 * 保存模式选项：取值与原 Flutter 版一致。
 */
private data class SaveModeOption(
    val value: Int,
    val label: String,
    val description: String,
)

/**
 * 将保存模式数值转换为显示文案。
 */
private fun Int.toSaveModeLabel(): String = when (this) {
    0 -> "Media"
    1 -> "SAF"
    2 -> "Legacy"
    else -> "Media"
}
