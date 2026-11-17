package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSettingsLazyColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository

/**
 * COUI 欢迎页设置：固定顶栏 + 单选分组卡片。
 *
 * 选项编码与 [com.perol.pixez.shared.data.settings.SettingsRepository.welcomePageType] 保持一致；
 * 选中态由共用 [CouiRadioRow] 承担：整行 selectable 提供点击与无障碍语义，指示器用 COUI 库 RadioButton。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingWelcomePage,
            onBack = onBack,
        )

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            CouiSettingsLazyColumn {
                item(key = "coui_welcome_page", contentType = "section") {
                    CouiSettingSection(title = strings.settingSectionStartup) {
                        options.forEach { (type, label) ->
                            CouiRadioRow(
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
    }
}
