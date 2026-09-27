package com.perol.pixez.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 应用内语言序号到 BCP-47 标签的映射测试：该映射驱动 Android 平台层（通知/分享面板/Toast）的
 * 资源语言，序号语义须与 `AppStrings.fromLanguageNum` 一一对应且不重复，否则切换界面语言后
 * 平台层会取到另一种语言、或静默回退到默认（简体中文）资源。
 */
class AppLanguageTagsTest {

    @Test
    fun `1 到 11 各自映射到唯一标签`() {
        val tags = (1..11).map { languageTagForNum(it) }

        assertTrue(tags.all { it != null }, "1..11 都应有标签，实际 $tags")
        assertEquals(11, tags.distinct().size, "标签不得重复: $tags")
        assertEquals("zh-CN", languageTagForNum(1))
        assertEquals("zh-TW", languageTagForNum(2))
        assertEquals("vi", languageTagForNum(11))
    }

    @Test
    fun `跟随系统与未知序号返回空表示不覆盖语言`() {
        assertNull(languageTagForNum(0))
        assertNull(languageTagForNum(-1))
        assertNull(languageTagForNum(12))
        assertNull(languageTagForNum(Int.MAX_VALUE))
    }
}
