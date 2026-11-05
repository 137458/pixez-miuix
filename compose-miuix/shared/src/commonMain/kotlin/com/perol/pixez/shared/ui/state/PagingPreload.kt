package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.AppConstants

/**
 * 触底预加载判定（架构审查 UI-2）：把「何时该发起下一页请求」的四条件守卫
 * （有数据 / 还有下一页 / 未在途 / 无翻页错误）连同末项索引比较收敛到单点实现。
 *
 * 持有分页状态机的调用方走 [PagedListState.shouldPreloadMore] /
 * [IllustPagedListState.shouldPreloadMore]；瀑布流等只拿到列表与标志的调用方
 * 直接按自有条目计数调用本函数。
 *
 * @param itemCount 当前已加载条目数；0 表示没有可翻页的内容。
 * @param hasMore 是否还有下一页。
 * @param isLoadingMore 翻页是否在途。
 * @param hasLoadMoreError 最近一次翻页是否失败（失败后等待用户重试，不自动续拉）。
 * @param lastVisibleItemIndex 布局上报的最后一个可见项下标；未知时传 -1。
 * @param thresholdItems 距末尾不足该条目数即触发预加载（列表 4 / 瀑布流 6）。
 */
internal fun shouldPreloadMore(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
    lastVisibleItemIndex: Int,
    thresholdItems: Int = AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS,
): Boolean = itemCount > 0 &&
    hasMore &&
    !isLoadingMore &&
    !hasLoadMoreError &&
    lastVisibleItemIndex >= itemCount - thresholdItems
