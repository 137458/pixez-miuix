package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * 中-6a Child 镜像删除后的 [RootComponent] 行为守护：
 * 1. Main child 创建时经 createChild 接线跨页恢复容器（高-5 语义迁移，原 Child.Main.feedRestore）；
 * 2. 二级页出入栈不重建恢复容器（Main child 的 InstanceKeeper 实例跨页保留）；
 * 3. 登录成功等配置栈重置复用 Main child，恢复容器实例保留；
 * 4. [Config] 平移为顶层类后序列化判别符钉住平移前 FQN，wire format 不变。
 */
class RootComponentFeedRestoreWiringTest {

    private fun createRootComponent(nodePath: String): Pair<RootComponent, SettingsRepository> {
        val node = Preferences.userRoot().node(nodePath)
        node.clear()
        val settings = SettingsRepository(PreferencesSettings(node))
        // 完成向导 → 初始路由为 Main(Hello)，childStack 构造时即创建 Main child。
        settings.hasCompletedGuide = true
        val component = RootComponent(
            DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            settings,
        )
        return component to settings
    }

    @Test
    fun `Main 初始 child 创建时接线跨页恢复容器`() {
        val (component, _) = createRootComponent("com/perol/pixez/test/wiring/main")

        assertEquals(Config.Main(RootComponent.MainTab.Hello), component.stack.value.active.configuration)
        assertNull(component.mainFeedRestore.hello.snapshot, "新会话的推荐页恢复缓存应为空快照")

        component.mainFeedRestore.ranking.update("day_2026-10-11", emptyList(), nextUrl = null)
        assertEquals(
            emptyList(),
            component.mainFeedRestore.ranking.get("day_2026-10-11")?.illustIds,
            "接线后的排行榜恢复缓存应可正常读写",
        )
    }

    @Test
    fun `二级页出入栈不重建 Main 恢复容器`() {
        val (component, _) = createRootComponent("com/perol/pixez/test/wiring/pushpop")
        val original = component.mainFeedRestore

        component.onIllustClicked(42L)
        assertEquals(Config.IllustDetail(illustId = 42L), component.stack.value.active.configuration)

        component.onBack()
        assertEquals(Config.Main(RootComponent.MainTab.Hello), component.stack.value.active.configuration)
        assertSame(original, component.mainFeedRestore, "Main child 未销毁，恢复容器实例应跨页保留")
    }

    @Test
    fun `登录成功等配置栈重置复用恢复容器实例`() {
        val (component, _) = createRootComponent("com/perol/pixez/test/wiring/reset")
        val original = component.mainFeedRestore

        component.onLoginSuccess()

        assertEquals(Config.Main(RootComponent.MainTab.Hello), component.stack.value.active.configuration)
        // Decompose 对等配置的栈重置复用既有 back-stack Main child：工厂不再调用，
        // 恢复容器随 InstanceKeeper 保留；若 createMainFeedRestore 被重复执行，
        // stateKeeper 重复 register 同 key 会抛 IllegalStateException，此处实例身份一并守护。
        assertSame(original, component.mainFeedRestore, "等配置栈重置应复用 Main child 的恢复容器")
    }

    @Test
    fun `Config 平移后序列化保持平移前 wire format`() {
        // 判别符必须钉住平移前嵌套 FQN（RootComponent.Config.*），否则跨版本
        // savedInstanceState（Essenty StateKeeper）恢复反序列化失败。
        val illustJson = Json.encodeToString(Config.serializer(), Config.IllustDetail(illustId = 42L))
        assertEquals(
            """{"type":"com.perol.pixez.shared.ui.navigation.RootComponent.Config.IllustDetail","illustId":42}""",
            illustJson,
            "sealed 子类判别符应保持平移前 FQN，字段内联其后",
        )
        assertEquals(Config.IllustDetail(illustId = 42L), Json.decodeFromString(Config.serializer(), illustJson))

        val objectJson = Json.encodeToString(Config.serializer(), Config.Settings)
        assertEquals(
            """{"type":"com.perol.pixez.shared.ui.navigation.RootComponent.Config.Settings"}""",
            objectJson,
            "data object 子类应仅携带判别符",
        )
        assertEquals(Config.Settings, Json.decodeFromString(Config.serializer(), objectJson))
    }
}
