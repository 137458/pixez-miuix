package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 布局设置页：管理平板模式、竖屏固定列数、横屏固定列数。
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

    // 当前正在编辑的布局类型，null 表示没有对话框打开。
    var editingType by rememberSaveable { mutableStateOf<LayoutType?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "布局设置",
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
                SmallTitle(text = "平板")
            }
            item {
                LayoutSettingItem(
                    title = "平板模式",
                    summary = padMode.toPadModeLabel(),
                    onClick = { editingType = LayoutType.PadMode },
                )
            }

            item {
                SmallTitle(text = "竖屏")
            }
            item {
                LayoutSettingItem(
                    title = "固定列数",
                    summary = crossCount.toCrossCountLabel(),
                    onClick = { editingType = LayoutType.CrossCount },
                )
            }

            item {
                SmallTitle(text = "横屏")
            }
            item {
                LayoutSettingItem(
                    title = "固定列数",
                    summary = hCrossCount.toCrossCountLabel(),
                    onClick = { editingType = LayoutType.HCrossCount },
                )
            }
        }

        // 布局选项选择对话框。
        val currentType = editingType
        if (currentType != null) {
            when (currentType) {
                LayoutType.PadMode -> LayoutSelectDialog(
                    title = currentType.title,
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
                    title = currentType.title,
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
                    title = currentType.title,
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
private enum class LayoutType(val title: String) {
    PadMode("平板模式"),
    CrossCount("竖屏固定列数"),
    HCrossCount("横屏固定列数"),
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
    SuperDialog(
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
