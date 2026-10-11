package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.appendDistinct

/**
 * 「原始列表 / 过滤后列表」双列表状态（架构审查 高-3）：
 * 过滤是展示期派生——原始数据保留在 [rawIllusts]，过滤版本号（SettingsRepository.filterChangeVersion）
 * 变化时对原始列表本地重跑过滤（[refilterIfChanged]），零网络成本，屏蔽与过滤开关即时生效。
 *
 * 仅承担过滤链，不承担分页/竞态状态（分页状态机属后续批次 IllustPagedListState）。
 */
class FilterableIllustListState(
    /** 注入的过滤闭包：实现须在每次调用时读取最新过滤设置（如 banRepository.filterIllusts + 当前开关）。 */
    private val filter: suspend (List<Illust>) -> List<Illust>,
    /** 构造时的过滤版本号基线，作为首个版本变化比较基准。 */
    initialFilterVersion: Int,
) {
    /** 过滤前的原始累积列表；无数据（未加载且无跨页快照）时为 null。 */
    var rawIllusts by mutableStateOf<List<Illust>?>(null)
        private set

    /** 过滤后的展示列表。 */
    var filteredIllusts by mutableStateOf<List<Illust>?>(null)
        private set

    /** 当前展示列表所依据的过滤版本号。 */
    var appliedFilterVersion: Int = initialFilterVersion
        private set

    /** 全量替换原始列表（首屏加载/下拉刷新成功），按 [filterVersion] 过滤。 */
    suspend fun replace(raw: List<Illust>, filterVersion: Int) {
        rawIllusts = raw
        appliedFilterVersion = filterVersion
        filteredIllusts = filter(raw)
    }

    /** 追加下一页原始数据（触底翻页成功），按 id 去重合并后按 [filterVersion] 过滤。 */
    suspend fun append(rawPage: List<Illust>, filterVersion: Int) {
        replace(rawIllusts.orEmpty().appendDistinct(rawPage), filterVersion)
    }

    /** 跨页恢复：以仓库快照（按旧设置过滤）同时作为原始与展示列表，待版本信号或刷新后重过滤。 */
    fun restore(snapshot: List<Illust>) {
        rawIllusts = snapshot
        filteredIllusts = snapshot
    }

    /**
     * 过滤版本号变化时对原始列表本地重过滤（零网络成本）。
     * @return true 表示已重过滤；无原始数据或版本未变时返回 false（调用方可安全早退）。
     */
    suspend fun refilterIfChanged(filterVersion: Int): Boolean {
        val raw = rawIllusts ?: return false
        if (filterVersion == appliedFilterVersion) return false
        appliedFilterVersion = filterVersion
        filteredIllusts = filter(raw)
        return true
    }
}
