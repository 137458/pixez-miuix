package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 中-6b：作品详情页子 Component 试点（Decompose 惯例，child 实例由 Config 换为
 * [IllustDetailComponent]）的行为守护。
 *
 * 覆盖关键路径：
 * 1. 栈项实例——入栈 IllustDetail 后 child 实例为子组件且携带路由参数，configuration 不变；
 * 2. 跳转回调——子组件的每个出页回调逐项驱动根导航入栈对应 Config；
 * 3. 返回委托——子组件 onBack 委托根导航出栈；
 * 4. 嵌套实例——详情页内再点作品，两层栈项持有相互独立的子组件实例；
 * 5. 进程死亡恢复——返回栈经 stateKeeper 往返后，IllustDetail 栈项恢复为携带原参数的子组件。
 */
class IllustDetailComponentTest {

    private fun newSettings(nodePath: String): SettingsRepository {
        val node = Preferences.userRoot().node(nodePath)
        node.clear()
        return SettingsRepository(PreferencesSettings(node)).also {
            it.hasCompletedGuide = true
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

    /** 入栈一个 IllustDetail(42) 并断言 child 实例为子组件，返回该实例供后续断言复用。 */
    private fun pushDetail(root: RootComponent): IllustDetailComponent {
        root.onIllustClicked(42L)
        return assertIs<IllustDetailComponent>(
            root.stack.value.active.instance,
            "IllustDetail 栈项的 child 实例应为子组件",
        )
    }

    @Test
    fun `IllustDetail 栈项的实例为子组件且携带路由参数`() {
        val root = createComponent(newSettings("com/perol/pixez/test/detail/instance"))

        val detail = pushDetail(root)

        assertEquals(42L, detail.illustId, "子组件应携带入栈的作品 ID")
        assertEquals(42L, detail.config.illustId, "子组件应保留原始路由配置")
        assertEquals(
            Config.IllustDetail(illustId = 42L),
            root.stack.value.active.configuration,
            "child 实例组件化后 configuration 应保持不变",
        )
    }

    @Test
    fun `子组件跳转回调逐项驱动根导航`() {
        val root = createComponent(newSettings("com/perol/pixez/test/detail/callbacks"))
        val detail = pushDetail(root)

        detail.onCommentsClick(7L)
        assertEquals(
            Config.Comments(illustId = 7L),
            root.stack.value.active.configuration,
            "onCommentsClick 应入栈评论页",
        )
        assertTrue(root.onBack())

        detail.onUserClick(9L)
        assertEquals(
            Config.UserDetail(userId = 9L, initialTab = 0),
            root.stack.value.active.configuration,
            "onUserClick 应以默认作品标签入栈用户详情页",
        )
        assertTrue(root.onBack())

        detail.onTagClick("miku")
        assertEquals(
            Config.Search(query = "miku"),
            root.stack.value.active.configuration,
            "onTagClick 应以标签为关键词入栈搜索页",
        )
        assertTrue(root.onBack())

        detail.onRelatedIllustsClick(42L)
        assertEquals(
            Config.RelatedIllusts(illustId = 42L),
            root.stack.value.active.configuration,
            "onRelatedIllustsClick 应入栈相关作品页",
        )
        assertTrue(root.onBack())

        detail.onIllustSeriesClick(5L)
        assertEquals(
            Config.IllustSeries(seriesId = 5L),
            root.stack.value.active.configuration,
            "onIllustSeriesClick 应入栈插画系列页",
        )
        assertTrue(root.onBack())

        detail.onNovelClick(11L)
        assertEquals(
            Config.NovelViewer(novelId = 11L),
            root.stack.value.active.configuration,
            "onNovelClick 应入栈小说阅读页",
        )
        assertTrue(root.onBack())

        assertEquals(
            Config.IllustDetail(illustId = 42L),
            root.stack.value.active.configuration,
            "全部回调走查后应仍在详情页",
        )
    }

    @Test
    fun `子组件 onBack 委托根返回出栈`() {
        val root = createComponent(newSettings("com/perol/pixez/test/detail/back"))
        val detail = pushDetail(root)
        detail.onCommentsClick(7L)

        val consumed = detail.onBack()

        assertTrue(consumed, "子组件 onBack 应消费返回事件并委托根导航出栈")
        assertEquals(
            Config.IllustDetail(illustId = 42L),
            root.stack.value.active.configuration,
            "出栈后应回到详情页",
        )
    }

    @Test
    fun `详情页内嵌套入栈的作品持有独立子组件实例`() {
        val root = createComponent(newSettings("com/perol/pixez/test/detail/nested"))
        val first = pushDetail(root)

        first.onIllustClick(43L)
        val second = assertIs<IllustDetailComponent>(
            root.stack.value.active.instance,
            "嵌套入栈的详情页栈项实例同样应为子组件",
        )

        assertNotSame(first, second, "两层详情页应持有相互独立的子组件实例")
        assertEquals(42L, first.illustId)
        assertEquals(43L, second.illustId)
        assertTrue(second.onBack(), "嵌套详情页的 onBack 应出栈回到外层详情页")
        assertEquals(Config.IllustDetail(illustId = 42L), root.stack.value.active.configuration)
    }

    @Test
    fun `进程死亡后 IllustDetail 栈项恢复为携带原参数的子组件`() {
        val liveDispatcher = StateKeeperDispatcher()
        val live = createComponent(newSettings("com/perol/pixez/test/detail/restore/live"), liveDispatcher)
        live.onIllustClicked(42L)
        val saved = assertNotNull(liveDispatcher.save(), "存在注册项时保存态不应为空")

        val restored = createComponent(
            newSettings("com/perol/pixez/test/detail/restore/dead"),
            StateKeeperDispatcher(saved),
        )

        assertEquals(
            Config.IllustDetail(illustId = 42L),
            restored.stack.value.active.configuration,
            "进程死亡恢复：返回栈应经 stateKeeper 往返",
        )
        val detail = assertIs<IllustDetailComponent>(
            restored.stack.value.active.instance,
            "进程死亡恢复：详情页栈项实例应重建为子组件",
        )
        assertEquals(42L, detail.illustId, "重建的子组件应携带恢复出的作品 ID")
    }
}
