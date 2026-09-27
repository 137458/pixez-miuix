package com.perol.pixez.shared.platform

import com.perol.pixez.shared.ui.screens.LANGUAGE_OPTIONS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 应用内语言序号到 BCP-47 标签的映射测试：该映射驱动 Android 平台层（通知/分享面板/Toast）的
 * 资源语言，序号语义必须同时与 `AppStrings.fromLanguageNum` 和语言选择列表下标对齐，
 * 否则切换界面语言后平台层会取到另一种语言、或静默回退到默认（简体中文）资源。
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
}
