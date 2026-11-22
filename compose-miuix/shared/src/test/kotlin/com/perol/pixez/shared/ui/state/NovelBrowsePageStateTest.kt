package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.NovelImageUrls
import com.perol.pixez.shared.data.model.NovelProfileImageUrls
import com.perol.pixez.shared.data.model.NovelTag
import com.perol.pixez.shared.data.model.NovelUser
import com.perol.pixez.shared.data.repository.NovelRankingMode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * [NovelBrowsePageState] 的跨 Tab 与跨页状态保留验证。
 *
 * 覆盖：
 * 1. 推荐与各排行榜模式各自持有独立分页状态，切换 Tab/Mode 复用既有实例不重建；
 * 2. 复用后已有数据的榜单 `loadFirst()` 缓存早退，不触发重复网络请求；
 * 3. 各 Tab/Mode 独立保存与恢复滚动位置；
 * 4. 账号切换 `resetAll()` 清空所有已缓存列表与滚动位置。
 */
class NovelBrowsePageStateTest {

    private fun novelFixture(id: Long): Novel = Novel(
        id = id,
        title = "小说 $id",
        restrict = 0,
        xRestrict = 0,
        isOriginal = false,
        imageUrls = NovelImageUrls("s$id", "m$id", "l$id"),
        createDate = "2026-01-01T00:00:00+09:00",
        tags = listOf(NovelTag("原创", null, true)),
        pageCount = 1,
        textLength = 500,
        user = NovelUser(1L, "作者", "author", NovelProfileImageUrls("pm"), isFollowed = false),
        isBookmarked = false,
        visible = true,
        isMuted = false,
        isMypixivOnly = false,
        isXRestricted = false,
        novelAIType = 0,
    )

    @Test
    fun `推荐与排行榜各模式独立持有并复用分页状态`() = runBlocking {
        val state = NovelBrowsePageState()
        var recommendFetches = 0
        var dayRankingFetches = 0

        val rec1 = state.getOrCreateList(NovelBrowseTab.Recommend, NovelRankingMode.Day) {
            PagedListState(
                fetch = {
                    recommendFetches++
                    PagedListPage(listOf(novelFixture(1L)), nextUrl = null)
                },
                mergeItems = { old, new -> old + new },
            )
        }
        rec1.loadFirst()

        val day1 = state.getOrCreateList(NovelBrowseTab.Ranking, NovelRankingMode.Day) {
            PagedListState(
                fetch = {
                    dayRankingFetches++
                    PagedListPage(listOf(novelFixture(2L)), nextUrl = null)
                },
                mergeItems = { old, new -> old + new },
            )
        }
        day1.loadFirst()

        val week = state.getOrCreateList(NovelBrowseTab.Ranking, NovelRankingMode.Week) {
            PagedListState(
                fetch = { PagedListPage(listOf(novelFixture(3L)), nextUrl = null) },
                mergeItems = { old, new -> old + new },
            )
        }

        // 切回推荐或日榜时复用原实例，且 loadFirst 早退不重拉网络
        val rec2 = state.getOrCreateList(NovelBrowseTab.Recommend, NovelRankingMode.Day) {
            error("已有实例时不应重建工厂")
        }
        val day2 = state.getOrCreateList(NovelBrowseTab.Ranking, NovelRankingMode.Day) {
            error("已有实例时不应重建工厂")
        }
        rec2.loadFirst()
        day2.loadFirst()

        assertSame(rec1, rec2)
        assertSame(day1, day2)
        assertNotSame(day1, week)
        assertEquals(1, recommendFetches, "切回推荐页应缓存早退，不得重复请求")
        assertEquals(1, dayRankingFetches, "切回日榜应缓存早退，不得重复请求")
        assertEquals(1L, rec2.items?.single()?.id)
        assertEquals(2L, day2.items?.single()?.id)
    }

    @Test
    fun `各分类与榜单独立保存与恢复滚动位置`() {
        val state = NovelBrowsePageState()

        state.saveScrollPosition(NovelBrowseTab.Recommend, NovelRankingMode.Day, index = 5, offset = 42)
        state.saveScrollPosition(NovelBrowseTab.Ranking, NovelRankingMode.Week, index = 12, offset = 88)

        assertEquals(
            GridScrollPosition(index = 5, offset = 42),
            state.getScrollPosition(NovelBrowseTab.Recommend, NovelRankingMode.Day),
        )
        assertEquals(
            GridScrollPosition(index = 12, offset = 88),
            state.getScrollPosition(NovelBrowseTab.Ranking, NovelRankingMode.Week),
        )
        assertEquals(
            GridScrollPosition.Zero,
            state.getScrollPosition(NovelBrowseTab.Ranking, NovelRankingMode.Month),
        )
    }

    @Test
    fun `resetAll 清空所有缓存列表与滚动位置`() = runBlocking {
        val state = NovelBrowsePageState()
        val rec = state.getOrCreateList(NovelBrowseTab.Recommend, NovelRankingMode.Day) {
            PagedListState(
                fetch = { PagedListPage(listOf(novelFixture(10L)), nextUrl = null) },
                mergeItems = { old, new -> old + new },
            )
        }
        rec.loadFirst()
        state.saveScrollPosition(NovelBrowseTab.Recommend, NovelRankingMode.Day, index = 4, offset = 16)

        state.resetAll()

        val recAfter = state.getOrCreateList(NovelBrowseTab.Recommend, NovelRankingMode.Day) {
            PagedListState(
                fetch = { PagedListPage(emptyList(), nextUrl = null) },
                mergeItems = { old, new -> old + new },
            )
        }
        assertNotSame(rec, recAfter, "resetAll 后应清除旧账号列表实例")
        assertEquals(
            GridScrollPosition.Zero,
            state.getScrollPosition(NovelBrowseTab.Recommend, NovelRankingMode.Day),
            "resetAll 后应清空滚动位置",
        )
    }
}
