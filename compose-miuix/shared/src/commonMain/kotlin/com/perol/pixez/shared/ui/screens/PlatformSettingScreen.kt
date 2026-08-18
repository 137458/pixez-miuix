package com.perol.pixez.shared.ui.screens

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "平台专属设置",
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
            if (!isAndroidPlatform()) {
                // Desktop 等不支持 Android 专属设置的平台展示占位提示。
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BasicComponent(
                            title = "当前平台不支持",
                            summary = "平台专属设置仅适用于 Android 设备",
                            onClick = {},
                        )
                    }
                }
                return@LazyColumn
            }

            item {
                SmallTitle(text = "显示")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "显示模式",
                        summary = displayMode.toDisplayModeLabel(),
                        onClick = { editingDialog = PlatformDialogType.DisplayMode },
                    )
                }
            }

            item {
                SmallTitle(text = "选择器")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "使用系统图片选择器",
                        summary = if (imagePickerType == "1") "使用 Photo Picker" else "使用传统文件选择器",
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
                SmallTitle(text = "默认打开方式")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "默认打开链接",
                        summary = "允许在此应用中打开网络链接",
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

        // 根据当前打开的对话框类型展示对应 SuperDialog。
        when (editingDialog) {
            PlatformDialogType.DisplayMode -> DisplayModeDialog(
                currentValue = displayMode,
                onDismiss = { editingDialog = null },
                onSelected = { value ->
                    displayMode = value
                    settingsRepository.displayMode = value
                    // TODO: 后续接入实际平台显示模式设置接口（FlutterDisplayMode KMP 等价能力）。
                    editingDialog = null
                },
            )

            null -> Unit
        }
    }
}

/**
 * 显示模式选择对话框：列出可选刷新率模式，选中后立即关闭并回写。
 */
@Composable
private fun DisplayModeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    OverlayDialog(
        title = "显示模式",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            DISPLAY_MODE_OPTIONS.forEach { (value, label) ->
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
 * 将显示模式数值转换为展示文案；若数值不在选项范围内，返回默认「跟随系统」。
 */
private fun Int.toDisplayModeLabel(): String {
    return DISPLAY_MODE_OPTIONS.firstOrNull { it.first == this }?.second ?: "跟随系统"
}

/**
 * 当前支持的对话框类型。
 */
private enum class PlatformDialogType {
    DisplayMode,
}

/**
 * 显示模式选项：0 跟随系统、1 60Hz、2 120Hz。
 * 当前版本先做 UI 与持久化，实际刷新率能力待后续接入系统 API。
 */
private val DISPLAY_MODE_OPTIONS = listOf(
    0 to "跟随系统",
    1 to "60Hz",
    2 to "120Hz",
)
