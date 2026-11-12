package com.perol.pixez.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidLocaleResourcesTest {

    private fun androidResDir(): File {
        val candidates = listOf(
            File("src/androidMain/res"),
            File("composeApp/src/androidMain/res"),
            File("compose-miuix/composeApp/src/androidMain/res"),
        )
        return candidates.first { it.isDirectory }
    }

    @Test
    fun `locales_config declares all 13 supported app locales`() {
        val resDir = androidResDir()
        val localesConfig = File(resDir, "xml/locales_config.xml").readText()
        val expectedLocales = listOf(
            "en", "zh-CN", "zh-TW", "ja", "ko", "ru", "es", "tr", "id", "fil", "de", "vi", "fa",
        )
        val missing = expectedLocales.filter { code ->
            !localesConfig.contains("""android:name="$code"""")
        }
        assertTrue(missing.isEmpty(), "locales_config.xml 缺少语言声明: $missing")
    }

    @Test
    fun `values-fa provides Persian strings for Android shortcuts and widget`() {
        val resDir = androidResDir()
        val faStringsFile = File(resDir, "values-fa/strings.xml")
        assertTrue(faStringsFile.exists(), "缺少 Android 波斯语资源文件: ${faStringsFile.path}")
        val content = faStringsFile.readText()
        val requiredKeys = listOf(
            "app_name",
            "shortcut_ranking",
            "shortcut_search",
            "shortcut_downloads",
            "shortcut_history",
            "widget_name",
            "widget_description",
        )
        val missing = requiredKeys.filter { key -> !content.contains("""name="$key"""") }
        assertTrue(missing.isEmpty(), "values-fa/strings.xml 缺少词条: $missing")
    }
}
