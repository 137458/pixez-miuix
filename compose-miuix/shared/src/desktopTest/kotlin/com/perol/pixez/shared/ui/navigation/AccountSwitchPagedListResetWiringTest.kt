package com.perol.pixez.shared.ui.navigation

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * CHANGELOG [未发布]「退出登录或切换账号后清空上一账号分页列表」的接线守护。
 *
 * 覆盖面：共 13 个页面文件，分两组。
 * 1. 7 个皮肤无关页面（shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens）：
 *    NovelScreen / CommentsScreen / RecomUserScreen / UserFollowListScreen /
 *    UserFollowerListScreen / SearchResultsState / SearchResultSection。
 *    搜索结果的两套状态机按皮肤无关状态下沉拆分：插画结果在 SearchResultsState（listState），
 *    画师与小说结果在 SearchResultSection（pagedList），各自独立守护。
 * 2. 6 个 COUI 手写接线页面（shared/src/androidMain/kotlin/com/perol/pixez/shared/ui/coui/page）：
 *    CouiCommentsScreen / CouiNovelScreen / CouiRecomUserScreen / CouiSearchResultLists /
 *    CouiUserListScreen / CouiUserNovelListScreen。COUI 侧不复用 MIUIX 页面实现，各自复制了一份
 *    切号重置逻辑，必须单独纳入守护，否则只改 MIUIX 侧仍会出现「切号后残留上一账号列表」。
 *
 * 每个文件锁定四要素，缺一即判定接线缺失：
 * 1. import：显式 import com.perol.pixez.shared.ui.LocalAccountRepository；
 * 2. 注入：读取 LocalAccountRepository.current；
 * 3. 收集：收集 loginEventFlow（登录/登出/切号信号源，比照 HelloScreen）；
 * 4. 重置：以 loadedForAccount 跟踪账号身份检测 accountSwitched，并在检测到切号时对
 *    该页每个分页状态机调用 reset()。
 *
 * 测试形态说明：shared 模块未引入 compose ui-test 依赖（纯 JVM 测试无法驱动组合），
 * 且各仓储为具体类无法打桩，故接线以源码守护测试锁定；reset() 的清空与在途作废语义
 * 由 ui/state 的 PagedListStateTest / IllustPagedListStateTest 行为测试背书。
 */
class AccountSwitchPagedListResetWiringTest {

    /**
     * 一个待守护页面。
     * [sourceRoot] 为相对仓库根的源码目录，[stateMachines] 为该页需要重置的分页状态机变量名。
     */
    private data class GuardedPage(
        val sourceRoot: String,
        val fileName: String,
        val stateMachines: List<String>,
    )

    private val miuixScreensRoot =
        "shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens"

    private val couiPagesRoot =
        "shared/src/androidMain/kotlin/com/perol/pixez/shared/ui/coui/page"

    /** 皮肤无关页面：commonMain ui/screens。 */
    private val miuixPages: List<GuardedPage> = listOf(
        GuardedPage(miuixScreensRoot, "NovelScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "CommentsScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "RecomUserScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "UserFollowListScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "UserFollowerListScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "SearchResultsState.kt", listOf("listState")),
        GuardedPage(miuixScreensRoot, "SearchResultSection.kt", listOf("pagedList")),
    )

    /** COUI 手写接线页面：androidMain coui/page（不复用 MIUIX 实现，需独立守护）。 */
    private val couiPages: List<GuardedPage> = listOf(
        GuardedPage(couiPagesRoot, "CouiCommentsScreen.kt", listOf("pagedList")),
        GuardedPage(couiPagesRoot, "CouiNovelScreen.kt", listOf("pagedList")),
        GuardedPage(couiPagesRoot, "CouiRecomUserScreen.kt", listOf("pagedList")),
        GuardedPage(couiPagesRoot, "CouiSearchResultLists.kt", listOf("pagedList")),
        GuardedPage(couiPagesRoot, "CouiUserListScreen.kt", listOf("pagedList")),
        GuardedPage(couiPagesRoot, "CouiUserNovelListScreen.kt", listOf("pagedList")),
    )

    /** 全部受守护页面（7 个皮肤无关页面 + 6 个 COUI 页面）。 */
    private val pages: List<GuardedPage> = miuixPages + couiPages

    /** 从 Gradle 测试工作目录向上定位仓库根：以 shared/src 目录为锚点。 */
    private fun repoRoot(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (dir != null) {
            if (dir.resolve("shared/src").isDirectory()) return dir
            dir = dir.parent
        }
        error("未定位到仓库根目录（working dir=${System.getProperty("user.dir")}）")
    }

    private fun sourcePath(page: GuardedPage): Path =
        repoRoot().resolve(page.sourceRoot).resolve(page.fileName)

    @Test
    fun `十三个页面文件均显式 import 账号仓库并收集登录事件作为切换信号源`() {
        for (page in pages) {
            val file = sourcePath(page)
            assertTrue(file.exists(), "缺少待守护源码：${file}")
            val source = file.readText()
            assertTrue(
                "import com.perol.pixez.shared.ui.LocalAccountRepository" in source,
                "${page.fileName} 应显式 import LocalAccountRepository（四要素之一：依赖可见）",
            )
            assertTrue(
                "LocalAccountRepository.current" in source,
                "${page.fileName} 应注入 LocalAccountRepository 以观测账号身份",
            )
            assertTrue(
                "loginEventFlow.collect" in source,
                "${page.fileName} 应收集 loginEventFlow（登录/登出/切号信号源，比照 HelloScreen）",
            )
            assertTrue(
                "loadedForAccount != " in source,
                "${page.fileName} 应以 loadedForAccount != 账号对象 跟踪账号身份变化",
            )
        }
    }

    @Test
    fun `十三个页面的每个分页状态机都在账号切换时重置`() {
        for (page in pages) {
            val source = sourcePath(page).readText()
            for (state in page.stateMachines) {
                assertTrue(
                    Regex(Regex.escape("if (accountSwitched) $state.reset()")).containsMatchIn(source),
                    "${page.fileName} 的 $state 应在 accountSwitched 检测到切号/登出时调用 reset() 作废旧账号列表",
                )
            }
        }
    }

    /**
     * 完整性守护：COUI 页面目录下任何「检测账号切换并重置」的文件都必须登记在 [couiPages] 中，
     * 否则新增 COUI 列表页时容易漏掉守护（本测试曾因清单漏项而与实际扫描范围失配）。
     */
    @Test
    fun `COUI 页面目录无遗漏：新增切号重置页面必须同步纳入守护清单`() {
        val couiDir = repoRoot().resolve(couiPagesRoot)
        val guardedNames = couiPages.map { it.fileName }.toSet()
        val wired = couiDir.listDirectoryEntries("*.kt")
            .filter { file ->
                val source = file.readText()
                "accountSwitched" in source && ".reset()" in source
            }
            .map { it.name }
            .toSet()
        assertTrue(
            wired.isNotEmpty(),
            "未在 $couiDir 发现切号重置接线文件，请确认源码目录未被移动",
        )
        val missing = wired - guardedNames
        assertTrue(
            missing.isEmpty(),
            "以下 COUI 页面已接线切号重置但未纳入守护清单：$missing",
        )
    }
}
