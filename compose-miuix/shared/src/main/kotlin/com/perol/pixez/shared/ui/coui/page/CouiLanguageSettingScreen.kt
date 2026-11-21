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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.core.CouiRadioOptionRow
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.Sponsor
import com.perol.pixez.shared.ui.LocalSettingsRepository

/**
 * COUI 语言设置页：折叠大标题顶栏 + 语言单选分组 + 当前语言的 Sponsor 横滚条。
 *
 * 语言索引写入 [SettingsRepository.languageNum] 后即刻生效（由根宿主订阅 changeVersion 实时切换语言与 RTL 排版）；
 * 选中态由 core 的 [CouiRadioOptionRow] 承担：整行 selectable 提供点击与无障碍语义，
 * 圆形指示器由官方 RadioButton 绘制，行与指示器共享同一 InteractionSource 以避免双重触发。
 * 分组间距、卡片圆角、行间分隔线全部来自官方 Group / Item / Divider。
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

    CouiSettingsScaffold(
        title = strings.settingLanguage,
        onBack = onBack,
    ) {
        item(key = "coui_language", contentType = "section") {
            CouiSettingSection(title = strings.settingLanguage) {
                LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                    if (index > 0) CouiRowDivider()
                    CouiRadioOptionRow(
                        title = "${option.nativeName} (${option.displayName})",
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            settingsRepository.languageNum = index
                        },
                        summary = option.code,
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
