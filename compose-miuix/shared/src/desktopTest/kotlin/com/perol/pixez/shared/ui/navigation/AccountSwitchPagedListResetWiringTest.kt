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
 * 覆盖面：共 12 个来源文件，分三组。
 * 1. 5 个皮肤无关页面（shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens）：
 *    NovelScreen / CommentsScreen / RecomUserScreen / SearchResultsState / SearchResultSection。
 *    搜索结果的两套状态机按皮肤无关状态下沉拆分：插画结果在 SearchResultsState（listState），
 *    画师与小说结果在 SearchResultSection（pagedList），各自独立守护。
 * 1b. 1 个共享实现（shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/components 的
 *    UserPagedListScreen.kt）：用户关注、粉丝、小说、相关用户四页的重复骨架已合并到这里，
 *    切号作废逻辑只有这一份，四页薄壳必须委托它（见下方委托守护用例）。
 * 2. 6 个 COUI 手写接线页面（shared/src/androidMain/kotlin/com/perol/pixez/shared/ui/coui/page）：
 *    CouiCommentsScreen / CouiNovelScreen / CouiRecomUserScreen / CouiSearchResultLists /
 *    CouiUserListScreen / CouiUserNovelListScreen。COUI 侧不复用 MIUIX 页面实现，各自复制了一份
 *    切号重置逻辑，必须单独纳入守护，否则只改 MIUIX 侧仍会出现「切号后残留上一账号列表」。
 *
 * 每个页面锁定两个要素，缺一即判定接线缺失：
 * 1. 信号源：经 rememberAccountSession() 读取当前账号并跟随 loginEventFlow 刷新
 *    （唯一实现在 ui/state/AccountSession.kt，由下方独立用例守护其内部要素）；
 * 2. 重置：以 loadedForAccount 跟踪账号身份检测 accountSwitched，并在检测到切号时对
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

    private val miuixComponentsRoot =
        "shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/components"

    private val couiPagesRoot =
        "shared/src/androidMain/kotlin/com/perol/pixez/shared/ui/coui/page"

    private val accountSessionSource =
        "shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/state/AccountSession.kt"

    /** 皮肤无关页面：commonMain ui/screens。 */
    private val miuixPages: List<GuardedPage> = listOf(
        GuardedPage(miuixScreensRoot, "NovelScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "CommentsScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "RecomUserScreen.kt", listOf("pagedList")),
        GuardedPage(miuixScreensRoot, "SearchResultsState.kt", listOf("listState")),
        GuardedPage(miuixScreensRoot, "SearchResultSection.kt", listOf("pagedList")),
        GuardedPage(miuixComponentsRoot, "UserPagedListScreen.kt", listOf("pagedList")),
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

    /** 全部受守护来源（6 个皮肤无关来源 + 6 个 COUI 页面）。 */
    private val pages: List<GuardedPage> = miuixPages + couiPages

    /** 合并后的四个用户列表薄壳：自有骨架已删，切号作废必须经共享组件。 */
    private val delegatedUserListShells: List<String> = listOf(
        "UserFollowListScreen.kt",
        "UserFollowerListScreen.kt",
        "UserNovelListScreen.kt",
        "UserRelatedListScreen.kt",
    )

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
    fun `受守护来源均经共享会话组件读取账号并跟踪账号身份变化`() {
        for (page in pages) {
            val file = sourcePath(page)
            assertTrue(file.exists(), "缺少待守护源码：${file}")
            val source = file.readText()
            assertTrue(
                "rememberAccountSession()" in source,
                "${page.fileName} 应经 rememberAccountSession() 读取账号，禁止各自复制读取与订阅样板",
            )
            assertTrue(
                "loadedForAccount != " in source,
                "${page.fileName} 应以 loadedForAccount != 账号对象 跟踪账号身份变化",
            )
        }
    }

    /**
     * 单点守护：账号信号源收敛到 AccountSession.kt 后，读取必须容忍账号存储异常（密钥丢失不能崩页面），
     * 且必须跟随登录事件，否则切号后各页仍会显示上一账号的数据。
     */
    @Test
    fun `共享会话组件读取账号时吞掉存储异常并跟随登录事件`() {
        val file = repoRoot().resolve(accountSessionSource)
        assertTrue(file.exists(), "缺少账号会话组件：$file")
        val source = file.readText()
        assertTrue(
            "import com.perol.pixez.shared.ui.LocalAccountRepository" in source,
            "AccountSession.kt 应显式 import LocalAccountRepository",
        )
        assertTrue(
            "LocalAccountRepository.current" in source,
            "AccountSession.kt 应读取 LocalAccountRepository.current",
        )
        assertTrue(
            "loginEventFlow.collect" in source,
            "AccountSession.kt 应收集 loginEventFlow（登录/登出/切号信号源）",
        )
        assertTrue(
            "suspendRunCatchingNonCancel" in source,
            "AccountSession.kt 应包 suspendRunCatchingNonCancel，避免账号存储异常抛给 Recomposer",
        )
    }

    @Test
    fun `受守护来源的每个分页状态机都在账号切换时重置`() {
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
     * 委托守护：四个用户列表页合并后不再各写一份切号作废逻辑，
     * 若哪个薄壳绕过共享组件重新手搓骨架，切号残留会以另一种形式回归。
     */
    @Test
    fun `四个用户列表薄壳必须委托共享骨架`() {
        for (fileName in delegatedUserListShells) {
            val file = repoRoot().resolve(miuixScreensRoot).resolve(fileName)
            assertTrue(file.exists(), "缺少待守护源码：$file")
            val source = file.readText()
            assertTrue(
                "import com.perol.pixez.shared.ui.components.UserPagedListScreen" in source,
                "$fileName 应 import 共享骨架 UserPagedListScreen",
            )
            assertTrue(
                "UserPagedListScreen(" in source,
                "$fileName 应调用共享骨架 UserPagedListScreen(",
            )
            assertTrue(
                "accountSwitched" !in source,
                "$fileName 不应再自行实现账号切换判定（唯一实现在 UserPagedListScreen.kt）",
            )
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
