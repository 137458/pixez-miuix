package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.navigation.DeepLinkParser
import com.perol.pixez.shared.navigation.ParsedDeepLink
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 中-21：[RootComponent] 导航状态机安全网（中-6 路由重构前置防线）。
 *
 * 覆盖关键路径：
 * 1. 页面创建——初始路由随向导完成态与欢迎页设置解析（resolveWelcomePageConfig）；
 * 2. tab 状态机——二级页点标签回主栈且选中态更新、主页重选发射再选中事件、
 *    二级页点当前标签不发事件只更新选中态、切动态标签清除未读标记；
 * 3. 返回——onBack 在主页返回 false 不动栈、二级页出栈返回 true、多级栈逆序回退；
 * 4. 进程死亡恢复——selectedTab 与返回栈经 stateKeeper 保存/恢复；
 * 5. 深链——DeepLinkParser.dispatch 到 [RootComponent] 的栈效果（入栈/回主栈/回退分支/OAuth 回调）。
 */
class RootComponentNavigationTest {

    private fun newSettings(nodePath: String, guideCompleted: Boolean = true): SettingsRepository {
        val node = Preferences.userRoot().node(nodePath)
        node.clear()
        return SettingsRepository(PreferencesSettings(node)).also {
            it.hasCompletedGuide = guideCompleted
        }
    }

    private fun createComponent(
        settings: SettingsRepository,
        stateKeeper: StateKeeperDispatcher = StateKeeperDispatcher(),
    ): RootComponent =
        RootComponent(
            DefaultComponentContext(
                LifecycleRegistry().apply { resume() },
                stateKeeper = stateKeeper,
            ),
            settings,
        )

    // region 页面创建：初始路由解析

    @Test
    fun `未完成向导时初始页面为向导页且选中态回退 Hello`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/create/guide", guideCompleted = false))

        assertEquals(Config.Guide, component.stack.value.active.configuration, "未完成向导应先进入向导页")
        assertEquals(
            RootComponent.MainTab.Hello,
            component.selectedTab.value,
            "初始路由非 Main 时选中态应回退为 Hello",
        )
    }

    @Test
    fun `已完成向导时初始页面随欢迎页设置解析`() {
        val cases = listOf(
            "default" to Config.Main(RootComponent.MainTab.Hello),
            "rank" to Config.Main(RootComponent.MainTab.Ranking),
            "quick_view" to Config.Main(RootComponent.MainTab.New),
            "search" to Config.Main(RootComponent.MainTab.Search),
            "setting" to Config.Settings,
        )
        cases.forEachIndexed { index, (type, expected) ->
            val settings = newSettings("com/perol/pixez/test/nav/create/w$type")
            if (type != "default") settings.welcomePageType = type

            val component = createComponent(settings)

            val expectedTab = (expected as? Config.Main)?.tab
            assertEquals(
                expected,
                component.stack.value.active.configuration,
                "欢迎页设置 $type（用例 #$index）的初始路由不符",
            )
            assertEquals(
                expectedTab ?: RootComponent.MainTab.Hello,
                component.selectedTab.value,
                "欢迎页设置 $type（用例 #$index）的初始选中态应随初始 Main 标签，非 Main 初始页回退 Hello",
            )
        }
    }

    // endregion

    // region tab 状态机：onMainTabSelected / onPageSwiped

    @Test
    fun `二级页点标签回主栈复用既有 Main 配置且选中态更新`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/tab/from-detail"))

        component.onIllustClicked(42L)
        assertEquals(Config.IllustDetail(illustId = 42L), component.stack.value.active.configuration)

        component.onMainTabSelected(RootComponent.MainTab.Ranking)

        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "二级页点标签应回主栈，且复用栈内既有 Main 配置而非以新标签新建",
        )
        assertEquals(
            RootComponent.MainTab.Ranking,
            component.selectedTab.value,
            "选中态应更新为所点标签",
        )
        assertEquals(1, component.stack.value.items.size, "回主栈后栈内应只剩一个 Main 配置")
    }

    @Test
    fun `主页重选当前标签发射再选中事件且选中态与栈不变`() = runBlocking {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/tab/reselect"))
        assertEquals(RootComponent.MainTab.Hello, component.selectedTab.value)
        val received = mutableListOf<RootComponent.MainTab>()
        // UNDISPATCHED + yield：与 RootComponentReselectFlowTest 相同的订阅时序保障。
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            component.tabReselectEvents.collect { received.add(it) }
        }

        component.onMainTabSelected(RootComponent.MainTab.Hello)
        yield()

        assertEquals(listOf(RootComponent.MainTab.Hello), received, "主页重选当前标签应发射一次再选中事件")
        assertEquals(
            RootComponent.MainTab.Hello,
            component.selectedTab.value,
            "重选不应改变选中态",
        )
        assertEquals(1, component.stack.value.items.size, "重选不应改动返回栈")

        collector.cancel()
    }

    @Test
    fun `二级页点当前标签回主栈后不发重选事件只更新选中态`() = runBlocking {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/tab/detail-current"))
        val received = mutableListOf<RootComponent.MainTab>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            component.tabReselectEvents.collect { received.add(it) }
        }

        // 栈：Main(Hello) → IllustDetail，selectedTab 仍为 Hello；此时点 Hello 标签。
        component.onIllustClicked(42L)
        component.onMainTabSelected(RootComponent.MainTab.Hello)
        yield()

        assertEquals(
            emptyList(),
            received,
            "决策时点活跃页为二级页（非 Main），不应触发再选中事件",
        )
        assertEquals(
            RootComponent.MainTab.Hello,
            component.selectedTab.value,
            "点当前标签仍应保持该标签选中",
        )
        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "二级页点标签应回主栈",
        )

        collector.cancel()
    }

    @Test
    fun `切到动态标签经点击与滑动两个入口都清除未读标记`() {
        val settings = newSettings("com/perol/pixez/test/nav/tab/badge")
        val component = createComponent(settings)

        settings.hasUnreadFeedBadge = true
        component.onMainTabSelected(RootComponent.MainTab.New)
        assertEquals(RootComponent.MainTab.New, component.selectedTab.value)
        assertFalse(settings.hasUnreadFeedBadge, "点击切到动态标签应清除未读标记")

        // 重置标记后从 Pager 滑动入口再验证一次。
        settings.hasUnreadFeedBadge = true
        component.onPageSwiped(RootComponent.MainTab.New)
        assertFalse(settings.hasUnreadFeedBadge, "滑动切到动态标签应清除未读标记")
    }

    // endregion

    // region 返回：onBack 消费语义

    @Test
    fun `主页 onBack 返回 false 且不改动栈`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/back/main"))

        val consumed = component.onBack()

        assertFalse(consumed, "一级主页不消费返回事件，交由系统决定退出")
        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "主页返回不应改动栈",
        )
        assertEquals(1, component.stack.value.items.size)
    }

    @Test
    fun `二级页 onBack 出栈返回 true 且多级栈逆序回退`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/back/detail"))
        component.onIllustClicked(7L)
        component.onCommentsClicked(7L)
        assertEquals(Config.Comments(illustId = 7L), component.stack.value.active.configuration)

        assertTrue(component.onBack(), "二级页应消费返回事件")
        assertEquals(Config.IllustDetail(illustId = 7L), component.stack.value.active.configuration, "返回应按入栈逆序回到上一层")

        assertTrue(component.onBack(), "一级与二级之间仍应消费返回事件")
        assertEquals(Config.Main(RootComponent.MainTab.Hello), component.stack.value.active.configuration)

        assertFalse(component.onBack(), "回到主页后返回权交还系统")
        assertEquals(1, component.stack.value.items.size, "主页继续返回不应出栈")
    }

    // endregion

    // region 进程死亡恢复：stateKeeper 保存/恢复

    @Test
    fun `进程死亡后恢复选中标签与返回栈`() {
        val liveDispatcher = StateKeeperDispatcher()
        val live = createComponent(newSettings("com/perol/pixez/test/nav/restore/live"), liveDispatcher)
        live.onMainTabSelected(RootComponent.MainTab.Ranking)
        live.onIllustClicked(42L)
        val saved = assertNotNull(liveDispatcher.save(), "存在注册项时保存态不应为空")

        val restored = createComponent(newSettings("com/perol/pixez/test/nav/restore/dead"), StateKeeperDispatcher(saved))

        assertEquals(
            Config.IllustDetail(illustId = 42L),
            restored.stack.value.active.configuration,
            "进程死亡恢复：返回栈应经 stateKeeper 往返",
        )
        assertEquals(2, restored.stack.value.items.size, "恢复后的栈深度应与保存时一致")
        assertEquals(
            RootComponent.MainTab.Ranking,
            restored.selectedTab.value,
            "进程死亡恢复：选中标签应经 stateKeeper 往返而非回退初始值",
        )
    }

    // endregion

    // region 深链：DeepLinkParser.dispatch → RootComponent

    @Test
    fun `作品深链入栈打开详情且返回可回到主页`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/deep/illust"))
        val parsed = assertNotNull(
            DeepLinkParser.parse("https://www.pixiv.net/artworks/12345"),
            "artworks 链接应可解析",
        )
        assertTrue(parsed is ParsedDeepLink.IllustDetail, "artworks 链接应解析为作品深链，实际为 $parsed")

        DeepLinkParser.dispatch(parsed, component) {}

        assertEquals(Config.IllustDetail(illustId = 12345L), component.stack.value.active.configuration)
        assertTrue(component.onBack(), "深链打开的详情页同样可返回")
        assertEquals(Config.Main(RootComponent.MainTab.Hello), component.stack.value.active.configuration)
    }

    @Test
    fun `标签深链从二级页回主栈并更新选中态`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/deep/tab"))
        component.onIllustClicked(9L)

        val parsed = assertNotNull(DeepLinkParser.parse("pixez://ranking"), "ranking 深链应可解析")
        DeepLinkParser.dispatch(parsed, component) {}

        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "标签深链应从二级页回主栈",
        )
        assertEquals(RootComponent.MainTab.Ranking, component.selectedTab.value)
    }

    @Test
    fun `无主栈时标签深链新建对应标签的主页`() {
        // 欢迎页设置为「设置页」：初始栈无 Main 配置。
        val settings = newSettings("com/perol/pixez/test/nav/deep/no-main")
        settings.welcomePageType = "setting"
        val component = createComponent(settings)
        assertEquals(Config.Settings, component.stack.value.active.configuration)

        val parsed = assertNotNull(DeepLinkParser.parse("pixez://ranking"), "ranking 深链应可解析")
        DeepLinkParser.dispatch(parsed, component) {}

        assertEquals(
            Config.Main(RootComponent.MainTab.Ranking),
            component.stack.value.active.configuration,
            "栈内无 Main 配置时应以深链标签新建主页",
        )
        assertEquals(RootComponent.MainTab.Ranking, component.selectedTab.value)
    }

    @Test
    fun `搜索深链有关键词入栈空关键词回退搜索标签`() {
        val withKeyword = createComponent(newSettings("com/perol/pixez/test/nav/deep/search-word"))
        val word = assertNotNull(DeepLinkParser.parse("pixez://search?word=miku"), "带关键词搜索深链应可解析")
        DeepLinkParser.dispatch(word, withKeyword) {}
        assertEquals(Config.Search(query = "miku"), withKeyword.stack.value.active.configuration, "带关键词的搜索深链应入栈搜索页")

        val blank = createComponent(newSettings("com/perol/pixez/test/nav/deep/search-blank"))
        blank.onIllustClicked(3L)
        val empty = assertNotNull(DeepLinkParser.parse("pixez://search"), "空关键词搜索深链应可解析")
        DeepLinkParser.dispatch(empty, blank) {}
        assertEquals(
            RootComponent.MainTab.Search,
            blank.selectedTab.value,
            "空关键词搜索深链应回退为切换搜索标签",
        )
        assertEquals(Config.Main(RootComponent.MainTab.Hello), blank.stack.value.active.configuration)
    }

    @Test
    fun `OAuth 深链回调携带 code 且不改动导航栈`() {
        val component = createComponent(newSettings("com/perol/pixez/test/nav/deep/oauth"))
        var oauthCode: String? = null

        val parsed = assertNotNull(DeepLinkParser.parse("pixez://account?code=abc123"), "OAuth 深链应可解析")
        DeepLinkParser.dispatch(parsed, component) { oauthCode = it }

        assertEquals("abc123", oauthCode, "OAuth 深链应把 code 交给登录回调")
        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "OAuth 深链不应改动导航栈",
        )
    }

    // endregion
}
