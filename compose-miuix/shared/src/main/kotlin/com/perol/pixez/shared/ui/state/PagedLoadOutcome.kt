package com.perol.pixez.shared.ui.state

/**
 * 分页请求的可判定结果（架构审查 UI-8）。
 *
 * 状态机回报本轮是否真的写入了列表，屏幕据此决定写跨页恢复快照与回顶；
 * 不再以「列表引用是否变化」推断成功——那是状态机实现细节，一旦改为原地修改
 * 列表，推断会静默失效。
 */
enum class PagedLoadOutcome {
    /** 未发起请求：首屏早退命中既有数据 / 没有下一页 / 同类请求正在途。 */
    Skipped,

    /** 结果已写入状态机（首屏替换或翻页合并）。 */
    Written,

    /** 请求失败，错误已写入 [PagedListState.initialError] 或 [PagedListState.loadMoreError]。 */
    Failed,

    /** 代际失配：期间发生了刷新或 [PagedListState.reset]，本轮结果与错误被整体丢弃。 */
    Discarded,
}
