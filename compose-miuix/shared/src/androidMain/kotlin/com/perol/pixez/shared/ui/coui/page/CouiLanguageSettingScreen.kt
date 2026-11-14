package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.Sponsor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 语言设置页：固定顶栏 + 语言单选分组卡片 + 当前语言的 Sponsor。
 *
 * 语言索引持久化口径不变（只写 [SettingsRepository.languageNum]，不做应用内实时切换）；
 * COUI 库无单选控件，选中态走共用 [CouiRadioRow] 的圆点指示。
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.settingLanguage,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

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
