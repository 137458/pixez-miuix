package com.perol.pixez.shared.ui.coui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.HttpEngineDefaults
import com.perol.pixez.shared.network.RobustDohDns
import com.perol.pixez.shared.testutil.InMemorySettings
import com.perol.pixez.shared.ui.components.buildIllustCopyInfo
import com.perol.pixez.shared.ui.coui.core.resolveCouiEffectiveFloatingBottomBar
import com.perol.pixez.shared.ui.navigation.Config
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.screens.starAfterSaveIfNeeded
import com.perol.pixez.shared.ui.state.StateTestFixtures
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.forms.FormDataContent
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import java.io.File
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 审查并守护 COUI 皮肤下各项设置实际生效链路，避免“界面可点可写但业务层不消费”。
 */
class CouiSettingsEffectivenessTest {

    @Test
    fun `SettingsRepository preserves crossCount and hCrossCount across 2 to 6`() {
        val repo = SettingsRepository(InMemorySettings())
        (2..6).forEach { count ->
            repo.crossCount = count
            repo.hCrossCount = count
            assertEquals(count, repo.crossCount, "crossCount=$count 不应被截断")
            assertEquals(count, repo.hCrossCount, "hCrossCount=$count 不应被截断")
        }
    }

    @Test
    fun `COUI illust grids resolve columns from layout settings instead of hardcoding 150dp`() {
        val gridSource = locateSharedSourceFile("ui/coui/component/CouiIllustGrid.kt").readText()
        val historySource = locateSharedSourceFile("ui/coui/page/CouiHistoryScreen.kt").readText()

        assertFalse(
            gridSource.contains("StaggeredGridCells.Adaptive(minSize = 150.dp)"),
            "CouiIllustGrid 不应硬编码 150.dp 自适应列宽",
        )
        assertTrue(
            gridSource.contains("rememberIllustGridColumns("),
            "CouiIllustGrid 应接入 rememberIllustGridColumns 读取横竖屏列数与自适应阈值",
        )
        assertFalse(
            historySource.contains("StaggeredGridCells.Adaptive(minSize = 150.dp)"),
            "CouiHistoryScreen 插画历史网格不应硬编码 150.dp 自适应列宽",
        )
        assertTrue(
            historySource.contains("rememberIllustGridColumns("),
            "CouiHistoryScreen 应接入 rememberIllustGridColumns",
        )
    }

    @Test
    fun `RootComponent resolves spotlight welcome page and CouiWelcomePageScreen excludes unreachable search`() {
        val settings = SettingsRepository(InMemorySettings()).apply {
            hasCompletedGuide = true
            welcomePageType = "spotlight"
        }
        val component = RootComponent(
            DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            settings,
        )
        assertEquals(Config.Main(RootComponent.MainTab.Spotlight), component.stack.value.active.configuration)
        assertEquals(RootComponent.MainTab.Spotlight, component.selectedTab.value)

        val couiWelcomeSource = locateSharedSourceFile("ui/coui/page/CouiWelcomePageScreen.kt").readText()
        assertFalse(
            couiWelcomeSource.contains("\"search\" to strings.tabSearch"),
            "COUI 皮肤无 Search 一级页签，CouiWelcomePageScreen 不应提供无效的 search 欢迎页选项",
        )
        assertTrue(
            couiWelcomeSource.contains("\"quick_view\" to strings.tabNew"),
            "quick_view 路由对应动态页，文案应为 tabNew 而非 tabSpotlight",
        )
        assertTrue(
            couiWelcomeSource.contains("\"spotlight\" to strings.tabSpotlight"),
            "CouiWelcomePageScreen 应提供真正的 spotlight 欢迎页选项",
        )
    }

    @Test
    fun `RootContent disables floating bottom bar padding on COUI narrow screen below API 34`() {
        val rootSource = locateSharedSourceFile("ui/navigation/RootContent.kt").readText()
        assertTrue(
            rootSource.contains("resolveCouiEffectiveFloatingBottomBar("),
            "RootContent 应按皮肤与 SDK 能力计算实际生效的浮动底栏状态，避免 API 31-33 窄屏贴底栏多留 100dp 底部空白",
        )
        assertFalse(resolveCouiEffectiveFloatingBottomBar(isCouiSkin = true, sdkInt = 31, isWideScreen = false, useFloatingBottomBar = true))
        assertFalse(resolveCouiEffectiveFloatingBottomBar(isCouiSkin = true, sdkInt = 33, isWideScreen = false, useFloatingBottomBar = true))
        assertTrue(resolveCouiEffectiveFloatingBottomBar(isCouiSkin = true, sdkInt = 34, isWideScreen = false, useFloatingBottomBar = true))
        assertTrue(resolveCouiEffectiveFloatingBottomBar(isCouiSkin = true, sdkInt = 31, isWideScreen = true, useFloatingBottomBar = true))
        assertFalse(resolveCouiEffectiveFloatingBottomBar(isCouiSkin = true, sdkInt = 34, isWideScreen = false, useFloatingBottomBar = false))
    }

    @Test
    fun `network guide mode normalizes legacy direct and sni to standard and ech`() {
        val repo = SettingsRepository(InMemorySettings())
        repo.apiNetworkMode = "direct"
        repo.oauthNetworkMode = "sni"

        assertEquals(HttpEngineDefaults.MODE_STANDARD, repo.apiNetworkMode)
        assertEquals("ech", repo.oauthNetworkMode)

        val mockDoh = FakeDns("1.2.3.4")
        val mockFallback = FakeDns("5.6.7.8")
        val dns = RobustDohDns(
            doh = mockDoh,
            fallback = mockFallback,
            hasProxyProvider = { false },
            modeProvider = { "direct" },
        )
        assertEquals("5.6.7.8", dns.lookup("app-api.pixiv.net").first().hostAddress)
        assertEquals(0, mockDoh.calls)

        val couiGuideSource = locateSharedSourceFile("ui/coui/page/CouiGuideScreen.kt").readText()
        assertFalse(couiGuideSource.contains("\"direct\""), "CouiGuideScreen 不应写入无效网络模式 direct")
        assertFalse(couiGuideSource.contains("\"sni\""), "CouiGuideScreen 不应写入无效网络模式 sni")
    }

    @Test
    fun `CouiNetworkSettingScreen handles custom host selection from dropdown`() {
        val networkSource = locateSharedSourceFile("ui/coui/page/CouiNetworkSettingScreen.kt").readText()
        assertTrue(
            networkSource.contains("2 ->"),
            "CouiNetworkSettingScreen 图片源下拉选择自定义 Host（index=2）应有明确响应",
        )
    }

    @Test
    fun `novel cards and copyInfoText respect feedAIBadge and user template`() {
        val novelListSource = locateSharedSourceFile("ui/coui/page/CouiSearchResultLists.kt").readText()
        val userNovelSource = locateSharedSourceFile("ui/coui/page/CouiUserNovelListScreen.kt").readText()
        assertTrue(
            novelListSource.contains("feedAIBadge"),
            "CouiSearchResultLists 的 CouiNovelRow 应遵循 feedAIBadge 设置",
        )
        assertTrue(
            userNovelSource.contains("feedAIBadge"),
            "CouiUserNovelListScreen 的 CouiNovelCard 应遵循 feedAIBadge 设置",
        )

        val illust = StateTestFixtures.illust(id = 12345L)
        val actionMenuSource = locateSharedSourceFile("ui/components/IllustActionMenu.kt").readText()
        val moreMenuSource = locateSharedSourceFile("ui/components/IllustDetailMoreMenu.kt").readText()
        val couiDetailSource = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        val topBarSource = locateSharedSourceFile("ui/components/IllustDetailTopBar.kt").readText()
        assertTrue(
            actionMenuSource.contains("settings.copyInfoText"),
            "rememberIllustQuickActions 应读取 settings.copyInfoText 模板",
        )
        assertTrue(
            moreMenuSource.contains("copyInfoTemplate"),
            "buildIllustDetailMoreMenuEntries 应接收并传递 copyInfoTemplate 参数",
        )
        assertTrue(
            couiDetailSource.contains("copyInfoTemplate = settings.copyInfoText") &&
                topBarSource.contains("copyInfoTemplate ="),
            "COUI 与 MIUIX 详情页更多菜单均应传入 copyInfoText 模板",
        )
        // 验证默认模板仍保持向后兼容
        assertEquals(
            "title:${illust.title}\npainter:${illust.user.name}\nillust id:${illust.id}",
            buildIllustCopyInfo(illust),
        )
        assertEquals(
            "title:${illust.title}\npainter:${illust.user.name}\nillust id:${illust.id}",
            buildIllustCopyInfo(illust, "   "),
        )
        val taggedIllust = illust.copy(
            tags = listOf(IllustTag(name = "original"), IllustTag(name = "landscape")),
        )
        assertEquals(
            "${taggedIllust.title} #${taggedIllust.id} by ${taggedIllust.user.name}(${taggedIllust.user.id}) [original, landscape]",
            buildIllustCopyInfo(
                taggedIllust,
                "{title} #{illust_id} by {user_name}({user_id}) [{tags}]",
            ),
        )
        assertEquals(
            "author=${taggedIllust.user.name}",
            buildIllustCopyInfo(taggedIllust, "author={author}"),
        )
    }

    @Test
    fun `CouiDownloadSettingScreen does not display misleading share format summary for fileNameEval`() {
        val downloadSettingSource = locateSharedSourceFile("ui/coui/page/CouiDownloadSettingScreen.kt").readText()
        assertFalse(
            downloadSettingSource.contains("if (fileNameEval) strings.settingShareFormat else format"),
            "fileNameEval 未接通脚本命名，不应把保存格式摘要误显示为分享格式",
        )
    }

    @Test
    fun `starAfterSaveIfNeeded bookmarks unbookmarked illusts across all save paths`() = runBlocking {
        val capturedRestricts = mutableListOf<String>()
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val form = request.body as FormDataContent
                    capturedRestricts.add(form.formData["restrict"].orEmpty())
                    respondOk()
                }
            }
        }
        val repo = BookmarkRepository(client)
        val settings = SettingsRepository(InMemorySettings()).apply {
            starAfterSave = true
            defaultPrivateLike = true
        }
        var marked = false
        starAfterSaveIfNeeded(
            illustId = 42L,
            isBookmarked = false,
            settings = settings,
            bookmarkRepository = repo,
            onBookmarked = { marked = true },
        )
        assertTrue(marked, "保存后自动收藏应将作品标记为已收藏")
        assertEquals(listOf("private"), capturedRestricts)

        // 已收藏时不再重复发起请求
        starAfterSaveIfNeeded(
            illustId = 42L,
            isBookmarked = true,
            settings = settings,
            bookmarkRepository = repo,
        )
        assertEquals(1, capturedRestricts.size)

        // 入口接线核对：全屏看图、快捷菜单与多页单图均接入 starAfterSaveIfNeeded
        val viewerLogicSource = locateSharedSourceFile("ui/components/IllustViewerLogic.kt").readText()
        val actionMenuSource = locateSharedSourceFile("ui/components/IllustActionMenu.kt").readText()
        val couiDetailSource = locateSharedSourceFile("ui/coui/page/CouiIllustDetailScreen.kt").readText()
        assertTrue(viewerLogicSource.contains("starAfterSaveIfNeeded("))
        assertTrue(actionMenuSource.contains("starAfterSaveIfNeeded("))
        assertTrue(couiDetailSource.contains("starAfterSaveIfNeeded("))
        client.close()
    }

    private class FakeDns(private val ip: String) : Dns {
        var calls = 0
        override fun lookup(hostname: String): List<InetAddress> {
            calls += 1
            return listOf(InetAddress.getByName(ip))
        }
    }

    private fun locateSharedSourceFile(relativePath: String): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val suffix = "src/main/kotlin/com/perol/pixez/shared/$relativePath"
        while (dir != null) {
            val direct = File(dir, suffix)
            if (direct.isFile) return direct
            val nested = File(dir, "shared/$suffix")
            if (nested.isFile) return nested
            val rootNested = File(dir, "compose-miuix/shared/$suffix")
            if (rootNested.isFile) return rootNested
            dir = dir.parentFile
        }
        error("Unable to locate $relativePath from ${System.getProperty("user.dir")}")
    }
}
