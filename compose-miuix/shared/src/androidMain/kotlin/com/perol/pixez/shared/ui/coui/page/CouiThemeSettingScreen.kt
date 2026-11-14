package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSelectRow
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.parseHexColor
import com.perol.pixez.shared.ui.skin.UiSkin
import com.perol.pixez.shared.ui.skin.isCouiSkinSupported
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 主题设置页：固定顶栏 + 主题/皮肤/显示/个性化四组卡片。
 *
 * 主题模式、界面皮肤、AMOLED、动态取色、种子色、调色板风格与 2025 规范开关全部保留；
 * 差异只有皮肤层：单选/开关/下拉改用 COUI 行组件，种子色改用 COUI `Dialog`
 * 承载预设色板与 HEX 输入（COUI 无连续拾色器，等价交互为预设色 + 自定义 HEX）。
 */
@Composable
internal fun CouiThemeSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current

    var themeMode by remember { mutableStateOf(settingsRepository.themeMode) }
    var isAmoled by remember { mutableStateOf(settingsRepository.isAmoled) }
    var useDynamicColor by remember { mutableStateOf(settingsRepository.useDynamicColor) }
    var seedColor by remember { mutableStateOf(settingsRepository.seedColor ?: 0xFF2196F3.toInt()) }
    var paletteStyle by remember { mutableStateOf(settingsRepository.miuixPaletteStyle) }
    var useSpec2025 by remember { mutableStateOf(settingsRepository.miuixUseSpec2025) }
    var uiSkinIndex by remember { mutableStateOf(settingsRepository.uiSkinIndex) }

    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    val paletteStyleOptions = listOf(
        "TonalSpot (Material You)",
        "Neutral",
        "Vibrant",
        "Expressive",
        "Rainbow",
        "FruitSalad",
        "Monochrome",
        "Fidelity",
        "Content",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.settingTheme,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = LocalBottomBarContentPadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_theme_mode", contentType = "section") {
                    CouiSettingSection(title = strings.settingTheme) {
                        CouiRadioRow(
                            title = strings.themeModeSystem,
                            selected = themeMode == 0,
                            onClick = {
                                themeMode = 0
                                settingsRepository.themeMode = 0
                            },
                        )
                        CouiRadioRow(
                            title = strings.themeModeLight,
                            selected = themeMode == 1,
                            onClick = {
                                themeMode = 1
                                settingsRepository.themeMode = 1
                            },
                        )
                        CouiRadioRow(
                            title = strings.themeModeDark,
                            selected = themeMode == 2,
                            onClick = {
                                themeMode = 2
                                settingsRepository.themeMode = 2
                            },
                        )
                    }
                }

                if (isCouiSkinSupported) {
                    item(key = "coui_theme_skin", contentType = "section") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CouiSettingSection(title = strings.settingUiSkin) {
                                CouiRadioRow(
                                    title = strings.skinHyperOs,
                                    selected = uiSkinIndex == UiSkin.HYPEROS.storageIndex,
                                    onClick = {
                                        uiSkinIndex = UiSkin.HYPEROS.storageIndex
                                        settingsRepository.uiSkinIndex = UiSkin.HYPEROS.storageIndex
                                    },
                                )
                                CouiRadioRow(
                                    title = strings.skinColorOs,
                                    selected = uiSkinIndex == UiSkin.COLOROS.storageIndex,
                                    onClick = {
                                        uiSkinIndex = UiSkin.COLOROS.storageIndex
                                        settingsRepository.uiSkinIndex = UiSkin.COLOROS.storageIndex
                                    },
                                )
                            }
                            Text(
                                text = strings.settingUiSkinSummary,
                                kind = CouiTextKind.ItemSummary,
                                color = CouiTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 22.dp),
                            )
                        }
                    }
                }

                item(key = "coui_theme_layout", contentType = "section") {
                    CouiSettingSection(title = strings.settingSectionDisplayLayout) {
                        CouiSwitchRow(
                            title = strings.themeAmoled,
                            summary = strings.themeAmoledSummary,
                            checked = isAmoled,
                            onCheckedChange = {
                                isAmoled = it
                                settingsRepository.isAmoled = it
                            },
                        )
                        CouiSwitchRow(
                            title = strings.themeDynamicColor,
                            summary = if (useDynamicColor) {
                                strings.themeDynamicColorSummaryOn
                            } else {
                                strings.themeDynamicColorSummaryOff
                            },
                            checked = useDynamicColor,
                            onCheckedChange = {
                                useDynamicColor = it
                                settingsRepository.useDynamicColor = it
                            },
                        )
                    }
                }

                if (!useDynamicColor) {
                    item(key = "coui_theme_seed", contentType = "section") {
                        CouiSettingSection(title = strings.dialogPickSeedColor) {
                            CouiSeedColorRow(
                                color = seedColor,
                                onClick = { showColorPicker = true },
                            )
                        }
                    }
                }

                item(key = "coui_theme_personalization", contentType = "section") {
                    CouiSettingSection(title = strings.themePersonalization) {
                        CouiSelectRow(
                            title = strings.dialogPaletteStyle,
                            items = paletteStyleOptions,
                            selectedIndex = paletteStyle.coerceIn(0, paletteStyleOptions.lastIndex),
                            onSelect = {
                                paletteStyle = it
                                settingsRepository.miuixPaletteStyle = it
                            },
                        )
                        CouiSwitchRow(
                            title = strings.themeSpec2025,
                            summary = if (useSpec2025) {
                                strings.themeSpec2025SummaryOn
                            } else {
                                strings.themeSpec2025SummaryOff
                            },
                            checked = useSpec2025,
                            onCheckedChange = {
                                useSpec2025 = it
                                settingsRepository.miuixUseSpec2025 = it
                            },
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        CouiSeedColorDialog(
            currentColor = seedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { selectedColor ->
                seedColor = selectedColor
                settingsRepository.seedColor = selectedColor
                showColorPicker = false
            },
        )
    }
}

/** 种子色行：标题 + 说明 + 右侧色块。 */
@Composable
private fun CouiSeedColorRow(
    color: Int,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.dialogPickSeedColor,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.dialogPickSeedColorSummary,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(color))
                .border(
                    width = 1.dp,
                    color = scheme.onSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                ),
        )
    }
}

/**
 * 种子色弹窗：COUI 无连续拾色器，用预设色板 + 自定义 HEX 提供等价取色能力。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiSeedColorDialog(
    currentColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current

    // 预设颜色，对齐 Flutter 版 ColorPickPage.skinList。
    val presetColors = listOf(
        0xFF00BCD4.toInt() to strings.themePresetCyan,
        0xFFE91E63.toInt() to strings.themePresetPink,
        0xFF4CAF50.toInt() to strings.themePresetGreen,
        0xFF795548.toInt() to strings.themePresetBrown,
        0xFF9C27B0.toInt() to strings.themePresetPurple,
        0xFF2196F3.toInt() to strings.themePresetBlue,
        0xFFFB7299.toInt() to strings.themePresetBilibiliPink,
    )

    var selectedColor by remember { mutableStateOf(Color(currentColor)) }
    var customHex by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogPickSeedColor,
        message = strings.dialogPickSeedColorSummary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.confirm,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = {
                if (customHex.isNotBlank()) {
                    val parsed = parseHexColor(customHex)
                    if (parsed != null) {
                        onColorSelected(parsed)
                    } else {
                        toastSink(ToastData(strings.themeColorFormatError, ToastType.Error))
                    }
                } else {
                    onColorSelected(selectedColor.copy(alpha = 1f).toArgb())
                }
            },
        ),
        dismissAction = CouiDialogAction(
            text = strings.cancel,
            onClick = onDismiss,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 4,
            ) {
                presetColors.forEach { (color, name) ->
                    CouiColorPresetItem(
                        color = color,
                        name = name,
                        selected = color == selectedColor.copy(alpha = 1f).toArgb(),
                        onClick = {
                            selectedColor = Color(color)
                            customHex = ""
                        },
                    )
                }
            }
            CouiHexInputField(
                value = customHex,
                placeholder = strings.themeDialogCustomColorLabel,
                onValueChange = { hex ->
                    customHex = hex
                    val parsed = parseHexColor(hex)
                    if (parsed != null) {
                        selectedColor = Color(parsed)
                    }
                },
            )
        }
    }
}

/** 预设颜色项：色块 + 名称，选中态用强调色描边。 */
@Composable
private fun CouiColorPresetItem(
    color: Int,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(color))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) scheme.accent else scheme.onSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
        )
        Text(
            text = name,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** HEX 输入框：COUI 容器底色 + 胶囊圆角。 */
@Composable
private fun CouiHexInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.container)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
