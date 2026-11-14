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
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 欢迎页设置：固定顶栏 + 单选分组卡片。
 *
 * 选项编码与 [com.perol.pixez.shared.data.settings.SettingsRepository.welcomePageType] 保持一致；
 * COUI 库无单选控件，选中态走共用 [CouiRadioRow] 的圆点指示。
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
