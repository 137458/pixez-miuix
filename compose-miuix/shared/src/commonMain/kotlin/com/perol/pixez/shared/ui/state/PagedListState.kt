package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 单页数据源结果：本页条目与下一页 URL（null 表示没有更多页）。
 */
data class PagedListPage<T>(
    val items: List<T>,
    val nextUrl: String?,
)

/**
 * 通用分页加载状态机：首屏加载 + 触底翻页 + generation 竞态守卫 + 失败重试语义，
 * 以 [IllustPagedListState] 为语义基准，替代各 Screen（用户/评论/小说等）手抄的分页状态机。
 *
 * 数据源以 [fetch] suspend lambda 注入（nextUrl == null 拉首页），
 * 翻页合并策略以 [mergeItems] 注入（如各模型的 appendDistinct 去重合并），不引入新依赖。
 */
class PagedListState<T>(
    /** 数据源：nextUrl == null 拉首页，非 null 拉指定页；失败以异常抛出（取消异常照常传播）。 */
    private val fetch: suspend (nextUrl: String?) -> PagedListPage<T>,
    /** 翻页合并策略：旧累积列表 + 新页 → 新累积列表。 */
    private val mergeItems: (old: List<T>, new: List<T>) -> List<T> = { old, new -> old + new },
) {
    /** 累积条目列表；未加载时为 null。快照状态：驱动 Loading/Empty/列表切换。 */
    var items by mutableStateOf<List<T>?>(null)
        private set

    /** 下一页 URL；null 表示没有更多页。快照状态：UI 组合期直读（如 hasMore = nextUrl != null）。 */
    var nextUrl by mutableStateOf<String?>(null)
        private set

    /** 首屏失败且无任何数据时的错误（保留旧列表的失败不写入）。快照状态：驱动 Loading/Error 占位切换。 */
    var initialError by mutableStateOf<Throwable?>(null)
        private set

    /** 首屏/强制刷新进行中。快照状态：驱动下拉刷新指示。 */
    var isRefreshing by mutableStateOf(false)
        private set

    /** 触底翻页进行中。快照状态：驱动列表尾部加载指示。 */
    var isLoadingMore by mutableStateOf(false)
        private set

    /** 最近一次翻页失败的错误（generation 失配的过期错误不写入）。快照状态：驱动触底重试入口。 */
    var loadMoreError by mutableStateOf<Throwable?>(null)
        private set

    /**
     * 竞态守卫代数：首屏/刷新递增，翻页捕获当前值，失配的在途结果一律丢弃。
     * 仅状态机内部守卫与测试断言消费，UI 不读，普通 var 即可（同 IllustPagedListState.generation）。
     */
    var generation: Int = 0
        private set

    /**
     * 首屏加载或强制刷新：递增 [generation] 作废所有在途翻页并复位其加载态，再拉取首页。
     * 已有数据且无首屏错误时默认早退，避免重复请求覆盖既有列表（force = true 跳过早退）。
     */
    suspend fun loadFirst(force: Boolean = false) {
        val myGeneration = ++generation
        isLoadingMore = false
        loadMoreError = null
        if (!force && items != null && initialError == null) return
        isRefreshing = true
        if (items == null) initialError = null
        suspendRunCatchingNonCancel { fetch(null) }
            .onSuccess { page ->
                if (myGeneration == generation) {
                    items = page.items
                    nextUrl = page.nextUrl
                    initialError = null
                    loadMoreError = null
                }
            }
            .onFailure { error ->
                // 已有数据时刷新失败不得清空/报错占位，旧列表原样保留
                if (myGeneration == generation && items == null) initialError = error
            }
        if (myGeneration == generation) isRefreshing = false
    }

    /**
     * 触底翻页：捕获当前 [generation]，拉取下一页并以 [mergeItems] 合并进累积列表。
     * 在途检查与加载标志写入同点完成（首个挂起点之前），杜绝双请求竞态；
     * 完成时的标志复位以 generation 守卫，过期翻页不得清除新一代的在途标志。
     */
    suspend fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        val myGeneration = generation
        isLoadingMore = true
        loadMoreError = null
        suspendRunCatchingNonCancel { fetch(currentNextUrl) }
            .onSuccess { page ->
                if (myGeneration == generation) {
                    items = mergeItems(items.orEmpty(), page.items)
                    nextUrl = page.nextUrl
                    loadMoreError = null
                }
            }
            .onFailure { error ->
                if (myGeneration == generation) loadMoreError = error
            }
        if (myGeneration == generation) isLoadingMore = false
    }

    /**
     * 作废当前列表并复位全部分页状态（登录/账号切换场景）：清列表与 nextUrl/错误/加载标志，
     * 并递增 [generation] 使在途 loadFirst/loadMore 的结果写入与标志复位全部失配丢弃。
     * 后续 loadFirst() 因 items == null 自然重拉首屏。
     */
    fun reset() {
        generation++
        items = null
        nextUrl = null
        initialError = null
        isRefreshing = false
        isLoadingMore = false
        loadMoreError = null
    }
}
