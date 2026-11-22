package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.repository.NovelRankingMode

/** 小说浏览页一级分类：推荐与排行榜。 */
enum class NovelBrowseTab {
    Recommend,
    Ranking,
}

/**
 * 小说浏览页（推荐 / 排行榜）跨 Tab 与跨页退栈状态容器。
 *
 * 1. 各 `(Tab, RankingMode)` 独立持有 [PagedListState] 与 [GridScrollPosition]，顶部切换 Tab 或排行榜周期后切回时直接复用既有数据与滚动位置，不重复发起网络请求；
 * 2. 由 Decompose 栈项 `Config.Novel` 的 `InstanceKeeper` 持有：进入小说阅读器压栈后保活，返回不触发首屏重拉；真正退出小说浏览页出栈时随 child 销毁；
 * 3. 账号切换时通过 [resetAll] 清空所有缓存列表与滚动位置。
 */
class NovelBrowsePageState {

    private data class ScopeKey(
        val tab: NovelBrowseTab,
        val rankingMode: NovelRankingMode,
    )

    private fun scopeKey(tab: NovelBrowseTab, rankingMode: NovelRankingMode): ScopeKey =
        if (tab == NovelBrowseTab.Recommend) {
            ScopeKey(NovelBrowseTab.Recommend, NovelRankingMode.Day)
        } else {
            ScopeKey(NovelBrowseTab.Ranking, rankingMode)
        }

    var currentTab: NovelBrowseTab by mutableStateOf(NovelBrowseTab.Recommend)
    var rankingMode: NovelRankingMode by mutableStateOf(NovelRankingMode.Day)

    /** 是否已至少绑定过一次账号身份（用于区分首次加载与切号）。 */
    var hasLoadedAccount: Boolean = false
    var loadedAccount: AccountPersist? = null

    /** 手动点击刷新或重试时置 true，下一次 LaunchedEffect 消费后立即清零。 */
    var pendingForceRefresh: Boolean = false

    private val lists = mutableMapOf<ScopeKey, PagedListState<Novel>>()
    private val scrollPositions = mutableMapOf<ScopeKey, GridScrollPosition>()

    fun getOrCreateList(
        tab: NovelBrowseTab,
        rankingMode: NovelRankingMode,
        factory: () -> PagedListState<Novel>,
    ): PagedListState<Novel> = lists.getOrPut(scopeKey(tab, rankingMode), factory)

    internal fun putList(
        tab: NovelBrowseTab,
        rankingMode: NovelRankingMode,
        state: PagedListState<Novel>,
    ) {
        lists[scopeKey(tab, rankingMode)] = state
    }

    fun getScrollPosition(
        tab: NovelBrowseTab,
        rankingMode: NovelRankingMode,
    ): GridScrollPosition = scrollPositions[scopeKey(tab, rankingMode)] ?: GridScrollPosition.Zero

    fun saveScrollPosition(
        tab: NovelBrowseTab,
        rankingMode: NovelRankingMode,
        index: Int,
        offset: Int,
    ) {
        scrollPositions[scopeKey(tab, rankingMode)] = GridScrollPosition(index = index, offset = offset)
    }

    fun resetAll() {
        lists.values.forEach { it.reset() }
        lists.clear()
        scrollPositions.clear()
    }
}
