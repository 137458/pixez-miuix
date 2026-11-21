package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.perol.pixez.shared.ui.state.UserDetailPageState

/** 用户详情页保留状态在该 child InstanceKeeper 作用域内的 key。 */
internal const val KEY_USER_DETAIL_PAGE_STATE = "key_user_detail_page_state"

/**
 * 用户详情页子组件（中-6b 子 Component 范式的扩展）：childStack 的 child 实例由
 * [Config.UserDetail] 换为本类，承载路由参数与跨组合重建的页面状态 [pageState]。
 *
 * 与 [IllustDetailComponent] 的差别只在状态归属：本页的资料与分页列表属于「组合重建即空白」
 * 的高成本数据，故经 [ComponentContext.instanceKeeper] 持有 [UserDetailPageState]。实例随
 * 该栈项 child 存活——被作品详情页压住时保活，出栈时随 child 销毁，不会残留上一个用户的
 * 数据（登录/切号会重置返回栈，同样带动销毁）。组合层经参数读取同一容器，栈换层重建后
 * 首帧即可复用。
 *
 * 数据仓储与跳转回调仍由 Screen 侧注入（中-9 单通道），本组件不感知导航实现。
 */
class UserDetailComponent(
    componentContext: ComponentContext,
    val config: Config.UserDetail,
) : ComponentContext by componentContext {

    /** 页面保留状态；child 出栈销毁时随之释放。 */
    val pageState: UserDetailPageState =
        instanceKeeper.getOrCreateSimple(KEY_USER_DETAIL_PAGE_STATE) { UserDetailPageState() }

    /** 本页展示的用户 ID（[Config.UserDetail.userId] 的直读别名）。 */
    val userId: Long get() = config.userId
}
