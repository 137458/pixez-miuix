package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.preference.SwitchPreference

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
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.feedSettingTitle,
                scrollBehavior = scrollBehavior,
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.feedSettingSectionDisplay)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.feedSettingAiBadge,
                            summary = if (feedAIBadge) strings.feedSettingAiBadgeSummaryOn else strings.feedSettingAiBadgeSummaryOff,
                            checked = feedAIBadge,
                            onCheckedChange = { checked ->
                                feedAIBadge = checked
                                settingsRepository.feedAIBadge = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.feedSettingSectionBehavior)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.feedSettingFollowAfterStar,
                            summary = if (followAfterStar) strings.feedSettingFollowAfterStarSummaryOn else strings.feedSettingFollowAfterStarSummaryOff,
                            checked = followAfterStar,
                            onCheckedChange = { checked ->
                                followAfterStar = checked
                                settingsRepository.followAfterStar = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.feedSettingSectionSearchImage)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.feedSettingSauceNaoWebview,
                            summary = if (useSaunceNaoWebview) strings.feedSettingSauceNaoWebviewSummaryOn else strings.feedSettingSauceNaoWebviewSummaryOff,
                            checked = useSaunceNaoWebview,
                            onCheckedChange = { checked ->
                                useSaunceNaoWebview = checked
                                settingsRepository.useSaunceNaoWebview = checked
                            },
                        )
                    }
                }
            }
        }
    }
}
