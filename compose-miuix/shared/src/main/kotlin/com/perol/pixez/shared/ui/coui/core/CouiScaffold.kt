package com.perol.pixez.shared.ui.coui.core

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.backdrop.ProvideCouiBackdrop
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.scroll.couiVerticalScroll
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.search.CouiSearchBarState
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import com.itos.coui.theme.LocalCouiTextStyle
import com.itos.coui.topappbar.CollapsingTopAppBar
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarScrollBehavior
import com.itos.coui.topappbar.LargeTopAppBar
import com.itos.coui.topappbar.TopAppBar
import com.itos.coui.topappbar.TopAppBarButton
import com.itos.coui.topappbar.TopAppBarButtonGroup
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings

/*
 * 页面骨架层：容器、顶栏、滚动与安全区全部收敛到这里，页面只提供业务内容。
 *
 * 四条官方规则决定了本文件的所有写法：
 * 1. 顶栏背景是库内合成的两层 —— 采样正文的模糊层 + 颜色渐隐遮罩层。遮罩色取页面底色
 *    （CouiTheme.colorScheme.background）；留空只会回落到纯白/纯黑，与本工程非纯色底板不同源。
 * 2. 正文必须留在 Scaffold 的正文槽里滚动，把护栏 padding 交给列表的 contentPadding 或
 *    滚动容器之后的 .padding()。Scaffold 已自动为正文区挂接 couiBackdrop 并经
 *    LocalCouiBackdrop 注入 topBar 槽，因此放在 topBarBottom 里的 ChipTabRow /
 *    CouiTopBarSegmentedTabs 天然是正文的兄弟节点，可安全采样滚动正文而不自采样。
 * 3. 折叠顶栏的护栏必须返回延迟求值的活对象（实时读取 state.contentPadding），切不可在组合期
 *    静态冻结 calculateTopPadding()，否则顶栏折叠/展开或 topBarBottom 变化时正文留白会错位。
 * 4. 顶栏多按钮在顶栏效果作用域内统一由 TopAppBarButtonGroup 承接，保持共享位移与原厂排布。
 */

/**
 * 唯一共享正文采样源：COUI 库的 `LocalCouiBackdrop` 为 `internal`，
 * 当页面装配 `topBarBottom` 时由 [CouiSingleBackdropScaffold] 持有唯一一个 [CouiBackdrop]，
 * 经 `ProvideCouiBackdrop` 注入官方顶栏并经本 Local 共享给 `CouiChipTabRow` / `CouiTopBarSegmentedTabs`，
 * 避免在 `Scaffold` 内部二次套壳 `couiBackdrop` 造成双层全屏离屏录制。
 */
internal val LocalCouiSharedBackdrop = staticCompositionLocalOf<CouiBackdrop?> { null }

private enum class SingleBackdropScaffoldSlot {
    Content,
    TopBar,
    Fab,
}

private class LiveScaffoldPadding : PaddingValues {
    var value by mutableStateOf(PaddingValues(0.dp))

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        value.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = value.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        value.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp = value.calculateBottomPadding()
}

/**
 * 单层 Backdrop 页面骨架：用于装配了 `topBarBottom` 的页面。
 * 正文区仅挂载唯一一层 `couiBackdrop`，同时服务于顶栏渐变模糊与吸顶 Tab 凝光毛玻璃，零双层嵌套录制。
 */
@Composable
private fun CouiSingleBackdropScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberCouiBackdrop()
    val containerColor = CouiTheme.colorScheme.background
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val livePadding = remember { LiveScaffoldPadding() }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val topBarPlaceables = subcompose(SingleBackdropScaffoldSlot.TopBar) {
            ProvideCouiBackdrop(backdrop) {
                CompositionLocalProvider(LocalCouiSharedBackdrop provides backdrop) {
                    topBar()
                }
            }
        }.map { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0

        val topDp = if (topBarHeight > 0) topBarHeight.toDp() else insets.getTop(this).toDp()
        val bottomDp = insets.getBottom(this).toDp()
        val nextPadding = PaddingValues(top = topDp, bottom = bottomDp)
        if (livePadding.value != nextPadding) {
            livePadding.value = nextPadding
        }

        val contentPlaceables = subcompose(SingleBackdropScaffoldSlot.Content) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .couiBackdrop(backdrop)
                    .background(containerColor),
            ) {
                content(livePadding)
            }
        }.map { it.measure(constraints) }

        val fabPlaceables = if (floatingActionButton != null) {
            subcompose(SingleBackdropScaffoldSlot.Fab) {
                ProvideCouiBackdrop(backdrop) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        floatingActionButton()
                    }
                }
            }.map { it.measure(constraints) }
        } else {
            emptyList()
        }

        layout(layoutWidth, layoutHeight) {
            contentPlaceables.forEach { it.place(0, 0) }
            topBarPlaceables.forEach { it.place(0, 0) }
            fabPlaceables.forEach { it.place(0, 0) }
        }
    }
}

/** 顶栏标题：官方顶栏只通过 [LocalCouiTextStyle] 下发标题样式，不用 COUI 的 8 个文字角色。 */
@Composable
internal fun CouiBarTitle(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    val style = LocalCouiTextStyle.current
    val color = LocalCouiContentColor.current.takeOrElse { CouiTheme.colorScheme.onSurface }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 顶栏图标按钮。
 *
 * API 34+ 走官方 [TopAppBarButton]（材质、按压反馈与图标色都由顶栏效果层提供，
 * 底层 [com.itos.coui.effect.CouiFloatingRenderer] 经对象池借还复用，避免切页现编 AGSL Shader）；
 * API 31-33 官方按钮不可用，用同尺寸自绘按钮平替，图标色仍取顶栏下发的内容色。
 * 传入 [tint] 时覆盖默认图标色（例如已收藏态高亮）。
 */
@Composable
internal fun CouiTopBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    iconModifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiFloatingTopBarIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            enabled = enabled,
            tint = tint,
            iconModifier = iconModifier,
        )
    } else {
        CouiPlainTopBarIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            enabled = enabled,
            tint = tint,
            iconModifier = iconModifier,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun CouiFloatingTopBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    iconModifier: Modifier = Modifier,
) {
    val renderer = rememberPooledCouiFloatingRenderer()
    if (renderer == null) {
        CouiPlainTopBarIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            enabled = enabled,
            tint = tint,
            iconModifier = iconModifier,
        )
        return
    }
    TopAppBarButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        renderer = renderer,
    ) {
        CouiBarIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier,
        )
    }
}

@Composable
private fun CouiPlainTopBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    iconModifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = source, enabled = enabled, list = false)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CouiBarIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier,
        )
    }
}

@Composable
private fun CouiBarIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color? = null,
    modifier: Modifier = Modifier,
) {
    val resolvedTint = tint ?: LocalCouiContentColor.current.takeOrElse { CouiTheme.colorScheme.onSurface }
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier.size(22.dp),
        colorFilter = ColorFilter.tint(resolvedTint),
    )
}

/** 构造带额外底部留白的活 [PaddingValues]，避免在组合期静态冻结顶栏折叠高度。 */
private fun PaddingValues.withExtraBottom(extraBottom: Dp): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        this@withExtraBottom.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp =
        this@withExtraBottom.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        this@withExtraBottom.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp =
        this@withExtraBottom.calculateBottomPadding() + extraBottom
}

/**
 * 紧凑顶栏（52dp）：给不需要大标题的简单页面或沉浸内容页用。
 */
@Composable
internal fun CouiTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    TopAppBar(
        title = { CouiBarTitle(text = title) },
        colors = CouiTopAppBarDefaults.colors(
            backgroundColor = CouiTheme.colorScheme.background,
            dark = CouiTheme.isDark,
        ),
        navigationIcon = {
            if (onBack != null) {
                CouiTopBarIconButton(onClick = onBack, icon = CouiIcons.Back, contentDescription = strings.back)
            }
        },
        actions = {
            if (actions != null) {
                TopAppBarButtonGroup { actions() }
            }
        },
    )
}

/**
 * 紧凑顶栏骨架（52dp）：适合作品详情页等首屏以视觉内容为主的二级页面。
 *
 * 首屏不被 110dp 大标题抢占高度，正文内容仍可向上滚进顶栏毛玻璃区；
 * 若传入 [topBarBottom]，其位于顶栏槽内、与正文形成兄弟节点，可采样正文 [com.itos.coui.backdrop.LocalCouiBackdrop]。
 */
@Composable
internal fun CouiCompactScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topBarModifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    topBarBottom: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val bottomReserve = LocalBottomBarContentPadding.current
    val scaffoldModifier = modifier
        .fillMaxSize()
        .imePadding()
    val topBarSlot: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().then(topBarModifier)) {
            CouiTopBar(title = title, onBack = onBack, actions = actions)
            if (topBarBottom != null) topBarBottom()
        }
    }
    if (topBarBottom != null) {
        CouiSingleBackdropScaffold(
            modifier = scaffoldModifier,
            topBar = topBarSlot,
            floatingActionButton = floatingActionButton,
        ) { innerPadding ->
            val resolvedPadding = remember(innerPadding, bottomReserve) {
                innerPadding.withExtraBottom(bottomReserve)
            }
            content(resolvedPadding)
        }
    } else {
        Scaffold(
            modifier = scaffoldModifier,
            topBar = topBarSlot,
            floatingActionButton = floatingActionButton,
        ) { innerPadding ->
            val resolvedPadding = remember(innerPadding, bottomReserve) {
                innerPadding.withExtraBottom(bottomReserve)
            }
            content(resolvedPadding)
        }
    }
}

/**
 * 折叠大标题顶栏（展开 110dp / 收起 52dp）。
 */
@Composable
private fun CouiCollapsingTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    behavior: CouiTopAppBarScrollBehavior,
) {
    val strings = LocalStrings.current
    LargeTopAppBar(
        title = { CouiBarTitle(text = title) },
        colors = CouiTopAppBarDefaults.colors(
            backgroundColor = CouiTheme.colorScheme.background,
            dark = CouiTheme.isDark,
        ),
        navigationIcon = {
            if (onBack != null) {
                CouiTopBarIconButton(onClick = onBack, icon = CouiIcons.Back, contentDescription = strings.back)
            }
        },
        actions = {
            if (actions != null) {
                TopAppBarButtonGroup { actions() }
            }
        },
        scrollBehavior = behavior,
    )
}

/**
 * 二级页骨架：折叠大标题顶栏 + 返回按钮 + 可选吸顶扩展条（[topBarBottom]）。
 */
@Composable
internal fun CouiSecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    topBarBottom: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    CouiCollapsingScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        actions = actions,
        topBarBottom = topBarBottom,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/** 一级页骨架：折叠大标题顶栏 + 可选吸顶扩展条（[topBarBottom]），无返回按钮。 */
@Composable
internal fun CouiMainScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    topBarBottom: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    CouiCollapsingScaffold(
        title = title,
        onBack = null,
        modifier = modifier,
        actions = actions,
        topBarBottom = topBarBottom,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
private fun CouiCollapsingScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier,
    actions: (@Composable RowScope.() -> Unit)?,
    topBarBottom: (@Composable () -> Unit)?,
    floatingActionButton: (@Composable BoxScope.() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit,
) {
    val behavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    val bottomReserve = LocalBottomBarContentPadding.current
    val scaffoldModifier = modifier
        .fillMaxSize()
        .imePadding()
        .nestedScroll(behavior.nestedScrollConnection)
    val topBarSlot: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            CouiCollapsingTopBar(title = title, onBack = onBack, actions = actions, behavior = behavior)
            if (topBarBottom != null) topBarBottom()
        }
    }
    if (topBarBottom != null) {
        CouiSingleBackdropScaffold(
            modifier = scaffoldModifier,
            topBar = topBarSlot,
            floatingActionButton = floatingActionButton,
        ) { innerPadding ->
            val resolvedPadding = remember(behavior, innerPadding, density, bottomReserve) {
                behavior.state.contentPadding(innerPadding, density).withExtraBottom(bottomReserve)
            }
            content(resolvedPadding)
        }
    } else {
        Scaffold(
            modifier = scaffoldModifier,
            topBar = topBarSlot,
            floatingActionButton = floatingActionButton,
        ) { innerPadding ->
            val resolvedPadding = remember(behavior, innerPadding, density, bottomReserve) {
                behavior.state.contentPadding(innerPadding, density).withExtraBottom(bottomReserve)
            }
            content(resolvedPadding)
        }
    }
}

/**
 * 搜索页骨架：官方 [CollapsingTopAppBar] 的常驻搜索槽。
 */
@Composable
internal fun CouiSearchScaffold(
    title: String,
    searchState: CouiSearchBarState,
    modifier: Modifier = Modifier,
    searchBar: @Composable (collapsedFraction: Float) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val behavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    val bottomReserve = LocalBottomBarContentPadding.current
    val bar = behavior.state
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .nestedScroll(behavior.nestedScrollConnection),
        topBar = {
            CollapsingTopAppBar(
                state = bar,
                searchState = searchState,
                title = title,
                backgroundColor = CouiTheme.colorScheme.background,
                dark = CouiTheme.isDark,
            ) { searchBar(bar.collapsedFraction) }
        },
    ) { innerPadding ->
        val resolvedPadding = remember(bar, innerPadding, density, bottomReserve) {
            bar.contentPadding(innerPadding, density).withExtraBottom(bottomReserve)
        }
        content(resolvedPadding)
    }
}

/** 设置页骨架：顶栏 + 居中的可读宽度设置列表，分组间距 12dp。 */
@Composable
internal fun CouiSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    CouiSecondaryScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        actions = actions,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 0.dp,
                    bottom = padding.calculateBottomPadding() + CouiPageDefaults.groupSpacing,
                ),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                content()
            }
        }
    }
}

/** 非列表页骨架：单列纵向滚动，护栏放在滚动容器之后，内容同样能滚进顶栏覆盖区。 */
@Composable
internal fun CouiScrollableScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CouiSecondaryScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        actions = actions,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .couiVerticalScroll(rememberScrollState())
                    .padding(padding)
                    .consumeWindowInsets(padding),
                content = content,
            )
        }
    }
}

/** 页面内正文容器：横向 16dp、纵向统一节奏，供自绘布局的详情页复用。 */
@Composable
internal fun CouiBodyColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** 正文小标题：用于设置页分组之外的自绘区块标题。 */
@Composable
internal fun CouiBodyTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        kind = CouiTextKind.GroupTitle,
        color = CouiTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        textAlign = TextAlign.Start,
    )
}
