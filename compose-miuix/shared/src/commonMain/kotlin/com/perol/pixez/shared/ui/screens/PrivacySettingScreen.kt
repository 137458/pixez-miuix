package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.FrostedTopAppBar

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
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
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 隐私与安全偏好设置页：
 * 管理 NSFW 遮罩、默认非公开收藏等与隐私相关的偏好设置。
 *
 * @param settingsRepository 设置仓库，用于读写隐私相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun PrivacySettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前隐私相关设置。
    var nsfwMask by remember { mutableStateOf(settingsRepository.nsfwMask) }
    var defaultPrivateLike by remember { mutableStateOf(settingsRepository.defaultPrivateLike) }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FrostedTopAppBar(
                title = strings.settingPrivacy,
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
                // ── 1. 内容遮罩 ──
                item {
                    SmallTitle(text = strings.settingPrivacy)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.nsfwMask,
                            summary = if (nsfwMask) strings.nsfwMaskSummaryOn else strings.nsfwMaskSummaryOff,
                            checked = nsfwMask,
                            onCheckedChange = { checked ->
                                nsfwMask = checked
                                settingsRepository.nsfwMask = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.defaultPrivateLike,
                            summary = if (defaultPrivateLike) strings.defaultPrivateLikeSummaryOn else strings.defaultPrivateLikeSummaryOff,
                            checked = defaultPrivateLike,
                            onCheckedChange = { checked ->
                                defaultPrivateLike = checked
                                settingsRepository.defaultPrivateLike = checked
                            },
                        )
                    }
                }
            }
        }
    }
}
