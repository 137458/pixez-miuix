package com.perol.pixez.shared.ui.skin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiColorScheme
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.CouiTypography
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.state.UserDetailPageState
import com.perol.pixez.shared.ui.coui.page.CouiAboutScreen
import com.perol.pixez.shared.ui.coui.page.CouiAccountEditScreen
import com.perol.pixez.shared.ui.coui.page.CouiAccountManageScreen
import com.perol.pixez.shared.ui.coui.page.CouiAiSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiBoardScreen
import com.perol.pixez.shared.ui.coui.page.CouiBookTagScreen
import com.perol.pixez.shared.ui.coui.page.CouiThemeSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiCommentsScreen
import com.perol.pixez.shared.ui.coui.page.CouiCopyTextSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiDownloadHistoryScreen
import com.perol.pixez.shared.ui.coui.page.CouiDownloadSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiDownloadTaskScreen
import com.perol.pixez.shared.ui.coui.page.CouiHelloScreen
import com.perol.pixez.shared.ui.coui.page.CouiHistoryScreen
import com.perol.pixez.shared.ui.coui.page.CouiIllustSeriesScreen
import com.perol.pixez.shared.ui.coui.page.CouiInteractionSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiLoginScreen
import com.perol.pixez.shared.ui.coui.page.CouiNetworkSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiNovelScreen
import com.perol.pixez.shared.ui.coui.page.CouiLanguageSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiQualitySettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiRecomUserScreen
import com.perol.pixez.shared.ui.coui.page.CouiShieldScreen
import com.perol.pixez.shared.ui.coui.page.CouiThanksScreen
import com.perol.pixez.shared.ui.coui.page.CouiUserListScreen
import com.perol.pixez.shared.ui.coui.page.CouiUserNovelListScreen
import com.perol.pixez.shared.ui.coui.page.CouiWelcomePageScreen
import com.perol.pixez.shared.ui.coui.page.CouiWidgetRecommendScreen
import com.perol.pixez.shared.ui.coui.page.CouiIllustDetailScreen
import com.perol.pixez.shared.ui.coui.page.CouiNewScreen
import com.perol.pixez.shared.ui.coui.page.CouiNovelViewerScreen
import com.perol.pixez.shared.ui.coui.page.CouiRankingScreen
import com.perol.pixez.shared.ui.coui.page.CouiRelatedIllustsScreen
import com.perol.pixez.shared.ui.coui.page.CouiSearchScreen
import com.perol.pixez.shared.ui.coui.page.CouiSettingsScreen
import com.perol.pixez.shared.ui.coui.page.CouiSpotlightScreen
import com.perol.pixez.shared.ui.coui.page.CouiUserDetailScreen
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.itos.coui.theme.CouiTheme as CouiKitTheme

actual val isCouiSkinSupported: Boolean = true

@Composable
actual fun CouiSkinHost(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val colors = MiuixTheme.colorScheme
    val textStyles = MiuixTheme.textStyles
    val dark = colors.surface.luminance() < 0.5f
    val colorScheme = remember(colors, dark) { couiColorSchemeOf(colors, dark) }
    val typography = remember(textStyles) {
        CouiTypography(
            body = textStyles.body1,
            label = textStyles.footnote1,
            menuItem = textStyles.body2,
            chipLabelFontFamily = textStyles.body2.fontFamily,
            dialogTitleFontFamily = textStyles.title4.fontFamily,
            dialogMessageFontFamily = textStyles.body2.fontFamily,
            dialogButtonFontFamily = textStyles.body1.fontFamily,
            dialogInputFontFamily = textStyles.body1.fontFamily,
            dialogCounterFontFamily = textStyles.footnote1.fontFamily,
        )
    }
    CouiKitTheme(
        darkTheme = dark,
        colorScheme = colorScheme,
        typography = typography,
        materialTint = Color.Transparent,
        content = content,
    )
}

/** ColorOS 色板由 MIUIX 主题派生，保证两套皮肤共用同一份颜色真源。 */
private fun couiColorSchemeOf(colors: Colors, isDark: Boolean): CouiColorScheme =
    CouiColorScheme(
        accent = colors.primary,
        onAccent = colors.onPrimary,
        surface = colors.surfaceContainer,
        container = if (isDark) {
            colors.surfaceContainerHighest
        } else {
            lerp(colors.surface, colors.surfaceContainerHigh, 0.55f)
        },
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.onSurfaceVariantSummary,
        outlineVariant = if (isDark) colors.dividerLine else colors.dividerLine.copy(alpha = 0.7f),
        accentOnSurface = if (isDark) colors.onPrimaryVariant else colors.primary,
        menuSurface = colors.surfaceContainer,
        checkboxAccent = colors.primary,
        menuCheckboxAccent = if (isDark) colors.onPrimaryVariant else colors.primary,
        background = colors.surface,
        card = colors.surfaceContainer,
    )

@Composable
internal actual fun CouiMainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    if (Build.VERSION.SDK_INT >= 34) {
        CouiLibraryNavigationBar(items, selectedIndex, onSelect, modifier)
    } else {
        CouiPlainNavigationBar(items, selectedIndex, onSelect, modifier)
    }
}

/**
 * COUI 官方浮动底栏：API 34+ 可用（库内部依赖新版窗口特性）。
 * 官方条目不带徽标槽，未读红点在该分支不展示，由低版本平替与 MIUIX 皮肤承载。
 */
@RequiresApi(34)
@Composable
private fun CouiLibraryNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    val backdrop = rememberCouiBackdrop()
    val renderer = remember { CouiFloatingRenderer() }
    NavigationBar(
        modifier = modifier,
        backdrop = backdrop,
        renderer = renderer,
        items = items.map { CouiNavigationItem(title = it.title, icon = it.icon) },
        selectedIndex = selectedIndex,
        dark = CouiTheme.isDark,
        onSelection = onSelect,
    )
}

/** API 31-33 平替：同版式的浮动胶囊，不依赖库的窗口特性。 */
@Composable
private fun CouiPlainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val dark = CouiTheme.isDark
    val selectedColor = if (dark) Color.White else Color.Black
    val unselectedColor = selectedColor.copy(alpha = 0.55f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(scheme.container)
            .height(64.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val pressInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .couiPressFeedback(pressInteraction)
                    .selectable(
                        selected = selected,
                        interactionSource = pressInteraction,
                        indication = null,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Image(
                            painter = rememberVectorPainter(item.icon),
                            contentDescription = item.title,
                            colorFilter = ColorFilter.tint(if (selected) selectedColor else unselectedColor),
                            modifier = Modifier.size(24.dp),
                        )
                        if (item.showBadge) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(scheme.accent),
                            )
                        }
                    }
                    Text(
                        text = item.title,
                        kind = CouiTextKind.ItemSummary,
                        color = if (selected) selectedColor else unselectedColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal actual fun CouiMainTabPage(
    tab: RootComponent.MainTab,
    component: RootComponent,
    feedRestore: MainFeedRestore,
    fallback: @Composable () -> Unit,
) {
    when (tab) {
        RootComponent.MainTab.Hello -> {
            val reselectFlow = remember(tab) { component.reselectFlowOf(tab) }
            CouiHelloScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
                onSettingsClick = component::onSettingsClicked,
                onLoginClick = component::onLoginClicked,
                onRecomUserClick = component::onRecomUserListClicked,
                feedRestore = feedRestore.hello,
                reselectFlow = reselectFlow,
            )
        }
        RootComponent.MainTab.Ranking -> {
            CouiRankingScreen(
                onIllustClick = component::onIllustClicked,
                feedRestore = feedRestore.ranking,
            )
        }
        RootComponent.MainTab.Spotlight -> {
            CouiSpotlightScreen(
                onArticleClick = component::onSpotlightArticleClicked,
            )
        }
        RootComponent.MainTab.New -> {
            val reselectFlow = remember(tab) { component.reselectFlowOf(tab) }
            CouiNewScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
                onLoginClick = component::onLoginClicked,
                onSeriesClick = component::onIllustSeriesClicked,
                reselectFlow = reselectFlow,
            )
        }
        RootComponent.MainTab.Search -> {
            CouiSearchScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
                onNovelClick = component::onNovelClicked,
            )
        }
        // 其余标签尚未迁移：走 MIUIX 回退，功能与既有页面完全一致。
        else -> fallback()
    }
}

@Composable
internal actual fun CouiIllustDetailPage(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)?,
    onNovelClick: ((Long) -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    CouiIllustDetailScreen(
        illustId = illustId,
        onBack = onBack,
        onUserClick = onUserClick,
        onCommentsClick = onCommentsClick,
        onRelatedIllustsClick = onRelatedIllustsClick,
        onIllustSeriesClick = onIllustSeriesClick,
        onTagClick = onTagClick,
        onIllustClick = onIllustClick,
        onNovelClick = onNovelClick,
    )
}

@Composable
internal actual fun CouiUserDetailPage(
    userId: Long,
    pageState: UserDetailPageState,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onFollowListClick: (Long) -> Unit,
    onFollowerListClick: (Long) -> Unit,
    onRelatedUsersClick: ((Long) -> Unit)?,
    onUserNovelsClick: ((Long, String) -> Unit)?,
    initialTab: Int,
    fallback: @Composable () -> Unit,
) {
    CouiUserDetailScreen(
        userId = userId,
        pageState = pageState,
        onBack = onBack,
        onIllustClick = onIllustClick,
        onFollowListClick = onFollowListClick,
        onFollowerListClick = onFollowerListClick,
        onRelatedUsersClick = onRelatedUsersClick,
        onUserNovelsClick = onUserNovelsClick,
        initialTab = initialTab,
    )
}

@Composable
internal actual fun CouiNovelViewerPage(
    novelId: Long,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiNovelViewerScreen(
        novelId = novelId,
        onBack = onBack,
        onNovelClick = onNovelClick,
        onIllustClick = onIllustClick,
        onUserClick = onUserClick,
        onCommentsClick = onCommentsClick,
    )
}

@Composable
internal actual fun CouiCommentsPage(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    isNovel: Boolean,
    fallback: @Composable () -> Unit,
) {
    CouiCommentsScreen(
        illustId = illustId,
        onBack = onBack,
        onUserClick = onUserClick,
        isNovel = isNovel,
    )
}

@Composable
internal actual fun CouiRelatedIllustsPage(
    illustId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiRelatedIllustsScreen(
        illustId = illustId,
        onBack = onBack,
        onIllustClick = onIllustClick,
    )
}

@Composable
internal actual fun CouiDownloadHistoryPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiDownloadHistoryScreen(
        onBack = onBack,
        onIllustClick = onIllustClick,
    )
}

@Composable
internal actual fun CouiAccountEditPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiAccountEditScreen(onBack = onBack)
}

@Composable
internal actual fun CouiLoginPage(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNetworkSettingClick: (() -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    CouiLoginScreen(
        onBack = onBack,
        onLoginSuccess = onLoginSuccess,
        onNetworkSettingClick = onNetworkSettingClick,
    )
}

@Composable
internal actual fun CouiIllustSeriesPage(
    seriesId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiIllustSeriesScreen(
        seriesId = seriesId,
        onBack = onBack,
        onIllustClick = onIllustClick,
    )
}

@Composable
internal actual fun CouiUserNovelListPage(
    userId: Long,
    authorName: String,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    CouiUserNovelListScreen(
        userId = userId,
        authorName = authorName,
        onBack = onBack,
        onNovelClick = onNovelClick,
        onUserClick = onUserClick,
    )
}

@Composable
internal actual fun CouiUserListPage(
    kind: CouiUserListKind,
    userId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiUserListScreen(
        kind = kind,
        userId = userId,
        onBack = onBack,
        onUserClick = onUserClick,
    )
}

@Composable
internal actual fun CouiDownloadTaskPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiDownloadTaskScreen(
        onBack = onBack,
        onIllustClick = onIllustClick,
    )
}

@Composable
internal actual fun CouiHistoryPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiHistoryScreen(
        onBack = onBack,
        onIllustClick = onIllustClick,
        onNovelClick = onNovelClick,
    )
}

@Composable
internal actual fun CouiBoardPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiBoardScreen(onBack = onBack)
}

@Composable
internal actual fun CouiRecomUserPage(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiRecomUserScreen(
        onBack = onBack,
        onUserClick = onUserClick,
    )
}

@Composable
internal actual fun CouiShieldPage(
    onBack: () -> Unit,
    onAISettingClick: (showAI: Boolean) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiShieldScreen(
        onBack = onBack,
        onAISettingClick = onAISettingClick,
    )
}

@Composable
internal actual fun CouiAiSettingPage(
    showAI: Boolean,
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiAiSettingScreen(
        showAI = showAI,
        onBack = onBack,
    )
}

@Composable
internal actual fun CouiWelcomePageSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiWelcomePageScreen(onBack = onBack)
}

@Composable
internal actual fun CouiWidgetRecommendPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiWidgetRecommendScreen(onBack = onBack)
}

@Composable
internal actual fun CouiInteractionSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiInteractionSettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiQualitySettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiQualitySettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiLanguageSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiLanguageSettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiCopyTextSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiCopyTextSettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiDownloadSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiDownloadSettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiNetworkSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiNetworkSettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiAboutPage(
    onBack: () -> Unit,
    onThanksClick: () -> Unit,
    onUpdateClick: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiAboutScreen(
        onBack = onBack,
        onThanksClick = onThanksClick,
        onUpdateClick = onUpdateClick,
    )
}

@Composable
internal actual fun CouiThanksPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiThanksScreen(onBack = onBack)
}

@Composable
internal actual fun CouiBookTagPage(
    onBack: () -> Unit,
    onTagSearch: (String) -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiBookTagScreen(
        onBack = onBack,
        onTagSearch = onTagSearch,
    )
}

@Composable
internal actual fun CouiThemeSettingPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiThemeSettingScreen(onBack = onBack)
}

@Composable
internal actual fun CouiAccountManagePage(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiAccountManageScreen(
        onBack = onBack,
        onAddAccount = onAddAccount,
    )
}

@Composable
internal actual fun CouiNovelPage(
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    CouiNovelScreen(
        onBack = onBack,
        onNovelClick = onNovelClick,
        onUserClick = onUserClick,
    )
}

@Composable
internal actual fun CouiSettingsPage(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    onShieldClick: () -> Unit,
    onLoginClick: () -> Unit,
    onDownloadHistoryClick: () -> Unit,
    onThemeSettingClick: () -> Unit,
    onNetworkSettingClick: () -> Unit,
    onDownloadSettingClick: () -> Unit,
    onLayoutSettingClick: () -> Unit,
    onLanguageSettingClick: () -> Unit,
    onWidgetRecommendSettingClick: () -> Unit,
    onInteractionSettingClick: () -> Unit,
    onQualitySettingClick: () -> Unit,
    onCopyTextSettingClick: () -> Unit,
    onWelcomePageSettingClick: () -> Unit,
    onBookTagClick: () -> Unit,
    onUpdateSettingClick: () -> Unit,
    onAccountEditClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDownloadTaskClick: () -> Unit,
    onDataExportClick: () -> Unit,
    onBoardClick: () -> Unit,
    onGuideClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    onUserBookmarksClick: (Long) -> Unit,
    onAccountManageClick: () -> Unit,
    onNovelBrowseClick: () -> Unit,
    fallback: @Composable () -> Unit,
) {
    CouiSettingsScreen(
        onBack = onBack,
        onAboutClick = onAboutClick,
        onShieldClick = onShieldClick,
        onLoginClick = onLoginClick,
        onDownloadHistoryClick = onDownloadHistoryClick,
        onThemeSettingClick = onThemeSettingClick,
        onNetworkSettingClick = onNetworkSettingClick,
        onDownloadSettingClick = onDownloadSettingClick,
        onLayoutSettingClick = onLayoutSettingClick,
        onLanguageSettingClick = onLanguageSettingClick,
        onWidgetRecommendSettingClick = onWidgetRecommendSettingClick,
        onInteractionSettingClick = onInteractionSettingClick,
        onQualitySettingClick = onQualitySettingClick,
        onCopyTextSettingClick = onCopyTextSettingClick,
        onWelcomePageSettingClick = onWelcomePageSettingClick,
        onBookTagClick = onBookTagClick,
        onUpdateSettingClick = onUpdateSettingClick,
        onAccountEditClick = onAccountEditClick,
        onHistoryClick = onHistoryClick,
        onDownloadTaskClick = onDownloadTaskClick,
        onDataExportClick = onDataExportClick,
        onBoardClick = onBoardClick,
        onGuideClick = onGuideClick,
        onUserClick = onUserClick,
        onUserBookmarksClick = onUserBookmarksClick,
        onAccountManageClick = onAccountManageClick,
        onNovelBrowseClick = onNovelBrowseClick,
    )
}
