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
 * Feed 设置页：管理 Feed AI 标识、收藏后关注画师以及 SauceNAO 打开方式等开关。
 *
 * @param settingsRepository 设置仓库，用于读写 Feed 相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun FeedSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前 Feed 相关设置。
    var feedAIBadge by remember { mutableStateOf(settingsRepository.feedAIBadge) }
    var followAfterStar by remember { mutableStateOf(settingsRepository.followAfterStar) }
    var useSaunceNaoWebview by remember { mutableStateOf(settingsRepository.useSaunceNaoWebview) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "Feed 设置",
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
                    title = "显示 Feed AI 标识",
                    summary = if (feedAIBadge) "在 Feed 中显示 AI 生成标识" else "在 Feed 中隐藏 AI 生成标识",
                    endActions = {
                        Switch(
                            checked = feedAIBadge,
                            onCheckedChange = { checked ->
                                // 更新本地状态并立即写回仓库，保证偏好持久化。
                                feedAIBadge = checked
                                settingsRepository.feedAIBadge = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "收藏行为")
            }
            item {
                BasicComponent(
                    title = "收藏后关注画师",
                    summary = if (followAfterStar) "收藏作品后自动关注画师" else "收藏作品后不自动关注画师",
                    endActions = {
                        Switch(
                            checked = followAfterStar,
                            onCheckedChange = { checked ->
                                // 更新本地状态并立即写回仓库，保证偏好持久化。
                                followAfterStar = checked
                                settingsRepository.followAfterStar = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "搜图")
            }
            item {
                BasicComponent(
                    title = "使用 WebView 打开 SauceNAO",
                    summary = if (useSaunceNaoWebview) "SauceNAO 结果在应用内 WebView 打开" else "SauceNAO 结果使用外部浏览器打开",
                    endActions = {
                        Switch(
                            checked = useSaunceNaoWebview,
                            onCheckedChange = { checked ->
                                // 更新本地状态并立即写回仓库，保证偏好持久化。
                                useSaunceNaoWebview = checked
                                settingsRepository.useSaunceNaoWebview = checked
                            },
                        )
                    },
                )
            }
        }
    }
}
