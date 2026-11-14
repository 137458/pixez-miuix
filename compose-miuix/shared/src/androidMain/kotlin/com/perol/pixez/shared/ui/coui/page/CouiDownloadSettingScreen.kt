package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.platform.rememberDirectoryPicker
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSelectRow
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.insertTextAtSelection
import io.github.aakira.napier.Napier
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 下载设置页：固定顶栏 + 三组卡片（路径、保存格式、收藏联动）。
 *
 * 保存路径选择、面包屑逐级回退、格式模板编辑与全部开关语义保持不变；
 * COUI 库无原生开关/下拉/输入，分别使用共用 `CouiSwitchRow`、
 * `CouiSelectRow` 与按主题令牌自绘的输入框。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiDownloadSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme

    var storePath by remember { mutableStateOf(settingsRepository.storePath.orEmpty()) }
    val effectiveStorePath = remember(storePath) {
        storePath.ifBlank { com.perol.pixez.shared.platform.getDefaultPictureDirectory() }
    }
    var format by remember { mutableStateOf(settingsRepository.format) }
    var fileNameEval by remember { mutableStateOf(settingsRepository.fileNameEval) }
    var singleFolder by remember { mutableStateOf(settingsRepository.singleFolder) }
    var overSanityLevelFolder by remember { mutableStateOf(settingsRepository.overSanityLevelFolder) }
    var maxRunningTask by remember {
        mutableIntStateOf(DownloadRepository.coerceParallelTasks(settingsRepository.maxRunningTask))
    }
    val taskCountOptions = remember { DownloadRepository.PARALLEL_TASK_OPTIONS.map(Int::toString) }

    var showFormatDialog by rememberSaveable { mutableStateOf(false) }

    var defaultPrivateLike by remember { mutableStateOf(settingsRepository.defaultPrivateLike) }
    var autoTagWhenStar by remember { mutableStateOf(settingsRepository.autoTagWhenStar) }
    var followAfterStar by remember { mutableStateOf(settingsRepository.followAfterStar) }
    var saveAfterStar by remember { mutableStateOf(settingsRepository.saveAfterStar) }
    var starAfterSave by remember { mutableStateOf(settingsRepository.starAfterSave) }

    val pickDirectory = rememberDirectoryPicker(
        initialDirectory = storePath,
        dialogTitle = strings.dialogSavePath,
    ) { pickedPath ->
        if (!pickedPath.isNullOrBlank()) {
            storePath = pickedPath
            settingsRepository.storePath = pickedPath
        }
    }

    // 面包屑：把当前路径拆成可点击前缀，保留 MIUIX 版的逐级回退能力。
    val breadcrumbs = remember(effectiveStorePath) {
        val isWindows = effectiveStorePath.contains('\\') ||
            (effectiveStorePath.length >= 2 && effectiveStorePath[1] == ':')
        val separator = if (isWindows) "\\" else "/"
        val rawSegments = effectiveStorePath.split('/', '\\').filter { it.isNotBlank() }
        val items = mutableListOf<Pair<String, String>>()
        var currentAcc = if (!isWindows && effectiveStorePath.startsWith("/")) "/" else ""
        rawSegments.forEach { seg ->
            currentAcc = if (currentAcc.isEmpty() || currentAcc == "/" || currentAcc.endsWith("\\")) {
                currentAcc + seg
            } else {
                currentAcc + separator + seg
            }
            items.add(seg to currentAcc)
        }
        items
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.settingDownload,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = LocalBottomBarContentPadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_download_path", contentType = "section") {
                    CouiSettingSection(title = strings.dialogSavePath) {
                        CouiNavigationRow(
                            title = strings.dialogSavePath,
                            summary = effectiveStorePath,
                            onClick = { pickDirectory() },
                        )
                        if (breadcrumbs.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                breadcrumbs.forEach { (segment, path) ->
                                    CouiAssistChip(
                                        text = segment,
                                        selected = path == effectiveStorePath,
                                        onClick = {
                                            if (path.isNotBlank() && path != effectiveStorePath) {
                                                storePath = path
                                                settingsRepository.storePath = path
                                            } else {
                                                pickDirectory()
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        CouiNavigationRow(
                            title = strings.downloadTaskShowInFolder,
                            summary = effectiveStorePath,
                            onClick = {
                                runCatching {
                                    okio.FileSystem.SYSTEM.createDirectories(
                                        with(okio.Path) { effectiveStorePath.toPath() },
                                    )
                                    com.perol.pixez.shared.platform.FileLocator()
                                        .showInFileManager(effectiveStorePath)
                                }.onFailure {
                                    Napier.e("打开所在文件夹失败 storePath=$effectiveStorePath", it)
                                }
                            },
                        )
                        CouiSwitchRow(
                            title = strings.downloadSingleFolder,
                            summary = strings.downloadSingleFolderSummary,
                            checked = singleFolder,
                            onCheckedChange = { checked ->
                                singleFolder = checked
                                settingsRepository.singleFolder = checked
                            },
                        )
                        CouiSwitchRow(
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

                item(key = "coui_download_format", contentType = "section") {
                    CouiSettingSection(title = strings.dialogSaveFormat) {
                        CouiNavigationRow(
                            title = strings.dialogSaveFormat,
                            summary = if (fileNameEval) strings.settingShareFormat else format,
                            onClick = { showFormatDialog = true },
                        )
                        CouiSelectRow(
                            title = strings.dialogTaskCount,
                            items = taskCountOptions,
                            selectedIndex = (maxRunningTask - 1).coerceIn(0, taskCountOptions.lastIndex),
                            onSelect = { index ->
                                val count = index + 1
                                maxRunningTask = count
                                settingsRepository.maxRunningTask = count
                            },
                        )
                    }
                }

                item(key = "coui_download_bookmark", contentType = "section") {
                    CouiSettingSection(title = strings.settingSectionBookmarkShare) {
                        CouiSwitchRow(
                            title = strings.defaultPrivateLike,
                            summary = if (defaultPrivateLike) {
                                strings.defaultPrivateLikeSummaryOn
                            } else {
                                strings.defaultPrivateLikeSummaryOff
                            },
                            checked = defaultPrivateLike,
                            onCheckedChange = { checked ->
                                defaultPrivateLike = checked
                                settingsRepository.defaultPrivateLike = checked
                            },
                        )
                        CouiSwitchRow(
                            title = strings.autoTagWhenStar,
                            summary = if (autoTagWhenStar) {
                                strings.autoTagWhenStarSummaryOn
                            } else {
                                strings.autoTagWhenStarSummaryOff
                            },
                            checked = autoTagWhenStar,
                            onCheckedChange = { checked ->
                                autoTagWhenStar = checked
                                settingsRepository.autoTagWhenStar = checked
                            },
                        )
                        CouiSwitchRow(
                            title = strings.feedSettingFollowAfterStar,
                            summary = if (followAfterStar) {
                                strings.feedSettingFollowAfterStarSummaryOn
                            } else {
                                strings.feedSettingFollowAfterStarSummaryOff
                            },
                            checked = followAfterStar,
                            onCheckedChange = { checked ->
                                followAfterStar = checked
                                settingsRepository.followAfterStar = checked
                            },
                        )
                        CouiSwitchRow(
                            title = strings.saveAfterStar,
                            summary = if (saveAfterStar) {
                                strings.saveAfterStarSummaryOn
                            } else {
                                strings.saveAfterStarSummaryOff
                            },
                            checked = saveAfterStar,
                            onCheckedChange = { checked ->
                                saveAfterStar = checked
                                settingsRepository.saveAfterStar = checked
                            },
                        )
                        CouiSwitchRow(
                            title = strings.starAfterSave,
                            summary = if (starAfterSave) {
                                strings.starAfterSaveSummaryOn
                            } else {
                                strings.starAfterSaveSummaryOff
                            },
                            checked = starAfterSave,
                            onCheckedChange = { checked ->
                                starAfterSave = checked
                                settingsRepository.starAfterSave = checked
                            },
                        )
                    }
                }
            }
        }
    }

    if (showFormatDialog) {
        CouiSaveFormatDialog(
            initialFormat = format,
            onDismiss = { showFormatDialog = false },
            onConfirm = { trimmed ->
                format = trimmed
                settingsRepository.format = trimmed
                showFormatDialog = false
            },
        )
    }
}

/** 保存格式编辑弹窗：多行输入 + 占位符胶囊，确认后去空格保存。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiSaveFormatDialog(
    initialFormat: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialFormat,
                selection = TextRange(initialFormat.length),
            ),
        )
    }
    val chips = remember(strings) {
        listOf(
            strings.copyTextChipIllustId to "{illust_id}",
            "part" to "{part}",
            strings.copyTextChipTitle to "{title}",
            strings.copyTextChipUserId to "{user_id}",
            strings.copyTextChipUserName to "{user_name}",
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogSaveFormat,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.confirm,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = { onConfirm(fieldValue.text.trim()) },
        ),
        dismissAction = CouiDialogAction(
            text = strings.cancel,
            onClick = onDismiss,
        ),
    ) {
        CouiFormatField(
            value = fieldValue,
            onValueChange = { fieldValue = it },
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { (label, text) ->
                CouiAssistChip(
                    text = label,
                    selected = false,
                    onClick = { fieldValue = insertTextAtSelection(fieldValue, text) },
                )
            }
        }
    }
}

/** 弹窗输入框：COUI 容器底色 + 圆角。 */
@Composable
private fun CouiFormatField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
