package com.perol.pixez.shared.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.perol.pixez.shared.data.repository.InMemorySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.navigation.RootComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClipboardOAuthGuardTest {

    private fun createRootComponent(): RootComponent {
        val lifecycle = LifecycleRegistry()
        val settings = SettingsRepository(InMemorySettings())
        return RootComponent(
            componentContext = DefaultComponentContext(lifecycle),
            settingsRepository = settings,
        )
    }

    @Test
    fun `剪贴板来源的 OAuth 回调只触发二次确认而不直接静默登录`() {
        val root = createRootComponent()
        var confirmCode: String? = null

        DeepLinkParser.dispatchTextFromClipboard(
            raw = "pixiv://account/login?code=clip_secret_code",
            rootComponent = root,
            onConfirmOAuthCode = { code ->
                confirmCode = code
                root.requestClipboardOAuthConfirmation(code)
            },
        )

        assertEquals("clip_secret_code", confirmCode)
        assertEquals("clip_secret_code", root.pendingClipboardOAuthCode.value)

        assertEquals("clip_secret_code", root.consumeClipboardOAuthConfirmation())
        assertNull(root.pendingClipboardOAuthCode.value)

        // 取消时也应清空待确认 code
        root.requestClipboardOAuthConfirmation("another_code")
        assertEquals("another_code", root.pendingClipboardOAuthCode.value)
        root.dismissClipboardOAuthConfirmation()
        assertNull(root.pendingClipboardOAuthCode.value)
    }

    @Test
    fun `非 OAuth 的剪贴板深链正常完成导航分发`() {
        val root = createRootComponent()
        var oauthTriggered = false
        DeepLinkParser.dispatchTextFromClipboard(
            raw = "https://www.pixiv.net/artworks/98765432",
            rootComponent = root,
            onConfirmOAuthCode = { oauthTriggered = true },
        )
        assertEquals(false, oauthTriggered)
        assertNull(root.pendingClipboardOAuthCode.value)
    }
}
