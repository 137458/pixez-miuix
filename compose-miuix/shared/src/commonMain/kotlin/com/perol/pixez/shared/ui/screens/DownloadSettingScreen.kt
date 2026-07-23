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
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

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
    // 页面状态：从 SettingsRepository 读取当前下载设置。
    var storePath by remember { mutableStateOf(settingsRepository.storePath ?: "") }
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
                title = "下载设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
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
                SmallTitle(text = "保存路径")
            }
            item {
                BasicComponent(
                    title = "保存路径",
                    summary = storePath.ifEmpty { "未设置" },
                    onClick = {
                        pathInput = storePath
                        showPathDialog = true
                    },
                )
            }

            item {
                SmallTitle(text = "保存模式")
            }
            item {
                BasicComponent(
                    title = "保存模式",
                    summary = saveMode.toSaveModeLabel(),
                    onClick = { showSaveModeDialog = true },
                )
            }

            item {
                SmallTitle(text = "保存格式")
            }
            item {
                BasicComponent(
                    title = "保存格式",
                    summary = if (fileNameEval) "脚本文件名" else format,
                    onClick = { showFormatDialog = true },
                )
            }

            item {
                SmallTitle(text = "脚本文件名")
            }
            item {
                BasicComponent(
                    title = "使用脚本文件名",
                    summary = "由 name_eval 脚本计算保存文件名",
                    endActions = {
                        Switch(
                            checked = fileNameEval,
                            onCheckedChange = { checked ->
                                fileNameEval = checked
                                settingsRepository.fileNameEval = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "下载任务")
            }
            item {
                BasicComponent(
                    title = "同时下载任务数",
                    summary = maxRunningTask.toString(),
                    onClick = { showTaskDialog = true },
                )
            }

            item {
                SmallTitle(text = "文件夹")
            }
            item {
                BasicComponent(
                    title = "单文件夹模式",
                    summary = "所有图片保存到同一文件夹",
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
            }
            item {
                BasicComponent(
                    title = "Sanity 单独文件夹",
                    summary = "R18 作品保存到独立文件夹",
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

        // 保存路径编辑对话框。
        SuperDialog(
            title = "保存路径",
            show = showPathDialog,
            onDismissRequest = { showPathDialog = false },
        ) {
            TextField(
                value = pathInput,
                onValueChange = { pathInput = it },
                label = "输入保存路径",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showPathDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "确认",
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
        SuperDialog(
            title = "保存模式",
            show = showSaveModeDialog,
            onDismissRequest = { showSaveModeDialog = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SAVE_MODE_OPTIONS.forEach { option ->
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
        SuperDialog(
            title = "保存格式",
            show = showFormatDialog,
            onDismissRequest = { showFormatDialog = false },
        ) {
            TextField(
                value = formatFieldValue,
                onValueChange = { formatFieldValue = it },
                label = "输入文件命名格式",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FORMAT_PLACEHOLDER_CHIPS.forEach { chip ->
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
                    text = "取消",
                    onClick = { showFormatDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "确认",
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
        SuperDialog(
            title = "同时下载任务数",
            show = showTaskDialog,
            onDismissRequest = { showTaskDialog = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MAX_RUNNING_TASK_RANGE.forEach { value ->
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
    2 -> "旧模式"
    else -> "未知"
}

/**
 * 同时下载任务数可选范围：1-5，与原应用常见取值一致。
 */
private val MAX_RUNNING_TASK_RANGE = 1..5

/**
 * 保存模式可选项：Media / SAF / 旧模式。
 */
private val SAVE_MODE_OPTIONS = listOf(
    SaveModeOption(0, "Media", "使用 MediaStore 保存到系统相册"),
    SaveModeOption(1, "SAF", "使用存储访问框架选择目录"),
    SaveModeOption(2, "旧模式", "传统直接写入存储路径"),
)

/**
 * 保存格式可用变量占位符。
 */
private val FORMAT_PLACEHOLDER_CHIPS = listOf(
    FormatPlaceholderChip(label = "作品ID", text = "{illust_id}"),
    FormatPlaceholderChip(label = "分段", text = "{part}"),
    FormatPlaceholderChip(label = "标题", text = "{title}"),
    FormatPlaceholderChip(label = "用户ID", text = "{user_id}"),
    FormatPlaceholderChip(label = "画师名", text = "{user_name}"),
)
