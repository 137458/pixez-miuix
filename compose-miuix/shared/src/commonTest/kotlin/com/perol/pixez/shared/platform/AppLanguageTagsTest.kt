package com.perol.pixez.shared.platform

import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 应用内语言序号到 BCP-47 标签的映射测试：该映射驱动 Android 平台层（通知/分享面板/Toast）的
 * 资源语言，序号语义必须同时与 `AppStrings.fromLanguageNum` 和语言选择列表下标对齐，
 * 否则切换界面语言后平台层会取到另一种语言、或静默回退到默认（简体中文）资源。
 *
 * 另覆盖下沉后的语言码有序表 [APP_LANGUAGE_CODES]（架构审查 中-1）：它是 Accept-Language
 * （AppDependencies 注入）与 ui.i18n 语言选择列表的单点数据源，兜底锁定其序号语义与同源引用。
 * 协议语言码解析 [languageCodeForNum]（架构审查 中-14）为装配层与小组件后台拉取的共用入口，
 * 一并锁定取表与越界回退语义。
 */
class AppLanguageTagsTest {

    @Test
    fun `有效序号各自映射到唯一标签`() {
        val tags = (0..11).map { languageTagForNum(it) }

        assertTrue(tags.all { it != null }, "0..11 都应有标签，实际 $tags")
        assertEquals(12, tags.distinct().size, "标签不得重复: $tags")
        assertEquals("en", languageTagForNum(0))
        assertEquals("zh-CN", languageTagForNum(1))
        assertEquals("zh-TW", languageTagForNum(2))
        assertEquals("vi", languageTagForNum(11))
    }

    @Test
    fun `越界序号返回空表示不覆盖语言`() {
        assertNull(languageTagForNum(-1))
        assertNull(languageTagForNum(12))
        assertNull(languageTagForNum(Int.MAX_VALUE))
    }

    /** 语言选择列表按下标写 languageNum，标签与列表项必须逐项同语言，漏一项就会串语言。 */
    @Test
    fun `标签映射与语言选择列表逐项对齐`() {
        LANGUAGE_OPTIONS.forEachIndexed { index, option ->
            val tag = languageTagForNum(index)

            assertEquals(
                option.code.substringBefore('-').lowercase(),
                tag?.substringBefore('-')?.lowercase(),
                "下标 $index（${option.code}）与 languageTagForNum 不一致",
            )
        }
    }

    /** 语言码表按序号驱动 Accept-Language，序号语义与长度锁定，出现重复码说明表已损坏。 */
    @Test
    fun `语言码有序表锁定序号语义且无重复`() {
        assertEquals(12, APP_LANGUAGE_CODES.size, "语言码数量必须与序号语义一致")
        assertEquals(12, APP_LANGUAGE_CODES.distinct().size, "语言码不得重复: $APP_LANGUAGE_CODES")
        assertEquals("en-US", APP_LANGUAGE_CODES[0])
        assertEquals("zh-CN", APP_LANGUAGE_CODES[1])
        assertEquals("vi", APP_LANGUAGE_CODES[11])
    }

    /** 语言选择列表的语言码必须单源引用语言码表，绕开引用回写字面量即视为双表回归。 */
    @Test
    fun `语言选择列表语言码逐项单源引用语言码有序表`() {
        assertEquals(APP_LANGUAGE_CODES, LANGUAGE_OPTIONS.map { it.code })
    }

    /** 标签表与码表除序号 0（en / en-US）外逐项同值，出现第二处分叉说明两侧映射已漂移。 */
    @Test
    fun `标签映射与语言码表除序号0外逐项同值`() {
        APP_LANGUAGE_CODES.forEachIndexed { index, code ->
            val tag = languageTagForNum(index)

            if (index == 0) {
                assertEquals("en", tag)
            } else {
                assertEquals(code, tag, "序号 $index 的标签与语言码不一致")
            }
        }
        assertNull(languageTagForNum(APP_LANGUAGE_CODES.size), "越界序号必须返回 null")
    }

    /**
     * 协议语言码解析（中-14）：小组件后台拉取与装配层共用同一入口。
     * 有效序号逐项取语言码表，越界序号回退沿用既有默认 zh-CN；
     * 序号 0 必须解析出 en-US，防止实现退化成硬编码 zh-CN（小组件旧缺陷）。
     */
    @Test
    fun `协议语言码有效序号逐项取表且越界回退zh-CN`() {
        assertEquals("en-US", languageCodeForNum(0), "序号 0 应解析为英文完整码")
        assertEquals("zh-CN", languageCodeForNum(1))
        assertEquals("ja", languageCodeForNum(3))
        assertEquals("vi", languageCodeForNum(11))
        APP_LANGUAGE_CODES.forEachIndexed { index, code ->
            assertEquals(code, languageCodeForNum(index), "序号 $index 未按语言码表解析")
        }
        assertEquals("zh-CN", languageCodeForNum(-1), "负序号应回退默认 zh-CN")
        assertEquals("zh-CN", languageCodeForNum(APP_LANGUAGE_CODES.size), "越界序号应回退默认 zh-CN")
        assertEquals("zh-CN", languageCodeForNum(Int.MAX_VALUE), "极端越界应回退默认 zh-CN")
    }
}
