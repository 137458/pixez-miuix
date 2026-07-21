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
 * 隐私设置页：管理 NSFW 遮罩、默认私密收藏等与隐私相关的开关。
 *
 * @param settingsRepository 设置仓库，用于读写隐私相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun PrivacySettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前隐私设置。
    var nsfwMask by remember { mutableStateOf(settingsRepository.nsfwMask) }
    var defaultPrivateLike by remember { mutableStateOf(settingsRepository.defaultPrivateLike) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "隐私设置",
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
                SmallTitle(text = "内容展示")
            }
            item {
                BasicComponent(
                    title = "NSFW 遮罩",
                    summary = if (nsfwMask) "已开启敏感内容遮罩" else "已关闭敏感内容遮罩",
                    endActions = {
                        Switch(
                            checked = nsfwMask,
                            onCheckedChange = { checked ->
                                nsfwMask = checked
                                settingsRepository.nsfwMask = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "收藏")
            }
            item {
                BasicComponent(
                    title = "默认私密收藏",
                    summary = if (defaultPrivateLike) "收藏时默认不公开" else "收藏时默认公开",
                    endActions = {
                        Switch(
                            checked = defaultPrivateLike,
                            onCheckedChange = { checked ->
                                defaultPrivateLike = checked
                                settingsRepository.defaultPrivateLike = checked
                            },
                        )
                    },
                )
            }
        }
    }
}
