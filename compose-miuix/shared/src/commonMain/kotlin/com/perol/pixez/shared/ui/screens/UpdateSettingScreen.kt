package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.ToastMessage
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 更新设置页：展示当前版本、检查最新版本、管理忽略更新开关。
 *
 * @param settingsRepository 设置仓库，用于读写忽略版本号。
 * @param updateCheckClient 检查更新的 HttpClient，由应用生命周期统一管理。
 * @param onBack 返回上一级页面。
 */
@Composable
fun UpdateSettingScreen(
    settingsRepository: SettingsRepository,
    updateCheckClient: HttpClient = defaultUpdateCheckClient,
    onBack: () -> Unit,
) {
    // 协程作用域，用于手动触发版本检查与切到 IO 写 Setting。
    val coroutineScope = rememberCoroutineScope()

    // 最新版本号；null 表示尚未获取或获取失败。
    var latestVersion by remember { mutableStateOf<String?>(null) }
    // 是否正在检查版本。
    var isChecking by remember { mutableStateOf(false) }
    // 检查失败的提示信息，通过 ToastMessage 展示。
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // 当前忽略的版本号，作为本地状态以便开关即时响应。
    var ignoredVersion by remember { mutableStateOf(settingsRepository.ignoreUpdateVersion) }

    // 是否存在可忽略的新版本；使用 ?.let 避免 !! 非空断言。
    val hasNewVersion = latestVersion?.let { hasNewVersion(it) } ?: false

    /**
     * 执行一次版本检查：更新加载态、最新版本号与错误提示。
     *
     * 使用 try/finally 保证协程取消时加载态也会被重置，避免按钮/开关永久禁用。
     * 由 LaunchedEffect 直接调用，确保页面离开后自动检查任务随组合取消而终止。
     */
    suspend fun doCheck() {
        if (isChecking) return
        try {
            isChecking = true
            val result = checkLatestVersion(updateCheckClient)
            result
                .onSuccess { version ->
                    latestVersion = version
                }
                .onFailure { error ->
                    val message = error.message ?: "未知错误"
                    toastMessage = "检查更新失败: $message"
                }
        } finally {
            isChecking = false
        }
    }

    // 进入页面时自动检查一次最新版本；LaunchedEffect 会在 Composable 离开组合时自动取消协程。
    LaunchedEffect(Unit) {
        doCheck()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "更新设置",
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
                SmallTitle(text = "版本信息")
            }
            item {
                BasicComponent(
                    title = "当前版本",
                    summary = AppInfo.VERSION_NAME,
                )
            }
            item {
                BasicComponent(
                    title = "最新版本",
                    summary = when {
                        isChecking -> "检查中…"
                        latestVersion != null -> latestVersion!!
                        else -> "点击「检查更新」获取最新版本"
                    },
                )
            }

            item {
                SmallTitle(text = "更新选项")
            }
            item {
                BasicComponent(
                    title = "忽略当前版本更新",
                    summary = when {
                        isChecking -> "检查完成后可设置"
                        !hasNewVersion -> "当前已是最新版本"
                        else -> "开启后不再提醒当前最新版本"
                    },
                    endActions = {
                        Switch(
                            checked = ignoredVersion != null && ignoredVersion == latestVersion,
                            onCheckedChange = { checked ->
                                val newValue = if (checked) latestVersion else null
                                // Setting 写操作切到 IO 线程，避免阻塞主线程。
                                coroutineScope.launch(Dispatchers.IO) {
                                    settingsRepository.ignoreUpdateVersion = newValue
                                }
                                ignoredVersion = newValue
                            },
                            enabled = hasNewVersion,
                        )
                    },
                )
            }

            item {
                Button(
                    onClick = { coroutineScope.launch { doCheck() } },
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(text = if (isChecking) "检查中…" else "检查更新")
                }
            }
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}
