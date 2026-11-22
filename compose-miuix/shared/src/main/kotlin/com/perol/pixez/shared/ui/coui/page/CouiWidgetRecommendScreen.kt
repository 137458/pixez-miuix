package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings

/** 解析小组件推荐类型在候选列表中的选中下标，兼容旧版 `rank` 归并到 `day`。 */
internal fun resolveCouiWidgetFeedIndex(selectedType: String, optionTypes: List<String>): Int {
    val normalized = if (selectedType == "rank") "day" else selectedType
    return optionTypes.indexOf(normalized).coerceAtLeast(0)
}

/**
 * COUI 桌面小组件设置页：设置骨架 + 内容来源与独立图源下拉弹窗分组。
 *
 * 内容来源与独立图源的取值、以及 `rank` 归并到「日榜」的选中口径保持不变。
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
        item(key = "coui_widget_options", contentType = "section") {
            CouiSettingSection(title = strings.settingWidgetRecommend) {
                CouiMenuSelectRow(
                    title = strings.settingWidgetFeedSection,
                    items = feedOptions.map { it.second },
                    selectedIndex = resolveCouiWidgetFeedIndex(selectedType, feedOptions.map { it.first }),
                    onSelect = { index ->
                        val type = feedOptions.getOrNull(index)?.first ?: return@CouiMenuSelectRow
                        selectedType = type
                        settingsRepository.widgetIllustType = type
                    },
                )
                CouiRowDivider()
                CouiMenuSelectRow(
                    title = strings.settingWidgetPictureSourceSection,
                    items = pictureSourceOptions.map { it.second },
                    selectedIndex = pictureSourceOptions.indexOfFirst { it.first == selectedPictureSource }.coerceAtLeast(0),
                    onSelect = { index ->
                        val source = pictureSourceOptions.getOrNull(index)?.first ?: return@CouiMenuSelectRow
                        selectedPictureSource = source
                        settingsRepository.widgetPictureSource = source
                    },
                )
            }
        }
    }
}
