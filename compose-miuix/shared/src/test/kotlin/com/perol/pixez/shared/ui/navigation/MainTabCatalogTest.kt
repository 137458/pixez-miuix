package com.perol.pixez.shared.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 一级标签目录（nav13）回归：目录表是 MAIN_TAB_ORDER、底栏/侧栏徽标判定的唯一来源。
 */
class MainTabCatalogTest {

    private val descriptors get() = MAIN_TAB_DESCRIPTORS

    @Test
    fun `目录表与 MAIN_TAB_ORDER 同序同长`() {
        assertEquals(
            RootComponent.MAIN_TAB_ORDER.size,
            descriptors.size,
            "目录表长度必须与 MAIN_TAB_ORDER 一致",
        )
        assertEquals(
            RootComponent.MAIN_TAB_ORDER,
            descriptors.map { it.tab },
            "目录表顺序即 Pager 页序，必须与 MAIN_TAB_ORDER 完全一致",
        )
    }

    @Test
    fun `每个一级标签都有目录条目且不重复`() {
        assertEquals(
            RootComponent.MainTab.entries.toSet(),
            descriptors.map { it.tab }.toSet(),
            "新增一级标签必须同时登记目录条目，否则该页无法被渲染",
        )
        assertEquals(
            descriptors.size,
            descriptors.map { it.tab }.distinct().size,
            "同一标签不得出现两条目录条目",
        )
        assertEquals(5, descriptors.size, "本项目一级标签固定为 5 个")
    }

    @Test
    fun `仅动态页承载未读徽标`() {
        val badgeTabs = descriptors.filter { it.carriesUnreadFeedBadge }.map { it.tab }
        assertEquals(listOf(RootComponent.MainTab.New), badgeTabs, "只有动态页承载未读动态徽标")
    }

    @Test
    fun `徽标判定在非当前页且有未读时显示`() {
        val newTab = descriptors.first { it.tab == RootComponent.MainTab.New }
        assertTrue(
            shouldShowUnreadFeedBadge(newTab, activeTab = RootComponent.MainTab.Hello, hasUnreadFeedBadge = true),
            "停在其它标签且存在未读动态时应显示徽标",
        )
        assertTrue(
            shouldShowUnreadFeedBadge(newTab, activeTab = RootComponent.MainTab.Search, hasUnreadFeedBadge = true),
            "停在任意非动态标签且存在未读动态时应显示徽标",
        )
    }

    @Test
    fun `徽标判定在当前页或无未读时不显示`() {
        val newTab = descriptors.first { it.tab == RootComponent.MainTab.New }
        assertFalse(
            shouldShowUnreadFeedBadge(newTab, activeTab = RootComponent.MainTab.New, hasUnreadFeedBadge = true),
            "已停留在动态页时不显示徽标",
        )
        assertFalse(
            shouldShowUnreadFeedBadge(newTab, activeTab = RootComponent.MainTab.Hello, hasUnreadFeedBadge = false),
            "无未读动态时不显示徽标",
        )
    }

    @Test
    fun `不承载徽标的标签永不显示徽标`() {
        val hello = descriptors.first { it.tab == RootComponent.MainTab.Hello }
        assertFalse(
            shouldShowUnreadFeedBadge(hello, activeTab = RootComponent.MainTab.New, hasUnreadFeedBadge = true),
            "仅动态页承载徽标，其它标签即使存在未读也不得显示",
        )
        assertFalse(
            shouldShowUnreadFeedBadge(hello, activeTab = RootComponent.MainTab.New, hasUnreadFeedBadge = false),
            "不承载徽标的标签在无未读时同样不显示",
        )
    }
}
