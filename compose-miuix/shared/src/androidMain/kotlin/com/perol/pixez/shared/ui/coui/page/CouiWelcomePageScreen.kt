package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiRadioOptionRow
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 欢迎页设置：设置骨架 + 单选分组卡片。
 *
 * 选项编码与 [com.perol.pixez.shared.data.settings.SettingsRepository.welcomePageType] 保持一致；
 * 选中态由官方 RadioButton 指示器承担，整行 selectable 提供点击与无障碍语义，点中即写库。
 */
@Composable
internal fun CouiWelcomePageScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var selectedType by remember { mutableStateOf(settingsRepository.welcomePageType) }

    val options = remember(strings) {
        listOf(
            "home" to strings.tabRecommend,
            "rank" to strings.tabRanking,
            "quick_view" to strings.tabSpotlight,
            "search" to strings.tabSearch,
            "setting" to strings.tabSettings,
        )
    }

    CouiSettingsScaffold(
        title = strings.settingWelcomePage,
        onBack = onBack,
    ) {
        item(key = "coui_welcome_page", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionStartup) {
                options.forEachIndexed { index, (type, label) ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = label,
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            settingsRepository.welcomePageType = type
                        },
                    )
                }
            }
        }
    }
}
