package com.perol.pixez.shared.ui.screens

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
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.AppConstants
import kotlin.math.max

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
    // 页面状态：从 SettingsRepository 读取当前布局设置。
    var padMode by remember { mutableIntStateOf(settingsRepository.padMode) }
    var crossCount by remember { mutableIntStateOf(settingsRepository.crossCount) }
    var hCrossCount by remember { mutableIntStateOf(settingsRepository.hCrossCount) }
    var crossAdapt by remember { mutableStateOf(settingsRepository.crossAdapt) }
    var crossAdapterWidth by remember { mutableStateOf(settingsRepository.crossAdapterWidth) }
    var hCrossAdapt by remember { mutableStateOf(settingsRepository.hCrossAdapt) }
    var hCrossAdapterWidth by remember { mutableStateOf(settingsRepository.hCrossAdapterWidth) }
    var useFloatingBottomBar by remember { mutableStateOf(settingsRepository.useFloatingBottomBar) }

    // 当前正在编辑的布局类型，null 表示没有对话框打开。
    var editingType by rememberSaveable { mutableStateOf<LayoutType?>(null) }
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingLayout,
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
                SmallTitle(text = strings.floatingBottomBar)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.floatingBottomBar,
                        summary = if (useFloatingBottomBar) strings.floatingBottomBarSummaryOn else strings.floatingBottomBarSummaryOff,
                        endActions = {
                            Switch(
                                checked = useFloatingBottomBar,
                                onCheckedChange = {
                                    useFloatingBottomBar = it
                                    settingsRepository.useFloatingBottomBar = it
                                },
                            )
                        },
                    )
                    BasicComponent(
                        title = strings.padMode,
                        summary = padMode.toPadModeLabel(),
                        onClick = { editingType = LayoutType.PadMode },
                    )
                }
            }

            item {
                SmallTitle(text = strings.crossCountPortrait)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.crossAdaptAuto,
                        summary = if (crossAdapt) strings.crossAdaptAutoSummaryOn else strings.crossAdaptAutoSummaryOff,
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
                    } else {
                        LayoutSettingItem(
                            title = strings.crossCountPortrait,
                            summary = crossCount.toCrossCountLabel(),
                            onClick = { editingType = LayoutType.CrossCount },
                        )
                    }
                }
            }

            item {
                SmallTitle(text = strings.crossCountLandscape)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.crossAdaptAuto,
                        summary = if (hCrossAdapt) strings.crossAdaptAutoSummaryOn else strings.crossAdaptAutoSummaryOff,
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
                    } else {
                        LayoutSettingItem(
                            title = strings.crossCountLandscape,
                            summary = hCrossCount.toCrossCountLabel(),
                            onClick = { editingType = LayoutType.HCrossCount },
                        )
                    }
                }
            }
        }

        // 布局选项选择对话框。
        val currentType = editingType
        if (currentType != null) {
            val dialogTitle = when (currentType) {
                LayoutType.PadMode -> strings.padMode
                LayoutType.CrossCount -> strings.crossCountPortrait
                LayoutType.HCrossCount -> strings.crossCountLandscape
            }
            when (currentType) {
                LayoutType.PadMode -> LayoutSelectDialog(
                    title = dialogTitle,
                    currentValue = padMode,
                    options = PAD_MODE_OPTIONS,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        padMode = value
                        settingsRepository.padMode = value
                        editingType = null
                    },
                )

                LayoutType.CrossCount -> LayoutSelectDialog(
                    title = dialogTitle,
                    currentValue = crossCount,
                    options = CROSS_COUNT_OPTIONS,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        crossCount = value
                        settingsRepository.crossCount = value
                        editingType = null
                    },
                )

                LayoutType.HCrossCount -> LayoutSelectDialog(
                    title = dialogTitle,
                    currentValue = hCrossCount,
                    options = CROSS_COUNT_OPTIONS,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        hCrossCount = value
                        settingsRepository.hCrossCount = value
                        editingType = null
                    },
                )
            }
        }
    }
}

/**
 * 正在编辑的布局设置类型。
 */
private enum class LayoutType {
    PadMode,
    CrossCount,
    HCrossCount,
}

/**
 * 单个布局设置项：展示标题与当前选项摘要，点击后打开选择对话框。
 */
@Composable
private fun LayoutSettingItem(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        onClick = onClick,
    )
}

/**
 * 布局选项选择对话框：展示互斥选项列表，选中后立即关闭并回调。
 */
@Composable
private fun LayoutSelectDialog(
    title: String,
    currentValue: Int,
    options: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    OverlayDialog(
        title = title,
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { (value, label) ->
                BasicComponent(
                    title = label,
                    onClick = { onSelected(value) },
                    endActions = {
                        CheckIndicator(selected = currentValue == value)
                    },
                )
            }
        }
    }
}

/**
 * 将平板模式数值转换为展示文案；若数值不在选项范围内，返回默认 "V:H"。
 */
private fun Int.toPadModeLabel(): String {
    return PAD_MODE_OPTIONS.firstOrNull { it.first == this }?.second ?: "V:H"
}

/**
 * 将固定列数数值转换为展示文案；若数值不在选项范围内，返回默认 "2"。
 */
private fun Int.toCrossCountLabel(): String {
    return CROSS_COUNT_OPTIONS.firstOrNull { it.first == this }?.second ?: "2"
}

/**
 * 平板模式选项：0=V:H、1=V:V、2=H:H，与旧 Flutter 版 padMode 取值约定一致。
 */
private val PAD_MODE_OPTIONS = listOf(
    0 to "V:H",
    1 to "V:V",
    2 to "H:H",
)

/**
 * 固定网格列数选项：2 / 3 / 4，与旧 Flutter 版 crossCount / hCrossCount 取值约定一致。
 */
private val CROSS_COUNT_OPTIONS = listOf(
    2 to "2",
    3 to "3",
    4 to "4",
)

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val containerWidth = maxWidth.value
        val columnCount = max((containerWidth / sliderValue).toInt(), 1)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = strings.crossAdapterThreshold.format(sliderValue.toInt(), columnCount),
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
                text = strings.crossAdapterPreview,
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
