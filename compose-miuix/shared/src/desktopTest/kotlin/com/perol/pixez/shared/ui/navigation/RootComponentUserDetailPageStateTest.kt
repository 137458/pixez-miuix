package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.state.GridScrollPosition
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * 个人收藏页返回空白的修复接线守护：UserDetail 栈项的 child 实例由 [Config.UserDetail]
 * 换为 [UserDetailComponent]，页面状态（资料 / 分页列表 / 网格滚动位置）经该 child 的
 * InstanceKeeper 保留，栈换层重建组合后首帧即可复用。
 *
 * 覆盖关键路径：
 * 1. 栈项实例——入栈 UserDetail 后 child 实例为子组件且携带路由参数，configuration 不变；
 * 2. 压栈保活——作品详情页压住再返回，复用同一子组件与同一页面状态容器；
 * 3. 零重复请求——复用回来的分页状态已有数据，loadFirst 走缓存早退不再请求首屏；
 * 4. 出栈销毁——出栈后重新进入得到全新的状态容器，不残留上一个用户页的保留数据。
 */
class RootComponentUserDetailPageStateTest {

    private fun createRootComponent(nodePath: String): RootComponent {
        val node = Preferences.userRoot().node(nodePath)
        node.clear()
        val settings = SettingsRepository(PreferencesSettings(node))
        // 完成向导 → 初始路由为 Main(Hello)，childStack 构造时即创建 Main child。
        settings.hasCompletedGuide = true
        return RootComponent(
            DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            settings,
        )
    }

    /** 当前栈顶的 UserDetail 子组件（断言非 Config 直传形态）。 */
    private fun activeUserDetail(root: RootComponent): UserDetailComponent =
        assertIs<UserDetailComponent>(
            root.stack.value.active.instance,
            "UserDetail 栈项的 child 实例应为子组件",
        )

    /** 从「个人收藏」入口入栈用户页（initialTab = 1）并返回其子组件。 */
    private fun pushUserDetail(root: RootComponent, userId: Long = 9L): UserDetailComponent {
        root.onUserClicked(userId, initialTab = 1)
        return activeUserDetail(root)
    }

    /** 记录请求次数的分页状态：首屏返回空列表（空列表同样是「已加载」而非「未加载」）。 */
    private fun countingListState(fetches: MutableList<String?>): IllustPagedListState =
        IllustPagedListState(
            fetch = { nextUrl ->
                fetches += nextUrl
                IllustPagedPage(illusts = emptyList(), nextUrl = null)
            },
            filter = { it },
            filterVersionProvider = { 0 },
        )

    @Test
    fun `UserDetail 栈项的实例为子组件并携带路由参数`() {
        val root = createRootComponent("com/perol/pixez/test/userdetail/instance")

        val user = pushUserDetail(root, userId = 9L)

        assertEquals(9L, user.userId, "子组件应携带入栈的用户 ID")
        assertEquals(
            Config.UserDetail(userId = 9L, initialTab = 1),
            user.config,
            "子组件应保留原始路由配置",
        )
        assertEquals(
            Config.UserDetail(userId = 9L, initialTab = 1),
            root.stack.value.active.configuration,
            "child 实例组件化后 configuration 应保持不变",
        )
    }

    @Test
    fun `作品详情页压栈并返回后复用同一页面状态`() {
        val root = createRootComponent("com/perol/pixez/test/userdetail/retain")
        val user = pushUserDetail(root)
        val scroll = GridScrollPosition(index = 7, offset = 24)
        user.pageState.worksScroll = scroll

        root.onIllustClicked(42L)
        assertEquals(Config.IllustDetail(illustId = 42L), root.stack.value.active.configuration)

        root.onBack()
        val restored = activeUserDetail(root)

        assertSame(user, restored, "UserDetail child 未销毁，子组件实例应跨页保留")
        assertEquals(
            scroll,
            restored.pageState.worksScroll,
            "返回后应读回同一状态容器里的滚动位置快照（重建首帧据此复位网格）",
        )
    }

    @Test
    fun `重建后复用的分页状态不再请求首屏`() {
        val root = createRootComponent("com/perol/pixez/test/userdetail/noreload")
        val user = pushUserDetail(root)
        val fetches = mutableListOf<String?>()
        val listState = countingListState(fetches)
        user.pageState.works = listState
        runBlocking { listState.loadFirst() }
        assertEquals(listOf<String?>(null), fetches, "首次加载应请求首屏一次")

        root.onIllustClicked(42L)
        root.onBack()

        val restored = activeUserDetail(root).pageState.works
        assertSame(listState, restored, "重建后应复用同一分页状态实例（数据随实例保留）")
        runBlocking { restored!!.loadFirst() }
        assertEquals(1, fetches.size, "复用的分页状态已有数据，loadFirst 应缓存早退不再请求首屏")
    }

    @Test
    fun `出栈后重新进入得到全新的页面状态容器`() {
        val root = createRootComponent("com/perol/pixez/test/userdetail/recreate")
        val first = pushUserDetail(root)
        first.pageState.worksScroll = GridScrollPosition(index = 3, offset = 5)

        root.onBack()
        val second = pushUserDetail(root)

        assertNotSame(first, second, "出栈销毁 child 后应重建子组件")
        assertEquals(
            GridScrollPosition.Zero,
            second.pageState.worksScroll,
            "新栈项不应残留上一个用户页的保留状态",
        )
    }
}
