package com.perol.pixez.shared.ui.i18n

import com.perol.pixez.shared.platform.APP_LANGUAGE_CODES
import com.perol.pixez.shared.platform.isRtlLanguage
import com.perol.pixez.shared.platform.languageTagForNum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * R-09 Android 语言资源与代码语言表的一致性守护测试。
 *
 * 为什么在 shared 的 desktopTest 里做：语言码的单一来源是 shared 的
 * [com.perol.pixez.shared.platform.APP_LANGUAGE_CODES] 与
 * [com.perol.pixez.shared.platform.languageTagForNum]（同为 internal，只有同模块的测试源集
 * 看得见），而语言资源目录在 composeApp 的 androidMain/res。两侧都在本仓库工作区内，
 * 纯 JVM 测试可以直接比对：不需要 Android 设备、不需要 Robolectric，也不会因为
 * 资源目录改名/漏建而在运行时才暴露。
 *
 * 判定口径（缺陷 R-09 的整改）：旧测试对 locales_config.xml 用子串 contains 校验，
 * 多一条 en-US、少一条 tr 都能通过。这里改为「解析目录集合 → 折算成语言码 → 与代码侧
 * 精确集合相等」，任何新增/删除/改名都会红，并打印双向差集。
 *
 * 目录名到语言码的映射表形态是决策 D3（取默认 A）的落地：只在测试里加映射，不改 res 目录名。
 * `values-in` 是 Android 遗留的 ISO 639 旧码（in = 现 id，印尼语），改名会牵动资源合并与
 * 存量设备上的语言匹配，因此保持目录名不变、由本表折算。
 */
class LocaleResourcesParityTest {

    /**
     * 断言 1：values* 目录集合经映射折算后，必须与代码侧平台语言标签集合逐项相等。
     *
     * 覆盖 R-09 的两个方向：目录多出来（例如误建 values-en、values-night）会红；
     * 目录缺失（例如新增语言只改了代码侧 [APP_LANGUAGE_CODES]）也会红。
     */
    @Test
    fun androidResValuesDirectoriesMatchPlatformLanguageTags() {
        val resDir = LocaleResourceSources.androidResDir()
        val valuesDirNames = LocaleResourceSources.valuesDirectories()

        val unmapped = valuesDirNames - LocaleResourceSources.VALUES_DIR_LANGUAGE.keys
        val missing = LocaleResourceSources.VALUES_DIR_LANGUAGE.keys - valuesDirNames
        assertEquals(
            emptySet(),
            unmapped + missing,
            "androidMain/res 下的 values* 目录与映射表不一致：" +
                "未登记目录=" + unmapped.sorted() +
                "，映射表登记但目录缺失=" + missing.sorted() +
                "；取证目录=" + resDir.absolutePath,
        )

        val mappedLanguages = LocaleResourceSources.valuesDirectories()
            .map { dirName -> LocaleResourceSources.VALUES_DIR_LANGUAGE.getValue(dirName) }
            .toSet()
        val codeLanguages = LocaleResourceSources.platformLanguageTags()

        assertEquals(
            codeLanguages.sorted(),
            mappedLanguages.sorted(),
            "语言资源目录折算结果与代码侧语言表不一致：" +
                LocaleResourceSources.describeDifference(codeLanguages, mappedLanguages) +
                "；目录明细=" + LocaleResourceSources.directoryEvidence(),
        )
    }

    /**
     * 断言 2：代码侧平台语言标签就是「13 国语言码，序号 0 用基础标签 en」。
     *
     * 这条断言把映射表的两个关键口径钉死在代码事实上，避免测试自己臆造：
     * - 数量 13，与 `AppLanguageCodes`（APP_LANGUAGE_CODES）一致；
     * - 序号 0 是 "en" 而不是 "en-US"：平台 Configuration 覆盖与 Android 资源目录都用基础标签
     *   （取证：shared/src/commonMain/kotlin/com/perol/pixez/shared/platform/AppLanguageTags.kt
     *   第 11-12 行注释与第 38-53 行 languageTagForNum）；
     * - 波斯语 fa 在表内且 isRtlLanguage(12) 为 true，RTL 不被漏掉。
     */
    @Test
    fun platformLanguageTagsAreThirteenCodeLanguagesWithEnBaseTag() {
        val codeLanguages = LocaleResourceSources.platformLanguageTags()

        assertEquals(
            APP_LANGUAGE_CODES.size,
            codeLanguages.size,
            "平台语言标签数量应与 APP_LANGUAGE_CODES 一致，实测 " + codeLanguages.size +
                " 个：" + codeLanguages.sorted(),
        )
        assertEquals(
            13,
            codeLanguages.size,
            "13 国语言基线被改动，实测 " + codeLanguages.size + " 个：" + codeLanguages.sorted(),
        )
        assertEquals(
            APP_LANGUAGE_CODES.map { code -> if (code == "en-US") "en" else code }.toSet(),
            codeLanguages,
            "平台语言标签应等价于语言码表仅把 en-US 换成基础标签 en：实测 " +
                LocaleResourceSources.describeDifference(
                    APP_LANGUAGE_CODES.map { code -> if (code == "en-US") "en" else code }.toSet(),
                    codeLanguages,
                ),
        )
        assertTrue("en" in codeLanguages, "平台语言标签必须包含基础标签 en，实测：" + codeLanguages.sorted())
        assertTrue("en-US" !in codeLanguages, "平台资源覆盖用基础标签 en，不应出现 en-US，实测：" + codeLanguages.sorted())
        assertTrue("fa" in codeLanguages, "波斯语 fa 必须在平台语言标签内，实测：" + codeLanguages.sorted())
        assertTrue(isRtlLanguage(12), "序号 12 必须是 RTL 语言（fa），否则 RTL 布局守护失效")
        assertEquals("fa", languageTagForNum(12), "序号 12 的语言标签应为 fa")
    }
}

/**
 * 语言资源侧取证工具：仓库根定位、values* 目录枚举、目录名到语言码的映射表。
 */
private object LocaleResourceSources {

    private const val ANDROID_RES = "compose-miuix/composeApp/src/androidMain/res"
    private const val SHARED_SRC = "compose-miuix/shared/src"

    /**
     * Android 资源目录名 → 语言码。
     *
     * 目录名形态由 Android 资源限定符规则决定：`values-<language>-r<REGION>`（例如
     * values-zh-rCN）。本表逐条登记，不做「去掉前缀就是语言码」的自动折算——自动折算会
     * 把 values-in 读成语言码 "in"（Android 遗留旧码，实为印尼语 id），也发现不了目录改名。
     */
    val VALUES_DIR_LANGUAGE: Map<String, String> = linkedMapOf(
        "values" to "en",
        "values-zh-rCN" to "zh-CN",
        "values-zh-rTW" to "zh-TW",
        "values-ja" to "ja",
        "values-ko" to "ko",
        "values-ru" to "ru",
        "values-es" to "es",
        "values-tr" to "tr",
        "values-in" to "id",
        "values-fil" to "fil",
        "values-de" to "de",
        "values-vi" to "vi",
        "values-fa" to "fa",
    )

    val repoRoot: File = locateRepoRoot()

    fun androidResDir(): File {
        val dir = File(repoRoot, ANDROID_RES)
        assertTrue(dir.isDirectory, "未找到 Android 资源目录：" + dir.absolutePath)
        return dir
    }

    /** res 下全部 values* 目录名（不含 values-night 之类的非语言限定符例外，出现即视为不一致）。 */
    fun valuesDirectories(): Set<String> =
        androidResDir().listFiles().orEmpty()
            .filter { it.isDirectory && (it.name == "values" || it.name.startsWith("values-")) }
            .map { it.name }
            .toSet()

    /** 代码侧平台语言标签集合：序号 0..12 经 languageTagForNum 解析。 */
    fun platformLanguageTags(): Set<String> =
        (0..APP_LANGUAGE_CODES.lastIndex).map { languageNum ->
            languageTagForNum(languageNum)
                ?: error("languageTagForNum(" + languageNum + ") 返回 null，平台语言标签出现断层")
        }.toSet()

    fun describeDifference(expected: Set<String>, actual: Set<String>): String =
        "期望 " + expected.size + " 个=" + expected.sorted() +
            "；实际 " + actual.size + " 个=" + actual.sorted() +
            "；代码侧多出（资源缺失）=" + (expected - actual).sorted() +
            "；资源侧多出（代码侧无此语言）=" + (actual - expected).sorted()

    fun directoryEvidence(): String = valuesDirectories().sorted().joinToString { dirName ->
        dirName + "->" + VALUES_DIR_LANGUAGE[dirName].orEmpty()
    }

    private fun locateRepoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            if (File(dir, SHARED_SRC).isDirectory) return dir
            dir = dir.parentFile
        }
        error("无法从工作目录 " + System.getProperty("user.dir") + " 向上定位仓库根（应包含 " + SHARED_SRC + "）")
    }
}
