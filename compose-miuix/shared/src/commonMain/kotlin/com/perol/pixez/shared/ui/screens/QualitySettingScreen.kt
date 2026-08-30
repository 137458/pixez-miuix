package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
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
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.blur.layerBackdrop

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

    val strings = LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingQuality,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
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
                    }
                }
            }
        }
    }
}
