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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

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

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingQuality,
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
        val qualityOptions3 = listOf(
            0 to strings.qualityLow,
            1 to strings.qualityMedium,
            2 to strings.qualityOriginal,
        )
        val qualityOptions2 = listOf(
            0 to strings.qualityLarge,
            1 to strings.qualityOriginal,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = strings.feedPreviewQuality)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    QualitySettingItem(
                        title = strings.feedPreviewQuality,
                        summary = feedPreviewQuality.toQualityLabel(qualityOptions3),
                        onClick = { editingType = QualityType.FeedPreview },
                    )
                }
            }

            item {
                SmallTitle(text = strings.pictureQuality)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    QualitySettingItem(
                        title = strings.pictureQuality,
                        summary = pictureQuality.toQualityLabel(qualityOptions3),
                        onClick = { editingType = QualityType.Picture },
                    )
                    QualitySettingItem(
                        title = strings.mangaQuality,
                        summary = mangaQuality.toQualityLabel(qualityOptions3),
                        onClick = { editingType = QualityType.Manga },
                    )
                }
            }

            item {
                SmallTitle(text = strings.zoomQuality)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    QualitySettingItem(
                        title = strings.zoomQuality,
                        summary = zoomQuality.toQualityLabel(qualityOptions2),
                        onClick = { editingType = QualityType.Zoom },
                    )
                }
            }
        }

        // 画质选择对话框。
        val currentType = editingType
        if (currentType != null) {
            val dialogTitle = when (currentType) {
                QualityType.FeedPreview -> strings.feedPreviewQuality
                QualityType.Picture -> strings.pictureQuality
                QualityType.Manga -> strings.mangaQuality
                QualityType.Zoom -> strings.zoomQuality
            }
            val dialogOptions = when (currentType) {
                QualityType.Zoom -> qualityOptions2
                else -> qualityOptions3
            }
            val currentValue = when (currentType) {
                QualityType.FeedPreview -> feedPreviewQuality
                QualityType.Picture -> pictureQuality
                QualityType.Manga -> mangaQuality
                QualityType.Zoom -> zoomQuality
            }
            QualitySelectDialog(
                title = dialogTitle,
                currentValue = currentValue,
                options = dialogOptions,
                onDismiss = { editingType = null },
                onSelected = { value ->
                    when (currentType) {
                        QualityType.FeedPreview -> {
                            feedPreviewQuality = value
                            settingsRepository.feedPreviewQuality = value
                        }
                        QualityType.Picture -> {
                            pictureQuality = value
                            settingsRepository.pictureQuality = value
                        }
                        QualityType.Manga -> {
                            mangaQuality = value
                            settingsRepository.mangaQuality = value
                        }
                        QualityType.Zoom -> {
                            zoomQuality = value
                            settingsRepository.zoomQuality = value
                        }
                    }
                    editingType = null
                },
            )
        }
    }
}

/**
 * 正在编辑的画质类型。
 */
private enum class QualityType {
    FeedPreview,
    Picture,
    Manga,
    Zoom,
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
 * 将画质数值转换为展示文案；若数值不在选项范围内，返回选项中的第一个文案作为兜底。
 */
private fun Int.toQualityLabel(options: List<Pair<Int, String>>): String {
    return options.firstOrNull { it.first == this }?.second
        ?: options.first().second
}

