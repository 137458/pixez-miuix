package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.settings.SettingsRepository
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 保存设置页：管理与保存/收藏行为相关的开关。
 *
 * @param settingsRepository 设置仓库，用于读写保存相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun SaveSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前保存相关设置。
    var saveAfterStar by remember { mutableStateOf(settingsRepository.saveAfterStar) }
    var starAfterSave by remember { mutableStateOf(settingsRepository.starAfterSave) }
    var longPressSaveConfirm by remember { mutableStateOf(settingsRepository.longPressSaveConfirm) }
    var illustDetailSaveSkipLongPress by remember {
        mutableStateOf(settingsRepository.illustDetailSaveSkipLongPress)
    }
    var autoTagWhenStar by remember { mutableStateOf(settingsRepository.autoTagWhenStar) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "保存设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                SmallTitle(text = "保存联动")
            }
            item {
                BasicComponent(
                    title = "收藏后保存",
                    summary = if (saveAfterStar) "收藏作品后自动保存" else "收藏作品后不自动保存",
                    endActions = {
                        Switch(
                            checked = saveAfterStar,
                            onCheckedChange = { checked ->
                                saveAfterStar = checked
                                settingsRepository.saveAfterStar = checked
                            },
                        )
                    },
                )
            }
            item {
                BasicComponent(
                    title = "保存后收藏",
                    summary = if (starAfterSave) "保存作品后自动收藏" else "保存作品后不自动收藏",
                    endActions = {
                        Switch(
                            checked = starAfterSave,
                            onCheckedChange = { checked ->
                                starAfterSave = checked
                                settingsRepository.starAfterSave = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "交互确认")
            }
            item {
                BasicComponent(
                    title = "长按保存确认",
                    summary = if (longPressSaveConfirm) "长按保存时弹出确认" else "长按保存时直接保存",
                    endActions = {
                        Switch(
                            checked = longPressSaveConfirm,
                            onCheckedChange = { checked ->
                                longPressSaveConfirm = checked
                                settingsRepository.longPressSaveConfirm = checked
                            },
                        )
                    },
                )
            }
            item {
                BasicComponent(
                    title = "详情页保存跳过长按",
                    summary = if (illustDetailSaveSkipLongPress) "点击保存按钮直接保存" else "需长按保存按钮",
                    endActions = {
                        Switch(
                            checked = illustDetailSaveSkipLongPress,
                            onCheckedChange = { checked ->
                                illustDetailSaveSkipLongPress = checked
                                settingsRepository.illustDetailSaveSkipLongPress = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "收藏标签")
            }
            item {
                BasicComponent(
                    title = "收藏时自动添加标签",
                    summary = if (autoTagWhenStar) "收藏作品时自动使用收藏标签" else "收藏作品时不自动添加标签",
                    endActions = {
                        Switch(
                            checked = autoTagWhenStar,
                            onCheckedChange = { checked ->
                                autoTagWhenStar = checked
                                settingsRepository.autoTagWhenStar = checked
                            },
                        )
                    },
                )
            }
        }
    }
}
