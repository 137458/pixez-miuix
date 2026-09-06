package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.isAndroidPlatform
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.mutableFloatStateOf
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.AppConstants
import kotlin.math.max
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 布局设置页：管理底栏模式、平板模式、竖屏自适应/固定列数、横屏自适应/固定列数。
 *
 * @param settingsRepository 设置仓库，用于读写布局相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun LayoutSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前布局设置，用于驱动组件显示与回写。
    var crossCount by remember { mutableStateOf(settingsRepository.crossCount) }
    var hCrossCount by remember { mutableStateOf(settingsRepository.hCrossCount) }
    var crossAdapt by remember { mutableStateOf(settingsRepository.crossAdapt) }
    var crossAdapterWidth by remember { mutableStateOf(settingsRepository.crossAdapterWidth) }
    var hCrossAdapt by remember { mutableStateOf(settingsRepository.hCrossAdapt) }
    var hCrossAdapterWidth by remember { mutableStateOf(settingsRepository.hCrossAdapterWidth) }
    var useFloatingBottomBar by remember { mutableStateOf(settingsRepository.useFloatingBottomBar) }
    var displayMode by remember { mutableIntStateOf(settingsRepository.displayMode) }
    var showDisplayModeDialog by rememberSaveable { mutableStateOf(false) }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    val crossCountOptions = remember { listOf("2", "3", "4") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingLayout,
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
                item {
                    SmallTitle(text = strings.floatingBottomBar)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.floatingBottomBar,
                            summary = if (useFloatingBottomBar) strings.floatingBottomBarSummaryOn else strings.floatingBottomBarSummaryOff,
                            checked = useFloatingBottomBar,
                            onCheckedChange = {
                                useFloatingBottomBar = it
                                settingsRepository.useFloatingBottomBar = it
                            },
                        )
                    }
                }

                if (isAndroidPlatform()) {
                    item {
                        SmallTitle(text = strings.dialogDisplayMode)
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            BasicComponent(
                                title = strings.dialogDisplayMode,
                                summary = displayMode.toDisplayModeLabel(),
                                onClick = { showDisplayModeDialog = true },
                            )
                        }
                    }
                }

                item {
                    SmallTitle(text = strings.crossCountPortrait)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.crossAdaptAuto,
                            summary = if (crossAdapt) strings.crossAdaptAutoSummaryOn else strings.crossAdaptAutoSummaryOff,
                            checked = crossAdapt,
                            onCheckedChange = { checked ->
                                crossAdapt = checked
                                settingsRepository.crossAdapt = checked
                            },
                        )
                        if (crossAdapt) {
                            AdapterWidthSlider(
                                width = crossAdapterWidth,
                                onWidthChangeFinished = { newWidth ->
                                    crossAdapterWidth = newWidth
                                    settingsRepository.crossAdapterWidth = newWidth
                                },
                            )
                        } else {
                            OverlayDropdownPreference(
                                title = strings.crossCountPortrait,
                                items = crossCountOptions,
                                selectedIndex = (crossCount - 2).coerceIn(0, crossCountOptions.lastIndex),
                                onSelectedIndexChange = { index ->
                                    val count = index + 2
                                    crossCount = count
                                    settingsRepository.crossCount = count
                                },
                            )
                        }
                    }
                }

                item {
                    SmallTitle(text = strings.crossCountLandscape)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SwitchPreference(
                            title = strings.crossAdaptAuto,
                            summary = if (hCrossAdapt) strings.crossAdaptAutoSummaryOn else strings.crossAdaptAutoSummaryOff,
                            checked = hCrossAdapt,
                            onCheckedChange = { checked ->
                                hCrossAdapt = checked
                                settingsRepository.hCrossAdapt = checked
                            },
                        )
                        if (hCrossAdapt) {
                            AdapterWidthSlider(
                                width = hCrossAdapterWidth,
                                onWidthChangeFinished = { newWidth ->
                                    hCrossAdapterWidth = newWidth
                                    settingsRepository.hCrossAdapterWidth = newWidth
                                },
                            )
                        } else {
                            OverlayDropdownPreference(
                                title = strings.crossCountLandscape,
                                items = crossCountOptions,
                                selectedIndex = (hCrossCount - 2).coerceIn(0, crossCountOptions.lastIndex),
                                onSelectedIndexChange = { index ->
                                    val count = index + 2
                                    hCrossCount = count
                                    settingsRepository.hCrossCount = count
                                },
                            )
                        }
                    }
                }
            }
        }

        val displayModeOptions = remember(strings) {
            listOf(
                0 to strings.platformDisplayModeFollowSystem,
                1 to strings.refreshRate60Hz,
                2 to strings.refreshRateHigh,
            )
        }
        OverlayDialog(
            title = strings.dialogDisplayMode,
            show = showDisplayModeDialog,
            onDismissRequest = { showDisplayModeDialog = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                displayModeOptions.forEach { (value, label) ->
                    BasicComponent(
                        title = label,
                        onClick = {
                            displayMode = value
                            settingsRepository.displayMode = value
                            showDisplayModeDialog = false
                        },
                        endActions = {
                            CheckIndicator(selected = displayMode == value)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 将屏幕刷新率数值转换为展示文案。
 */
@Composable
private fun Int.toDisplayModeLabel(): String {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    return when (this) {
        1 -> strings.refreshRate60Hz
        2 -> strings.refreshRateHigh
        else -> strings.platformDisplayModeFollowSystem
    }
}

/**
 * 宽度阈值滑块与实时预览。
 */
@Composable
private fun AdapterWidthSlider(
    width: Int,
    onWidthChangeFinished: (Int) -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var sliderValue by remember(width) { mutableFloatStateOf(width.toFloat()) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val containerWidth = maxWidth.value
        val columnCount = max((containerWidth / sliderValue).toInt(), 1)

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SliderPreference(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                title = strings.crossAdapterThreshold.format(sliderValue.toInt(), columnCount),
                valueText = "${sliderValue.toInt()} dp",
                valueRange = AppConstants.CrossAdapter.WIDTH_MIN.toFloat()..AppConstants.CrossAdapter.WIDTH_MAX.toFloat(),
                onValueChangeFinished = {
                    onWidthChangeFinished(sliderValue.toInt())
                },
            )

            Text(
                text = strings.crossAdapterPreview,
                style = MiuixTheme.textStyles.subtitle,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Box(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                PreviewGrid(
                    columnCount = columnCount,
                    itemCount = AppConstants.CrossAdapter.PREVIEW_ITEM_COUNT,
                )
            }
        }
    }
}

/**
 * 简易网格预览：使用纯色方块展示当前阈值下的列数效果。
 */
@Composable
private fun PreviewGrid(
    columnCount: Int,
    itemCount: Int,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(itemCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MiuixTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
