package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.AppConstants

/**
 * 触底预加载判定（架构审查 UI-2）：把「何时该发起下一页请求」的五条件守卫
 * （有数据 / 还有下一页 / 未在途 / 无翻页错误 / 未在刷新）连同末项索引比较收敛到单点实现。
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
 * @param isRefreshing 首屏/强制刷新是否在途（R-01）：刷新未落盘前列表仍是刷新前的游标页，
 *   此时续拉会把旧游标页并入刷新后的列表，故刷新在途一律不预加载。无默认值：每个调用点
 *   必须显式给出自己的刷新态，避免沉默退化成「刷新在途仍预加载」。
 */
internal fun shouldPreloadMore(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
    lastVisibleItemIndex: Int,
    thresholdItems: Int = AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS,
    isRefreshing: Boolean,
): Boolean = itemCount > 0 &&
    hasMore &&
    !isLoadingMore &&
    !hasLoadMoreError &&
    !isRefreshing &&
    lastVisibleItemIndex >= itemCount - thresholdItems
