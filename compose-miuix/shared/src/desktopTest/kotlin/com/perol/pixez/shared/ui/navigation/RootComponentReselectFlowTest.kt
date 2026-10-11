package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [RootComponent.reselectFlowOf] 过滤语义：仅透传目标标签的再选中事件，
 * 其他标签的事件被拦截；不同标签目标互相独立。
 */
class RootComponentReselectFlowTest {

    private fun createRootComponent(nodePath: String): RootComponent {
        val node = Preferences.userRoot().node(nodePath)
        node.clear()
        return RootComponent(
            DefaultComponentContext(LifecycleRegistry()),
            SettingsRepository(PreferencesSettings(node)),
        )
    }

    @Test
    fun `Hello 标签流透传 Hello 再选中并拦截其他标签`() = runBlocking {
        val component = createRootComponent("com/perol/pixez/test/reselect/hello")
        val received = mutableListOf<Unit>()
        // UNDISPATCHED 启动：collect 同步完成 SharedFlow 订阅后挂起，保证后续事件不因缓冲溢出被丢弃。
        // SharedFlow 收集永不结束，runBlocking 等待子协程完成，故断言结束后必须 cancel。
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            component.reselectFlowOf(RootComponent.MainTab.Hello).collect { received.add(Unit) }
        }

        component.onTabReselected(RootComponent.MainTab.New)
        yield()
        assertEquals(0, received.size, "其他标签的再选中事件不应透传")

        component.onTabReselected(RootComponent.MainTab.Hello)
        yield()
        assertEquals(1, received.size, "目标标签的再选中事件应透传一次")

        component.onTabReselected(RootComponent.MainTab.Search)
        yield()
        component.onTabReselected(RootComponent.MainTab.Hello)
        yield()
        assertEquals(2, received.size, "混杂其他标签后目标标签事件仍应继续透传")

        collector.cancel()
    }

    @Test
    fun `New 标签流对同一事件序列独立过滤`() = runBlocking {
        val component = createRootComponent("com/perol/pixez/test/reselect/new")
        val received = mutableListOf<Unit>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            component.reselectFlowOf(RootComponent.MainTab.New).collect { received.add(Unit) }
        }

        component.onTabReselected(RootComponent.MainTab.Hello)
        yield()
        assertEquals(0, received.size, "Hello 的再选中事件不应进入 New 标签流")

        component.onTabReselected(RootComponent.MainTab.New)
        yield()
        assertEquals(1, received.size, "New 的再选中事件应透传一次")

        collector.cancel()
    }
}
