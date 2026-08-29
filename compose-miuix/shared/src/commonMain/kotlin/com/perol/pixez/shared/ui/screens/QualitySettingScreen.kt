package com.perol.pixez.shared.ui.screens

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 画质与保存偏好设置页：
 * 管理 Feed 预览、插画详情、漫画详情、大图缩放等各页面画质偏好，以及收藏与保存的联动开关。
 *
 * @param settingsRepository 设置仓库，用于读写画质与保存相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun QualitySettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前画质设置。
    var feedPreviewQuality by remember {
        mutableIntStateOf(settingsRepository.feedPreviewQuality)
    }
    var pictureQuality by remember {
        mutableIntStateOf(settingsRepository.pictureQuality)
    }
    var mangaQuality by remember {
        mutableIntStateOf(settingsRepository.mangaQuality)
    }
    var zoomQuality by remember {
        mutableIntStateOf(settingsRepository.zoomQuality)
    }

    // 保存行为联动开关
    var saveAfterStar by remember { mutableStateOf(settingsRepository.saveAfterStar) }
    var starAfterSave by remember { mutableStateOf(settingsRepository.starAfterSave) }
    var longPressSaveConfirm by remember { mutableStateOf(settingsRepository.longPressSaveConfirm) }
    var illustDetailSaveSkipLongPress by remember {
        mutableStateOf(settingsRepository.illustDetailSaveSkipLongPress)
    }

    val strings = LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = LocalBackdrop.current
    val colorScheme = MiuixTheme.colorScheme

    val feedPreviewQualityOptions = remember(strings) {
        listOf(
            strings.qualityLarge,
            strings.qualityMedium,
            strings.qualityLow,
        )
    }

    val detailQualityOptions = remember(strings) {
        listOf(
            strings.qualityOriginal,
            strings.qualityLarge,
            strings.qualityMedium,
        )
    }

    val zoomQualityOptions = remember(strings) {
        listOf(
            strings.qualityOriginal,
            strings.qualityLarge,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingQuality,
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
                // ── 1. 画质选择 ──
                item {
                    SmallTitle(text = strings.settingQuality)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        OverlayDropdownPreference(
                            title = strings.feedPreviewQuality,
                            items = feedPreviewQualityOptions,
                            selectedIndex = feedPreviewQuality.coerceIn(0, feedPreviewQualityOptions.lastIndex),
                            onSelectedIndexChange = { index ->
                                feedPreviewQuality = index
                                settingsRepository.feedPreviewQuality = index
                            },
                        )
                        OverlayDropdownPreference(
                            title = strings.pictureQuality,
                            items = detailQualityOptions,
                            selectedIndex = pictureQuality.coerceIn(0, detailQualityOptions.lastIndex),
                            onSelectedIndexChange = { index ->
                                pictureQuality = index
                                settingsRepository.pictureQuality = index
                            },
                        )
                        OverlayDropdownPreference(
                            title = strings.mangaQuality,
                            items = detailQualityOptions,
                            selectedIndex = mangaQuality.coerceIn(0, detailQualityOptions.lastIndex),
                            onSelectedIndexChange = { index ->
                                mangaQuality = index
                                settingsRepository.mangaQuality = index
                            },
                        )
                        OverlayDropdownPreference(
                            title = strings.zoomQuality,
                            items = zoomQualityOptions,
                            selectedIndex = zoomQuality.coerceIn(0, zoomQualityOptions.lastIndex),
                            onSelectedIndexChange = { index ->
                                zoomQuality = index
                                settingsRepository.zoomQuality = index
                            },
                        )
                    }
                }

                // ── 2. 保存行为联动 ──
                item {
                    SmallTitle(text = strings.settingSectionBookmarkShare)
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

                // ── 3. 长按确认 ──
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
            }
        }
    }
}
