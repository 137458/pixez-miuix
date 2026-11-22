package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.Sponsor

/**
 * COUI 语言设置页：折叠大标题顶栏 + 语言下拉弹窗选择行 + 当前语言的 Sponsor 横滚条。
 *
 * 语言索引写入 [com.perol.pixez.shared.data.settings.SettingsRepository.languageNum] 后即刻生效
 * （由根宿主订阅 changeVersion 实时切换语言与 RTL 排版）。
 */
@Composable
internal fun CouiLanguageSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var selectedIndex by remember(settingsRepository.languageNum) {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }
    val selectedLanguage = LANGUAGE_OPTIONS[selectedIndex]
    val languageLabels = remember {
        LANGUAGE_OPTIONS.map { option -> "${option.nativeName} (${option.displayName})" }
    }

    CouiSettingsScaffold(
        title = strings.settingLanguage,
        onBack = onBack,
    ) {
        item(key = "coui_language", contentType = "section") {
            CouiSettingSection(title = strings.settingLanguage) {
                CouiMenuSelectRow(
                    title = strings.settingLanguage,
                    items = languageLabels,
                    selectedIndex = selectedIndex,
                    summary = selectedLanguage.code,
                    onSelect = { index ->
                        selectedIndex = index
                        settingsRepository.languageNum = index
                    },
                )
            }
        }

        if (selectedLanguage.sponsors.isNotEmpty()) {
            item(key = "coui_language_sponsors", contentType = "section") {
                CouiSettingSection(title = strings.sponsor) {
                    CouiSponsorRow(sponsors = selectedLanguage.sponsors)
                }
            }
        }
    }
}

/** Sponsor 横向列表：圆形头像 + 名称。 */
@Composable
private fun CouiSponsorRow(sponsors: List<Sponsor>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sponsors.forEach { sponsor ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AsyncImage(
                    model = sponsor.avatar,
                    contentDescription = sponsor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = sponsor.name,
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}
