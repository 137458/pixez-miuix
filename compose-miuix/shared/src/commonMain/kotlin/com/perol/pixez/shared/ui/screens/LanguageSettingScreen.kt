package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

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
    var selectedIndex by remember(settingsRepository.languageNum, settingsRepository.changeVersion) {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }
    val selectedLanguage = LANGUAGE_OPTIONS[selectedIndex]
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingLanguage,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
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
                            BasicComponent(
                                title = "${option.nativeName} (${option.displayName})",
                                summary = option.code,
                                onClick = {
                                    selectedIndex = index
                                    settingsRepository.languageNum = index
                                },
                                endActions = {
                                    CheckIndicator(selected = selectedIndex == index)
                                },
                            )
                        }
                    }
                }

                if (selectedLanguage.sponsors.isNotEmpty()) {
                    item {
                        SmallTitle(text = strings.sponsor)
                    }
                    item {
                        SponsorSection(sponsors = selectedLanguage.sponsors)
                    }
                }
            }
        }
    }
}


/**
 * Sponsor 横向列表：头像 + 名称，统一只做展示。
 *
 * 任务要求 Android 端点击可跳转、Desktop 端不跳转；但当前仅有 `openBrowser` expect/actual，
 * 没有现成的平台判断工具，且本次仅允许创建屏幕文件，因此先统一只展示不跳转，
 * 后续若需要平台相关跳转可在平台层补充判断能力后再接入。
 */
@Composable
private fun SponsorSection(sponsors: List<Sponsor>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(sponsors.size) { index ->
            SponsorItem(sponsor = sponsors[index])
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

/**
 * Sponsor 数据：名称、头像 URL、个人主页 URL。
 */
internal data class Sponsor(
    val name: String,
    val avatar: String,
    val uri: String,
)

/**
 * 语言选项数据：语言代码、显示名称、本地名称与 Sponsor 列表。
 */
internal data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val sponsors: List<Sponsor> = emptyList(),
)

/**
 * 可选语言列表，顺序与旧 Flutter 版 `languages.dart` 保持一致。
 * languageNum 0 对应 `en-US`，后续按此顺序递增。
 */
internal val LANGUAGE_OPTIONS = listOf(
    LanguageOption(
        code = "en-US",
        displayName = "English (US)",
        nativeName = "English",
        sponsors = listOf(
            Sponsor(
                name = "Xian",
                avatar = "https://avatars.githubusercontent.com/u/34748039?v=4",
                uri = "https://github.com/itzXian",
            ),
            Sponsor(
                name = "Takase",
                avatar = "https://avatars.githubusercontent.com/u/20792268?v=4",
                uri = "https://github.com/takase1121",
            ),
        ),
    ),
    LanguageOption(
        code = "zh-CN",
        displayName = "简体中文",
        nativeName = "中文 (简体)",
        sponsors = listOf(
            Sponsor(
                name = "Skimige",
                avatar = "https://avatars.githubusercontent.com/u/9017470?v=4",
                uri = "https://github.com/Skimige",
            ),
        ),
    ),
    LanguageOption(
        code = "zh-TW",
        displayName = "繁體中文",
        nativeName = "中文 (繁體)",
        sponsors = listOf(
            Sponsor(
                name = "Tragic Life",
                avatar = "https://avatars.githubusercontent.com/u/16817202?v=4",
                uri = "https://github.com/TragicLifeHu",
            ),
        ),
    ),
    LanguageOption(
        code = "ja",
        displayName = "日本語",
        nativeName = "日本語",
        sponsors = listOf(
            Sponsor(
                name = "karin722",
                avatar = "https://avatars.githubusercontent.com/u/54385201?v=4",
                uri = "https://github.com/karin722",
            ),
            Sponsor(
                name = "arrow2nd",
                avatar = "https://avatars.githubusercontent.com/u/44780846?v=4",
                uri = "https://github.com/arrow2nd",
            ),
        ),
    ),
    LanguageOption(
        code = "ko",
        displayName = "한국어",
        nativeName = "한국어",
        sponsors = listOf(
            Sponsor(
                name = "San Kang",
                avatar = "https://avatars.githubusercontent.com/u/40086827?v=4",
                uri = "https://github.com/RivMt",
            ),
        ),
    ),
    LanguageOption(
        code = "ru",
        displayName = "Русский",
        nativeName = "Русский язык",
        sponsors = listOf(
            Sponsor(
                name = "Vlad Afonin",
                avatar = "https://avatars.githubusercontent.com/u/20505643?v=4",
                uri = "https://github.com/mytecor",
            ),
        ),
    ),
    LanguageOption(
        code = "es",
        displayName = "Español",
        nativeName = "Español",
        sponsors = listOf(
            Sponsor(
                name = "SugarBlank",
                avatar = "https://avatars.githubusercontent.com/u/64178604?v=4",
                uri = "https://github.com/SugarBlank",
            ),
        ),
    ),
    LanguageOption(
        code = "tr",
        displayName = "Türkçe",
        nativeName = "Türkçe",
        sponsors = listOf(
            Sponsor(
                name = "KYOYA",
                avatar = "https://avatars.githubusercontent.com/u/63583961?v=4",
                uri = "https://github.com/kyoyacchi",
            ),
        ),
    ),
    LanguageOption(
        code = "id",
        displayName = "Bahasa Indonesia",
        nativeName = "Bahasa Indonesia",
        sponsors = listOf(
            Sponsor(
                name = "ReikiAigawara",
                avatar = "https://avatars.githubusercontent.com/u/66962815?v=4",
                uri = "https://github.com/ReikiAigawara",
            ),
        ),
    ),
    LanguageOption(
        code = "fil",
        displayName = "Filipino",
        nativeName = "Wikang Filipino",
        sponsors = listOf(
            Sponsor(
                name = "searingmoonlight",
                avatar = "https://avatars.githubusercontent.com/u/114207889?v=4",
                uri = "https://github.com/searinminecraft",
            ),
        ),
    ),
    LanguageOption(
        code = "de",
        displayName = "Deutsch",
        nativeName = "Deutsch",
        sponsors = listOf(
            Sponsor(
                name = "PanChi",
                avatar = "https://avatars.githubusercontent.com/u/140990709?v=4",
                uri = "https://github.com/justpanchi",
            ),
        ),
    ),
)
