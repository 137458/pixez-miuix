package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil3.compose.AsyncImage
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.Sponsor
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.blurBackdropSource
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 语言设置页：选择应用显示语言。
 *
 * 语言列表与 Sponsor 数据直接硬编码自旧 Flutter 版 `lib/page/about/languages.dart`，
 * 选中后写入 [SettingsRepository.languageNum]。
 * 本次仅持久化语言索引，不触发应用内实时切换语言或刷新 UI 文案。
 *
 * @param settingsRepository 设置仓库，用于读写语言索引。
 * @param onBack 返回上一级页面。
 */
@Composable
fun LanguageSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var selectedIndex by remember(settingsRepository.languageNum) {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }
    val selectedLanguage = LANGUAGE_OPTIONS[selectedIndex]
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingLanguage,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = com.perol.pixez.shared.ui.AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.settingLanguage)
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                            RadioButtonPreference(
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
                    item {
                        SmallTitle(text = strings.sponsor)
                        top.yukonga.miuix.kmp.basic.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            SponsorSection(sponsors = selectedLanguage.sponsors)
                        }
                    }
                }
            }
        }
    }
}


/**
 * Sponsor 列表：头像 + 名称，统一做展示。
 */
@Composable
private fun SponsorSection(sponsors: List<Sponsor>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sponsors.forEach { sponsor ->
            SponsorItem(sponsor = sponsor)
        }
    }
}

/**
 * 单个 Sponsor 项：圆形头像与名称纵向排列。
 * GitHub 头像不需要 Referer，直接使用 Coil [AsyncImage]。
 */
@Composable
private fun SponsorItem(sponsor: Sponsor) {
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
        Text(text = sponsor.name)
    }
}
