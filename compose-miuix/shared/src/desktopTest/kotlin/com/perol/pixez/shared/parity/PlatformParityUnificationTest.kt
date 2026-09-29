package com.perol.pixez.shared.parity

import com.perol.pixez.shared.navigation.DeepLinkParser
import com.perol.pixez.shared.navigation.ParsedDeepLink
import com.perol.pixez.shared.network.TrustedUrlPolicy
import com.perol.pixez.shared.platform.DesktopBackDispatcher
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.screens.GitHubReleaseAsset
import com.perol.pixez.shared.ui.screens.selectPlatformReleaseAsset
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformParityUnificationTest {

    @AfterTest
    fun tearDown() {
        DesktopBackDispatcher.clear()
    }

    @Test
    fun `selectPlatformReleaseAsset selects exe or msi or zip on Desktop and apk on Android`() {
        val assets = listOf(
            GitHubReleaseAsset(
                name = "pixez-miuix-1.2.0-release.apk",
                browser_download_url = "https://github.com/137458/pixez-miuix/releases/download/v1.2.0/pixez-miuix-1.2.0-release.apk",
                size = 25_000_000L,
            ),
            GitHubReleaseAsset(
                name = "PixEz-windows-x64-1.2.0.exe",
                browser_download_url = "https://github.com/137458/pixez-miuix/releases/download/v1.2.0/PixEz-windows-x64-1.2.0.exe",
                size = 68_000_000L,
            ),
            GitHubReleaseAsset(
                name = "PixEz-windows-portable-1.2.0.zip",
                browser_download_url = "https://github.com/137458/pixez-miuix/releases/download/v1.2.0/PixEz-windows-portable-1.2.0.zip",
                size = 64_000_000L,
            ),
        )

        val desktopSelected = selectPlatformReleaseAsset(assets, isDesktop = true, isAndroid = false)
        assertEquals("PixEz-windows-x64-1.2.0.exe", desktopSelected?.name)

        val androidSelected = selectPlatformReleaseAsset(assets, isDesktop = false, isAndroid = true)
        assertEquals("pixez-miuix-1.2.0-release.apk", androidSelected?.name)

        // Fallback to .zip on Desktop when no .exe/.msi exists
        val zipOnlyAssets = listOf(assets[0], assets[2])
        val desktopZipFallback = selectPlatformReleaseAsset(zipOnlyAssets, isDesktop = true, isAndroid = false)
        assertEquals("PixEz-windows-portable-1.2.0.zip", desktopZipFallback?.name)

        // If release only has Android APK, Desktop should NOT falsely select the .apk
        val apkOnlyAssets = listOf(assets[0])
        assertNull(selectPlatformReleaseAsset(apkOnlyAssets, isDesktop = true, isAndroid = false))
    }

    @Test
    fun `DeepLinkParser unifies OAuth callback and artwork user ranking routes across Desktop and Mobile`() {
        // 1. OAuth callbacks
        assertEquals(
            ParsedDeepLink.OAuthLogin("xyz_auth_code_123"),
            DeepLinkParser.parse("pixiv://account/login?code=xyz_auth_code_123&via=login"),
        )
        assertEquals(
            ParsedDeepLink.OAuthLogin("web_callback_code_999"),
            DeepLinkParser.parse("https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback?state=abc&code=web_callback_code_999"),
        )

        // 2. Custom URI scheme routes (must NOT be misrouted to OAuthLogin!)
        assertEquals(
            ParsedDeepLink.IllustDetail(11223344),
            DeepLinkParser.parse("pixiv://illust/11223344"),
        )
        assertEquals(
            ParsedDeepLink.UserDetail(998877),
            DeepLinkParser.parse("pixez://users/998877"),
        )
        assertEquals(
            ParsedDeepLink.MainTab(RootComponent.MainTab.Ranking),
            DeepLinkParser.parse("pixez://ranking"),
        )
        assertEquals(
            ParsedDeepLink.Search("初音ミク"),
            DeepLinkParser.parse("pixiv://search?word=%E5%88%9D%E9%9F%B3%E3%83%9F%E3%82%AF"),
        )
        assertEquals(
            ParsedDeepLink.DownloadTasks,
            DeepLinkParser.parse("pixez://downloads"),
        )
        assertEquals(
            ParsedDeepLink.History,
            DeepLinkParser.parse("pixez://history"),
        )

        // 3. Web URLs & clipboard text extraction
        assertEquals(
            ParsedDeepLink.IllustDetail(104857600),
            DeepLinkParser.parse("来看看这个作品 https://www.pixiv.net/artworks/104857600 很棒"),
        )
        assertEquals(
            ParsedDeepLink.UserDetail(543210),
            DeepLinkParser.parse("https://www.pixiv.net/en/users/543210/illustrations"),
        )
        assertEquals(
            ParsedDeepLink.IllustDetail(87654321),
            DeepLinkParser.parse("87654321"),
        )
    }

    @Test
    fun `DesktopBackDispatcher dispatches in LIFO order and skips disabled or disposed handlers`() {
        val invoked = mutableListOf<String>()
        val removeFirst = DesktopBackDispatcher.register(enabled = true) { invoked += "first" }
        val removeSecond = DesktopBackDispatcher.register(enabled = true) { invoked += "second" }
        val removeThirdDisabled = DesktopBackDispatcher.register(enabled = false) { invoked += "third_disabled" }

        assertTrue(DesktopBackDispatcher.dispatchBack())
        assertEquals(listOf("second"), invoked)

        removeSecond()
        removeThirdDisabled()

        assertTrue(DesktopBackDispatcher.dispatchBack())
        assertEquals(listOf("second", "first"), invoked)

        removeFirst()
        assertFalse(DesktopBackDispatcher.dispatchBack())
    }

    @Test
    fun `TrustedUrlPolicy imageUrl permits built-in pixiv-re mirror and user custom mirror host`() {
        val officialUrl = "https://i.pximg.net/img-original/img/2026/09/29/00/00/00/12345678_p0.png"
        val pixivReUrl = "https://i.pixiv.re/img-original/img/2026/09/29/00/00/00/12345678_p0.png"
        val customMirrorUrl = "https://pximg.my-mirror.example.org/img-original/img/2026/09/29/00/00/00/12345678_p0.png"

        assertEquals(officialUrl, TrustedUrlPolicy.imageUrl(officialUrl))
        assertEquals(pixivReUrl, TrustedUrlPolicy.imageUrl(pixivReUrl))
        assertEquals(
            customMirrorUrl,
            TrustedUrlPolicy.imageUrl(customMirrorUrl, mirrorHost = "pximg.my-mirror.example.org"),
        )
    }
}
