package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.slider.Slider
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.data.settings.parseCrossAdapterWidthInput
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.coui.core.CouiToggleRow
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import kotlin.math.max

/**
 * COUI 布局设置页：设置骨架 + 官方 Group 卡片。
 *
 * 底栏模式、平板模式、竖屏/横屏列数与自适应阈值的取值与回写口径与 MIUIX 版一致。
 * 自适应阈值用 COUI 库 Slider + 数值行内输入保留连续调节与精确输入：拖动中只更新预览值，松手才落盘，
 * 与 MIUIX 版的写入时机一致（每帧写设置会让阈值抖动且放大磁盘写入）。
 */
@Composable
internal fun CouiLayoutSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var crossCount by remember { mutableIntStateOf(settingsRepository.crossCount) }
    var hCrossCount by remember { mutableIntStateOf(settingsRepository.hCrossCount) }
    var crossAdapt by remember { mutableStateOf(settingsRepository.crossAdapt) }
    var crossAdapterWidth by remember { mutableIntStateOf(settingsRepository.crossAdapterWidth) }
    var hCrossAdapt by remember { mutableStateOf(settingsRepository.hCrossAdapt) }
    var hCrossAdapterWidth by remember { mutableIntStateOf(settingsRepository.hCrossAdapterWidth) }
    var useFloatingBottomBar by remember { mutableStateOf(settingsRepository.useFloatingBottomBar) }
    var displayMode by remember { mutableIntStateOf(settingsRepository.displayMode) }

    val crossCountOptions = remember { listOf("2", "3", "4", "5", "6") }
    // 显示模式下拉选项：索引与 displayMode 数值一一对应（0 跟随系统 / 1 60Hz / 2 高刷）。
    val displayModeLabels = remember(strings) {
        listOf(
            strings.platformDisplayModeFollowSystem,
            strings.refreshRate60Hz,
            strings.refreshRateHigh,
        )
    }

    CouiSettingsScaffold(
        title = strings.settingLayout,
        onBack = onBack,
    ) {
        item(key = "coui_layout_bottom_bar", contentType = "section") {
            CouiSettingSection(title = strings.floatingBottomBar) {
                CouiToggleRow(
                    title = strings.floatingBottomBar,
                    summary = if (useFloatingBottomBar) {
                        strings.floatingBottomBarSummaryOn
                    } else {
                        strings.floatingBottomBarSummaryOff
                    },
                    checked = useFloatingBottomBar,
                    onCheckedChange = { checked ->
                        useFloatingBottomBar = checked
                        settingsRepository.useFloatingBottomBar = checked
                    },
                )
            }
        }

        item(key = "coui_layout_display_mode", contentType = "section") {
            CouiSettingSection(title = strings.dialogDisplayMode) {
                CouiMenuSelectRow(
                    title = strings.dialogDisplayMode,
                    items = displayModeLabels,
                    selectedIndex = displayMode.coerceIn(0, displayModeLabels.lastIndex),
                    onSelect = { index ->
                        displayMode = index
                        settingsRepository.displayMode = index
                    },
                )
            }
        }

        item(key = "coui_layout_portrait", contentType = "section") {
            CouiSettingSection(title = strings.crossCountPortrait) {
                CouiToggleRow(
                    title = strings.crossAdaptAuto,
                    summary = if (crossAdapt) {
                        strings.crossAdaptAutoSummaryOn
                    } else {
                        strings.crossAdaptAutoSummaryOff
                    },
                    checked = crossAdapt,
                    onCheckedChange = { checked ->
                        crossAdapt = checked
                        settingsRepository.crossAdapt = checked
                    },
                )
                CouiRowDivider()
                if (crossAdapt) {
                    CouiAdapterWidthSlider(
                        width = crossAdapterWidth,
                        onWidthChangeFinished = { newWidth ->
                            crossAdapterWidth = newWidth
                            settingsRepository.crossAdapterWidth = newWidth
                        },
                    )
                } else {
                    CouiMenuSelectRow(
                        title = strings.crossCountPortrait,
                        items = crossCountOptions,
                        selectedIndex = (crossCount - 2).coerceIn(0, crossCountOptions.lastIndex),
                        onSelect = { index ->
                            val count = index + 2
                            crossCount = count
                            settingsRepository.crossCount = count
                        },
                    )
                }
            }
        }

        item(key = "coui_layout_landscape", contentType = "section") {
            CouiSettingSection(title = strings.crossCountLandscape) {
                CouiToggleRow(
                    title = strings.crossAdaptAuto,
                    summary = if (hCrossAdapt) {
                        strings.crossAdaptAutoSummaryOn
                    } else {
                        strings.crossAdaptAutoSummaryOff
                    },
                    checked = hCrossAdapt,
                    onCheckedChange = { checked ->
                        hCrossAdapt = checked
                        settingsRepository.hCrossAdapt = checked
                    },
                )
                CouiRowDivider()
                if (hCrossAdapt) {
                    CouiAdapterWidthSlider(
                        width = hCrossAdapterWidth,
                        onWidthChangeFinished = { newWidth ->
                            hCrossAdapterWidth = newWidth
                            settingsRepository.hCrossAdapterWidth = newWidth
                        },
                    )
                } else {
                    CouiMenuSelectRow(
                        title = strings.crossCountLandscape,
                        items = crossCountOptions,
                        selectedIndex = (hCrossCount - 2).coerceIn(0, crossCountOptions.lastIndex),
                        onSelect = { index ->
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

/**
 * 宽度阈值滑块与实时预览。
 *
 * 轨道滑动保留连续调节；右侧数值可点击切换为行内输入，用精确值回写。
 */
@Composable
private fun CouiAdapterWidthSlider(
    width: Int,
    onWidthChangeFinished: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    var sliderValue by remember(width) { mutableFloatStateOf(width.toFloat()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidth = maxWidth.value
        val columnCount = max((containerWidth / sliderValue).toInt(), 1)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = strings.crossAdapterThreshold.format(sliderValue.toInt(), columnCount),
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!editing) {
                    // 数值即入口：点击切换为行内输入，避免小屏长距离拖动才能命中目标阈值。
                    Text(
                        text = "${sliderValue.toInt()} dp",
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.accent,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                inputText = sliderValue.toInt().toString()
                                editing = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            // 库 Slider 的分工与自绘滑块一致：onValueChange 连续回传只在本地更新预览，
            // onValueChangeFinished 在抬手/取消时触发，落盘只发生一次。
            // 库控件自带 disabled 与 Progress 语义（javap 见 SemanticsPropertiesKt.disabled/setProgress），
            // 读屏可播报并调节数值；colors 不传即库默认令牌
            // （CouiSliderDefaults.colors(accent, isDark, enabled)），不再自绘轨道配色。
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = SettingsRepository.CROSS_ADAPTER_WIDTH_MIN.toFloat()..
                    SettingsRepository.CROSS_ADAPTER_WIDTH_MAX.toFloat(),
                onValueChangeFinished = { onWidthChangeFinished(sliderValue.toInt()) },
            )

            if (editing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(scheme.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = strings.crossAdapterWidthInputHint,
                                kind = CouiTextKind.ItemSummary,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { text ->
                                inputText = text.filter { it in '0'..'9' }.take(4)
                            },
                            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
                            cursorBrush = SolidColor(scheme.accent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        )
                    }
                    CouiTextButton(
                        text = strings.cancel,
                        onClick = {
                            editing = false
                            focusManager.clearFocus()
                        },
                        filled = false,
                    )
                    CouiTextButton(
                        text = strings.confirm,
                        enabled = parseCrossAdapterWidthInput(inputText) != null,
                        onClick = {
                            val parsed = parseCrossAdapterWidthInput(inputText)
                            if (parsed != null) {
                                sliderValue = parsed.toFloat()
                                onWidthChangeFinished(parsed)
                            }
                            editing = false
                            focusManager.clearFocus()
                        },
                    )
                }
            }

            Text(
                text = strings.crossAdapterPreview,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            CouiPreviewGrid(
                columnCount = columnCount,
                itemCount = AppConstants.CrossAdapter.PREVIEW_ITEM_COUNT,
            )
        }
    }
}

/** 简易网格预览：纯色方块展示当前阈值下的列数效果。 */
@Composable
private fun CouiPreviewGrid(
    columnCount: Int,
    itemCount: Int,
) {
    val scheme = CouiTheme.colorScheme
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.container),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
