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
import com.perol.pixez.shared.platform.rememberDirectoryPicker
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BreadcrumbBar
import top.yukonga.miuix.kmp.basic.BreadcrumbItem
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

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

    var saveAfterStar by remember { mutableStateOf(settingsRepository.saveAfterStar) }
    var starAfterSave by remember { mutableStateOf(settingsRepository.starAfterSave) }
    var followAfterStar by remember { mutableStateOf(settingsRepository.followAfterStar) }
    var longPressSaveConfirm by remember { mutableStateOf(settingsRepository.longPressSaveConfirm) }
    var illustDetailSaveSkipLongPress by remember {
        mutableStateOf(settingsRepository.illustDetailSaveSkipLongPress)
    }
    var autoTagWhenStar by remember { mutableStateOf(settingsRepository.autoTagWhenStar) }

    val pickDirectory = rememberDirectoryPicker { pickedPath ->
        if (!pickedPath.isNullOrBlank()) {
            storePath = pickedPath
            settingsRepository.storePath = pickedPath
        }
    }

    val saveModeLabels = remember(strings) {
        listOf(
            "Media (${strings.downloadSaveModeMedia})",
            "SAF (${strings.downloadSaveModeSaf})",
            "${strings.qualityMedium} (${strings.downloadSaveModeLegacy})",
        )
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingDownload,
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.dialogSavePath)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ArrowPreference(
                            title = strings.dialogSavePath,
                            summary = storePath.ifEmpty { strings.noData },
                            onClick = { pickDirectory() },
                        )
                        if (storePath.isNotBlank()) {
                            val pathSegments = remember(storePath) {
                                storePath.split(Regex("[/\\\\]")).filter { it.isNotBlank() }
                            }
                            if (pathSegments.isNotEmpty()) {
                                val breadcrumbs = remember(pathSegments) {
                                    pathSegments.map { BreadcrumbItem(path = it, text = it) }
                                }
                                BreadcrumbBar(
                                    items = breadcrumbs,
                                    onItemClick = { pickDirectory() },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }
                        OverlayDropdownPreference(
                            title = strings.dialogSaveMode,
                            items = saveModeLabels,
                            selectedIndex = saveMode.coerceIn(0, saveModeLabels.lastIndex),
                            onSelectedIndexChange = { index ->
                                saveMode = index
                                settingsRepository.saveMode = index
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.dialogSaveFormat)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ArrowPreference(
                            title = strings.dialogSaveFormat,
                            summary = if (fileNameEval) strings.settingShareFormat else format,
                            onClick = { showFormatDialog = true },
                        )
                        SwitchPreference(
                            title = strings.downloadSingleFolder,
                            summary = strings.downloadSingleFolderSummary,
                            checked = singleFolder,
                            onCheckedChange = { checked ->
                                singleFolder = checked
                                settingsRepository.singleFolder = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.downloadSanityFolder,
                            summary = strings.downloadSanityFolderSummary,
                            checked = overSanityLevelFolder,
                            onCheckedChange = { checked ->
                                overSanityLevelFolder = checked
                                settingsRepository.overSanityLevelFolder = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.settingDownloadTask)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ArrowPreference(
                            title = strings.dialogTaskCount,
                            summary = maxRunningTask.toString(),
                            onClick = { showTaskDialog = true },
                        )
                        SwitchPreference(
                            title = strings.longPressSaveConfirm,
                            summary = if (longPressSaveConfirm) strings.longPressSaveConfirmSummaryOn else strings.longPressSaveConfirmSummaryOff,
                            checked = longPressSaveConfirm,
                            onCheckedChange = { checked ->
                                longPressSaveConfirm = checked
                                settingsRepository.longPressSaveConfirm = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.illustDetailSkipLongPress,
                            summary = if (illustDetailSaveSkipLongPress) strings.illustDetailSkipLongPressSummaryOn else strings.illustDetailSkipLongPressSummaryOff,
                            checked = illustDetailSaveSkipLongPress,
                            onCheckedChange = { checked ->
                                illustDetailSaveSkipLongPress = checked
                                settingsRepository.illustDetailSaveSkipLongPress = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.settingSectionBookmarkShare)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.saveAfterStar,
                            summary = if (saveAfterStar) strings.saveAfterStarSummaryOn else strings.saveAfterStarSummaryOff,
                            checked = saveAfterStar,
                            onCheckedChange = { checked ->
                                saveAfterStar = checked
                                settingsRepository.saveAfterStar = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.starAfterSave,
                            summary = if (starAfterSave) strings.starAfterSaveSummaryOn else strings.starAfterSaveSummaryOff,
                            checked = starAfterSave,
                            onCheckedChange = { checked ->
                                starAfterSave = checked
                                settingsRepository.starAfterSave = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.feedSettingFollowAfterStar,
                            summary = if (followAfterStar) strings.feedSettingFollowAfterStarSummaryOn else strings.feedSettingFollowAfterStarSummaryOff,
                            checked = followAfterStar,
                            onCheckedChange = { checked ->
                                followAfterStar = checked
                                settingsRepository.followAfterStar = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.autoTagWhenStar,
                            summary = if (autoTagWhenStar) strings.autoTagWhenStarSummaryOn else strings.autoTagWhenStarSummaryOff,
                            checked = autoTagWhenStar,
                            onCheckedChange = { checked ->
                                autoTagWhenStar = checked
                                settingsRepository.autoTagWhenStar = checked
                            },
                        )
                    }
                }
            }
        }

        val formatPlaceholderChips = listOf(
            FormatPlaceholderChip(label = strings.copyTextChipIllustId, text = "{illust_id}"),
            FormatPlaceholderChip(label = "part", text = "{part}"),
            FormatPlaceholderChip(label = strings.copyTextChipTitle, text = "{title}"),
            FormatPlaceholderChip(label = strings.copyTextChipUserId, text = "{user_id}"),
            FormatPlaceholderChip(label = strings.copyTextChipUserName, text = "{user_name}"),
        )

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

        var tempTaskCount by remember(showTaskDialog) { mutableIntStateOf(maxRunningTask) }

        // 同时下载任务数选择对话框，使用原生 NumberPicker 滚轮。
        OverlayDialog(
            title = strings.dialogTaskCount,
            show = showTaskDialog,
            onDismissRequest = { showTaskDialog = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NumberPicker(
                    value = tempTaskCount,
                    onValueChange = { tempTaskCount = it },
                    range = AppConstants.Download.TASK_COUNT_RANGE,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        text = strings.cancel,
                        onClick = { showTaskDialog = false },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = strings.confirm,
                        onClick = {
                            maxRunningTask = tempTaskCount
                            settingsRepository.maxRunningTask = tempTaskCount
                            showTaskDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
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
            style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote2,
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
