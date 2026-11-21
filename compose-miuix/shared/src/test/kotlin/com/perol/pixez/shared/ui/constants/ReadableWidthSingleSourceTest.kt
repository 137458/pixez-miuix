package com.perol.pixez.shared.ui.constants

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 可读内容宽度「单一来源」守护测试。
 *
 * 为什么用源码扫描而不是布局测试：
 * MIUIX 与 COUI 两侧的可读内容宽度都必须来自
 * `AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP`（760dp），当前全树共 36 个 .kt 文件引用它。
 * COUI 侧的封装 CouiReadableColumn.kt 与 MIUIX 侧的引用点均位于 shared/src/main
 * （MIUIX 侧散布在数十个页面里）；shared 模块没有引入 compose ui-test 依赖，纯 JVM 测试既无法组合这些页面，
 * 也无法测量 Compose 组件的布局结果。而这类回归的真实形态是「有人新写一处
 * widthIn(max = 760.dp)」或者「新增一份同名常量」——两者都是源码事实、不是运行时行为，
 * 因此直接对源码文本断言：跑得动、覆盖整棵树，失败时能直接指到文件与行号。
 *
 * 扫描范围：shared/src 下全部 .kt 文件，唯一例外是本测试文件自身（其中含有用于断言的
 * 字面量与正则，纳入扫描会自己命中自己）。
 */
class ReadableWidthSingleSourceTest {

    /**
     * 断言 1：常量在全树只出现一次声明，且这一处就在 AppConstants.kt。
     *
     * 只要出现第二处 `const val` 声明，可读宽度就可能悄悄分叉成两个数值。
     */
    @Test
    fun tabletContentMaxWidthIsDeclaredExactlyOnceInAppConstants() {
        val declaringFiles = SharedKotlinSources.sources.filter {
            it.text.contains(SharedKotlinSources.DECLARATION)
        }
        val declarationCount = SharedKotlinSources.sources.sumOf {
            countOccurrences(it.text, SharedKotlinSources.DECLARATION)
        }
        assertEquals(
            1,
            declarationCount,
            "可读宽度常量必须全树唯一声明，实测 " + declarationCount + " 处，分布：" +
                declaringFiles.joinToString { it.relativePath },
        )
        assertEquals(
            SharedKotlinSources.APP_CONSTANTS_PATH,
            declaringFiles.single().relativePath,
            "可读宽度常量必须定义在 AppConstants.kt，实测定义在 " +
                declaringFiles.single().relativePath,
        )
    }

    /**
     * 断言 2：全树不存在把可读宽度写成字面量的写法。
     *
     * 两条正则分别覆盖「把数字直接塞进 widthIn 的 max」与「760.dp 这种直接取值」；
     * 其中的空白类允许跨行，格式化拆行也躲不过去。
     */
    @Test
    fun noHardcodedReadableWidthLiteralsExist() {
        val patterns = linkedMapOf(
            "widthIn(max = 数字.dp)" to Regex("""widthIn\(\s*max\s*=\s*\d+\.(dp|Dp)"""),
            "760.dp" to Regex("""760\.dp"""),
        )
        for ((label, pattern) in patterns) {
            val hits = regexHits(pattern)
            assertEquals(
                0,
                hits.size,
                "禁止硬编码可读宽度字面量（" + label + "），实测 " + hits.size + " 处：" +
                    hits.joinToString { it.relativePath + ":" + it.line + " -> " + it.snippet },
            )
        }
    }

    /**
     * 断言 3：COUI 阅读列引用常量，且文件内不出现数值 760。
     *
     * 这是 COUI 侧唯一一处列宽实现，一旦就地写死数字，MIUIX 侧换宽度时两侧就不再一致。
     */
    @Test
    fun couiReadableColumnUsesTheSharedConstantWithoutTheLiteralValue() {
        val coui = SharedKotlinSources.textOf(SharedKotlinSources.COUI_READABLE_COLUMN_PATH)
        val identifierHits = countOccurrences(coui, SharedKotlinSources.WIDTH_IDENTIFIER)
        assertTrue(
            identifierHits >= 1,
            "CouiReadableColumn.kt 必须引用 " + SharedKotlinSources.WIDTH_IDENTIFIER +
                "，实测 " + identifierHits + " 次",
        )
        val literalHits = countOccurrences(coui, HARDCODED_WIDTH_VALUE)
        assertEquals(
            0,
            literalHits,
            "CouiReadableColumn.kt 不得出现硬编码宽度字面量 " + HARDCODED_WIDTH_VALUE +
                "，实测 " + literalHits + " 处",
        )
    }

    /**
     * 断言 4：引用该常量的 .kt 文件数不少于 30 个。
     *
     * 引用面是这份常量「真的是单一来源」的证据：数量骤降通常意味着某些页面改回了自己算宽度。
     */
    @Test
    fun atLeastThirtyKotlinFilesReferenceTheSharedWidthConstant() {
        val referencingFiles = SharedKotlinSources.sources.filter {
            it.text.contains(SharedKotlinSources.WIDTH_IDENTIFIER)
        }
        assertTrue(
            referencingFiles.size >= 30,
            "引用 " + SharedKotlinSources.WIDTH_IDENTIFIER + " 的文件数应 >= 30，实测 " +
                referencingFiles.size + " 个",
        )
    }

    /**
     * 断言 5：COUI 侧与 MIUIX 侧引用的是同一个标识符。
     *
     * 两侧按文件统计的命中数合计 >= 2，且 COUI 侧至少 1 次；再由断言 1 保证全树只有一处
     * 声明，因此两侧命中的同名标识符必然解析到 AppConstants.kt 里的那一份定义。
     * 同时禁止任一侧自行声明同名常量，避免「看起来同名、实际是本地副本」的假一致。
     */
    @Test
    fun couiAndMiuixSidesShareTheSameWidthIdentifier() {
        val coui = SharedKotlinSources.textOf(SharedKotlinSources.COUI_READABLE_COLUMN_PATH)
        val miuix = SharedKotlinSources.textOf(SharedKotlinSources.MIUIX_READABLE_WIDTH_CONSUMER_PATH)
        val couiHits = countOccurrences(coui, SharedKotlinSources.WIDTH_IDENTIFIER)
        val miuixHits = countOccurrences(miuix, SharedKotlinSources.WIDTH_IDENTIFIER)
        assertTrue(
            couiHits >= 1,
            "COUI 侧 CouiReadableColumn.kt 必须引用 " + SharedKotlinSources.WIDTH_IDENTIFIER +
                "，实测 " + couiHits + " 次",
        )
        assertTrue(
            miuixHits >= 1,
            "MIUIX 侧 " + SharedKotlinSources.MIUIX_READABLE_WIDTH_CONSUMER_PATH + " 必须引用 " +
                SharedKotlinSources.WIDTH_IDENTIFIER + "，实测 " + miuixHits + " 次",
        )
        assertTrue(
            couiHits + miuixHits >= 2,
            "两侧引用同一个标识符的命中数合计应 >= 2，实测 COUI " + couiHits +
                " 次 + MIUIX " + miuixHits + " 次",
        )
        assertFalse(
            coui.contains(SharedKotlinSources.DECLARATION),
            "CouiReadableColumn.kt 不得自行声明宽度常量，必须复用 AppConstants.kt 的定义",
        )
        assertFalse(
            miuix.contains(SharedKotlinSources.DECLARATION),
            "IllustDetailInfoSection.kt 不得自行声明宽度常量，必须复用 AppConstants.kt 的定义",
        )
    }
}

/** 硬编码宽度数值：出现这个词说明可读宽度被复制成了字面量。 */
private const val HARDCODED_WIDTH_VALUE = "760"

/** 守护测试自身的文件名：扫描时需要跳过，否则断言用的字面量与正则会命中自己。 */
private const val GUARD_FILE_NAME = "ReadableWidthSingleSourceTest.kt"

/** 一个待扫描的 Kotlin 源文件：相对 shared/src 的路径（统一用 / 分隔）与全文。 */
private data class KotlinSource(val relativePath: String, val text: String)

/** 源码命中的位置：文件、行号与命中片段（多行命中折成单行，便于读断言消息）。 */
private data class SourceHit(val relativePath: String, val line: Int, val snippet: String)

/**
 * shared/src 源码树扫描器。
 *
 * 不复用 StateSourceReader：那个工具只能按相对路径读取 main/ui 下的
 * 单个文件，不能枚举整棵树；本测试的断言 1、2、4
 * 都要求全树扫描，因此这里实现最小读取器（定位仓库根的锚点与 StateSourceReader 保持一致，
 * 同样以 shared/src 目录为锚点向上找仓库根）。
 */
private object SharedKotlinSources {

    /** 常量标识符：两侧都必须引用它，而不是复制数值。 */
    const val WIDTH_IDENTIFIER = "TABLET_CONTENT_MAX_WIDTH_DP"

    /** 常量声明的完整写法，用于统计「唯一定义」。 */
    val DECLARATION = "const val " + WIDTH_IDENTIFIER

    const val APP_CONSTANTS_PATH = "main/kotlin/com/perol/pixez/shared/ui/AppConstants.kt"

    const val COUI_READABLE_COLUMN_PATH =
        "main/kotlin/com/perol/pixez/shared/ui/coui/component/CouiReadableColumn.kt"

    const val MIUIX_READABLE_WIDTH_CONSUMER_PATH =
        "main/kotlin/com/perol/pixez/shared/ui/screens/IllustDetailInfoSection.kt"

    /** 从 Gradle 测试工作目录向上定位仓库根：以 shared/src 目录为锚点。 */
    private fun repoRoot(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (dir != null) {
            if (dir.resolve("shared/src").isDirectory()) return dir
            dir = dir.parent
        }
        error("未定位到仓库根目录，working dir=" + System.getProperty("user.dir"))
    }

    /** 全树 .kt 源码，按相对路径排序，保证断言消息稳定可复现。 */
    val sources: List<KotlinSource> by lazy {
        val srcRoot = repoRoot().resolve("shared/src")
        srcRoot.toFile().walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") && it.name != GUARD_FILE_NAME }
            .map { file ->
                val relativePath = srcRoot.relativize(file.toPath()).toString().replace('\\', '/')
                KotlinSource(relativePath, file.toPath().readText())
            }
            .sortedBy { it.relativePath }
            .toList()
    }

    /** 读取指定相对路径的源码；缺失即视为待守护文件被改名或删除，直接失败。 */
    fun textOf(relativePath: String): String =
        sources.firstOrNull { it.relativePath == relativePath }?.text
            ?: error(
                "缺少待守护源码：shared/src/" + relativePath +
                    "，本次扫描到 " + sources.size + " 个 .kt 文件",
            )
}

/** 统计 [needle] 在 [text] 中出现的次数（非重叠匹配）。 */
private fun countOccurrences(text: String, needle: String): Int {
    var count = 0
    var index = text.indexOf(needle)
    while (index >= 0) {
        count++
        index = text.indexOf(needle, index + needle.length)
    }
    return count
}

/** 把字符偏移换算成 1 起始的行号。 */
private fun lineNumberOf(text: String, offset: Int): Int {
    var line = 1
    var index = 0
    while (index < offset) {
        if (text[index] == '\n') line++
        index++
    }
    return line
}

/** 全树正则扫描：返回每一处命中及其文件与行号（跨行命中也只报起始行）。 */
private fun regexHits(pattern: Regex): List<SourceHit> =
    SharedKotlinSources.sources.flatMap { source ->
        pattern.findAll(source.text).map { match ->
            SourceHit(
                relativePath = source.relativePath,
                line = lineNumberOf(source.text, match.range.first),
                snippet = match.value.replace('\n', ' ').trim(),
            )
        }.toList()
    }
