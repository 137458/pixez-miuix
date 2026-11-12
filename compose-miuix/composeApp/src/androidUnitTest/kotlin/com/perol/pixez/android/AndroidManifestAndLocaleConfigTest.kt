package com.perol.pixez.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Android 资源与 Manifest 基础约束测试（运行于 composeApp androidUnitTest）：
 * 确保自定义 scheme 放开 host 限制、13 国语言 locales_config 与波斯语资源存在且完整。
 */
class AndroidManifestAndLocaleConfigTest {

    private fun composeAppDir(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".").canonicalFile
        return if (cwd.name == "composeApp") cwd else File(cwd, "composeApp")
    }

    @Test
    fun `AndroidManifest 对 pixiv 和 pixez scheme 放开 host 过滤`() {
        val manifest = File(composeAppDir(), "src/androidMain/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""<data android:scheme="pixiv" />"""))
        assertTrue(manifest.contains("""<data android:scheme="pixez" />"""))
        val schemeLines = manifest.lines().filter {
            it.contains("""android:scheme="pixiv"""") || it.contains("""android:scheme="pixez"""")
        }
        assertTrue(schemeLines.isNotEmpty())
        schemeLines.forEach { line ->
            assertFalse(line.contains("android:host="), "自定义 scheme 不应硬编码 host 过滤: $line")
        }
    }

    @Test
    fun `Android locales_config 包含 13 国语言及 values-fa 字符串資源`() {
        val appDir = composeAppDir()
        val localesXml = File(appDir, "src/androidMain/res/xml/locales_config.xml").readText()
        val expectedLocales = listOf("en", "zh-CN", "zh-TW", "ja", "ko", "ru", "es", "tr", "id", "fil", "de", "vi", "fa")
        expectedLocales.forEach { locale ->
            assertTrue(localesXml.contains("""android:name="$locale""""), "locales_config.xml 缺少 $locale")
        }

        val faStrings = File(appDir, "src/androidMain/res/values-fa/strings.xml").readText()
        assertTrue(faStrings.contains("""name="shortcut_ranking""""))
        assertTrue(faStrings.contains("""name="widget_description""""))
    }
}
