package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
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

/**
 * 下载设置页：管理保存路径、同时下载任务数、单文件夹模式。
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
    var maxRunningTask by remember { mutableIntStateOf(settingsRepository.maxRunningTask) }
    var singleFolder by remember { mutableStateOf(settingsRepository.singleFolder) }

    // 对话框显隐状态。
    var showPathDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }

    // 保存路径输入框状态，打开对话框时与当前设置同步。
    var pathInput by remember(showPathDialog) { mutableStateOf(storePath) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "下载设置",
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
 * 同时下载任务数可选范围：1-5，与原应用常见取值一致。
 */
private val MAX_RUNNING_TASK_RANGE = 1..5
