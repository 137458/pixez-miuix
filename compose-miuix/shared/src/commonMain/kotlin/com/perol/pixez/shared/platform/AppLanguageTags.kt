package com.perol.pixez.shared.platform

/**
 * 应用内语言序号（`SettingsRepository.languageNum`）到 BCP-47 语言标签的映射。
 *
 * 供 Android 平台层按用户设定的界面语言覆盖 `Configuration`，使通知、系统分享面板、Toast
 * 这类走系统资源解析的文案与应用内 `AppStrings` 语言保持一致。
 * 序号语义必须与 `AppStrings.fromLanguageNum` 对齐；0 表示跟随系统语言，返回 null 不做覆盖。
 */
fun languageTagForNum(languageNum: Int): String? = when (languageNum) {
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
