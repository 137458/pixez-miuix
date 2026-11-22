package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 画质设置页：设置骨架 + 下拉弹窗分组卡片（与 MIUIX 版画质设置结构一致）。
 *
 * 四项画质偏好的取值索引与回写口径不变，选中下标按 `coerceIn(0, lastIndex)` 收口，点中即写库。
 */
@Composable
internal fun CouiQualitySettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var feedPreviewQuality by remember { mutableIntStateOf(settingsRepository.feedPreviewQuality) }
    var pictureQuality by remember { mutableIntStateOf(settingsRepository.pictureQuality) }
    var mangaQuality by remember { mutableIntStateOf(settingsRepository.mangaQuality) }
    var zoomQuality by remember { mutableIntStateOf(settingsRepository.zoomQuality) }

    val feedPreviewQualityOptions = remember(strings) {
        listOf(strings.qualityMedium, strings.qualityLarge, strings.qualityLow)
    }
    val detailQualityOptions = remember(strings) {
        listOf(strings.qualityLarge, strings.qualityOriginal, strings.qualityMedium)
    }
    val zoomQualityOptions = remember(strings) {
        listOf(strings.qualityOriginal, strings.qualityLarge, strings.qualityMedium)
    }

    CouiSettingsScaffold(
        title = strings.settingQuality,
        onBack = onBack,
    ) {
        item(key = "coui_quality_options", contentType = "section") {
            CouiSettingSection(title = strings.settingQuality) {
                CouiMenuSelectRow(
                    title = strings.feedPreviewQuality,
                    items = feedPreviewQualityOptions,
                    selectedIndex = feedPreviewQuality.coerceIn(0, feedPreviewQualityOptions.lastIndex),
                    onSelect = { index ->
                        feedPreviewQuality = index
                        settingsRepository.feedPreviewQuality = index
                    },
                )
                CouiRowDivider()
                CouiMenuSelectRow(
                    title = strings.pictureQuality,
                    items = detailQualityOptions,
                    selectedIndex = pictureQuality.coerceIn(0, detailQualityOptions.lastIndex),
                    onSelect = { index ->
                        pictureQuality = index
                        settingsRepository.pictureQuality = index
                    },
                )
                CouiRowDivider()
                CouiMenuSelectRow(
                    title = strings.mangaQuality,
                    items = detailQualityOptions,
                    selectedIndex = mangaQuality.coerceIn(0, detailQualityOptions.lastIndex),
                    onSelect = { index ->
                        mangaQuality = index
                        settingsRepository.mangaQuality = index
                    },
                )
                CouiRowDivider()
                CouiMenuSelectRow(
                    title = strings.zoomQuality,
                    items = zoomQualityOptions,
                    selectedIndex = zoomQuality.coerceIn(0, zoomQualityOptions.lastIndex),
                    onSelect = { index ->
                        zoomQuality = index
                        settingsRepository.zoomQuality = index
                    },
                )
            }
        }
    }
}
