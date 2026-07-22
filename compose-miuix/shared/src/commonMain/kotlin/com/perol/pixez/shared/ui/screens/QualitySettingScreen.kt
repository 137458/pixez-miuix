package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog

/**
 * 画质设置页：管理 Feed 预览、插画详情、漫画详情、大图缩放等画质偏好。
 *
 * @param settingsRepository 设置仓库，用于读写画质相关偏好。
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
    var zoomQuality by remember {
        mutableIntStateOf(settingsRepository.zoomQuality)
    }

    // 当前正在编辑的画质类型，null 表示没有对话框打开。
    var editingType by rememberSaveable { mutableStateOf<QualityType?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "画质设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                SmallTitle(text = "浏览")
            }
            item {
                QualitySettingItem(
                    title = "Feed 预览画质",
                    summary = feedPreviewQuality.toQualityLabel(QUALITY_OPTIONS_3),
                    onClick = { editingType = QualityType.FeedPreview },
                )
            }

            item {
                SmallTitle(text = "详情")
            }
            item {
                QualitySettingItem(
                    title = "插画详情页画质",
                    summary = pictureQuality.toQualityLabel(QUALITY_OPTIONS_3),
                    onClick = { editingType = QualityType.Picture },
                )
            }
            item {
                QualitySettingItem(
                    title = "漫画详情页画质",
                    summary = mangaQuality.toQualityLabel(QUALITY_OPTIONS_3),
                    onClick = { editingType = QualityType.Manga },
                )
            }

            item {
                SmallTitle(text = "预览")
            }
            item {
                QualitySettingItem(
                    title = "大图预览缩放画质",
                    summary = zoomQuality.toQualityLabel(QUALITY_OPTIONS_2),
                    onClick = { editingType = QualityType.Zoom },
                )
            }
        }

        // 画质选择对话框。
        val currentType = editingType
        if (currentType != null) {
            when (currentType) {
                QualityType.FeedPreview -> QualitySelectDialog(
                    title = currentType.title,
                    currentValue = feedPreviewQuality,
                    options = QUALITY_OPTIONS_3,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        feedPreviewQuality = value
                        settingsRepository.feedPreviewQuality = value
                        editingType = null
                    },
                )

                QualityType.Picture -> QualitySelectDialog(
                    title = currentType.title,
                    currentValue = pictureQuality,
                    options = QUALITY_OPTIONS_3,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        pictureQuality = value
                        settingsRepository.pictureQuality = value
                        editingType = null
                    },
                )

                QualityType.Manga -> QualitySelectDialog(
                    title = currentType.title,
                    currentValue = mangaQuality,
                    options = QUALITY_OPTIONS_3,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        mangaQuality = value
                        settingsRepository.mangaQuality = value
                        editingType = null
                    },
                )

                QualityType.Zoom -> QualitySelectDialog(
                    title = currentType.title,
                    currentValue = zoomQuality,
                    options = QUALITY_OPTIONS_2,
                    onDismiss = { editingType = null },
                    onSelected = { value ->
                        zoomQuality = value
                        settingsRepository.zoomQuality = value
                        editingType = null
                    },
                )
            }
        }
    }
}

/**
 * 正在编辑的画质类型。
 */
private enum class QualityType(val title: String) {
    FeedPreview("Feed 预览画质"),
    Picture("插画详情页画质"),
    Manga("漫画详情页画质"),
    Zoom("大图预览缩放画质"),
}

/**
 * 单个画质设置项：展示标题与当前选项摘要，点击后打开选择对话框。
 */
@Composable
private fun QualitySettingItem(
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
 * 画质选择对话框：展示互斥选项列表，选中后立即关闭并回调。
 */
@Composable
private fun QualitySelectDialog(
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
 * 将画质数值转换为展示文案；若数值不在选项范围内，返回选项中的第一个文案作为兜底。
 */
private fun Int.toQualityLabel(options: List<Pair<Int, String>>): String {
    return options.firstOrNull { it.first == this }?.second
        ?: options.first().second
}

/**
 * 三档画质选项：0=标准、1=高画质、2=原图。
 * 对应旧 Flutter 版 feedPreviewQuality / pictureQuality / mangaQuality 的取值约定。
 */
private val QUALITY_OPTIONS_3 = listOf(
    0 to "标准",
    1 to "高画质",
    2 to "原图",
)

/**
 * 两档缩放画质选项：0=高画质、1=原图。
 * 对应旧 Flutter 版 zoomQuality 的取值约定。
 */
private val QUALITY_OPTIONS_2 = listOf(
    0 to "高画质",
    1 to "原图",
)
