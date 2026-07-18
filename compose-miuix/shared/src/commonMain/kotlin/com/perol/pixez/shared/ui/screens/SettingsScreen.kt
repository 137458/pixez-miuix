package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.AppInfo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 设置页：分组展示常用设置入口，M4 接入 SettingsRepository 后持久化。
 *
 * @param themeMode 当前主题模式：0 跟随系统，1 浅色，2 深色。
 * @param onThemeModeChange 主题模式变更回调，由外层 [RootContent] 应用到 [MiuixTheme]。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                SmallTitle(text = "主题")
            }
            item {
                ThemeModeSelector(
                    selected = themeMode,
                    onSelect = onThemeModeChange,
                )
            }
            item {
                SmallTitle(text = "关于")
            }
            item {
                BasicComponent(
                    title = "关于 PixEz",
                    summary = "版本 ${AppInfo.VERSION_NAME}",
                    onClick = onAboutClick,
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf("跟随系统" to 0, "浅色" to 1, "深色" to 2)
    Column {
        options.forEach { (label, value) ->
            BasicComponent(
                title = label,
                onClick = { onSelect(value) },
                endActions = {
                    if (selected == value) {
                        Text(text = "✓")
                    }
                },
            )
        }
    }
}
