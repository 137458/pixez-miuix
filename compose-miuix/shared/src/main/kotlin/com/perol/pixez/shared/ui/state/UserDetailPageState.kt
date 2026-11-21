package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.UserDetail

/**
 * 用户详情页（含「作品 / 收藏」Tab）的跨组合重建状态容器。
 *
 * Decompose 栈换层（压入作品详情页、或从作品详情页返回）会销毁并重建页面组合，页面内
 * remember 持有的资料、分页列表与网格滚动位置随之丢失：重建首帧退回整页加载占位，
 * 出栈收回动画的落点因此锁在重建出来的空白底图上（个人收藏页返回空白）。本容器由
 * [com.perol.pixez.shared.ui.navigation.UserDetailComponent] 经该栈项 child 的
 * InstanceKeeper 持有，生命周期即「该页在返回栈中」：被上层页面压住时保活，出栈时随
 * child 销毁。组合层只做读写，不感知保留策略。
 *
 * 仅 UI 主线程访问（组合、DisposableEffect 与 LaunchedEffect），无需加锁。
 */
class UserDetailPageState {

    /** 最近一次成功加载的用户资料；null 表示本页尚未成功加载过。 */
    var userDetail: UserDetail? = null

    /** 作品列表状态：首次组合时创建并写回，重建后复用（已有数据则 loadFirst 缓存早退）。 */
    var works: IllustPagedListState? = null

    /** 公开收藏列表状态，语义同 [works]。 */
    var publicBookmarks: IllustPagedListState? = null

    /** 私密收藏列表状态，语义同 [works]。 */
    var privateBookmarks: IllustPagedListState? = null

    /** 作品网格滚动位置快照：组合销毁时回写，重建时用作网格状态的初值。 */
    var worksScroll: GridScrollPosition = GridScrollPosition.Zero

    /** 收藏网格滚动位置快照，语义同 [worksScroll]。 */
    var bookmarksScroll: GridScrollPosition = GridScrollPosition.Zero
}

/** 交错网格滚动位置快照（首项索引 + 偏移），仅本进程内跨组合保留，不参与进程死亡恢复。 */
data class GridScrollPosition(val index: Int, val offset: Int) {
    companion object {
        val Zero: GridScrollPosition = GridScrollPosition(index = 0, offset = 0)
    }
}
