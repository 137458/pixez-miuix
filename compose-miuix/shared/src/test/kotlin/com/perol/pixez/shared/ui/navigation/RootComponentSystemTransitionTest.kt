package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.testutil.InMemorySettings
import com.perol.pixez.shared.ui.navigation.animation.TransitionTarget
import com.perol.pixez.shared.ui.navigation.animation.TransitionTargetKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RootComponentSystemTransitionTest {

    private fun createSettings(): SettingsRepository =
        SettingsRepository(InMemorySettings()).apply {
            hasCompletedGuide = true
        }

    @Test
    fun `独立详情窗口根组件在栈底执行 onBack 时触发 onExitRoot`() {
        var exited = false
        val component = RootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            settingsRepository = createSettings(),
            initialConfig = Config.IllustDetail(12345L),
            onExitRoot = { exited = true },
        )

        val initial = assertIs<IllustDetailComponent>(component.stack.value.active.instance)
        assertEquals(12345L, initial.illustId)

        component.onUserClicked(99L)
        assertIs<UserDetailComponent>(component.stack.value.active.instance)
        assertTrue(component.onBack())
        assertFalse(exited)
        assertIs<IllustDetailComponent>(component.stack.value.active.instance)

        assertTrue(component.onBack())
        assertTrue(exited)
    }

    @Test
    fun `onIllustClicked 优先走窗口转场拦截器，未接管或回调 fallback 时回退入栈`() {
        val component = RootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            settingsRepository = createSettings(),
        )
        var interceptedId: Long? = null
        component.illustWindowInterceptor = { illustId, _ ->
            interceptedId = illustId
            false
        }

        component.onIllustClicked(54321L)
        assertEquals(54321L, interceptedId)
        val detail = assertIs<IllustDetailComponent>(component.stack.value.active.instance)
        assertEquals(54321L, detail.illustId)

        var deferredFallback: (() -> Unit)? = null
        component.illustWindowInterceptor = { illustId, fallback ->
            interceptedId = illustId
            deferredFallback = fallback
            true
        }
        component.onIllustClicked(67890L)
        assertEquals(67890L, interceptedId)
        assertEquals(54321L, assertIs<IllustDetailComponent>(component.stack.value.active.instance).illustId)

        deferredFallback!!.invoke()
        assertEquals(67890L, assertIs<IllustDetailComponent>(component.stack.value.active.instance).illustId)
    }

    @Test
    fun `cardWindowInterceptor 统一覆盖小说、特辑与用户详情，未接管或回调 fallback 时回退入栈`() {
        val component = RootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            settingsRepository = createSettings(),
        )
        val intercepted = mutableListOf<Config>()
        val deferred = mutableListOf<() -> Unit>()
        component.cardWindowInterceptor = { config, fallback ->
            intercepted += config
            deferred += fallback
            true
        }

        val article = SpotlightArticle(
            id = 42L,
            title = "Pixivision 42",
            pureTitle = "Pixivision 42",
            thumbnail = "https://example.com/thumb.jpg",
            articleUrl = "https://www.pixivision.net/zh/a/42",
            publishDate = "2026-11-24",
        )
        component.onNovelClicked(42L)
        component.onSpotlightArticleClicked(article)
        component.onUserClicked(42L, initialTab = 1)
        component.onCommentsClicked(42L)
        component.onNovelCommentsClicked(42L)

        assertEquals(
            listOf<Config>(
                Config.NovelViewer(42L),
                Config.SpotlightDetail(article),
                Config.UserDetail(42L, initialTab = 1),
                Config.Comments(42L),
                Config.Comments(42L, isNovel = true),
            ),
            intercepted,
        )
        assertIs<Config.Main>(component.stack.value.active.instance)

        deferred.forEach { it.invoke() }
        assertEquals(Config.Comments(42L, isNovel = true), component.stack.value.active.configuration)
        assertTrue(component.onBack())
        assertEquals(Config.Comments(42L), component.stack.value.active.configuration)
        assertTrue(component.onBack())
        val topUser = assertIs<UserDetailComponent>(component.stack.value.active.instance)
        assertEquals(Config.UserDetail(42L, initialTab = 1), topUser.config)
        assertTrue(component.onBack())
        assertEquals(Config.SpotlightDetail(article), component.stack.value.active.configuration)
        assertTrue(component.onBack())
        assertEquals(Config.NovelViewer(42L), component.stack.value.active.configuration)
    }

    @Test
    fun `多类卡片路由编码往返与转场目标类型隔离`() {
        val article = SpotlightArticle(
            id = 42L,
            title = "Test",
            pureTitle = "Test",
            thumbnail = "https://example.com/a.jpg",
            articleUrl = "https://www.pixivision.net/zh/a/42",
            publishDate = "2026-11-24",
        )
        val configs = listOf(
            Config.IllustDetail(42L),
            Config.NovelViewer(42L),
            Config.SpotlightDetail(article),
            Config.UserDetail(42L, initialTab = 0),
            Config.Comments(42L, isNovel = false),
            Config.Comments(42L, isNovel = true),
        )
        configs.forEach { config ->
            assertEquals(config, decodeWindowConfig(encodeWindowConfig(config)))
        }
        val targets = configs.mapNotNull { it.toTransitionTarget() }
        assertEquals(
            listOf(
                TransitionTarget(TransitionTargetKind.Illust, 42L),
                TransitionTarget(TransitionTargetKind.Novel, 42L),
                TransitionTarget(TransitionTargetKind.Spotlight, 42L),
                TransitionTarget(TransitionTargetKind.User, 42L),
                TransitionTarget(TransitionTargetKind.Comments, 42L),
                TransitionTarget(TransitionTargetKind.NovelComments, 42L),
            ),
            targets,
        )
        assertEquals(targets.size, targets.toSet().size)
    }

    @Test
    fun `小说与特辑独立详情窗口在栈底返回时触发 onExitRoot`() {
        val article = SpotlightArticle(
            id = 77L,
            title = "Spotlight",
            pureTitle = "Spotlight",
            thumbnail = "https://example.com/s.jpg",
            articleUrl = "https://www.pixivision.net/zh/a/77",
            publishDate = "2026-11-24",
        )
        listOf(
            Config.NovelViewer(77L),
            Config.SpotlightDetail(article),
            Config.UserDetail(77L),
            Config.Comments(77L),
        ).forEach { initial ->
            var exited = false
            val component = RootComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                settingsRepository = createSettings(),
                initialConfig = initial,
                onExitRoot = { exited = true },
            )
            assertTrue(component.onBack())
            assertTrue(exited)
        }
    }

    @Test
    fun `独立详情窗口内 onLoginSuccess 仅退出登录栈而不替换为第二个首页`() {
        var exited = false
        val component = RootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            settingsRepository = createSettings(),
            initialConfig = Config.IllustDetail(99L),
            onExitRoot = { exited = true },
        )
        component.onLoginClicked()
        assertEquals(Config.Login, component.stack.value.active.configuration)

        component.onLoginSuccess()
        assertFalse(exited)
        assertIs<IllustDetailComponent>(component.stack.value.active.instance)
    }
}
