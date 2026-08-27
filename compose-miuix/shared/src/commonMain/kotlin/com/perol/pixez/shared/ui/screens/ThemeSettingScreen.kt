package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.ToastMessage

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 主题设置页：提供主题模式、AMOLED、动态颜色与种子色设置。
 *
 * @param settingsRepository 设置仓库，用于读写主题相关偏好。
 * @param onBack 返回上一级页面。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前主题设置。
    var themeMode by remember { mutableIntStateOf(settingsRepository.themeMode) }
    var isAmoled by remember { mutableStateOf(settingsRepository.isAmoled) }
    var useDynamicColor by remember { mutableStateOf(settingsRepository.useDynamicColor) }
    var seedColor by remember { mutableIntStateOf(settingsRepository.seedColor ?: DEFAULT_SEED_COLOR) }
    var paletteStyle by remember { mutableIntStateOf(settingsRepository.miuixPaletteStyle) }
    var useSpec2025 by remember { mutableStateOf(settingsRepository.miuixUseSpec2025) }

    // 颜色选择对话框显示状态。
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    // 本地修改辅助函数：写回仓库并更新页面状态。
    fun setThemeMode(value: Int) {
        themeMode = value
        settingsRepository.themeMode = value
    }

    fun setIsAmoled(value: Boolean) {
        isAmoled = value
        settingsRepository.isAmoled = value
    }

    fun setUseDynamicColor(value: Boolean) {
        useDynamicColor = value
        settingsRepository.useDynamicColor = value
    }

    fun setSeedColor(value: Int) {
        seedColor = value
        settingsRepository.seedColor = value
    }

    fun setPaletteStyle(value: Int) {
        paletteStyle = value
        settingsRepository.miuixPaletteStyle = value
    }

    fun setUseSpec2025(value: Boolean) {
        useSpec2025 = value
        settingsRepository.miuixUseSpec2025 = value
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()

    val paletteStyleOptions = remember {
        listOf(
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
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingTheme,
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
                    SmallTitle(text = strings.settingTheme)
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        RadioButtonPreference(
                            title = strings.themeModeSystem,
                            selected = themeMode == 0,
                            onClick = { setThemeMode(0) },
                        )
                        RadioButtonPreference(
                            title = strings.themeModeLight,
                            selected = themeMode == 1,
                            onClick = { setThemeMode(1) },
                        )
                        RadioButtonPreference(
                            title = strings.themeModeDark,
                            selected = themeMode == 2,
                            onClick = { setThemeMode(2) },
                        )
                    }
                }

                item {
                    SmallTitle(text = strings.settingSectionDisplayLayout)
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        SwitchPreference(
                            title = strings.themeAmoled,
                            summary = strings.themeAmoledSummary,
                            checked = isAmoled,
                            onCheckedChange = { setIsAmoled(it) },
                        )
                        SwitchPreference(
                            title = strings.themeDynamicColor,
                            summary = if (useDynamicColor) strings.themeDynamicColorSummaryOn else strings.themeDynamicColorSummaryOff,
                            checked = useDynamicColor,
                            onCheckedChange = { setUseDynamicColor(it) },
                        )
                    }
                }

                if (!useDynamicColor) {
                    item {
                        SmallTitle(text = strings.dialogPickSeedColor)
                        top.yukonga.miuix.kmp.basic.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            BasicComponent(
                                title = strings.dialogPickSeedColor,
                                summary = strings.dialogPickSeedColorSummary,
                                onClick = { showColorPicker = true },
                                endActions = {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(seedColor))
                                            .border(
                                                width = 1.dp,
                                                color = MiuixTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(6.dp),
                                            ),
                                    )
                                },
                            )
                        }
                    }
                }

                item {
                    SmallTitle(text = strings.themePersonalization)
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        OverlayDropdownPreference(
                            title = strings.dialogPaletteStyle,
                            items = paletteStyleOptions,
                            selectedIndex = paletteStyle.coerceIn(0, paletteStyleOptions.lastIndex),
                            onSelectedIndexChange = { setPaletteStyle(it) },
                        )
                        SwitchPreference(
                            title = strings.themeSpec2025,
                            summary = if (useSpec2025) strings.themeSpec2025SummaryOn else strings.themeSpec2025SummaryOff,
                            checked = useSpec2025,
                            onCheckedChange = { setUseSpec2025(it) },
                        )
                    }
                }
            }
        }

        ColorPickerDialog(
            show = showColorPicker,
            currentColor = seedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { selectedColor ->
                setSeedColor(selectedColor)
                showColorPicker = false
            },
        )
    }
}

/**
 * 颜色选择对话框：提供预设颜色网格与自定义 HEX 输入。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    show: Boolean,
    currentColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

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

    var customHex by remember(show) { mutableStateOf("") }
    var toastMessage by remember(show) { mutableStateOf<String?>(null) }

    OverlayDialog(
        title = strings.dialogPickSeedColor,
        summary = strings.dialogPickSeedColorSummary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 当前选中的颜色预览。
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(currentColor))
                        .border(
                            width = 1.dp,
                            color = MiuixTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp),
                        ),
                )
                Text(
                    text = strings.themeDialogCurrentColor,
                    style = MiuixTheme.textStyles.body1,
                )
            }

            // 预设颜色网格。
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 4,
            ) {
                presetColors.forEach { (color, name) ->
                    ColorPresetItem(
                        color = color,
                        name = name,
                        selected = color == currentColor,
                        onClick = { onColorSelected(color) },
                    )
                }
            }

            // 自定义 HEX 输入。
            TextField(
                value = customHex,
                onValueChange = { customHex = it },
                label = strings.themeDialogCustomColorLabel,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = strings.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = strings.confirm,
                    onClick = {
                        val parsed = parseHexColor(customHex)
                        if (parsed != null) {
                            onColorSelected(parsed)
                        } else {
                            toastMessage = strings.themeColorFormatError
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

/**
 * 预设颜色项：色块 + 名称。
 */
@Composable
private fun ColorPresetItem(
    color: Int,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(color))
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MiuixTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MiuixTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                ),
        )
        Text(
            text = name,
            style = MiuixTheme.textStyles.footnote2,
        )
    }
}

/**
 * 解析 HEX 颜色字符串，支持 #RRGGBB 与 RRGGBB 两种格式。
 *
 * @return 解析成功返回 ARGB Int；失败返回 null。
 */
private fun parseHexColor(hex: String): Int? {
    val trimmed = hex.trim()
    if (trimmed.isBlank()) return null
    val withoutHash = if (trimmed.startsWith("#")) trimmed.substring(1) else trimmed
    if (withoutHash.length != 6) return null
    return try {
        // 固定不透明（alpha = 0xFF），与 ColorPickPage 行为一致。
        0xFF000000.toInt() or withoutHash.toInt(16)
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * 默认种子色：对齐 Flutter 版默认蓝色。
 */
internal const val DEFAULT_SEED_COLOR = 0xFF2196F3.toInt()
