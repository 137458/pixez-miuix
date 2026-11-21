package com.perol.pixez.shared.ui.state

/**
 * 分页 footer 的状态快照（架构审查 UI-3）：把「加载中 / 失败重试 / 没有更多」
 * 三态的判定从各列表页的 when 分支收进类型，footer 组件只按 [phase] 渲染。
 *
 * 状态机通过 [PagedListState.footerState] / [IllustPagedListState.footerState] 直接给出快照；
 * 瀑布流等只拿到标志的调用方可就地组装。
 */
data class PagingFooterState(
    /** 是否还有下一页。 */
    val hasMore: Boolean,
    /** 翻页是否在途。 */
    val isLoadingMore: Boolean,
    /** 最近一次翻页失败的错误。 */
    val loadMoreError: Throwable?,
    /** 列表是否已有内容：无内容时不展示「没有更多」。 */
    val hasItems: Boolean = true,
) {
    /** 由状态快照派生的渲染态：加载中优先于上次失败，「没有更多」只在列表非空时展示。 */
    val phase: PagingFooterPhase
        get() = when {
            isLoadingMore -> PagingFooterPhase.Loading
            loadMoreError != null -> PagingFooterPhase.Retry
            !hasMore && hasItems -> PagingFooterPhase.NoMore
            else -> PagingFooterPhase.Hidden
        }
}

/** footer 渲染态：三态 + 不渲染。 */
enum class PagingFooterPhase {
    /** 仍可翻页且不在加载/出错：不发射任何布局节点。 */
    Hidden,

    /** 正在加载下一页。 */
    Loading,

    /** 翻页失败，提供重试入口。 */
    Retry,

    /** 没有更多页。 */
    NoMore,
}
