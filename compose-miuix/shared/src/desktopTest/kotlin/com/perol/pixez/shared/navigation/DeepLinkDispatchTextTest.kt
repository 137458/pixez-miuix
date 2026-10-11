package com.perol.pixez.shared.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.navigation.Config
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 中-5：[DeepLinkParser.dispatchText]（parse + dispatch 包装）的端到端行为：
 * 深链文本入栈对应页面、OAuth code 交给登录回调、非深链文本不分发不改栈。
 */
class DeepLinkDispatchTextTest {

    private fun createComponent(nodePath: String): RootComponent {
        val node = Preferences.userRoot().node(nodePath)
        node.clear()
        val settings = SettingsRepository(PreferencesSettings(node)).also {
            it.hasCompletedGuide = true
        }
        return RootComponent(
            DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            settings,
        )
    }

    @Test
    fun `作品链接文本经 dispatchText 入栈详情页`() {
        val component = createComponent("com/perol/pixez/test/deeplink/dispatch-text/illust")

        DeepLinkParser.dispatchText("快看 https://www.pixiv.net/artworks/42", component) {}

        assertEquals(
            Config.IllustDetail(illustId = 42L),
            component.stack.value.active.configuration,
            "dispatchText 应把作品链接文本分发到详情页入栈",
        )
    }

    @Test
    fun `OAuth 链接文本经 dispatchText 交给登录回调且不改栈`() {
        val component = createComponent("com/perol/pixez/test/deeplink/dispatch-text/oauth")
        var oauthCode: String? = null

        DeepLinkParser.dispatchText("pixez://account?code=code777", component) { oauthCode = it }

        assertEquals("code777", oauthCode, "OAuth 链接文本应把 code 交给登录回调")
        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "OAuth 文本不应改动导航栈",
        )
    }

    @Test
    fun `非深链文本 dispatchText 不分发不改栈`() {
        val component = createComponent("com/perol/pixez/test/deeplink/dispatch-text/plain")
        var oauthCode: String? = null

        DeepLinkParser.dispatchText("just a plain sentence", component) { oauthCode = it }

        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "非深链文本不应改动导航栈",
        )
        assertNull(oauthCode, "非深链文本不应触发登录回调")
    }

    @Test
    fun `dispatchText 空文本静默忽略`() {
        val component = createComponent("com/perol/pixez/test/deeplink/dispatch-text/null")

        DeepLinkParser.dispatchText(null, component) {}

        assertEquals(
            Config.Main(RootComponent.MainTab.Hello),
            component.stack.value.active.configuration,
            "空文本不应改动导航栈",
        )
    }
}
