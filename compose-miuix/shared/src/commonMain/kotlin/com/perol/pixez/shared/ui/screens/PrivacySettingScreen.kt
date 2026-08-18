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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var nsfwMask by remember { mutableStateOf(settingsRepository.nsfwMask) }
    var defaultPrivateLike by remember { mutableStateOf(settingsRepository.defaultPrivateLike) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingPrivacy,
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
                SmallTitle(text = strings.settingSectionShieldPrivacy)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.nsfwMask,
                        summary = if (nsfwMask) strings.nsfwMaskSummaryOn else strings.nsfwMaskSummaryOff,
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
            }

            item {
                SmallTitle(text = strings.settingSectionBookmarkShare)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.defaultPrivateLike,
                        summary = if (defaultPrivateLike) strings.defaultPrivateLikeSummaryOn else strings.defaultPrivateLikeSummaryOff,
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
}
