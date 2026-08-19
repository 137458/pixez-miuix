package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.settings.SettingsRepository
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    // 页面状态：从 SettingsRepository 读取当前 Feed 相关设置。
    var feedAIBadge by remember { mutableStateOf(settingsRepository.feedAIBadge) }
    var followAfterStar by remember { mutableStateOf(settingsRepository.followAfterStar) }
    var useSaunceNaoWebview by remember { mutableStateOf(settingsRepository.useSaunceNaoWebview) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.feedSettingTitle,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = strings.feedSettingSectionDisplay)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.feedSettingAiBadge,
                        summary = if (feedAIBadge) strings.feedSettingAiBadgeSummaryOn else strings.feedSettingAiBadgeSummaryOff,
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
            }

            item {
                SmallTitle(text = strings.feedSettingSectionBehavior)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.feedSettingFollowAfterStar,
                        summary = if (followAfterStar) strings.feedSettingFollowAfterStarSummaryOn else strings.feedSettingFollowAfterStarSummaryOff,
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
            }

            item {
                SmallTitle(text = strings.feedSettingSectionSearchImage)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.feedSettingSauceNaoWebview,
                        summary = if (useSaunceNaoWebview) strings.feedSettingSauceNaoWebviewSummaryOn else strings.feedSettingSauceNaoWebviewSummaryOff,
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
}
