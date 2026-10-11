package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 单页数据源结果：本页作品与下一页 URL（null 表示没有更多页）。
 */
data class IllustPagedPage(
    val illusts: List<Illust>,
    val nextUrl: String?,
)

/**
 * 插画分页加载状态机（架构审查 高-4）：首屏加载 + 触底翻页 + generation 竞态守卫 +
 * 失败重试语义，以 RankingScreen 版语义为基准单点实现，替代各 Screen 手抄的分页状态机。
 *
 * 过滤链复用 [FilterableIllustListState]（高-3，原始/过滤双列表 + 版本信号本地重过滤），
 * 本类只承担分页与竞态：数据源以 [fetch] suspend lambda 注入（nextUrl == null 拉首页），
 * 过滤闭包与版本号以 [filter] / [filterVersionProvider] 注入，每次写入列表时读取最新版本，
 * 不引入新依赖。
 */
class IllustPagedListState(
    /** 数据源：nextUrl == null 拉首页，非 null 拉指定页；失败以异常抛出（取消异常照常传播）。 */
    private val fetch: suspend (nextUrl: String?) -> IllustPagedPage,
    /** 注入的过滤闭包：实现须在每次调用时读取最新过滤设置（如 banRepository.filterIllustsBySettings）。 */
    filter: suspend (List<Illust>) -> List<Illust>,
    /** 过滤版本号提供者（如 SettingsRepository::filterChangeVersion），每次写入/重过滤时读取最新值。 */
    private val filterVersionProvider: () -> Int,
) {
    private val filterable = FilterableIllustListState(filter, filterVersionProvider())

    /** 过滤前的原始累积列表；无数据时为 null。 */
    val rawIllusts: List<Illust>? get() = filterable.rawIllusts

    /** 过滤后的展示列表。 */
    val filteredIllusts: List<Illust>? get() = filterable.filteredIllusts

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
     * 仅状态机内部守卫与测试断言消费，UI 不读，普通 var 即可（同 FilterableIllustListState.appliedFilterVersion）。
     */
    var generation: Int = 0
        private set

    /**
     * 首屏加载或强制刷新：递增 [generation] 作废所有在途翻页并复位其加载态，再拉取首页。
     * 非 force 且已有数据且无首屏错误时早退（缓存命中，不做网络请求）。
     */
    suspend fun loadFirst(force: Boolean = false) {
        val myGeneration = ++generation
        // 作废在途翻页：复位其加载态与错误（RankingScreen.kt:127-128 语义），
        // 使旧 loadMore 完成时因代数失配被整体拒绝。
        isLoadingMore = false
        loadMoreError = null
        if (!force && filteredIllusts != null && initialError == null) return
        isRefreshing = true
        if (filteredIllusts == null) {
            initialError = null
        }
        suspendRunCatchingNonCancel { fetch(null) }
            .onSuccess { page ->
                if (myGeneration == generation) {
                    filterable.replace(page.illusts, filterVersionProvider())
                    nextUrl = page.nextUrl
                    initialError = null
                    loadMoreError = null
                }
            }
            .onFailure { error ->
                if (myGeneration == generation && filteredIllusts == null) {
                    initialError = error
                }
            }
        // 复位受守卫约束：并发的新一轮刷新在途时，过期的旧调用不得复位其刷新标志
        // （Screen 内 LaunchedEffect 取消旧协程时本行不触达，行为等价于 RankingScreen.kt:134）。
        if (myGeneration == generation) {
            isRefreshing = false
        }
    }

    /**
     * 触底翻页：无下一页或在途时直接返回；成功按 id 去重追加，失败记录 [loadMoreError]，
     * generation 失配（期间发生刷新）的过期结果与错误一律丢弃，也不复位 [isLoadingMore]。
     */
    suspend fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        // 翻页捕获当前代数不递增：刷新路径递增后本任务即失配（RankingScreen.kt:157 语义）。
        val myGeneration = generation
        isLoadingMore = true
        loadMoreError = null
        suspendRunCatchingNonCancel { fetch(currentNextUrl) }
            .onSuccess { page ->
                if (myGeneration == generation) {
                    filterable.append(page.illusts, filterVersionProvider())
                    nextUrl = page.nextUrl
                }
            }
            .onFailure { error ->
                if (myGeneration == generation) {
                    loadMoreError = error
                }
            }
        if (myGeneration == generation) {
            isLoadingMore = false
        }
    }

    /**
     * 过滤版本号变化时对原始列表本地重过滤（零网络成本）。
     * @return true 表示已重过滤；无原始数据或版本未变时返回 false。
     */
    suspend fun refilterIfChanged(): Boolean =
        filterable.refilterIfChanged(filterVersionProvider())

    /**
     * 跨页恢复（高-5）：快照按 ID 经仓库 illustsCache 回查得到的原始列表恢复，
     * 并恢复下一页 URL；恢复后首轮 refilterIfChanged() 强制按当前过滤设置重过滤。
     */
    fun restore(illusts: List<Illust>, nextUrl: String?) {
        filterable.restoreRaw(illusts)
        this.nextUrl = nextUrl
    }
}
