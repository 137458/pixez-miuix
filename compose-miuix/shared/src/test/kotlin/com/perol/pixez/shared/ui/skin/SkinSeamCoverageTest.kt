package com.perol.pixez.shared.ui.skin

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * R-06 皮肤接缝覆盖守护测试（源码结构断言，不做任何生产代码改动）。
 *
 * 为什么用源码扫描而不是运行时/组合测试：
 * [SkinPages] 是 44 个方法的接缝接口——[LocalSkinPages] 非空时页面走皮肤实现，为 null 时
 * 回退 MIUIX 内置渲染。这类回归的真实形态是「新增页面时忘了在 RootChildScreens 里补一条
 * skinPages 调用」或「删掉调用后接口方法变成死方法」，两者都是源码事实，而不是运行时行为。
 * shared 模块没有引入 compose ui-test 依赖，纯 JVM 测试既无法组合这些页面，也无法观察
 * CompositionLocal 是否为 null；直接对源码文本断言跑得动、覆盖整棵树，失败时能指出缺哪个方法。
 *
 * 扫描范围：shared/src/main 的生产源码。RootChildScreens.kt 之外还有
 * RootContent.kt 里的 skinPages.navigationBar 调用（底部导航栏），只扫 RootChildScreens.kt
 * 会漏掉这一条，因此必须扫整个生产源码树；测试源码不在扫描范围内，本文件中的断言字面量
 * 因此不会自己命中自己。
 */
class SkinSeamCoverageTest {

    /**
     * 断言 1：接口方法数等于审计基线，且每个方法都有生产调用点。
     *
     * 基线值来自审计 R-06 的 40 个方法；若接口有意增删方法，这行断言会先红，逼着改动者
     * 同步更新基线并复核调用点，而不是让新方法静悄悄地没有接缝实现。
     */
    @Test
    fun skinPagesInterfaceMethodsAllHaveProductionCallSites() {
        val interfaceMethods = SkinSeamSources.skinPagesInterfaceMethods()
        assertEquals(
            SkinSeamSources.SKIN_PAGES_METHOD_BASELINE,
            interfaceMethods.size,
            "SkinPages 接口方法数与审计基线 " + SkinSeamSources.SKIN_PAGES_METHOD_BASELINE +
                " 不一致，实测 " + interfaceMethods.size + " 个：" +
                interfaceMethods.sorted().joinToString() +
                "（取证：" + SkinSeamSources.SKIN_PAGES_PATH + "）",
        )

        val callSites = SkinSeamSources.skinPagesCallSites()
        val uncalled = interfaceMethods - callSites.keys
        assertEquals(
            emptySet(),
            uncalled,
            "以下 SkinPages 方法在生产代码里没有任何 `skinPages.<方法>(` 调用点（皮肤接缝空洞，" +
                "皮肤实现永远不会被调用）：" + uncalled.sorted().joinToString() +
                "；已扫描文件：" + SkinSeamSources.scannedFiles.joinToString { it.relativePath },
        )
    }

    /**
     * 断言 2：调用点方法名全部属于接口，且调用点方法数与接口方法数一致。
     *
     * 反向的一侧：调用点写了接口里不存在的方法（例如接口方法被改名而调用点漏改）会直接编译
     * 失败，但如果有人把调用点写进反射/字符串分派，或者接口方法被删而调用点仍在，就需要这条
     * 断言兜底。数量相等则排除了「同一方法被调用多次而另一方法无人调用」的组合型空洞。
     */
    @Test
    fun skinPagesCallSitesAllBelongToInterface() {
        val interfaceMethods = SkinSeamSources.skinPagesInterfaceMethods()
        val callSites = SkinSeamSources.skinPagesCallSites()

        val unknown = callSites.keys - interfaceMethods
        assertEquals(
            emptySet(),
            unknown,
            "以下调用点方法不在 SkinPages 接口内（接口与调用点已分叉）：" +
                unknown.sorted().joinToString { name -> name + " -> " + callSites.getValue(name).joinToString() },
        )
        assertEquals(
            interfaceMethods.size,
            callSites.size,
            "SkinPages 接口方法与生产调用点的方法数不一致：接口 " + interfaceMethods.size +
                " 个，调用点 " + callSites.size + " 个；仅在接口：" +
                (interfaceMethods - callSites.keys).sorted().joinToString() +
                "；调用点明细：" + callSites.toSortedMap().entries.joinToString { entry ->
                    entry.key + "=" + entry.value.joinToString(";")
                },
        )
    }

    /**
     * 断言 3：RootChildContent 的 when 分支覆盖全部 Config 子路由，并保留 error 兜底。
     *
     * 分发主体从密封 Config 改成了 Any（作品详情页/用户详情页改由子组件实例分发），
     * 编译器不再做穷举校验：漏掉一条分支的后果是运行时抛错（有兜底）或静默白屏（兜底被删）。
     * 组件路由是 when 里的例外通道，必须与"哪些 Config 由组件分发"互相印证：
     * 例外集变大意味着又一条路由绕开了 Config 分发，需要在本测试里显式登记。
     */
    @Test
    fun rootChildContentCoversEveryConfigRouteAndKeepsErrorFallback() {
        val body = SkinSeamSources.rootChildContentBody()
        assertTrue(
            body.text.contains("when (instance)"),
            "RootChildContent 内未找到 `when (instance)` 分发入口（取证：" +
                SkinSeamSources.ROOT_CONTENT_PATH + ":" + SkinSeamSources.rootChildContentStartLine() + "）",
        )

        // 兜底分支：Any 分发失去编译期穷举后，这是唯一能让"漏接线路由"立刻可见的地方。
        val fallbackIndex = body.text.indexOf("else -> error(")
        assertTrue(
            fallbackIndex >= 0,
            "RootChildContent 的 when 缺少 `else -> error(...)` 兜底：漏接线路由会静默白屏，" +
                "而不是在分发处即时抛错（取证：" + SkinSeamSources.ROOT_CONTENT_PATH + "）",
        )
        val fallbackLine = SkinSeamSources.lineNumberAt(body.text, fallbackIndex) + body.startLine - 1
        assertEquals(
            1,
            SkinSeamSources.countOccurrences(body.text, "else -> error("),
            "RootChildContent 的 error 兜底应恰好 1 处，避免分支内另有兜底吞掉漏接线路由",
        )

        val allConfigs = SkinSeamSources.configSubclasses()
        assertEquals(
            SkinSeamSources.CONFIG_SUBCLASS_BASELINE,
            allConfigs.size,
            "Config 子路由数量与审计基线 " + SkinSeamSources.CONFIG_SUBCLASS_BASELINE +
                " 不一致，实测 " + allConfigs.size + " 个：" + allConfigs.sorted().joinToString() +
                "（取证：" + SkinSeamSources.CONFIG_PATH + "）",
        )

        val configBranches = SkinSeamSources.configBranches(body.text)
        val componentRoutes = SkinSeamSources.componentRoutes(body.text)

        val strayBranches = configBranches - allConfigs
        assertEquals(
            emptySet(),
            strayBranches,
            "RootChildContent 存在指向不存在的 Config 子类的分支：" + strayBranches.sorted().joinToString(),
        )

        val strayComponentRoutes = componentRoutes - allConfigs
        assertEquals(
            emptySet(),
            strayComponentRoutes,
            "RootChildContent 的组件分支折算后找不到对应的 Config 子路由：" +
                strayComponentRoutes.sorted().joinToString() +
                "（组件名去掉 Component 后缀后必须等于某个 Config 子类名）",
        )

        assertEquals(
            setOf("IllustDetail", "UserDetail"),
            componentRoutes,
            "组件分发的 Config 例外集发生变化，实测 " + componentRoutes.sorted().joinToString() +
                "；新增组件路由等于又一条页面绕开 Config 分发，必须在审计与计划里显式登记后再更新本断言",
        )

        val uncovered = allConfigs - configBranches - componentRoutes
        assertEquals(
            emptySet(),
            uncovered,
            "以下 Config 子路由在 RootChildContent 的 when 里没有分支（运行到就是第 " +
                fallbackLine + " 行的 error 兜底）：" + uncovered.sorted().joinToString() +
                "；已覆盖 " + (configBranches + componentRoutes).size + "/" + allConfigs.size,
        )

        // 兜底可见性：把覆盖缺口与兜底位置一起写进失败信息（上面的消息已含兜底行号）。
        assertEquals(
            allConfigs.size,
            (configBranches + componentRoutes).size,
            "when 分支折算后数量与 Config 子路由数量不一致：" +
                (configBranches + componentRoutes).sorted().joinToString(),
        )
    }
}

/** 生产源码扫描结果：路径、相对仓库根的路径与全文。 */
private class ScannedSource(val path: File, val relativePath: String, val text: String)

/** RootChildContent 函数体与其在文件中的起始行号（1-based）。 */
private class FunctionBody(val text: String, val startLine: Int)

/**
 * 皮肤接缝扫描器：仓库根定位、接口方法抽取、调用点抽取、Config 分支抽取。
 *
 * 所有结果都在进程内缓存：三个用例共用同一份源码快照，避免同一文件被读三次而彼此不一致。
 */
private object SkinSeamSources {

    /** 审计 R-06 记录的方法数基线（40 页面/底栏 + 5 根级覆盖层与侧栏接缝）。 */
    const val SKIN_PAGES_METHOD_BASELINE = 44

    /** 审计记录的路由基线：sealed class Config 的子类数量。 */
    const val CONFIG_SUBCLASS_BASELINE = 40

    const val SKIN_PAGES_PATH = "compose-miuix/shared/src/main/kotlin/com/perol/pixez/shared/ui/skin/SkinPages.kt"
    const val ROOT_CONTENT_PATH = "compose-miuix/shared/src/main/kotlin/com/perol/pixez/shared/ui/navigation/RootContent.kt"
    const val CONFIG_PATH = "compose-miuix/shared/src/main/kotlin/com/perol/pixez/shared/ui/navigation/Config.kt"

    private const val SHARED_SRC = "compose-miuix/shared/src"

    val repoRoot: File = locateRepoRoot()

    val scannedFiles: List<ScannedSource> = listOf("main")
        .map { File(repoRoot, SHARED_SRC + "/" + it + "/kotlin") }
        .filter { it.isDirectory }
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
        .sortedBy { it.path }
        .map { file ->
            ScannedSource(file, file.relativeTo(repoRoot).path.replace('\\', '/'), file.readText())
        }

    private val methodDeclaration = Regex("""(?m)^\s*fun\s+([A-Za-z][A-Za-z0-9]*)\s*\(""")
    private val callSitePattern = Regex("""skinPages\s*\.\s*([A-Za-z][A-Za-z0-9]*)\s*\(""")
    private val configBranchPattern = Regex("""(?m)^\s*(?:is\s+)?Config\.([A-Z][A-Za-z0-9]*)\s*->""")
    private val componentBranchPattern = Regex("""(?m)^\s*is\s+([A-Z][A-Za-z0-9]*Component)\s*->""")
    private val configSubclassPattern = Regex("""(?:data\s+class|data\s+object)\s+([A-Z][A-Za-z0-9]*)""")

    private fun locateRepoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            if (File(dir, SHARED_SRC).isDirectory) return dir
            dir = dir.parentFile
        }
        error(
            "无法从工作目录 " + System.getProperty("user.dir") + " 向上定位仓库根（应包含 " + SHARED_SRC + "）",
        )
    }

    private fun source(relativePath: String): ScannedSource =
        scannedFiles.firstOrNull { it.relativePath == relativePath }
            ?: error("未在扫描范围 " + scannedFiles.size + " 个文件中找到 " + relativePath)

    /** 接口方法名集合：先定位 interface SkinPages 的大括号体，再抽其中的 fun 声明。 */
    fun skinPagesInterfaceMethods(): Set<String> {
        val text = source(SKIN_PAGES_PATH).text
        val body = bracedBody(text, "interface SkinPages", SKIN_PAGES_PATH)
        return methodDeclaration.findAll(body).map { it.groupValues[1] }.toSet()
    }

    /** 生产代码里 skinPages.<方法>( 的调用点：方法名 -> 命中的 文件:行号 列表。 */
    fun skinPagesCallSites(): Map<String, List<String>> {
        val hits = linkedMapOf<String, MutableList<String>>()
        scannedFiles.forEach { scanned ->
            callSitePattern.findAll(scanned.text).forEach { match ->
                hits.getOrPut(match.groupValues[1]) { mutableListOf() }
                    .add(scanned.relativePath + ":" + lineNumberAt(scanned.text, match.range.first))
            }
        }
        return hits
    }

    /** RootChildContent 的函数体（大括号配对）及其起始行号。 */
    fun rootChildContentBody(): FunctionBody {
        val text = source(ROOT_CONTENT_PATH).text
        val marker = "fun RootChildContent("
        val start = text.indexOf(marker)
        check(start >= 0) { "未在 " + ROOT_CONTENT_PATH + " 内找到 " + marker }
        return FunctionBody(bracedBody(text, marker, ROOT_CONTENT_PATH), lineNumberAt(text, start))
    }

    fun rootChildContentStartLine(): Int = rootChildContentBody().startLine

    /** sealed class Config 的子类名集合。 */
    fun configSubclasses(): Set<String> {
        val body = bracedBody(source(CONFIG_PATH).text, "sealed class Config", CONFIG_PATH)
        return configSubclassPattern.findAll(body).map { it.groupValues[1] }.toSet()
    }

    /** when(instance) 里 Config.X -> 的分支目标集合。 */
    fun configBranches(functionBody: String): Set<String> =
        configBranchPattern.findAll(functionBody).map { it.groupValues[1] }.toSet()

    /** when(instance) 里 is XxxComponent -> 的分支折算成 Config 子类名（去掉 Component 后缀）。 */
    fun componentRoutes(functionBody: String): Set<String> =
        componentBranchPattern.findAll(functionBody)
            .map { it.groupValues[1].removeSuffix("Component") }
            .toSet()

    private fun bracedBody(text: String, marker: String, sourcePath: String): String {
        val markerIndex = text.indexOf(marker)
        check(markerIndex >= 0) { "未在 " + sourcePath + " 内找到 " + marker }
        val open = text.indexOf('{', markerIndex)
        check(open > 0) { sourcePath + " 中 " + marker + " 之后没有可配对的大括号体" }
        var depth = 0
        for (index in open until text.length) {
            when (text[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(open + 1, index)
                }
            }
        }
        error(sourcePath + " 中 " + marker + " 的大括号没有闭合")
    }

    fun lineNumberAt(text: String, index: Int): Int {
        var line = 1
        for (position in 0 until minOf(index, text.length)) {
            if (text[position] == '\n') line++
        }
        return line
    }

    fun countOccurrences(text: String, needle: String): Int {
        var count = 0
        var from = 0
        while (true) {
            val found = text.indexOf(needle, from)
            if (found < 0) return count
            count++
            from = found + needle.length
        }
    }
}
