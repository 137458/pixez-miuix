package com.perol.pixez.shared.parity

import com.perol.pixez.shared.data.repository.GitHubReleaseAsset
import com.perol.pixez.shared.data.repository.selectPlatformReleaseAsset
import com.perol.pixez.shared.navigation.DeepLinkParser
import com.perol.pixez.shared.navigation.ParsedDeepLink
import com.perol.pixez.shared.network.TrustedUrlPolicy
import com.perol.pixez.shared.ui.navigation.RootComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformParityUnificationTest {

    @Test
    fun `selectPlatformReleaseAsset selects apk and ignores non-apk assets`() {
        val assets = listOf(
            GitHubReleaseAsset(
                name = "PixEz-windows-x64-1.2.0.exe",
                browser_download_url = "https://github.com/137458/pixez-miuix/releases/download/v1.2.0/PixEz-windows-x64-1.2.0.exe",
                size = 68_000_000L,
            ),
            GitHubReleaseAsset(
                name = "pixez-miuix-1.2.0-release.apk",
                browser_download_url = "https://github.com/137458/pixez-miuix/releases/download/v1.2.0/pixez-miuix-1.2.0-release.apk",
                size = 25_000_000L,
            ),
            GitHubReleaseAsset(
                name = "PixEz-windows-portable-1.2.0.zip",
                browser_download_url = "https://github.com/137458/pixez-miuix/releases/download/v1.2.0/PixEz-windows-portable-1.2.0.zip",
                size = 64_000_000L,
            ),
        )

        val selected = selectPlatformReleaseAsset(assets)
        assertEquals("pixez-miuix-1.2.0-release.apk", selected?.name)

        // When no .apk asset exists, null is returned rather than falling back to desktop archives.
        assertNull(selectPlatformReleaseAsset(listOf(assets[0], assets[2])))
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

    @Test
    fun `AppConstants Urls pixivNovelUrl formats correctly for Int and Long`() {
        assertEquals("https://www.pixiv.net/novel/show.php?id=987654321", com.perol.pixez.shared.ui.AppConstants.Urls.pixivNovelUrl(987654321L))
        assertEquals("https://www.pixiv.net/novel/show.php?id=123456", com.perol.pixez.shared.ui.AppConstants.Urls.pixivNovelUrl(123456))
    }
}
