package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.ComponentContext

/**
 * 作品详情页子组件（中-6b 试点）：Decompose 惯例的二级页 Component，childStack 的
 * child 实例由 [Config] 换为本类实例，承载该页的路由参数（[config]）与全部出页跳转回调。
 *
 * - 路由参数不可变：[illustId] 来自入栈的 [Config.IllustDetail]；进程死亡恢复时
 *   stateKeeper 反序列化配置后由 childFactory 以恢复值重建本组件。
 * - 跳转回调由 [RootComponent.createChild] 注入根导航方法引用，组件自身不感知栈实现，
 *   可脱离 RootComponent 独立实例化测试（desktopTest 有覆盖）。
 * - 数据仓储仍由渲染层从 [RepositoryRegistry] 取值传入 Screen，页面内状态仍在组合层持有；
 *   后续新页面按本范式接入，旧页面维持 Config 即 child 实例的现状逐步迁移。
 */
class IllustDetailComponent(
    componentContext: ComponentContext,
    val config: Config.IllustDetail,
    val onBack: () -> Boolean,
    val onUserClick: (Long) -> Unit,
    val onCommentsClick: (Long) -> Unit,
    val onRelatedIllustsClick: (Long) -> Unit,
    val onIllustSeriesClick: (Long) -> Unit,
    val onTagClick: (String) -> Unit,
    val onIllustClick: (Long) -> Unit,
    val onNovelClick: (Long) -> Unit,
) : ComponentContext by componentContext {

    /** 本页展示的作品 ID（[Config.IllustDetail.illustId] 的直读别名）。 */
    val illustId: Long get() = config.illustId
}
