package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 屏蔽设置页：管理本地屏蔽相关开关与列表入口。
 *
 * 当前 M48 仅实现 AI 作品过滤开关；标签/画师/作品管理留到后续里程碑。
 */
@Composable
fun ShieldScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository,
) {
    // 开关状态与设置存储同步，进程重建后由 SettingsRepository 恢复。
    var banAIIllust by remember { mutableStateOf(settingsRepository.banAIIllust) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "屏蔽设置",
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
                SmallTitle(text = "AI 作品")
            }
            item {
                BasicComponent(
                    title = "使带有 AI 生成标记的作品不可见",
                    summary = if (banAIIllust) "已开启" else "已关闭",
                    endActions = {
                        Switch(
                            checked = banAIIllust,
                            onCheckedChange = { checked ->
                                banAIIllust = checked
                                settingsRepository.banAIIllust = checked
                            },
                        )
                    },
                )
            }
        }
    }
}
