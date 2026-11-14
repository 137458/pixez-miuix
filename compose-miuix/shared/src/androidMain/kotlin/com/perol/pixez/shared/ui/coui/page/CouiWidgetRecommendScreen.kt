package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 桌面小组件设置页：固定顶栏 + 两个单选分组卡片。
 *
 * 内容来源与独立图源的取值、以及 `rank` 归并到「日榜」的选中口径保持不变；
 * COUI 库无单选控件，选中态走共用 [CouiRadioRow] 的圆点指示。
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
            AppConstants.Network.HOST_PXIMG to strings.settingWidgetPictureSourceOfficial,
            AppConstants.Network.HOST_PIXIV_RE to strings.settingWidgetPictureSourceMirror,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingWidgetRecommend,
            onBack = onBack,
        )

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
                item(key = "coui_widget_feed", contentType = "section") {
                    CouiSettingSection(title = strings.settingWidgetFeedSection) {
                        feedOptions.forEach { (type, label) ->
                            val isSelected = when (type) {
                                // 旧版存量值 rank 归并到日榜，与 MIUIX 版选中口径一致。
                                "day" -> selectedType == "day" || selectedType == "rank"
                                else -> selectedType == type
                            }
                            CouiRadioRow(
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
                        pictureSourceOptions.forEach { (source, label) ->
                            CouiRadioRow(
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
    }
}
