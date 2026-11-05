package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.AppConstants
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 架构审查 UI-7：折叠手势决策（搜索页大标题与详情页折叠进度同款时序）。
 *
 * 两条硬规则：
 * 1. 列表中间下拉不展开——只有触顶过冲才展开；
 * 2. 上滑超过折叠阈值才折叠，回顶过冲超过展开阈值才展开，阈值边界严格取「超过」。
 */
class CollapseGestureTest {

    private val collapseThreshold = AppConstants.Layout.COLLAPSE_SCROLL_THRESHOLD_PX
    private val expandThreshold = AppConstants.Layout.EXPAND_OVERSCROLL_THRESHOLD_PX

    // ---- 上滑折叠 ----

    @Test
    fun smallUpScrollOnlyMovesContentOffset() {
        val step = CollapseGesture.resolvePreScroll(
            delta = -3f,
            heightOffset = 0f,
            contentOffset = 0f,
            heightOffsetLimit = -120f,
            isCollapsed = false,
        )
        assertEquals(CollapseAction.None, step.action)
        assertEquals(0f, step.heightOffset)
        assertEquals(-3f, step.contentOffset)
    }

    @Test
    fun upScrollBeyondThresholdCollapsesAndAccumulatesHeightOffset() {
        val step = CollapseGesture.resolvePreScroll(
            delta = -30f,
            heightOffset = -20f,
            contentOffset = -10f,
            heightOffsetLimit = -120f,
            isCollapsed = false,
        )
        assertEquals(CollapseAction.Collapse, step.action)
        assertEquals(-50f, step.heightOffset)
        assertEquals(-40f, step.contentOffset)
    }

    @Test
    fun collapseThresholdBoundaryIsStrictlyGreater() {
        // 恰好等于阈值不折叠；刚超过才折叠
        assertEquals(
            CollapseAction.None,
            CollapseGesture.resolvePreScroll(
                delta = -collapseThreshold,
                heightOffset = 0f,
                contentOffset = 0f,
                heightOffsetLimit = -120f,
                isCollapsed = false,
            ).action,
        )
        assertEquals(
            CollapseAction.Collapse,
            CollapseGesture.resolvePreScroll(
                delta = -(collapseThreshold + 0.5f),
                heightOffset = 0f,
                contentOffset = 0f,
                heightOffsetLimit = -120f,
                isCollapsed = false,
            ).action,
        )
    }

    @Test
    fun alreadyCollapsedReportsNoActionButStillAccumulates() {
        val step = CollapseGesture.resolvePreScroll(
            delta = -40f,
            heightOffset = -100f,
            contentOffset = -5f,
            heightOffsetLimit = -120f,
            isCollapsed = true,
        )
        assertEquals(CollapseAction.None, step.action)
        // 累计位移被上限收敛：-100 + (-40) = -140 收敛到 -120
        assertEquals(-120f, step.heightOffset)
        assertEquals(-45f, step.contentOffset)
    }

    @Test
    fun heightOffsetIsClampedToLimitAndKeptWhenNoLimit() {
        assertEquals(
            -120f,
            CollapseGesture.resolvePreScroll(
                delta = -80f,
                heightOffset = -100f,
                contentOffset = 0f,
                heightOffsetLimit = -120f,
                isCollapsed = true,
            ).heightOffset,
        )
        // 高度限制为 0（无折叠空间）时偏移原样保留
        assertEquals(
            -10f,
            CollapseGesture.resolvePreScroll(
                delta = -80f,
                heightOffset = -10f,
                contentOffset = 0f,
                heightOffsetLimit = 0f,
                isCollapsed = true,
            ).heightOffset,
        )
    }

    // ---- 回拉展开 ----

    @Test
    fun pullingDownInTheMiddleOfListNeverExpands() {
        // 列表自身消费了下拉位移、没有过冲：折叠态与高度偏移都不动，只有毛玻璃内容偏移回零
        val step = CollapseGesture.resolvePostScroll(
            consumed = 120f,
            availableOverscroll = 0f,
            heightOffset = -120f,
            contentOffset = -200f,
            heightOffsetLimit = -120f,
            isCollapsed = true,
        )
        assertEquals(CollapseAction.None, step.action)
        assertEquals(-120f, step.heightOffset)
        // 下拉 120 只把毛玻璃内容偏移从 -200 收敛到 -80，仍未触顶
        assertEquals(-80f, step.contentOffset)
    }

    @Test
    fun topOverscrollExpandsCollapsedHeader() {
        val step = CollapseGesture.resolvePostScroll(
            consumed = 0f,
            availableOverscroll = 40f,
            heightOffset = -120f,
            contentOffset = -30f,
            heightOffsetLimit = -120f,
            isCollapsed = true,
        )
        assertEquals(CollapseAction.Expand, step.action)
        assertEquals(-80f, step.heightOffset)
        assertEquals(0f, step.contentOffset)
    }

    @Test
    fun expandThresholdBoundaryIsStrictlyGreater() {
        assertEquals(
            CollapseAction.None,
            CollapseGesture.resolvePostScroll(
                consumed = 0f,
                availableOverscroll = expandThreshold,
                heightOffset = -120f,
                contentOffset = 0f,
                heightOffsetLimit = -120f,
                isCollapsed = true,
            ).action,
        )
        assertEquals(
            CollapseAction.Expand,
            CollapseGesture.resolvePostScroll(
                consumed = 0f,
                availableOverscroll = expandThreshold + 0.5f,
                heightOffset = -120f,
                contentOffset = 0f,
                heightOffsetLimit = -120f,
                isCollapsed = true,
            ).action,
        )
    }

    @Test
    fun expandedHeaderStaysExpanded() {
        val step = CollapseGesture.resolvePostScroll(
            consumed = 0f,
            availableOverscroll = 60f,
            heightOffset = 0f,
            contentOffset = 0f,
            heightOffsetLimit = -120f,
            isCollapsed = false,
        )
        assertEquals(CollapseAction.None, step.action)
        assertEquals(0f, step.heightOffset)
    }

    // ---- 详情页折叠进度 ----

    @Test
    fun detailCollapseOffsetAccumulatesAndClamps() {
        val thresholdPx = 72f
        assertEquals(72f, CollapseGesture.accumulateCollapseOffset(delta = -100f, currentOffset = 0f, thresholdPx = thresholdPx))
        assertEquals(42f, CollapseGesture.accumulateCollapseOffset(delta = 30f, currentOffset = 72f, thresholdPx = thresholdPx))
        assertEquals(0f, CollapseGesture.accumulateCollapseOffset(delta = 200f, currentOffset = 42f, thresholdPx = thresholdPx))
    }

    @Test
    fun detailCollapseProgressFollowsFirstVisibleItem() {
        val thresholdPx = 72f
        assertEquals(1f, CollapseGesture.resolveCollapseProgress(firstVisibleItemIndex = 1, scrollOffset = 0f, thresholdPx = thresholdPx))
        assertEquals(0f, CollapseGesture.resolveCollapseProgress(firstVisibleItemIndex = 0, scrollOffset = 0f, thresholdPx = thresholdPx))
        assertEquals(
            0.5f,
            CollapseGesture.resolveCollapseProgress(firstVisibleItemIndex = 0, scrollOffset = 36f, thresholdPx = thresholdPx),
        )
        assertEquals(
            1f,
            CollapseGesture.resolveCollapseProgress(firstVisibleItemIndex = 0, scrollOffset = 100f, thresholdPx = thresholdPx),
        )
    }
}
