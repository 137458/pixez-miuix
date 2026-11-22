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
import androidx.compose.runtime.mutableIntStateOf
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
import com.perol.pixez.shared.ui.skin.LocalSkinPages
import com.perol.pixez.shared.ui.skin.CouiSkinHost
import com.perol.pixez.shared.ui.coui.core.resolveCouiEffectiveFloatingBottomBar
import com.perol.pixez.shared.ui.skin.skinFloatingBarSource
import com.perol.pixez.shared.ui.skin.LocalUiSkin
import com.perol.pixez.shared.ui.skin.SkinNavItem
import com.perol.pixez.shared.ui.skin.UiSkin

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

    // 皮肤：Android API 31+ 下 HyperOS 与 ColorOS 均可用。
    val uiSkin = remember(changeVersion) {
        UiSkin.fromIndex(settingsRepository.uiSkinIndex)
    }

    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance

    val floatingBackdrop = rememberBlurBackdrop(enableBlur = uiSkin != UiSkin.COLOROS)
    // 作品卡片几何信息源：列表卡片登记矩形，二级页面转场据此播放「卡片展开/收回」动画。
    val sharedBounds = remember { SharedBoundsRegistry() }
    // SideEffect：成功应用后再同步路由锚点；被丢弃的重组不会污染不可回滚的注册表字段
    SideEffect {
        sharedBounds.syncActiveRouteTarget(stack.active.configuration.toTransitionTarget())
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
    // 触发序号：相同文案连发两次时靠它区分，否则结构相等会让第二次投递不可观察。
    var rootToastSeq by remember { mutableIntStateOf(0) }
    val rootToastSink: (ToastData) -> Unit = remember {
        {
            rootToastSeq += 1
            rootToastMessage = it.copy(seq = rootToastSeq)
        }
    }

    // 凭证失效提示与恢复收敛：先以 UNDISPATCHED 挂上收集器，再触发启动自检，
    // 避免「账号凭证已被清理」的自检事件在订阅建立前发出而丢失；会话恢复或重新登录后自动收起失效弹窗。
    LaunchedEffect(Unit) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            registry.accountRepository.authExpiredFlow.collect {
                showAuthExpiredDialog = true
            }
        }
        launch(start = CoroutineStart.UNDISPATCHED) {
            registry.accountRepository.loginEventFlow.collect {
                if (registry.accountRepository.currentAccount()?.hasCredentials == true) {
                    showAuthExpiredDialog = false
                }
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
        CouiSkinHost(
            enabled = uiSkin == UiSkin.COLOROS,
            dark = appThemeSpec.resolvedDark,
            isAmoled = themeSettings.isAmoled,
            useDynamicColor = themeSettings.useDynamicColor,
            seedColor = themeSettings.seedColor,
        ) {
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
                val skinPages = LocalSkinPages.current
                val updateInfo = appReleaseInfo
                if (showAppUpdateDialog && updateInfo != null) {
                    val onOpenUpdateUrl: (String) -> Unit = { url ->
                        showAppUpdateDialog = false
                        openSafeUrl(url, strings, onError = { message ->
                            rootToastSink(ToastData(message, ToastType.Error))
                        })
                    }
                    val onIgnoreUpdate: (String) -> Unit = { ver ->
                        settingsRepository.ignoreUpdateVersion = ver
                        showAppUpdateDialog = false
                    }
                    if (skinPages != null) {
                        skinPages.updateDialog(
                            releaseInfo = updateInfo,
                            onDismiss = { showAppUpdateDialog = false },
                            onUpdate = onOpenUpdateUrl,
                            onIgnore = onIgnoreUpdate,
                        )
                    } else {
                        UpdateDialog(
                            show = showAppUpdateDialog,
                            releaseInfo = updateInfo,
                            onDismiss = { showAppUpdateDialog = false },
                            onUpdate = onOpenUpdateUrl,
                            onIgnore = onIgnoreUpdate,
                        )
                    }
                }

                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                val pendingOAuthCode by component.pendingClipboardOAuthCode.collectAsState()
                val showClipboardOAuth = !pendingOAuthCode.isNullOrBlank()
                val onConfirmClipboardOAuth: () -> Unit = {
                    val code = component.consumeClipboardOAuthConfirmation()
                    if (code != null) {
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
                    }
                }
                if (skinPages != null) {
                    skinPages.clipboardOAuthDialog(
                        show = showClipboardOAuth,
                        onConfirm = onConfirmClipboardOAuth,
                        onDismiss = { component.dismissClipboardOAuthConfirmation() },
                    )
                } else {
                    ClipboardOAuthDialog(
                        show = showClipboardOAuth,
                        onConfirm = onConfirmClipboardOAuth,
                        onDismiss = { component.dismissClipboardOAuthConfirmation() },
                    )
                }

                val onReloginExpired: () -> Unit = {
                    showAuthExpiredDialog = false
                    component.onAccountManageClicked()
                }
                if (skinPages != null) {
                    skinPages.authExpiredDialog(
                        show = showAuthExpiredDialog,
                        onRelogin = onReloginExpired,
                        onDismiss = { showAuthExpiredDialog = false },
                    )
                } else {
                    AuthExpiredDialog(
                        show = showAuthExpiredDialog,
                        onRelogin = onReloginExpired,
                        onDismiss = { showAuthExpiredDialog = false },
                    )
                }

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
                    // 底层页视差按皮肤语义决定：MIUIX 是「撑开」（底层左移 25%），ColorOS 是「覆盖」
                    // （底层静止、只叠暗色遮罩）。COUI 皮肤下底层页挂着窗口级 Backdrop 采样源与瀑布流
                    // 大图，整页位移会让每帧把整页重新合成，稳态帧时长 30ms -> 20ms 的差距即来自此。
                    val stackAnimation = remember(sharedBounds, uiSkin) {
                        miuixSeekableStackAnimation<Config, Any>(
                            registry = sharedBounds,
                            containerGeometry = { containerGeometry },
                            coverParallax = uiSkin.coverParallax,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface)
                            .onGloballyPositioned { coordinates ->
                                val newBounds = coordinates.boundsInWindow()
                                if (newBounds != containerGeometry.bounds) {
                                    containerGeometry = containerGeometry.withBounds(newBounds)
                                }
                            },
                    ) {
                        Children(
                            stack = component.stack,
                            modifier = Modifier.fillMaxSize(),
                            animation = miuixPredictiveBackAnimation(
                                backHandler = component.backHandler,
                                fallbackAnimation = stackAnimation,
                                selector = { initialBackEvent, exitChild, _ ->
                                    // 预测性返回手势来源页直接取自 exitChild 的 configuration，
                                    // 覆盖插画详情页与小说阅读器两种卡片展开目标；无锚点则回退侧滑。
                                    miuixCardExpandPredictiveBackAnimatable(
                                        initialBackEvent = initialBackEvent,
                                        registry = sharedBounds,
                                        target = exitChild.configuration.toTransitionTarget(),
                                        containerGeometry = { containerGeometry },
                                        coverParallax = uiSkin.coverParallax,
                                    )
                                },
                                onBack = { component.onBack() },
                                // 应用内拖拽线性关页：插画详情页与小说阅读器均启用（平台开关见 rememberDragBackGestureEnabled）。
                                dragBackEligible = { it is IllustDetailComponent || it is Config.NovelViewer },
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

                    // 全局 Toast：根层唯一实例，皮肤接缝非空时由皮肤渲染（避免 COUI 下弹出 MIUIX Toast）。
                    if (skinPages != null) {
                        skinPages.toastOverlay(
                            toast = rootToastMessage,
                            onDismiss = { rootToastMessage = null },
                        )
                    } else {
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
    // 设置仓储经 LocalSettingsRepository 读取（RootContent 根部 provide）。
    // hasUnreadFeedBadge 是普通字段而非可观察状态，只有把 changeVersion 一并读进组合作用域，
    // 未读红点才会在写入后立即刷新；导航条目同样以 changeVersion 为 remember 键。
    val settingsRepository = LocalSettingsRepository.current
    val changeVersion = settingsRepository.changeVersion.collectAsState().value
    val skinPages = LocalSkinPages.current
    val isCouiSkin = skinPages != null
    val strings = LocalStrings.current
    val hasUnreadFeedBadge = settingsRepository.hasUnreadFeedBadge
    // COUI 皮肤将搜索拆出到右侧独立按钮；若从历史状态/HyperOS 皮肤切入且停在 Search 页签，切回首页。
    LaunchedEffect(isCouiSkin, activeTab) {
        if (isCouiSkin && activeTab == RootComponent.MainTab.Search) {
            component.onMainTabSelected(RootComponent.MainTab.Hello)
        }
    }
    val visibleDescriptors = remember(isCouiSkin) {
        resolveVisibleMainTabDescriptors(isCouiSkin)
    }
    val couiNavItems = remember(visibleDescriptors, strings, hasUnreadFeedBadge, activeTab, changeVersion) {
        visibleDescriptors.map { descriptor ->
            SkinNavItem(
                title = descriptor.label(strings),
                icon = descriptor.icon,
                showBadge = shouldShowUnreadFeedBadge(descriptor, activeTab, hasUnreadFeedBadge),
                tab = descriptor.tab,
            )
        }
    }
    val selectedNavIndex = resolveMainTabIndex(visibleDescriptors, activeTab)
    val onNavIndexSelected: (Int) -> Unit = { index ->
        visibleDescriptors.getOrNull(index)?.let {
            component.onMainTabSelected(it.tab)
        }
    }
    val effectiveFloatingBottomBar = resolveCouiEffectiveFloatingBottomBar(
        isCouiSkin = isCouiSkin,
        sdkInt = android.os.Build.VERSION.SDK_INT,
        isWideScreen = isWideScreen,
        useFloatingBottomBar = useFloatingBottomBar,
    )
    // 底栏内容边距只在 Main 子树内提供且转场全程不随 active 翻转，
    // 消除进出详情页时列表因边距变化发生的重排；二级页面无底栏，
    // 回落为 CompositionLocal 默认值 16.dp。
    CompositionLocalProvider(
        LocalBottomBarContentPadding provides when {
            showNavigationRailInMain -> 16.dp
            bottomBarVisible.value && effectiveFloatingBottomBar -> 100.dp
            bottomBarVisible.value -> 68.dp
            else -> 16.dp
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface),
        ) {
            if (showNavigationRailInMain) {
                if (skinPages != null) {
                    skinPages.navigationRail(
                        items = couiNavItems,
                        selectedIndex = selectedNavIndex,
                        onSelect = onNavIndexSelected,
                        onSearchClick = { component.onSearchClicked("") },
                    )
                } else {
                    MainNavigationRail(
                        activeTab = activeTab,
                        onTabSelected = component::onMainTabSelected,
                        hasUnreadFeedBadge = hasUnreadFeedBadge,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(floatingBackdrop)
                        // COUI 皮肤开启时同一块正文再登记为 COUI 底栏的采样源（桌面端为空操作）。
                        .skinFloatingBarSource()
                        // 采样源内必须自带不透明底色：Pager 过滚/拖动露出的空白区在图层里是透明的，
                        // 底栏玻璃采到透明像素会渲染成黑块。
                        .background(MiuixTheme.colorScheme.surface),
                ) {
                    MainContent(
                        initialTab = tab,
                        component = component,
                        feedRestore = feedRestore,
                    )
                }
                if (bottomBarVisible.value && (!isWideScreen || useFloatingBottomBar)) {
                    // 皮肤开启时无论浮动或贴底都交给皮肤底栏，绝不回退 MIUIX MainBottomBar。
                    if (skinPages != null) {
                        skinPages.navigationBar(
                            items = couiNavItems,
                            selectedIndex = selectedNavIndex,
                            isFloating = effectiveFloatingBottomBar,
                            onSelect = onNavIndexSelected,
                            onLeftAccessoryClick = component::onSettingsClicked,
                            onRightAccessoryClick = { component.onSearchClicked("") },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    } else {
                        MainBottomBar(
                            activeTab = activeTab,
                            onTabSelected = component::onMainTabSelected,
                            hasUnreadFeedBadge = hasUnreadFeedBadge,
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