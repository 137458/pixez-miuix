package com.perol.pixez.shared.ui.i18n

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
    // 越南语此前只存在于 AppStrings 与旧版迁移值中，选择列表里没有条目：
    // languageNum=11 的存量用户会被 coerce 到末位（德语）显示，界面文案却走 ViStrings。
    // 译文为机器翻译，未经母语者校对，故标注 beta。
    LanguageOption(
        code = "vi",
        displayName = "Vietnamese (machine translated, beta)",
        nativeName = "Tiếng Việt",
    ),
)
