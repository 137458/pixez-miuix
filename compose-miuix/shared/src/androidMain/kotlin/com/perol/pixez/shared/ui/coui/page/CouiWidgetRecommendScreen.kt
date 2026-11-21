package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiRadioOptionRow
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 桌面小组件设置页：设置骨架 + 内容来源与独立图源两个单选分组。
 *
 * 内容来源与独立图源的取值、以及 `rank` 归并到「日榜」的选中口径保持不变；
 * 选中态由官方 RadioButton 指示器承担，整行 selectable 提供点击与无障碍语义。
 */
@Composable
internal fun CouiWidgetRecommendScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var selectedType by remember { mutableStateOf(settingsRepository.widgetIllustType) }
    var selectedPictureSource by remember { mutableStateOf(settingsRepository.widgetPictureSource) }

    val feedOptions = remember(strings) {
        listOf(
            "recom" to strings.tabRecommend,
            "day" to strings.rankingDay,
            "week" to strings.rankingWeek,
            "month" to strings.rankingMonth,
            "day_male" to strings.rankingDayMale,
            "day_female" to strings.rankingDayFemale,
            "news" to strings.tabNew,
            "follow" to strings.widgetSourceFollow,
        )
    }
    val pictureSourceOptions = remember(strings) {
        listOf(
            "" to strings.settingWidgetPictureSourceFollowGlobal,
            PixivHosts.HOST_PXIMG to strings.settingWidgetPictureSourceOfficial,
            PixivHosts.HOST_PIXIV_RE to strings.settingWidgetPictureSourceMirror,
        )
    }

    CouiSettingsScaffold(
        title = strings.settingWidgetRecommend,
        onBack = onBack,
    ) {
        item(key = "coui_widget_feed", contentType = "section") {
            CouiSettingSection(title = strings.settingWidgetFeedSection) {
                feedOptions.forEachIndexed { index, (type, label) ->
                    if (index > 0) CouiRowDivider()
                    val isSelected = when (type) {
                        // 旧版存量值 rank 归并到日榜，与 MIUIX 版选中口径一致。
                        "day" -> selectedType == "day" || selectedType == "rank"
                        else -> selectedType == type
                    }
                    CouiRadioOptionRow(
                        title = label,
                        selected = isSelected,
                        onClick = {
                            selectedType = type
                            settingsRepository.widgetIllustType = type
                        },
                    )
                }
            }
        }

        item(key = "coui_widget_picture_source", contentType = "section") {
            CouiSettingSection(title = strings.settingWidgetPictureSourceSection) {
                pictureSourceOptions.forEachIndexed { index, (source, label) ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = label,
                        selected = selectedPictureSource == source,
                        onClick = {
                            selectedPictureSource = source
                            settingsRepository.widgetPictureSource = source
                        },
                    )
                }
            }
        }
    }
}
