package com.perol.pixez.shared.platform

/**
 * 应用内语言序号（`SettingsRepository.languageNum`）到语言码的有序表。
 *
 * 顺序与旧 Flutter 版 `languages.dart` 锁定：languageNum 语义依赖序号，存量用户设置按序号
 * 存取，禁止调整顺序（架构审查 中-1）。语言码是网络协议（PixivHeaders 的 Accept-Language，
 * 装配层 [com.perol.pixez.shared.AppDependencies] 注入）与 ui.i18n 语言选择列表
 * `LANGUAGE_OPTIONS` 的单点数据源，避免双表漂移。
 *
 * 与 [languageTagForNum] 的取值差异仅在序号 0：协议与展示沿用旧版完整码 "en-US"，
 * 平台层资源覆盖用基础标签 "en"，其余序号两侧同值。
 */
internal val APP_LANGUAGE_CODES = listOf(
    "en-US",
    "zh-CN",
    "zh-TW",
    "ja",
    "ko",
    "ru",
    "es",
    "tr",
    "id",
    "fil",
    "de",
    "vi",
)

/**
 * 应用内语言序号（`SettingsRepository.languageNum`）到 BCP-47 语言标签的映射。
 *
 * 供 Android 平台层按用户设定的界面语言覆盖 `Configuration`，使通知、系统分享面板、Toast
 * 这类走系统资源解析的文案与应用内 `AppStrings` 语言保持一致。
 * 序号语义与 `AppStrings.fromLanguageNum` 与 [APP_LANGUAGE_CODES] 下标双方对齐：
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

/**
 * 按应用内语言序号（`SettingsRepository.languageNum`）解析 Pixiv 协议语言码（Accept-Language），
 * 越界序号回退既有默认 "zh-CN"。
 *
 * 装配层 [com.perol.pixez.shared.AppDependencies] 与小组件后台拉取（WidgetRepository）共用
 * 此入口（架构审查 中-14），保证 App 内与小组件请求语言一致；与 [languageTagForNum] 的取值
 * 差异仅在序号 0：协议沿用完整码 "en-US"，平台资源覆盖用基础标签 "en"。
 */
internal fun languageCodeForNum(languageNum: Int): String =
    APP_LANGUAGE_CODES.getOrNull(languageNum) ?: "zh-CN"
