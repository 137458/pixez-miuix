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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    // 读取当前 languageNum；若旧值越界则回退到 0（对应 en-US），与旧版行为一致。
    var selectedIndex by remember {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }
    val selectedLanguage = LANGUAGE_OPTIONS[selectedIndex]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "语言设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = "语言")
            }
            // 语言选项列表：每项显示语言代码，右侧对勾表示选中。
            items(LANGUAGE_OPTIONS.size) { index ->
                val option = LANGUAGE_OPTIONS[index]
                BasicComponent(
                    title = option.code,
                    onClick = {
                        selectedIndex = index
                        settingsRepository.languageNum = index
                    },
                    endActions = {
                        CheckIndicator(selected = selectedIndex == index)
                    },
                )
            }

            item {
                SmallTitle(text = "Sponsor")
            }
            // 展示当前选中语言的 Sponsor 头像与名称。
            item {
                SponsorSection(sponsors = selectedLanguage.sponsors)
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
private data class Sponsor(
    val name: String,
    val avatar: String,
    val uri: String,
)

/**
 * 语言选项数据：语言代码与该语言的 Sponsor 列表。
 */
private data class LanguageOption(
    val code: String,
    val sponsors: List<Sponsor>,
)

/**
 * 可选语言列表，顺序与旧 Flutter 版 `languages.dart` 保持一致。
 * languageNum 0 对应 `en-US`，后续按此顺序递增。
 */
private val LANGUAGE_OPTIONS = listOf(
    LanguageOption(
        code = "en-US",
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
        sponsors = listOf(
            Sponsor(
                name = "PanChi",
                avatar = "https://avatars.githubusercontent.com/u/140990709?v=4",
                uri = "https://github.com/justpanchi",
            ),
        ),
    ),
)
