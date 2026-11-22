package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 欢迎页设置：设置骨架 + 下拉弹窗选择行。
 *
 * 选项编码与 [com.perol.pixez.shared.data.settings.SettingsRepository.welcomePageType] 保持一致，
 * 点中菜单项即写库。
 */
@Composable
internal fun CouiWelcomePageScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var selectedType by remember {
        mutableStateOf(
            settingsRepository.welcomePageType.let { saved ->
                if (saved == "search") "home" else saved
            },
        )
    }

    val options = remember(strings) {
        listOf(
            "home" to strings.tabRecommend,
            "rank" to strings.tabRanking,
            "quick_view" to strings.tabNew,
            "spotlight" to strings.tabSpotlight,
            "setting" to strings.tabSettings,
        )
    }

    CouiSettingsScaffold(
        title = strings.settingWelcomePage,
        onBack = onBack,
    ) {
        item(key = "coui_welcome_page", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionStartup) {
                CouiMenuSelectRow(
                    title = strings.settingWelcomePage,
                    items = options.map { it.second },
                    selectedIndex = options.indexOfFirst { it.first == selectedType }.coerceAtLeast(0),
                    onSelect = { index ->
                        val target = options.getOrNull(index)?.first ?: return@CouiMenuSelectRow
                        selectedType = target
                        settingsRepository.welcomePageType = target
                    },
                )
            }
        }
    }
}
