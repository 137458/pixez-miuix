package com.perol.pixez.shared.ui.screens

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.isAndroidPlatform
import com.perol.pixez.shared.platform.openDefaultAppSettings
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/**
 * 平台专属设置页：仅 Android 平台展示实际设置项。
 *
 * 包含三项功能：
 * - 显示模式：选择索引后持久化到 [SettingsRepository.displayMode]，实际调用平台接口留待后续接入。
 * - 图片选择器类型：Switch 控制传统方式（"0"）或系统 Photo Picker（"1"）。
 * - 默认打开链接（Android 12+）：Switch 控制持久化状态并跳转系统设置页。
 *
 * @param settingsRepository 设置仓库，用于读写平台相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun PlatformSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前平台设置。
    var displayMode by remember { mutableIntStateOf(settingsRepository.displayMode) }
    var imagePickerType by remember { mutableStateOf(settingsRepository.imagePickerType) }
    var openByDefault by remember { mutableStateOf(settingsRepository.openByDefault) }

    // 当前正在编辑的对话框类型，null 表示没有对话框打开。
    var editingDialog by rememberSaveable { mutableStateOf<PlatformDialogType?>(null) }
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = LocalBackdrop.current
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingPlatform,
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
                if (!isAndroidPlatform()) {
                    // Desktop 等不支持 Android 专属设置的平台展示占位提示。
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            BasicComponent(
                                title = strings.noData,
                                summary = strings.settingPlatformSummary,
                                onClick = {},
                            )
                        }
                    }
                    return@LazyColumn
                }

                item {
                    SmallTitle(text = strings.dialogDisplayMode)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BasicComponent(
                            title = strings.dialogDisplayMode,
                            summary = displayMode.toDisplayModeLabel(),
                            onClick = { editingDialog = PlatformDialogType.DisplayMode },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.platformSettingSectionPicker)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BasicComponent(
                            title = strings.platformSettingPhotoPicker,
                            summary = if (imagePickerType == "1") strings.platformSettingPhotoPickerSummaryOn else strings.platformSettingPhotoPickerSummaryOff,
                            endActions = {
                                Switch(
                                    checked = imagePickerType == "1",
                                    onCheckedChange = { checked ->
                                        imagePickerType = if (checked) "1" else "0"
                                        settingsRepository.imagePickerType = imagePickerType
                                    },
                                )
                            },
                            onClick = {},
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.platformSettingSectionDefaultOpen)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BasicComponent(
                            title = strings.platformSettingDefaultOpenLinks,
                            summary = strings.platformSettingDefaultOpenLinksSummary,
                            endActions = {
                                Switch(
                                    checked = openByDefault,
                                    onCheckedChange = { checked ->
                                        openByDefault = checked
                                        settingsRepository.openByDefault = checked
                                        // 同步跳转系统「默认打开方式」设置页。
                                        openDefaultAppSettings()
                                    },
                                )
                            },
                            onClick = {},
                        )
                    }
                }
            }
        }

        // 根据当前打开的对话框类型展示对应 SuperDialog。
        when (editingDialog) {
            PlatformDialogType.DisplayMode -> DisplayModeDialog(
                currentValue = displayMode,
                onDismiss = { editingDialog = null },
                onSelected = { value ->
                    displayMode = value
                    settingsRepository.displayMode = value
                    editingDialog = null
                },
            )

            null -> Unit
        }
    }
}

/**
 * 屏幕刷新率模式选择对话框：列出可选刷新率模式，选中后立即关闭并回写。
 */
@Composable
private fun DisplayModeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val options = listOf(
        0 to strings.platformDisplayModeFollowSystem,
        1 to "标准 60Hz",
        2 to "高刷新率 (90Hz / 120Hz / 144Hz)",
    )

    OverlayDialog(
        title = strings.dialogDisplayMode,
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { (value, label) ->
                BasicComponent(
                    title = label,
                    onClick = { onSelected(value) },
                    endActions = {
                        CheckIndicator(selected = currentValue == value)
                    },
                )
            }
        }
    }
}

/**
 * 将屏幕刷新率数值转换为展示文案；若数值不在选项范围内，返回默认「跟随系统」。
 */
@Composable
private fun Int.toDisplayModeLabel(): String {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    return when (this) {
        1 -> "标准 60Hz"
        2 -> "高刷新率 (90Hz / 120Hz+)"
        else -> strings.platformDisplayModeFollowSystem
    }
}

/**
 * 当前支持的对话框类型。
 */
private enum class PlatformDialogType {
    DisplayMode,
}
