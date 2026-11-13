package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Rect
import com.perol.pixez.shared.platform.isRtlLanguage
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.perol.pixez.shared.platform.rememberScreenCornerRadius
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.theme.LocalAppExtraColors
import com.perol.pixez.shared.ui.theme.darkAppExtraColors
import com.perol.pixez.shared.ui.theme.lightAppExtraColors
import com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry
import com.perol.pixez.shared.ui.navigation.animation.NavTransitionLog
import com.perol.pixez.shared.ui.navigation.animation.PageContainerGeometry
import com.perol.pixez.shared.ui.navigation.animation.SharedBoundsRegistry
import com.perol.pixez.shared.ui.navigation.animation.miuixCardExpandPredictiveBackAnimatable
import com.perol.pixez.shared.ui.navigation.animation.miuixSeekableStackAnimation
import com.perol.pixez.shared.ui.navigation.animation.miuixPredictiveBackAnimation

import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.ProvideRepositories
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.screens.DEFAULT_SEED_COLOR
import androidx.compose.runtime.CompositionLocalProvider
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.components.AuthExpiredDialog
import com.perol.pixez.shared.ui.components.ClipboardOAuthDialog
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.utils.openSafeUrl
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.theme.AppThemeSettings
import com.perol.pixez.shared.ui.theme.resolveAppThemeSpec
import com.perol.pixez.shared.ui.theme.toThemeController
import com.perol.pixez.shared.ui.skin.CouiMainNavigationBar
import com.perol.pixez.shared.ui.skin.CouiSkinHost
import com.perol.pixez.shared.ui.skin.LocalUiSkin
import com.perol.pixez.shared.ui.skin.SkinNavItem
import com.perol.pixez.shared.ui.skin.UiSkin
import com.perol.pixez.shared.ui.skin.isCouiSkinSupported

/**
 * 根 UI：在 Decompose 页面栈外层包裹主题，并在一级页面底部显示导航栏。
 *
 * 本函数只负责：主题/语言等状态声明、更新检查副作用、Scaffold 容器骨架，
 * 以及对 [Config] 的分发编排（具体页面渲染见同包 RootChildScreens.kt 中的 render* 函数）。
 */
@OptIn(com.arkivanov.decompose.ExperimentalDecomposeApi::class)
@Composable
fun RootContent(
    component: RootComponent,
    registry: RepositoryRegistry,
    modifier: Modifier = Modifier,
) {
    val settingsRepository = registry.settingsRepository
    // 主题状态：每次重组直接从 SettingsRepository 读取当前值，
    // 监听 changeVersion 响应式自增，确保 ThemeSettingScreen 修改后即时全局重绘。
    val changeVersion = settingsRepository.changeVersion.collectAsState().value
    val themeSettings = AppThemeSettings(
        themeMode = settingsRepository.themeMode,
        isAmoled = settingsRepository.isAmoled,
        useDynamicColor = settingsRepository.useDynamicColor,
        seedColor = settingsRepository.seedColor ?: DEFAULT_SEED_COLOR,
        paletteStyleIndex = settingsRepository.miuixPaletteStyle,
        useSpec2025 = settingsRepository.miuixUseSpec2025,
    )
    val systemInDarkTheme = isSystemInDarkTheme()

    // 主题策略为纯函数（nav10）：本函数只负责读快照、装配并下发控制器，映射规则见 ui/theme。
    // 进程重建后上述状态会由 SettingsRepository 恢复（M4）。
    val appThemeSpec = remember(themeSettings, systemInDarkTheme, changeVersion) {
        resolveAppThemeSpec(themeSettings, systemInDarkTheme)
    }
    val themeController = remember(appThemeSpec, changeVersion) {
        appThemeSpec.toThemeController()
    }

    // 皮肤：COUI 仅 Android 提供；桌面端即便读到 COLOROS 也强制回退 HyperOS。
    val uiSkin = remember(changeVersion, isCouiSkinSupported) {
        if (isCouiSkinSupported) {
            UiSkin.fromIndex(settingsRepository.uiSkinIndex)
        } else {
            UiSkin.HYPEROS
        }
    }

    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance

    val floatingBackdrop = rememberBlurBackdrop()
    // 作品卡片几何信息源：列表卡片登记矩形，二级页面转场据此播放「卡片展开/收回」动画。
    val sharedBounds = remember { SharedBoundsRegistry() }
    // SideEffect：成功应用后再同步路由锚点；被丢弃的重组不会污染不可回滚的注册表字段
    SideEffect {
        NavTransitionLog.d("root") { "syncRoute ${(stack.active.configuration as? Config.IllustDetail)?.illustId}" }
        sharedBounds.syncActiveRouteIllustId((stack.active.configuration as? Config.IllustDetail)?.illustId)
    }
    val bottomBarVisible = remember { mutableStateOf(true) }
    val currentLanguageNum = settingsRepository.languageNum
    val strings = remember(currentLanguageNum, changeVersion) {
        AppStrings.fromLanguageNum(currentLanguageNum)
    }

    var appReleaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showAppUpdateDialog by remember { mutableStateOf(false) }
    var showAuthExpiredDialog by remember { mutableStateOf(false) }
    // 全局 Toast 宿主：唯一实例挂在根层，页面与组件通过 LocalToastSink 投递提示。
    var rootToastMessage by remember { mutableStateOf<ToastData?>(null) }
    val rootToastSink: (ToastData) -> Unit = remember { { rootToastMessage = it } }

    // 凭证失效提示：先以 UNDISPATCHED 挂上收集器，再触发启动自检，
    // 避免「账号凭证已被清理」的自检事件在订阅建立前发出而丢失。
    LaunchedEffect(Unit) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            registry.accountRepository.authExpiredFlow.collect {
                showAuthExpiredDialog = true
            }
        }
        registry.accountRepository.refreshSessionState()
    }

    LaunchedEffect(Unit) {
        if (settingsRepository.autoCheckUpdate) {
            registry.updateRepository.fetchLatestReleaseInfo().onSuccess { info ->
                if (info.isNew && info.versionName != settingsRepository.ignoreUpdateVersion) {
                    appReleaseInfo = info
                    showAppUpdateDialog = true
                }
            }
        }
    }

    // I-26：明暗判定由 themeMode 解析（与 resolveAppThemeSpec 的 ColorSchemeMode 同源，AMOLED/动态色不影响）
    val appExtraColors = if (appThemeSpec.resolvedDark) darkAppExtraColors else lightAppExtraColors

    MiuixTheme(controller = themeController) {
        // 皮肤主题宿主：ColorOS 时由 androidMain 叠加 COUI 配色与排版，其余平台/皮肤直接透传。
        CouiSkinHost(enabled = uiSkin == UiSkin.COLOROS) {
        // 仓储通道统一由 RepositoryRegistry 展开（nav3/DN14）：组合根不再逐条 provides 仓储 Local，
        // 新增仓储只需改 AppDependencies 字段 / Registry 字段 / ui.LocalRepositories.kt 的 provide 一行。
        ProvideRepositories(registry) {
            CompositionLocalProvider(
                LocalUiSkin provides uiSkin,
                LocalBottomBarVisibility provides bottomBarVisible,
                LocalToastSink provides rootToastSink,
                LocalStrings provides strings,
                LocalLayoutDirection provides (if (isRtlLanguage(currentLanguageNum)) LayoutDirection.Rtl else LayoutDirection.Ltr),
                LocalSharedBoundsRegistry provides sharedBounds,
                LocalAppExtraColors provides appExtraColors,
                LocalTabGesturePosition provides remember { mutableStateOf<Float?>(null) },
            ) {
                val updateInfo = appReleaseInfo
                if (showAppUpdateDialog && updateInfo != null) {
                    UpdateDialog(
                        show = showAppUpdateDialog,
                        releaseInfo = updateInfo,
                        onDismiss = { showAppUpdateDialog = false },
                        onUpdate = { url ->
                            showAppUpdateDialog = false
                            openSafeUrl(url, strings, onError = { message ->
                                rootToastSink(ToastData(message, ToastType.Error))
                            })
                        },
                        onIgnore = { ver ->
                            settingsRepository.ignoreUpdateVersion = ver
                            showAppUpdateDialog = false
                        },
                    )
                }

                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                val pendingOAuthCode by component.pendingClipboardOAuthCode.collectAsState()
                ClipboardOAuthDialog(
                    show = !pendingOAuthCode.isNullOrBlank(),
                    onConfirm = {
                        val code = component.consumeClipboardOAuthConfirmation() ?: return@ClipboardOAuthDialog
                        coroutineScope.launch {
                            runCatching { registry.accountRepository.loginWithCode(code) }
                                .onFailure { error ->
                                    if (error is kotlinx.coroutines.CancellationException) throw error
                                    rootToastSink(
                                        ToastData(
                                            PixivApiErrorFormatter.format(error, strings),
                                            ToastType.Error,
                                        ),
                                    )
                                }
                        }
                    },
                    onDismiss = { component.dismissClipboardOAuthConfirmation() },
                )

                AuthExpiredDialog(
                    show = showAuthExpiredDialog,
                    onRelogin = {
                        showAuthExpiredDialog = false
                        component.onAccountManageClicked()
                    },
                    onDismiss = { showAuthExpiredDialog = false },
                )

                BoxWithConstraints(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface),
                ) {
                    val isWideScreen = maxWidth >= AppConstants.Layout.WIDE_SCREEN_MIN_WIDTH_DP.dp
                    val useFloatingBottomBar = settingsRepository.useFloatingBottomBar
                    val showNavigationRailInMain = isWideScreen && !useFloatingBottomBar
                    val activeTab by component.selectedTab.collectAsState()
                    val density = LocalDensity.current
                    val screenCornerRadius = rememberScreenCornerRadius()
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    // 页面容器几何：矩形由布局回填，宽度与圆角由本层测量得出。
                    var containerGeometry by remember {
                        mutableStateOf(PageContainerGeometry(widthPx = containerWidthPx, cornerRadius = screenCornerRadius))
                    }
                    // 宽度与圆角随环境变化时同步到几何快照；矩形保持布局回填的最新值。
                    LaunchedEffect(containerWidthPx, screenCornerRadius) {
                        containerGeometry = containerGeometry.copy(
                            widthPx = containerWidthPx,
                            cornerRadius = screenCornerRadius,
                        )
                    }

                    // 转场动画器按容器几何记忆化：容器尺寸变化（旋转/分栏切换）时才重建，
                    // 避免每次重组都新建 StackAnimation 而击穿 Decompose 内部的按页动画器缓存。
                    // 几何以即时读取源传入：Decompose 会按 child 缓存动画器实例，
                    // 快照捕获会让冷启动首帧未回填的 Rect.Zero 被过期缓存固化。
                    // 各尺寸设备统一卡片展开/收回转场（双向视口裁切支持任意横纵比落点），
                    // 两层按同一锚点解析路径，无锚点时统一回退 Miuix 默认侧滑。
                    val stackAnimation = remember(sharedBounds) {
                        miuixSeekableStackAnimation<Config, Any>(
                            registry = sharedBounds,
                            containerGeometry = { containerGeometry },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface)
                            .onGloballyPositioned { coordinates ->
                                val newBounds = coordinates.boundsInWindow()
                                if (newBounds != containerGeometry.bounds) {
                                    NavTransitionLog.d("root") { "containerGeometry bounds=$newBounds" }
                                    containerGeometry = containerGeometry.withBounds(newBounds)
                                }
                            },
                    ) {
                        NavTransitionLog.d("root") { "stack=[${stack.items.joinToString { it.configuration.toString() }}] recompose" }
                        Children(
                            stack = component.stack,
                            modifier = Modifier.fillMaxSize(),
                            animation = miuixPredictiveBackAnimation(
                                backHandler = component.backHandler,
                                fallbackAnimation = stackAnimation,
                                selector = { initialBackEvent, exitChild, _ ->
                                    // 预测性返回手势来源页直接取自 exitChild 的 configuration，
                                    // 避免依赖 child 实例类型（中-6b 起详情页实例为子组件，路由参数只在配置上）。
                                    // 手势预览与常规转场共用同一锚点解析：能落点则按卡片收回，否则回退侧滑。
                                    miuixCardExpandPredictiveBackAnimatable(
                                        initialBackEvent = initialBackEvent,
                                        registry = sharedBounds,
                                        illustId = (exitChild.configuration as? Config.IllustDetail)?.illustId,
                                        containerGeometry = { containerGeometry },
                                    )
                                },
                                onBack = { component.onBack() },
                                // 应用内拖拽线性关页：仅作品详情页启用（平台开关见 rememberDragBackGestureEnabled）。
                                // 实例判定：详情页 child 实例为中-6b 试点引入的子组件。
                                dragBackEligible = { it is IllustDetailComponent },
                            ),
                        ) { child ->
                            RootChildContent(
                                instance = child.instance,
                                component = component,
                                activeTab = activeTab,
                                isWideScreen = isWideScreen,
                                useFloatingBottomBar = useFloatingBottomBar,
                                showNavigationRailInMain = showNavigationRailInMain,
                                bottomBarVisible = bottomBarVisible,
                                floatingBackdrop = floatingBackdrop,
                            )
                        }
                    }

                    // 全局 Toast：根层唯一实例，承载所有页面与组件投递的提示。
                    ToastMessage(
                        toast = rootToastMessage,
                        onDismiss = { rootToastMessage = null },
                    )
                }
            }
        }
        }
    }
}

/**
 * Decompose 栈顶页面的分发：按 child 实例类型路由到对应 render* 页面。
 * 实例类型（中-6b 试点起）：作品详情页为 [IllustDetailComponent]，其余页面为 [Config]。
 * 一级页面（Main）见 [MainChildSection]，其余二级页面渲染见同包 RootChildScreens.kt。
 */
@Composable
private fun RootChildContent(
    instance: Any,
    component: RootComponent,
    activeTab: RootComponent.MainTab,
    isWideScreen: Boolean,
    useFloatingBottomBar: Boolean,
    showNavigationRailInMain: Boolean,
    bottomBarVisible: MutableState<Boolean>,
    floatingBackdrop: LayerBackdrop?,
) {
    when (instance) {
        // 中-6b 试点：作品详情页 child 实例为子组件，渲染参数与跳转回调经组件透传。
        is IllustDetailComponent -> renderIllustDetail(
            component = instance,
        )

        is Config.Main -> MainChildSection(
            tab = instance.tab,
            feedRestore = component.mainFeedRestore,
            component = component,
            activeTab = activeTab,
            showNavigationRailInMain = showNavigationRailInMain,
            bottomBarVisible = bottomBarVisible,
            isWideScreen = isWideScreen,
                            useFloatingBottomBar = useFloatingBottomBar,
                            floatingBackdrop = floatingBackdrop,
                        )

        // 用户详情页同样组件化：页面状态由子组件的 InstanceKeeper 保留，栈换层重建不丢。
        is UserDetailComponent -> renderUserDetail(
            component = instance,
            root = component,
        )

        Config.Login -> renderLogin(
            component = component,
        )

        is Config.Comments -> renderComments(
            instance = instance,
            component = component,
        )

        is Config.RelatedIllusts -> renderRelatedIllusts(
            instance = instance,
            component = component,
        )

        is Config.IllustSeries -> renderIllustSeries(
            instance = instance,
            component = component,
        )

        is Config.UserFollowList -> renderUserFollowList(
            instance = instance,
            component = component,
        )

        is Config.UserFollowerList -> renderUserFollowerList(
            instance = instance,
            component = component,
        )

        Config.RecomUserList -> renderRecomUserList(
            component = component,
        )

        Config.Settings -> renderSettingsPage(
            component = component,
        )

        is Config.Search -> renderSearch(
            instance = instance,
            component = component,
        )

        Config.DownloadHistory -> renderDownloadHistory(
            component = component,
        )

        Config.Shield -> renderShield(
            component = component,
        )

        is Config.AISetting -> renderAISetting(
            instance = instance,
            component = component,
        )

        Config.ThemeSetting -> renderThemeSetting(
            onBack = component::onBack,
        )

        Config.NetworkSetting -> renderNetworkSetting(
            onBack = component::onBack,
        )

        Config.DownloadSetting -> renderDownloadSetting(
            onBack = component::onBack,
        )

        Config.LayoutSetting -> renderLayoutSetting(
            onBack = component::onBack,
        )

        Config.LanguageSetting -> renderLanguageSetting(
            onBack = component::onBack,
        )

        Config.WidgetRecommendSetting -> renderWidgetRecommendSetting(
            onBack = component::onBack,
        )

        Config.InteractionSetting -> renderInteractionSetting(
            onBack = component::onBack,
        )

        Config.UpdateSetting -> renderUpdateSetting(
            onBack = component::onBack,
        )

        Config.AccountEdit -> renderAccountEdit(
            component = component,
        )

        Config.History -> renderHistory(
            component = component,
        )

        Config.DownloadTask -> renderDownloadTask(
            component = component,
        )

        Config.DataExport -> renderDataExport(
            component = component,
        )

        Config.Board -> renderBoard(
            component = component,
        )

        Config.QualitySetting -> renderQualitySetting(
            onBack = component::onBack,
        )

        Config.CopyTextSetting -> renderCopyTextSetting(
            onBack = component::onBack,
        )

        Config.WelcomePageSetting -> renderWelcomePageSetting(
            onBack = component::onBack,
        )

        Config.About -> renderAbout(
            component = component,
        )

        Config.BookTag -> renderBookTag(
            component = component,
        )

        Config.Thanks -> renderThanks(
            component = component,
        )

        is Config.SpotlightDetail -> renderSpotlightDetail(
            instance = instance,
            component = component,
        )

        Config.Guide -> renderGuide(
            component = component,
        )

        Config.AccountManage -> renderAccountManage(
            component = component,
        )

        Config.Novel -> renderNovel(
            component = component,
        )

        is Config.NovelViewer -> renderNovelViewer(
            instance = instance,
            component = component,
        )

        is Config.UserRelatedList -> renderUserRelatedList(
            instance = instance,
            component = component,
        )

        is Config.UserNovelList -> renderUserNovelList(
            instance = instance,
            component = component,
        )

        // 分发主体改为 Any 后失去密封 Config 的编译期穷举校验，以此兜底：
        // 新增 Config 未接线时在分发处即时抛错，而非静默白屏。
        else -> error("RootChildContent 未分发的路由实例: $instance")
    }
}

/**
 * 一级页面（Main）区块：可选侧边导航栏 + MainContent + 底部导航栏。
 *
 * 中-6a 起 Child 镜像已删除：tab 取自栈顶 [Config.Main]，跨页恢复容器（高-5）经
 * [component.mainFeedRestore] 读取（由 RootComponent.createChild 在 Main child 创建时接线）。
 */
@Composable
private fun MainChildSection(
    tab: RootComponent.MainTab,
    feedRestore: MainFeedRestore,
    component: RootComponent,
    activeTab: RootComponent.MainTab,
    showNavigationRailInMain: Boolean,
    bottomBarVisible: MutableState<Boolean>,
    isWideScreen: Boolean,
    useFloatingBottomBar: Boolean,
    floatingBackdrop: LayerBackdrop?,
) {
    // 读取 changeVersion 以订阅设置变更，保证 Main 子树在设置修改后重组刷新。
    // 中-9：设置仓储经 LocalSettingsRepository 读取（RootContent 根部 provide）。
    val settingsRepository = LocalSettingsRepository.current
    val mainChangeVersion = settingsRepository.changeVersion.collectAsState().value
    val uiSkin = LocalUiSkin.current
    val strings = LocalStrings.current
    val hasUnreadFeedBadge = settingsRepository.hasUnreadFeedBadge
    val couiNavItems = remember(strings, hasUnreadFeedBadge, activeTab, uiSkin) {
        MAIN_TAB_DESCRIPTORS.map { descriptor ->
            SkinNavItem(
                title = descriptor.label(strings),
                icon = descriptor.icon,
                showBadge = shouldShowUnreadFeedBadge(descriptor, activeTab, hasUnreadFeedBadge),
            )
        }
    }
    // 底栏内容边距只在 Main 子树内提供且转场全程不随 active 翻转，
    // 消除进出详情页时列表因边距变化发生的重排；二级页面无底栏，
    // 回落为 CompositionLocal 默认值 16.dp。
    CompositionLocalProvider(
        LocalBottomBarContentPadding provides if (bottomBarVisible.value) 100.dp else 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface),
        ) {
            if (showNavigationRailInMain && uiSkin != UiSkin.COLOROS) {
                MainNavigationRail(
                    activeTab = activeTab,
                    onTabSelected = component::onMainTabSelected,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(floatingBackdrop),
                ) {
                    MainContent(
                        initialTab = tab,
                        component = component,
                        feedRestore = feedRestore,
                    )
                }
                if (bottomBarVisible.value) {
                    if (uiSkin == UiSkin.COLOROS) {
                        // COUI 底栏：API 34+ 官方浮动栏，31-33 由 androidMain 自绘平替。
                        CouiMainNavigationBar(
                            items = couiNavItems,
                            selectedIndex = MAIN_TAB_DESCRIPTORS
                                .indexOfFirst { it.tab == activeTab }
                                .coerceAtLeast(0),
                            onSelect = { index ->
                                MAIN_TAB_DESCRIPTORS.getOrNull(index)?.let {
                                    component.onMainTabSelected(it.tab)
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    } else if (!isWideScreen || useFloatingBottomBar) {
                        MainBottomBar(
                            activeTab = activeTab,
                            onTabSelected = component::onMainTabSelected,
                            isFloating = useFloatingBottomBar,
                            backdrop = floatingBackdrop,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}