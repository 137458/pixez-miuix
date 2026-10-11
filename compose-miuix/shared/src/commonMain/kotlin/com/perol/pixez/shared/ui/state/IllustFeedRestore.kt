package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import kotlinx.serialization.Serializable

/**
 * 跨页恢复快照（架构审查 高-5）：只存作品 ID 列表与下一页 URL 的可序列化轻量快照，
 * 不存 UI 过滤后的派生态；恢复时经仓库 [com.perol.pixez.shared.data.repository.IllustRepository.getCachedIllust]
 * 按 ID 回查原始数据。
 */
@Serializable
data class IllustFeedSnapshot(
    val illustIds: List<Long>,
    val nextUrl: String? = null,
) {
    companion object {
        /** 以原始列表构建快照；空列表同样是有效快照（保持「结果为空」的恢复语义）。 */
        fun of(illusts: List<Illust>, nextUrl: String?): IllustFeedSnapshot =
            IllustFeedSnapshot(illustIds = illusts.map { it.id }, nextUrl = nextUrl)
    }
}

/**
 * Main child 的跨页恢复容器（高-5）：推荐页单快照与排行榜多 key LRU 的聚合持有者，
 * 由 [com.perol.pixez.shared.ui.navigation.createMainFeedRestore] 在 Main child 组件
 * 初始化时接线（consume/register 各执行一次，不随组合重建重复执行）。
 */
class MainFeedRestore(
    val hello: IllustFeedRestoreCache,
    val ranking: RankingFeedRestoreCache,
)

/**
 * 推荐页跨页恢复缓存（高-5）：承接自仓库 ActiveFeed 的恢复态。实例随 Main child 的
 * InstanceKeeper 存活——Main 为栈底 child，正常会话期间与进程同生命周期，仅在登录成功
 * 栈重置（RootComponent.onLoginSuccess）或进程死亡时随 child 释放；stateKeeper 序列化
 * 快照仅用于进程死亡恢复。相比仓库版不再滞留「过期过滤结果」：快照只存原始 ID，
 * 恢复后由 restoreRaw 强制按当前设置重过滤，账号切换时另有 clearMemoryCache 兜底。
 */
class IllustFeedRestoreCache(initial: IllustFeedSnapshot? = null) {
    var snapshot: IllustFeedSnapshot? = initial
        private set

    /** 记录恢复快照（原始列表的 ID 集合与下一页 URL）。 */
    fun update(rawIllusts: List<Illust>, nextUrl: String?) {
        snapshot = IllustFeedSnapshot.of(rawIllusts, nextUrl)
    }
}

/** 排行榜跨页恢复快照集合（可序列化，供 stateKeeper 进程死亡恢复）。 */
@Serializable
data class RankingFeedSnapshot(
    val pages: Map<String, IllustFeedSnapshot> = emptyMap(),
)

/**
 * 排行榜跨页恢复缓存（高-5）：按 mode_date 多 key 快照 + 写入序 LRU 淘汰，
 * 容量与顺位语义自仓库 activeRankingPages（RANKING_CACHE_MAX_KEYS=8）原样迁移。
 * 生命周期同 [IllustFeedRestoreCache]（Main child 的 InstanceKeeper）。
 * 仅 UI 主线程访问（Screen 组合与 LaunchedEffect/loadMore 协程），无需加锁。
 */
class RankingFeedRestoreCache(initialPages: Map<String, IllustFeedSnapshot> = emptyMap()) {

    private companion object {
        /** 排行跨页缓存的 key 容量上限，超出按 LRU 淘汰最久未写的 key，防内存无界增长。 */
        const val MAX_KEYS = 8
    }

    private val pages = LinkedHashMap<String, IllustFeedSnapshot>()

    init {
        for ((key, value) in initialPages) {
            pages[key] = value
        }
        while (pages.size > MAX_KEYS) {
            pages.remove(pages.keys.first())
        }
    }

    /** 读取指定 key 的快照，未记录时为 null；纯读，不改变 LRU 顺位（与仓库版语义一致）。 */
    fun get(key: String): IllustFeedSnapshot? = pages[key]

    /**
     * 记录指定 key 的快照；同 key 重写视为最近使用（提升到最新），
     * 超过 [MAX_KEYS] 个 key 时淘汰最久未写的 key。
     */
    fun update(key: String, rawIllusts: List<Illust>, nextUrl: String?) {
        pages.remove(key)
        pages[key] = IllustFeedSnapshot.of(rawIllusts, nextUrl)
        while (pages.size > MAX_KEYS) {
            pages.remove(pages.keys.first())
        }
    }

    /** 全量快照（stateKeeper 序列化用），保持 LRU 写入序。 */
    fun snapshot(): Map<String, IllustFeedSnapshot> = pages.toMap()
}
