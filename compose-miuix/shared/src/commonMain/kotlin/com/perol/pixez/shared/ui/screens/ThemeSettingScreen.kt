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

    // 颜色选择对话框与调色板风格对话框显示状态。
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showPaletteStylePicker by rememberSaveable { mutableStateOf(false) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "主题设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
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
                SmallTitle(text = "主题模式")
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    ThemeModeOption(
                        label = "跟随系统",
                        selected = themeMode == 0,
                        onClick = { setThemeMode(0) },
                    )
                    ThemeModeOption(
                        label = "浅色",
                        selected = themeMode == 1,
                        onClick = { setThemeMode(1) },
                    )
                    ThemeModeOption(
                        label = "深色",
                        selected = themeMode == 2,
                        onClick = { setThemeMode(2) },
                    )
                }
            }

            item {
                SmallTitle(text = "显示")
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    BasicComponent(
                        title = "AMOLED 模式",
                        summary = "深色模式下使用纯黑背景，降低 OLED 屏幕耗电",
                        endActions = {
                            Switch(
                                checked = isAmoled,
                                onCheckedChange = { setIsAmoled(it) },
                            )
                        },
                    )
                    BasicComponent(
                        title = "动态颜色",
                        summary = if (useDynamicColor) "根据系统壁纸或种子色生成主题色" else "使用固定种子色",
                        endActions = {
                            Switch(
                                checked = useDynamicColor,
                                onCheckedChange = { setUseDynamicColor(it) },
                            )
                        },
                    )
                }
            }

            if (!useDynamicColor) {
                item {
                    SmallTitle(text = "种子色")
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        BasicComponent(
                            title = "选择种子色",
                            summary = "用于生成应用主题色",
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
                SmallTitle(text = "MIUIX 个性化")
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    BasicComponent(
                        title = "调色板风格",
                        summary = paletteStyleName(paletteStyle),
                        onClick = { showPaletteStylePicker = true },
                    )
                    BasicComponent(
                        title = "2025 色彩规范",
                        summary = if (useSpec2025) "使用新版 Spec2025 取色算法" else "使用兼容 Spec2021 取色算法",
                        endActions = {
                            Switch(
                                checked = useSpec2025,
                                onCheckedChange = { setUseSpec2025(it) },
                            )
                        },
                    )
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

        PaletteStylePickerDialog(
            show = showPaletteStylePicker,
            currentPaletteStyle = paletteStyle,
            onDismiss = { showPaletteStylePicker = false },
            onPaletteStyleSelected = { selectedStyle ->
                setPaletteStyle(selectedStyle)
                showPaletteStylePicker = false
            },
        )
    }
}

/**
 * 主题模式单选行。
 */
@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = label,
        onClick = onClick,
        endActions = {
            if (selected) {
                Text(text = "✓")
            }
        },
    )
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
    // 预设颜色，对齐 Flutter 版 ColorPickPage.skinList。
    val presetColors = listOf(
        0xFF00BCD4.toInt() to "青色",
        0xFFE91E63.toInt() to "粉色",
        0xFF4CAF50.toInt() to "绿色",
        0xFF795548.toInt() to "棕色",
        0xFF9C27B0.toInt() to "紫色",
        0xFF2196F3.toInt() to "蓝色",
        0xFFFB7299.toInt() to "哔哩粉",
    )

    var customHex by remember(show) { mutableStateOf("") }
    var toastMessage by remember(show) { mutableStateOf<String?>(null) }

    OverlayDialog(
        title = "选择种子色",
        summary = "点击预设颜色快速选择，或输入自定义 HEX 色值",
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
                    text = "当前",
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
                label = "自定义颜色 #RRGGBB",
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "确认",
                    onClick = {
                        val parsed = parseHexColor(customHex)
                        if (parsed != null) {
                            onColorSelected(parsed)
                        } else {
                            toastMessage = "颜色格式错误，请输入 #RRGGBB"
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
 * 调色板风格选择对话框：列出 MIUIX 支持的调色板风格并单选。
 */
@Composable
private fun PaletteStylePickerDialog(
    show: Boolean,
    currentPaletteStyle: Int,
    onDismiss: () -> Unit,
    onPaletteStyleSelected: (Int) -> Unit,
) {
    // 调色板风格列表，顺序与 [top.yukonga.miuix.kmp.theme.ThemePaletteStyle.entries] 保持一致。
    val paletteStyles = listOf(
        "TonalSpot" to "经典 Material You 色调",
        "Neutral" to "低饱和度、柔和中性",
        "Vibrant" to "高饱和度、鲜艳明快",
        "Expressive" to "大胆艺术、创意色移",
        "Rainbow" to "广色域彩虹渐变",
        "FruitSalad" to "活泼多彩、混合色相",
        "Monochrome" to "单色调灰阶",
        "Fidelity" to "最接近种子色",
        "Content" to "基于内容颜色取色",
    )

    OverlayDialog(
        title = "调色板风格",
        summary = "选择 Monet 动态取色的调色板风格",
        show = show,
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            paletteStyles.forEachIndexed { index, (name, description) ->
                BasicComponent(
                    title = name,
                    summary = description,
                    onClick = { onPaletteStyleSelected(index) },
                    endActions = {
                        if (index == currentPaletteStyle) {
                            Text(text = "✓")
                        }
                    },
                )
            }
        }
    }
}

/**
 * 根据调色板风格索引返回显示名称。
 */
private fun paletteStyleName(index: Int): String {
    // 顺序与 MIUIX [ThemePaletteStyle.entries] 一致，越界时回退到默认名称。
    return when (index) {
        0 -> "TonalSpot"
        1 -> "Neutral"
        2 -> "Vibrant"
        3 -> "Expressive"
        4 -> "Rainbow"
        5 -> "FruitSalad"
        6 -> "Monochrome"
        7 -> "Fidelity"
        8 -> "Content"
        else -> "TonalSpot"
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
