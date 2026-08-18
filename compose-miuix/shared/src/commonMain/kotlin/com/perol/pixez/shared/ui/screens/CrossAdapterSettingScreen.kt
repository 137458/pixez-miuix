package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * 跨适配设置页：调整竖屏/横屏下按宽度自适应网格列数的阈值。
 *
 * @param settingsRepository 设置仓库，用于读写跨适配相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun CrossAdapterSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current

    // 页面状态：从 SettingsRepository 读取当前跨适配设置。
    var crossAdapt by remember { mutableStateOf(settingsRepository.crossAdapt) }
    var crossAdapterWidth by remember { mutableStateOf(settingsRepository.crossAdapterWidth) }
    var hCrossAdapt by remember { mutableStateOf(settingsRepository.hCrossAdapt) }
    var hCrossAdapterWidth by remember { mutableStateOf(settingsRepository.hCrossAdapterWidth) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingCrossAdapter,
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
                SmallTitle(text = strings.crossCountPortrait)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.crossAdaptAuto,
                        summary = if (crossAdapt) strings.saveAfterStarSummaryOn else strings.saveAfterStarSummaryOff,
                        endActions = {
                            Switch(
                                checked = crossAdapt,
                                onCheckedChange = { checked ->
                                    crossAdapt = checked
                                    settingsRepository.crossAdapt = checked
                                },
                            )
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
                    }
                }
            }

            item {
                SmallTitle(text = strings.crossCountLandscape)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.crossAdaptAuto,
                        summary = if (hCrossAdapt) strings.saveAfterStarSummaryOn else strings.saveAfterStarSummaryOff,
                        endActions = {
                            Switch(
                                checked = hCrossAdapt,
                                onCheckedChange = { checked ->
                                    hCrossAdapt = checked
                                    settingsRepository.hCrossAdapt = checked
                                },
                            )
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
                    }
                }
            }
        }
    }
}

/**
 * 宽度阈值滑块与实时预览。
 *
 * @param width 当前阈值。
 * @param onWidthChangeFinished 滑动结束后的最终值回调。
 */
@Composable
private fun AdapterWidthSlider(
    width: Int,
    onWidthChangeFinished: (Int) -> Unit,
) {
    // 滑块拖动过程中的临时值，避免每次拖动都写回 SettingsRepository。
    var sliderValue by remember(width) { mutableFloatStateOf(width.toFloat()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 使用父容器可用宽度模拟原 Flutter MediaQuery 的屏幕宽度，保证跨平台一致。
        val containerWidth = maxWidth.value
        val columnCount = max((containerWidth / sliderValue).toInt(), 1)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "阈值：${sliderValue.toInt()} px，当前列数：$columnCount",
                style = MiuixTheme.textStyles.body2,
            )

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = AppConstants.CrossAdapter.WIDTH_MIN.toFloat()..AppConstants.CrossAdapter.WIDTH_MAX.toFloat(),
                onValueChangeFinished = {
                    onWidthChangeFinished(sliderValue.toInt())
                },
            )

            Text(
                text = "预览",
                style = MiuixTheme.textStyles.subtitle,
            )

            PreviewGrid(
                columnCount = columnCount,
                itemCount = AppConstants.CrossAdapter.PREVIEW_ITEM_COUNT,
            )
        }
    }
}

/**
 * 简易网格预览：使用纯色方块展示当前阈值下的列数效果。
 *
 * @param columnCount 预览列数。
 * @param itemCount 预览方块数量。
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
