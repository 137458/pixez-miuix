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
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 交互设置页：管理异形屏适配、H 内容过滤、再次返回退出、滑动切换作品等开关。
 *
 * @param settingsRepository 设置仓库，用于读写交互相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun InteractionSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前交互相关设置，用于驱动 Switch 的显示与回写。
    var isBangs by remember { mutableStateOf(settingsRepository.isBangs) }
    var hIsNotAllow by remember { mutableStateOf(settingsRepository.hIsNotAllow) }
    var isReturnAgainToExit by remember { mutableStateOf(settingsRepository.isReturnAgainToExit) }
    var swipeChangeArtwork by remember { mutableStateOf(settingsRepository.swipeChangeArtwork) }
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FrostedTopAppBar(
                title = strings.interactionSettingTitle,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
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
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .layerBackdrop(backdrop),
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
                    SmallTitle(text = strings.interactionSettingTitle)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.interactionSettingSwipeChange,
                            summary = if (swipeChangeArtwork) strings.interactionSettingSwipeChangeSummaryOn else strings.interactionSettingSwipeChangeSummaryOff,
                            checked = swipeChangeArtwork,
                            onCheckedChange = { checked ->
                                swipeChangeArtwork = checked
                                settingsRepository.swipeChangeArtwork = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.interactionSettingDoubleBackExit,
                            summary = if (isReturnAgainToExit) strings.interactionSettingDoubleBackExitSummaryOn else strings.interactionSettingDoubleBackExitSummaryOff,
                            checked = isReturnAgainToExit,
                            onCheckedChange = { checked ->
                                isReturnAgainToExit = checked
                                settingsRepository.isReturnAgainToExit = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.settingShield)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.interactionSettingHNotAllow,
                            summary = if (hIsNotAllow) strings.interactionSettingHNotAllowSummaryOn else strings.interactionSettingHNotAllowSummaryOff,
                            checked = hIsNotAllow,
                            onCheckedChange = { checked ->
                                hIsNotAllow = checked
                                settingsRepository.hIsNotAllow = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.settingSectionDisplayLayout)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.interactionSettingBangs,
                            summary = if (isBangs) strings.interactionSettingBangsSummaryOn else strings.interactionSettingBangsSummaryOff,
                            checked = isBangs,
                            onCheckedChange = { checked ->
                                isBangs = checked
                                settingsRepository.isBangs = checked
                            },
                        )
                    }
                }
            }
        }
    }
}
