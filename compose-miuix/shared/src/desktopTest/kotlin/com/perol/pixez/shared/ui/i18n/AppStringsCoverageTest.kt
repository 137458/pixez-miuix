package com.perol.pixez.shared.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * AppStrings 各语言实现覆盖测试：接口为漏翻成员提供了语言混杂的默认值，
 * 缺 override 时会静默回退（非中文语言看到中文）。本测试锁定关键用户可见成员
 * 必须有对应语言的显式实现。
 */
class AppStringsCoverageTest {

    /** 非中文语言：小说字数必须有本语言显式实现，不得回退到中文默认值"$count 字"。 */
    @Test
    fun `非中文语言的小说字数使用本语言格式而非中文回退`() {
        // 每语言精确期望（日语「文字」合法含汉字"字"，故不能用 contains("字") 判定）
        val expectedTexts = linkedMapOf(
            "En" to ("100 words" to EnStrings),
            "Ja" to ("100文字" to JaStrings),
            "Ko" to ("100자" to KoStrings),
            "Ru" to ("100 слов" to RuStrings),
            "Es" to ("100 palabras" to EsStrings),
            "Tr" to ("100 kelime" to TrStrings),
            "Id" to ("100 kata" to IdStrings),
            "Fil" to ("100 salita" to FilStrings),
            "De" to ("100 Wörter" to DeStrings),
        )
        for ((name, pair) in expectedTexts) {
            val (want, strings) = pair
            assertEquals(want, strings.formatNovelWordCount(100), "$name 的 formatNovelWordCount 回退到默认值")
        }
    }

    @Test
    fun `英文小说章节导航与字号不回退到中文`() {
        assertFalse(EnStrings.novelPrevChapter.contains("章"), "En novelPrevChapter 回退到中文: ${EnStrings.novelPrevChapter}")
        assertFalse(EnStrings.novelNextChapter.contains("章"), "En novelNextChapter 回退到中文: ${EnStrings.novelNextChapter}")
        assertFalse(EnStrings.novelFontSize.contains("号"), "En novelFontSize 回退到中文: ${EnStrings.novelFontSize}")
        assertEquals("Previous Chapter", EnStrings.novelPrevChapter)
        assertEquals("Next Chapter", EnStrings.novelNextChapter)
        assertEquals("Font Size", EnStrings.novelFontSize)
    }

    @Test
    fun `繁中小说字号使用繁体而非简体回退`() {
        assertEquals("字號", ZhTwStrings.novelFontSize)
        assertEquals("上一章", ZhTwStrings.novelPrevChapter)
        assertEquals("下一章", ZhTwStrings.novelNextChapter)
    }

    @Test
    fun `中文语言字数格式保持中文`() {
        assertEquals("100 字", ZhCnStrings.formatNovelWordCount(100))
        assertEquals("100 字", ZhTwStrings.formatNovelWordCount(100))
    }

    @Test
    fun `日语关键功能区不回退到中文默认值`() {
        assertEquals("スタンプ", JaStrings.stamp)
        assertEquals("再生", JaStrings.ugoiraPlay)
        assertEquals("一時停止", JaStrings.ugoiraPause)
        assertEquals("アカウント管理", JaStrings.accountManageTitle)
        assertEquals("小説", JaStrings.novelBrowseTitle)
        assertEquals("文字サイズ", JaStrings.novelFontSize)
        assertEquals("前の章", JaStrings.novelPrevChapter)
        assertEquals("Pixiv ID", JaStrings.pixivId)
    }
}
