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
 * 保存设置页：管理与保存/收藏行为相关的开关。
 *
 * @param settingsRepository 设置仓库，用于读写保存相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun SaveSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前保存相关设置。
    var saveAfterStar by remember { mutableStateOf(settingsRepository.saveAfterStar) }
    var starAfterSave by remember { mutableStateOf(settingsRepository.starAfterSave) }
    var longPressSaveConfirm by remember { mutableStateOf(settingsRepository.longPressSaveConfirm) }
    var illustDetailSaveSkipLongPress by remember {
        mutableStateOf(settingsRepository.illustDetailSaveSkipLongPress)
    }
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var autoTagWhenStar by remember { mutableStateOf(settingsRepository.autoTagWhenStar) }
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingSave,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.settingSectionQualitySave)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.saveAfterStar,
                            summary = if (saveAfterStar) strings.saveAfterStarSummaryOn else strings.saveAfterStarSummaryOff,
                            checked = saveAfterStar,
                            onCheckedChange = { checked ->
                                saveAfterStar = checked
                                settingsRepository.saveAfterStar = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.starAfterSave,
                            summary = if (starAfterSave) strings.starAfterSaveSummaryOn else strings.starAfterSaveSummaryOff,
                            checked = starAfterSave,
                            onCheckedChange = { checked ->
                                starAfterSave = checked
                                settingsRepository.starAfterSave = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.longPressSaveConfirm)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.longPressSaveConfirm,
                            summary = if (longPressSaveConfirm) strings.longPressSaveConfirmSummaryOn else strings.longPressSaveConfirmSummaryOff,
                            checked = longPressSaveConfirm,
                            onCheckedChange = { checked ->
                                longPressSaveConfirm = checked
                                settingsRepository.longPressSaveConfirm = checked
                            },
                        )
                        SwitchPreference(
                            title = strings.illustDetailSkipLongPress,
                            summary = if (illustDetailSaveSkipLongPress) strings.illustDetailSkipLongPressSummaryOn else strings.illustDetailSkipLongPressSummaryOff,
                            checked = illustDetailSaveSkipLongPress,
                            onCheckedChange = { checked ->
                                illustDetailSaveSkipLongPress = checked
                                settingsRepository.illustDetailSaveSkipLongPress = checked
                            },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.settingBookTags)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.autoTagWhenStar,
                            summary = if (autoTagWhenStar) strings.autoTagWhenStarSummaryOn else strings.autoTagWhenStarSummaryOff,
                            checked = autoTagWhenStar,
                            onCheckedChange = { checked ->
                                autoTagWhenStar = checked
                                settingsRepository.autoTagWhenStar = checked
                            },
                        )
                    }
                }
            }
        }
    }
}
