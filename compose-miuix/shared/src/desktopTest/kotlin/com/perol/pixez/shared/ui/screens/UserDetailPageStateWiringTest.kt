package com.perol.pixez.shared.ui.screens

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 个人收藏页返回空白的修复在组合层的接线守护。
 *
 * shared 模块未引入 compose ui-test 依赖（纯 JVM 测试无法驱动组合），故「组合层确实从
 * [com.perol.pixez.shared.ui.state.UserDetailPageState] 读写」这一接线以源码守护测试锁定：
 * 若有人把状态改回 `remember { ... }` 的组合态，返回重建后将退回加载占位，本测试即失败。
 *
 * 保留状态容器自身的生命周期与「复用后不再请求」语义由
 * [com.perol.pixez.shared.ui.navigation.RootComponentUserDetailPageStateTest] 行为测试背书。
 */
class UserDetailPageStateWiringTest {

    /** 从 Gradle 测试工作目录向上定位 ui 源码目录。 */
    private fun uiDir(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        repeat(8) {
            val current = dir ?: return@repeat
            val candidate = current.resolve("shared/src/commonMain/kotlin/com/perol/pixez/shared/ui")
            if (candidate.isDirectory()) return candidate
            dir = current.parent
        }
        error("未定位到 ui 源码目录（working dir=${System.getProperty("user.dir")}）")
    }

    @Test
    fun `用户资料初值取自保留状态，避免重建后退回整页加载占位`() {
        val source = uiDir().resolve("screens/UserDetailScreen.kt").readText()

        assertTrue(
            "pageState.userDetail?.let { Result.success(it) }" in source,
            "produceState 的用户资料初值应取 pageState.userDetail，重建后首帧直接展示内容",
        )
    }

    @Test
    fun `两个网格的滚动位置在组合销毁时回写保留状态并用作初值`() {
        val source = uiDir().resolve("screens/UserDetailScreen.kt").readText()

        assertTrue(
            "pageState.worksScroll =" in source,
            "作品网格滚动位置应在组合销毁时回写 pageState.worksScroll",
        )
        assertTrue(
            "pageState.bookmarksScroll =" in source,
            "收藏网格滚动位置应在组合销毁时回写 pageState.bookmarksScroll",
        )
        assertTrue(
            "initialFirstVisibleItemIndex = pageState.worksScroll.index" in source,
            "作品网格应以 pageState.worksScroll 播种初值",
        )
        assertTrue(
            "initialFirstVisibleItemIndex = pageState.bookmarksScroll.index" in source,
            "收藏网格应以 pageState.bookmarksScroll 播种初值",
        )
    }

    @Test
    fun `三个分页列表状态优先复用保留状态里的实例`() {
        val source = uiDir().resolve("screens/UserDetailTabSection.kt").readText()

        for (field in listOf("works", "publicBookmarks", "privateBookmarks")) {
            assertTrue(
                "pageState.$field ?:" in source,
                "分页列表状态 pageState.$field 应优先复用保留实例，缺失时才创建新实例",
            )
        }
    }
}
