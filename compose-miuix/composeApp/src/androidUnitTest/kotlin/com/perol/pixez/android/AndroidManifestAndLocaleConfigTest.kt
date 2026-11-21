package com.perol.pixez.android

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Android 资源与 Manifest 基础约束测试（运行于 composeApp test）：
 * 确保自定义 scheme 放开 host 限制、locales_config 与代码侧语言表精确一致、波斯语资源存在且完整。
 */
class AndroidManifestAndLocaleConfigTest {

    private fun composeAppDir(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".").canonicalFile
        return if (cwd.name == "composeApp") cwd else File(cwd, "composeApp")
    }

    @Test
    fun `AndroidManifest 对 pixiv 和 pixez scheme 放开 host 过滤`() {
        val manifest = File(composeAppDir(), "src/main/AndroidManifest.xml").readText()
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

    /**
     * R-09 整改：locales_config 的断言从「每条期望语言被 contains 命中」升级为「解析 XML 后
     * 声明集合与期望集合精确相等」——旧写法抓不到多声明一条（例如误加 en-GB）、漏声明一条
     * 以外的情况，也无法发现重复声明。
     *
     * 期望集合的取证（两侧对齐，不凭推测写常数）：
     * 1. 代码侧语言码（13 项）：
     *    compose-miuix/shared/src/main/kotlin/com/perol/pixez/shared/platform/AppLanguageTags.kt
     *    第 14-28 行 APP_LANGUAGE_CODES = en-US, zh-CN, zh-TW, ja, ko, ru, es, tr, id, fil, de, vi, fa。
     * 2. 实现侧语言数（13 个）：compose-miuix/shared/src/main/kotlin/com/perol/pixez/shared/ui/i18n/
     *    AppStrings.kt 第 755-769 行 fromLanguageNum 的 1..12 分支（ZhCn/ZhTw/Ja/Ko/Ru/Es/Tr/Id/Fil/De/Vi）
     *    加 else 的 EnStrings，共 12 个实现；FA 在 FaStrings.kt:7 的 FaStrings，合计 13 个语言实现，
     *    与第 1 条的 13 项语言码一一对应。
     * 3. 为什么还多一条 `en`：AppLanguageTags.kt 第 11-12 行注释与第 39 行 languageTagForNum(0) -> "en"
     *    规定「协议与展示沿用 en-US，平台层资源覆盖用基础标签 en」；而 compose-miuix/composeApp/src/
     *    main/res 下没有 values-en，英文由默认 values 目录承载（见同仓 LocaleResourcesParityTest
     *    的目录映射表），所以 per-app language 声明的合法标签集合是 13 条语言码再并上基础标签 en，
     *    合计 14 条：14 = 13 ∪ {en}。
     */
    @Test
    fun `Android locales_config 精确声明 13 国语言码与 en 基础标签`() {
        val configFile = File(composeAppDir(), "src/main/res/xml/locales_config.xml")
        assertTrue(configFile.isFile, "未找到 locales_config.xml：" + configFile.absolutePath)

        val declaredInOrder = parseDeclaredLocales(configFile)
        val declared = declaredInOrder.toSet()

        val appLanguageCodes = listOf(
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
            "fa",
        )
        val expected = appLanguageCodes.toSet() + "en"

        assertEquals(
            declaredInOrder.size,
            declared.size,
            "locales_config.xml 存在重复声明的语言：" + declaredInOrder +
                "（去重后 " + declared.size + " 条）",
        )
        assertEquals(
            expected.size,
            declared.size,
            "locales_config.xml 声明的语言条数应为 " + expected.size + " 条（13 语言码 + 基础标签 en），" +
                "实测 " + declared.size + " 条：" + declared.sorted(),
        )
        assertEquals(
            expected.sorted(),
            declared.sorted(),
            "locales_config.xml 声明的语言集合与代码侧不一致：" +
                "代码侧多出（资源里漏声明）=" + (expected - declared).sorted() +
                "；XML 多出（代码侧无此语言）=" + (declared - expected).sorted() +
                "；实测声明=" + declared.sorted(),
        )

        val faStrings = File(composeAppDir(), "src/main/res/values-fa/strings.xml").readText()
        assertTrue(faStrings.contains("""name="shortcut_ranking""""))
        assertTrue(faStrings.contains("""name="widget_description""""))
    }

    /**
     * 解析 locales_config.xml，按文档顺序取每条 `<locale android:name="..."/>` 的 name。
     *
     * 用 DOM 解析而不是子串匹配：子串匹配无法区分「声明了 en」与「声明了 en-US」这类前缀包含，
     * 也无法发现重复声明。命名空间感知关闭，属性名以 `android:name` 形式出现，局部名兜底。
     */
    private fun parseDeclaredLocales(configFile: File): List<String> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val document = factory.newDocumentBuilder().parse(configFile)
        val localeNodes = document.getElementsByTagName("locale")
        return (0 until localeNodes.length).map { index ->
            val element = localeNodes.item(index) as org.w3c.dom.Element
            val named = element.attributes.getNamedItem("android:name")
                ?: (0 until element.attributes.length)
                    .map { element.attributes.item(it) }
                    .firstOrNull { it.nodeName.endsWith("name") }
            requireNotNull(named) { "locales_config.xml 第 " + (index + 1) + " 条 <locale> 缺少 name 属性" }
            named.nodeValue
        }
    }
}
