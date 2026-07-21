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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 欢迎页设置页：选择应用启动后默认进入的页面。
 *
 * 选项沿用旧 Flutter 版的字符串编码，写入 [SettingsRepository.welcomePageType]，
 * 由 [RootComponent] 在启动时解析为初始路由。
 *
 * @param settingsRepository 设置仓库，用于读写欢迎页类型。
 * @param onBack 返回上一级页面。
 */
@Composable
fun WelcomePageSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前欢迎页类型。
    var selectedType by remember { mutableStateOf(settingsRepository.welcomePageType) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "欢迎页",
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
                SmallTitle(text = "启动时显示")
            }
            items(WELCOME_PAGE_OPTIONS.size) { index ->
                val option = WELCOME_PAGE_OPTIONS[index]
                BasicComponent(
                    title = option.label,
                    onClick = {
                        selectedType = option.type
                        settingsRepository.welcomePageType = option.type
                    },
                    endActions = {
                        SelectionIndicator(selected = selectedType == option.type)
                    },
                )
            }
        }
    }
}

/**
 * 欢迎页选项数据。
 */
private data class WelcomePageOption(
    val type: String,
    val label: String,
)

/**
 * 可选的欢迎页类型与展示文案，顺序与原 Flutter 应用保持一致。
 */
private val WELCOME_PAGE_OPTIONS = listOf(
    WelcomePageOption(type = "home", label = "首页"),
    WelcomePageOption(type = "rank", label = "排行榜"),
    WelcomePageOption(type = "quick_view", label = "速览"),
    WelcomePageOption(type = "search", label = "搜索"),
    WelcomePageOption(type = "setting", label = "设置"),
)

/**
 * 选中指示器：当前项显示对勾。
 */
@Composable
private fun SelectionIndicator(selected: Boolean) {
    if (selected) {
        Text(text = "✓")
    }
}
