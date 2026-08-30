package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 桌面小组件推荐类型与图源设置页：
 * 支持独立配置小组件展示的内容来源（推荐、日榜、周榜、月榜、最新、关注等）以及独立的图片 CDN 代理源。
 *
 * @param settingsRepository 设置仓库，用于读写小组件推荐类型与图源。
 * @param onBack 返回上一级页面。
 */
@Composable
fun WidgetRecommendSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前小组件推荐类型与独立图源。
    var selectedType by remember { mutableStateOf(settingsRepository.widgetIllustType) }
    var selectedPictureSource by remember { mutableStateOf(settingsRepository.widgetPictureSource) }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val widgetFeedOptions = remember(strings) {
        listOf(
            WidgetIllustOption(type = "recom", label = strings.tabRecommend),
            WidgetIllustOption(type = "day", label = strings.rankingDay),
            WidgetIllustOption(type = "week", label = strings.rankingWeek),
            WidgetIllustOption(type = "month", label = strings.rankingMonth),
            WidgetIllustOption(type = "day_male", label = strings.rankingDayMale),
            WidgetIllustOption(type = "day_female", label = strings.rankingDayFemale),
            WidgetIllustOption(type = "news", label = strings.tabNew),
            WidgetIllustOption(type = "follow", label = strings.widgetSourceFollow),
        )
    }

    val widgetPictureSourceOptions = remember(strings) {
        listOf(
            WidgetPictureSourceOption(source = "", label = strings.settingWidgetPictureSourceFollowGlobal),
            WidgetPictureSourceOption(source = "i.pximg.net", label = "i.pximg.net (Pixiv 官方原站)"),
            WidgetPictureSourceOption(source = "i.pixiv.re", label = "i.pixiv.re (免代理镜像)"),
        )
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingWidgetRecommend,
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
                // ── 1. 内容推荐类型 ──
                item {
                    SmallTitle(text = strings.settingWidgetFeedSection)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        widgetFeedOptions.forEach { option ->
                            val isSelected = when (option.type) {
                                "day" -> selectedType == "day" || selectedType == "rank"
                                else -> selectedType == option.type
                            }
                            BasicComponent(
                                title = option.label,
                                onClick = {
                                    selectedType = option.type
                                    settingsRepository.widgetIllustType = option.type
                                },
                                endActions = {
                                    CheckIndicator(selected = isSelected)
                                },
                            )
                        }
                    }
                }

                // ── 2. 小组件图片代理源 ──
                item {
                    SmallTitle(text = strings.settingWidgetPictureSourceSection)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        widgetPictureSourceOptions.forEach { option ->
                            BasicComponent(
                                title = option.label,
                                onClick = {
                                    selectedPictureSource = option.source
                                    settingsRepository.widgetPictureSource = option.source
                                },
                                endActions = {
                                    CheckIndicator(selected = selectedPictureSource == option.source)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 小组件推荐选项数据。
 */
private data class WidgetIllustOption(
    val type: String,
    val label: String,
)

/**
 * 小组件图片源选项数据。
 */
private data class WidgetPictureSourceOption(
    val source: String,
    val label: String,
)
