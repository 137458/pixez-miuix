package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSettingsLazyColumn
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.Sponsor
import com.perol.pixez.shared.ui.LocalSettingsRepository

/**
 * COUI 语言设置页：固定顶栏 + 语言单选分组卡片 + 当前语言的 Sponsor。
 *
 * 语言索引持久化口径不变（只写 [SettingsRepository.languageNum]，不做应用内实时切换）；
 * 选中态由共用 [CouiRadioRow] 承担：整行 selectable 提供点击与无障碍语义，指示器用 COUI 库 RadioButton。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingLanguage,
            onBack = onBack,
        )

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            CouiSettingsLazyColumn {
                item(key = "coui_language", contentType = "section") {
                    CouiSettingSection(title = strings.settingLanguage) {
                        LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                            CouiRadioRow(
                                title = "${option.nativeName} (${option.displayName})",
                                summary = option.code,
                                selected = selectedIndex == index,
                                onClick = {
                                    selectedIndex = index
                                    settingsRepository.languageNum = index
                                },
                            )
                        }
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
