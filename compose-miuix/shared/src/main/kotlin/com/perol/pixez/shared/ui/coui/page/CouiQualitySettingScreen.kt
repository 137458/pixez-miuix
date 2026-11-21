package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiRadioOptionRow
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 画质设置页：设置骨架 + 四项偏好的单选分组卡片。
 *
 * 四项画质偏好的取值索引与回写口径不变：每项偏好独立成组，候选顺序即索引顺序，
 * 选中下标仍按原口径 `coerceIn(0, lastIndex)` 收口，点中即写库。
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
        item(key = "coui_quality_feed_preview", contentType = "section") {
            CouiSettingSection(title = strings.feedPreviewQuality) {
                feedPreviewQualityOptions.forEachIndexed { index, label ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = label,
                        selected = feedPreviewQuality.coerceIn(0, feedPreviewQualityOptions.lastIndex) == index,
                        onClick = {
                            feedPreviewQuality = index
                            settingsRepository.feedPreviewQuality = index
                        },
                    )
                }
            }
        }

        item(key = "coui_quality_picture", contentType = "section") {
            CouiSettingSection(title = strings.pictureQuality) {
                detailQualityOptions.forEachIndexed { index, label ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = label,
                        selected = pictureQuality.coerceIn(0, detailQualityOptions.lastIndex) == index,
                        onClick = {
                            pictureQuality = index
                            settingsRepository.pictureQuality = index
                        },
                    )
                }
            }
        }

        item(key = "coui_quality_manga", contentType = "section") {
            CouiSettingSection(title = strings.mangaQuality) {
                detailQualityOptions.forEachIndexed { index, label ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = label,
                        selected = mangaQuality.coerceIn(0, detailQualityOptions.lastIndex) == index,
                        onClick = {
                            mangaQuality = index
                            settingsRepository.mangaQuality = index
                        },
                    )
                }
            }
        }

        item(key = "coui_quality_zoom", contentType = "section") {
            CouiSettingSection(title = strings.zoomQuality) {
                zoomQualityOptions.forEachIndexed { index, label ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = label,
                        selected = zoomQuality.coerceIn(0, zoomQualityOptions.lastIndex) == index,
                        onClick = {
                            zoomQuality = index
                            settingsRepository.zoomQuality = index
                        },
                    )
                }
            }
        }
    }
}
