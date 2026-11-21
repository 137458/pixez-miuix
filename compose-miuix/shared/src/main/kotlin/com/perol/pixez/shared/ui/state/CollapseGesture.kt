package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.AppConstants

/** 折叠动作：本次滑动是否改变标题栏折叠态。 */
enum class CollapseAction {
    /** 折叠态不变。 */
    None,

    /** 折叠大标题与次级 header。 */
    Collapse,

    /** 展开大标题与次级 header。 */
    Expand,
}

/**
 * 折叠手势一步决策结果：新的标题栏高度偏移、毛玻璃内容偏移与折叠动作。
 */
data class CollapseScrollStep(
    val heightOffset: Float,
    val contentOffset: Float,
    val action: CollapseAction,
)

/**
 * 折叠手势决策（架构审查 UI-7）：搜索页大标题/搜索框协同折叠与详情页大图折叠进度
 * 共用一组纯函数，阈值集中在 [AppConstants.Layout]。
 *
 * 时序规则（两页同款）：
 * 1. 上滑超过折叠阈值才折叠；下拉本身不展开，避免在列表中间浏览时误展开。
 * 2. 只有列表触顶过冲（可用位移溢出到外层）超过展开阈值才展开。
 */
internal object CollapseGesture {

    /**
     * 上滑阶段（NestedScrollConnection.onPreScroll）：先累加内容偏移驱动毛玻璃淡入，
     * 位移超过 [collapseThresholdPx] 才按高度限制协同折叠。
     */
    fun resolvePreScroll(
        delta: Float,
        heightOffset: Float,
        contentOffset: Float,
        heightOffsetLimit: Float,
        isCollapsed: Boolean,
        collapseThresholdPx: Float = AppConstants.Layout.COLLAPSE_SCROLL_THRESHOLD_PX,
    ): CollapseScrollStep {
        if (delta >= 0f) return CollapseScrollStep(heightOffset, contentOffset, CollapseAction.None)
        val nextContentOffset = contentOffset + delta
        if (delta >= -collapseThresholdPx) {
            return CollapseScrollStep(heightOffset, nextContentOffset, CollapseAction.None)
        }
        return CollapseScrollStep(
            heightOffset = applyHeightOffset(heightOffset, delta, heightOffsetLimit),
            contentOffset = nextContentOffset,
            action = if (isCollapsed) CollapseAction.None else CollapseAction.Collapse,
        )
    }

    /**
     * 回拉阶段（NestedScrollConnection.onPostScroll）：
     * [consumed] 为列表自身消费的下拉位移，[availableOverscroll] 为列表触顶后溢出的过冲位移
     * （列表中间下拉时恒为 0，故不展开）。
     */
    fun resolvePostScroll(
        consumed: Float,
        availableOverscroll: Float,
        heightOffset: Float,
        contentOffset: Float,
        heightOffsetLimit: Float,
        isCollapsed: Boolean,
        expandThresholdPx: Float = AppConstants.Layout.EXPAND_OVERSCROLL_THRESHOLD_PX,
    ): CollapseScrollStep {
        var nextContentOffset = contentOffset
        if (consumed > 0f) nextContentOffset = (nextContentOffset + consumed).coerceAtMost(0f)
        // 列表中间下拉：位移被列表自身消费，没有过冲，故不展开。
        if (availableOverscroll <= 0f) {
            return CollapseScrollStep(heightOffset, nextContentOffset, CollapseAction.None)
        }
        nextContentOffset = (nextContentOffset + availableOverscroll).coerceAtMost(0f)
        if (availableOverscroll <= expandThresholdPx) {
            return CollapseScrollStep(heightOffset, nextContentOffset, CollapseAction.None)
        }
        return CollapseScrollStep(
            heightOffset = applyHeightOffset(heightOffset, availableOverscroll, heightOffsetLimit),
            contentOffset = nextContentOffset,
            action = if (isCollapsed) CollapseAction.Expand else CollapseAction.None,
        )
    }

    /** 详情页折叠进度累计位移：仅在 [0, thresholdPx] 内累计，上滑累加、下拉回退。 */
    fun accumulateCollapseOffset(
        delta: Float,
        currentOffset: Float,
        thresholdPx: Float,
    ): Float = (currentOffset - delta).coerceIn(0f, thresholdPx)

    /** 详情页折叠进度：首项已移出视口即视为完全折叠，否则按累计位移比例。 */
    fun resolveCollapseProgress(
        firstVisibleItemIndex: Int,
        scrollOffset: Float,
        thresholdPx: Float,
    ): Float =
        if (firstVisibleItemIndex > 0) 1f else (scrollOffset / thresholdPx).coerceIn(0f, 1f)

    /** 高度偏移按上限收敛：无折叠空间（limit >= 0）时原样保留。 */
    private fun applyHeightOffset(heightOffset: Float, delta: Float, heightOffsetLimit: Float): Float =
        if (heightOffsetLimit < 0f) {
            (heightOffset + delta).coerceIn(heightOffsetLimit, 0f)
        } else {
            heightOffset
        }
}
