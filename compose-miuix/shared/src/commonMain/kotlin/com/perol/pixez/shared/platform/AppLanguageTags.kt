package com.perol.pixez.shared.platform

/**
 * 应用内语言序号（`SettingsRepository.languageNum`）到 BCP-47 语言标签的映射。
 *
 * 供 Android 平台层按用户设定的界面语言覆盖 `Configuration`，使通知、系统分享面板、Toast
 * 这类走系统资源解析的文案与应用内 `AppStrings` 语言保持一致。
 * 序号语义与 `AppStrings.fromLanguageNum` 与 `LANGUAGE_OPTIONS` 下标双方对齐：
 * 本项目没有「跟随系统」取值，0 就是英文；越界序号返回 null，表示不做任何覆盖。
 */
fun languageTagForNum(languageNum: Int): String? = when (languageNum) {
    0 -> "en"
    1 -> "zh-CN"
    2 -> "zh-TW"
    3 -> "ja"
    4 -> "ko"
    5 -> "ru"
    6 -> "es"
    7 -> "tr"
    8 -> "id"
    9 -> "fil"
    10 -> "de"
    11 -> "vi"
    else -> null
}
