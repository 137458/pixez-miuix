package com.perol.pixez.shared.ui.coui.core

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.scroll.couiVerticalScroll
import com.itos.coui.search.CouiSearchBarState
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import com.itos.coui.theme.LocalCouiTextStyle
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.CollapsingTopAppBar
import com.itos.coui.topappbar.CouiTopAppBarScrollBehavior
import com.itos.coui.topappbar.LargeTopAppBar
import com.itos.coui.topappbar.TopAppBar
import com.itos.coui.topappbar.TopAppBarButton
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/*
 * 页面骨架层：容器、顶栏、滚动与安全区全部收敛到这里，页面只提供业务内容。
 *
 * 三条官方规则决定了本文件的所有写法：
 * 1. 顶栏背景是库内合成的两层 —— 采样正文的模糊层 + 颜色渐隐遮罩层。遮罩色取页面底色
 *    （对应官方 DemoTheme 的 materialTheme 分支：LocalDemoTopBarGradient provides colors.background）；
 *    留空只会回落到纯白/纯黑，与本工程的非纯色底板不同源，顶部会浮出一条色差带。
 * 2. 正文必须留在 Scaffold 的正文槽里滚动，把护栏 padding 交给列表的 contentPadding 或
 *    滚动容器之后的 .padding()。若在外层再包 .padding()，正文视口整体缩到顶栏之下，
 *    模糊层就采样不到任何会动的内容。
 * 3. 折叠顶栏的护栏要经过 state.contentPadding(padding, density) 补偿，展开/收起过程中
 *    标题才不会与首项重叠；正文还要 consumeWindowInsets，避免子级二次扣减安全区。
 */

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
 * API 34+ 走官方 [TopAppBarButton]（材质、按压反馈与图标色都由顶栏效果层提供）；
 * API 31-33 官方按钮不可用，用同尺寸自绘按钮平替，图标色仍取顶栏下发的内容色。
 * 两种分支都必须在顶栏槽内调用，超出槽位会拿不到内容色与材质。
 */
@Composable
internal fun CouiTopBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiFloatingTopBarIconButton(onClick = onClick, icon = icon, contentDescription = contentDescription, modifier = modifier, enabled = enabled)
    } else {
        CouiPlainTopBarIconButton(onClick = onClick, icon = icon, contentDescription = contentDescription, modifier = modifier, enabled = enabled)
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
) {
    TopAppBarButton(modifier = modifier, onClick = onClick, enabled = enabled) {
        CouiBarIcon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun CouiPlainTopBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
        CouiBarIcon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun CouiBarIcon(imageVector: ImageVector, contentDescription: String?) {
    val tint = LocalCouiContentColor.current.takeOrElse { CouiTheme.colorScheme.onSurface }
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

/**
 * 紧凑顶栏（52dp）：给不需要大标题的简单页面用。
 *
 * 遮罩色显式取页面底色（与 Scaffold 的 containerColor 同源）—— 顶部实色与页面无缝衔接、
 * 向下渐隐出模糊层；留空会让浅色主题出现一条比页面更白的渐隐带。
 */
@Composable
internal fun CouiTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
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
                CouiTopBarIconButton(onClick = onBack, icon = MiuixIcons.Back, contentDescription = strings.back)
            }
        },
        actions = actions,
    )
}

/**
 * 折叠大标题顶栏（展开 110dp / 收起 52dp）。
 *
 * 高度、折叠曲线、模糊半径、渐变速度与 topExtension 全部由库内部决定，宿主不传也不叠加
 * statusBarsPadding；这里只负责把滚动联动挂上，并把护栏交给正文。
 */
@Composable
private fun CouiCollapsingTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
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
                CouiTopBarIconButton(onClick = onBack, icon = MiuixIcons.Back, contentDescription = strings.back)
            }
        },
        actions = actions,
        scrollBehavior = behavior,
    )
}

/**
 * 二级页骨架：折叠大标题顶栏 + 返回按钮。
 *
 * [content] 拿到的是补偿过的护栏：底部已含悬浮底栏的留白。列表页直接把它当 contentPadding，
 * 非列表页在 .couiVerticalScroll() 之后 .padding(它) 并 .consumeWindowInsets(原始 padding)。
 */
@Composable
internal fun CouiSecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    CouiCollapsingScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        actions = actions,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/** 一级页骨架：折叠大标题顶栏，无返回按钮。 */
@Composable
internal fun CouiMainScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    CouiCollapsingScaffold(
        title = title,
        onBack = null,
        modifier = modifier,
        actions = actions,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
private fun CouiCollapsingScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
    floatingActionButton: (@Composable BoxScope.() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit,
) {
    val behavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    val bottomReserve = LocalBottomBarContentPadding.current
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .nestedScroll(behavior.nestedScrollConnection),
        topBar = { CouiCollapsingTopBar(title = title, onBack = onBack, actions = actions, behavior = behavior) },
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        val guarded = behavior.state.contentPadding(innerPadding, density)
        // 正文槽的左右护栏恒为 0（横向安全区由 Scaffold 自己消费），因此只透传上下。
        content(
            PaddingValues(
                top = guarded.calculateTopPadding(),
                bottom = guarded.calculateBottomPadding() + bottomReserve,
            ),
        )
    }
}

/**
 * 搜索页骨架：官方 [CollapsingTopAppBar] 的常驻搜索槽。
 *
 * 搜索框必须留在顶栏的搜索槽里：槽位在 TopAppBarSurface 内部，属于顶栏采样作用域，
 * 官方 SearchBar 才能拿到不含自身的背景源。放到正文里既没有该作用域（默认参数直接抛
 * 「搜索需要显式 backdrop 或独立的顶栏作用域」），显式传源又会把搜索框自己录进模糊层。
 * 该顶栏没有 actions 槽，页面动作要放在正文里。
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
        val guarded = bar.contentPadding(innerPadding, density)
        content(
            PaddingValues(
                top = guarded.calculateTopPadding(),
                bottom = guarded.calculateBottomPadding() + bottomReserve,
            ),
        )
    }
}

/** 设置页骨架：顶栏 + 居中的可读宽度设置列表，分组间距 12dp。 */
@Composable
internal fun CouiSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
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
                // 横向留白由官方 Group 的 horizontalMargin 提供，这里不再叠加。
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 0.dp,
                    bottom = padding.calculateBottomPadding() + CouiPageDefaults.groupSpacing,
                ),
                // 分组之间由官方 Group 自己的 groupSpacing 撑开，列表不再重复加间距。
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
    actions: @Composable RowScope.() -> Unit = {},
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
