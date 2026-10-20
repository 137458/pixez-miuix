package com.perol.pixez.shared.ui.navigation

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * CHANGELOG [未发布]「退出登录或切换账号后清空上一账号分页列表」的接线守护：
 * HelloScreen / NewScreen 已接线 accountSwitched → reset()，本测试锁定 CHANGELOG 明列的
 * 其余六个页面（小说 / 评论 / 搜索结果（插画与画师两套状态机）/ 推荐用户 / 关注列表 / 粉丝列表）：
 * 1. 注入 LocalAccountRepository 并收集 loginEventFlow（登录/登出/切号信号源）；
 * 2. 以 loadedForAccount 跟踪账号身份，检测到切换（accountSwitched）时对页面每个
 *    分页状态机调用 reset() 作废旧账号列表与游标。
 *
 * 测试形态说明：shared 模块未引入 compose ui-test 依赖（纯 JVM 测试无法驱动组合），
 * 且各仓储为具体类无法打桩，故接线以源码守护测试锁定；reset() 的清空与在途作废语义
 * 由 ui/state 的 PagedListStateTest / IllustPagedListStateTest 行为测试背书。
 */
class AccountSwitchPagedListResetWiringTest {

    /** 页面源文件 → 其分页状态机变量名（SearchResultSection 含插画结果与画师结果两套状态机）。 */
    private val pages: List<Pair<String, List<String>>> = listOf(
        "NovelScreen.kt" to listOf("pagedList"),
        "CommentsScreen.kt" to listOf("pagedList"),
        "RecomUserScreen.kt" to listOf("pagedList"),
        "UserFollowListScreen.kt" to listOf("pagedList"),
        "UserFollowerListScreen.kt" to listOf("pagedList"),
        "SearchResultSection.kt" to listOf("listState", "pagedList"),
    )

    /** 从 Gradle 测试工作目录向上定位 ui/screens 源码目录。 */
    private fun screensDir(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        repeat(8) {
            val current = dir ?: return@repeat
            val candidate = current.resolve("shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens")
            if (candidate.isDirectory()) return candidate
            dir = current.parent
        }
        error("未定位到 ui/screens 源码目录（working dir=${System.getProperty("user.dir")}）")
    }

    @Test
    fun `六个页面均注入账号仓库并收集登录事件作为切换信号源`() {
        val base = screensDir()
        for ((fileName, _) in pages) {
            val source = base.resolve(fileName).readText()
            assertTrue(
                "LocalAccountRepository.current" in source,
                "$fileName 应注入 LocalAccountRepository 以观测账号身份",
            )
            assertTrue(
                "loginEventFlow.collect" in source,
                "$fileName 应收集 loginEventFlow（登录/登出/切号信号源，比照 HelloScreen）",
            )
            assertTrue(
                "loadedForAccount != " in source,
                "$fileName 应以 loadedForAccount != 账号对象 跟踪账号身份变化",
            )
        }
    }

    @Test
    fun `六个页面的每个分页状态机都在账号切换时重置`() {
        val base = screensDir()
        for ((fileName, stateMachines) in pages) {
            val source = base.resolve(fileName).readText()
            for (state in stateMachines) {
                assertTrue(
                    Regex(Regex.escape("if (accountSwitched) $state.reset()")).containsMatchIn(source),
                    "$fileName 的 $state 应在 accountSwitched 检测到切号/登出时调用 reset() 作废旧账号列表",
                )
            }
        }
    }
}
