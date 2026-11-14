package com.perol.pixez.shared.ui.coui.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/** 引导页分步切换的转场时长（毫秒）。 */
private const val COUI_GUIDE_PAGE_TRANSITION_MILLIS = 300

/**
 * COUI 首次启动引导页：固定顶栏 + 三步内容 + 底部步骤导航。
 *
 * 三步的写入项（语言、图源、网络模式、完成标记）与 MIUIX 版完全一致；
 * 第三步保留账号状态检测、登录入口、游客模式与开始旅程。
 */
@Composable
internal fun CouiGuideScreen(
    onLoginClick: () -> Unit,
    onFinish: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    var currentStep by remember { mutableIntStateOf(0) }
    val scheme = CouiTheme.colorScheme

    val finishGuide: () -> Unit = {
        settingsRepository.hasCompletedGuide = true
        onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (currentStep > 0) {
                CouiActionButton(
                    icon = MiuixIcons.Back,
                    contentDescription = strings.back,
                    onClick = { currentStep-- },
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
            Text(
                text = when (currentStep) {
                    0 -> "1. ${strings.guideStepLanguage}"
                    1 -> "2. ${strings.guideStepNetwork}"
                    else -> "3. ${strings.guideStepWelcome}"
                },
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CouiTextAction(
                text = strings.guideSkipLogin,
                onClick = finishGuide,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)) { width -> width } +
                            fadeIn(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)))
                            .togetherWith(
                                slideOutHorizontally(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)) { width -> -width } +
                                    fadeOut(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)),
                            )
                    } else {
                        (slideInHorizontally(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)) { width -> -width } +
                            fadeIn(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)))
                            .togetherWith(
                                slideOutHorizontally(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)) { width -> width } +
                                    fadeOut(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)),
                            )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { step ->
                CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
                    when (step) {
                        0 -> CouiGuideLanguageStep(settingsRepository = settingsRepository)
                        1 -> CouiGuideNetworkStep(settingsRepository = settingsRepository)
                        else -> CouiGuideWelcomeStep(
                            accountRepository = accountRepository,
                            onLoginClick = onLoginClick,
                            onFinish = finishGuide,
                        )
                    }
                }
            }
        }

        CouiGuideBottomBar(
            currentStep = currentStep,
            totalSteps = 3,
            onPrevious = { if (currentStep > 0) currentStep-- },
            onNext = {
                if (currentStep < 2) {
                    currentStep++
                } else {
                    finishGuide()
                }
            },
        )
    }
}

/** 第一步：欢迎语与界面语言。 */
@Composable
private fun CouiGuideLanguageStep(
    settingsRepository: SettingsRepository,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    var selectedIndex by remember {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_guide_hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_pixez_logo),
                        contentDescription = strings.logoContentDescription,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Text(
                    text = strings.guideHeroWelcome,
                    kind = CouiTextKind.GroupTitle,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = strings.guideHeroWelcomeSub,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
                )
            }
        }

        item(key = "coui_guide_language") {
            CouiSettingSection(title = strings.settingLanguage) {
                Column {
                    LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                        CouiRadioRow(
                            title = "${option.nativeName} (${option.displayName})",
                            summary = option.code,
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = index
                                settingsRepository.languageNum = index
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 第二步：图片源与网络模式。 */
@Composable
private fun CouiGuideNetworkStep(
    settingsRepository: SettingsRepository,
) {
    val strings = LocalStrings.current
    var isMirrorEnabled by remember {
        mutableStateOf(settingsRepository.pictureSource == AppConstants.Network.HOST_PIXIV_RE)
    }
    var currentNetworkMode by remember {
        mutableStateOf(settingsRepository.apiNetworkMode)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_guide_network_title") {
            CouiGuideStepHeader(
                title = strings.guideStepNetwork,
                description = strings.guideStepNetworkDesc,
            )
        }

        item(key = "coui_guide_image_source") {
            CouiSettingSection(title = strings.guideImageSource) {
                Column {
                    CouiRadioRow(
                        title = strings.guideImageMirror,
                        summary = "${strings.guideImageMirrorDesc} • ${strings.guideMirrorRecommendDirect}",
                        selected = isMirrorEnabled,
                        onClick = {
                            isMirrorEnabled = true
                            settingsRepository.pictureSource = AppConstants.Network.HOST_PIXIV_RE
                        },
                    )
                    CouiRadioRow(
                        title = strings.guideImageOfficial,
                        summary = "${strings.guideImageOfficialDesc} • ${strings.guideOfficialNeedProxy}",
                        selected = !isMirrorEnabled,
                        onClick = {
                            isMirrorEnabled = false
                            settingsRepository.pictureSource = AppConstants.Network.HOST_PXIMG
                        },
                    )
                }
            }
        }

        item(key = "coui_guide_network_mode") {
            CouiSettingSection(title = strings.guideStepNetwork) {
                Column {
                    CouiRadioRow(
                        title = strings.guideNetworkDirect,
                        summary = strings.guideNetworkDirectDesc,
                        selected = currentNetworkMode == "direct",
                        onClick = {
                            currentNetworkMode = "direct"
                            settingsRepository.apiNetworkMode = "direct"
                            settingsRepository.oauthNetworkMode = "direct"
                        },
                    )
                    CouiRadioRow(
                        title = strings.guideNetworkSni,
                        summary = "${strings.guideNetworkSniDesc} • ${strings.guideSniRecommendCn}",
                        selected = currentNetworkMode == "sni",
                        onClick = {
                            currentNetworkMode = "sni"
                            settingsRepository.apiNetworkMode = "sni"
                            settingsRepository.oauthNetworkMode = "sni"
                        },
                    )
                }
            }
        }

        item(key = "coui_guide_network_tip") {
            CouiCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.loginTroubleTip,
                        kind = CouiTextKind.ItemTitle,
                        color = CouiTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = strings.loginTroubleDesc,
                        kind = CouiTextKind.ItemSummary,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** 第三步：特性说明、账号状态与完成入口。 */
@Composable
private fun CouiGuideWelcomeStep(
    accountRepository: AccountRepository,
    onLoginClick: () -> Unit,
    onFinish: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var isCheckingAccount by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        suspendRunCatchingNonCancel {
            accountRepository.currentAccount()
        }.onSuccess {
            currentAccount = it
            isCheckingAccount = false
        }.onFailure {
            isCheckingAccount = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_guide_welcome_title") {
            CouiGuideStepHeader(
                title = strings.guideStepWelcome,
                description = strings.guideStepWelcomeDesc,
            )
        }

        item(key = "coui_guide_features") {
            CouiSettingSection(title = strings.guidePixEzFeatures) {
                Column {
                    CouiGuideFeatureRow(strings.guideFeatureIllust, strings.guideFeatureIllustDesc)
                    CouiGuideFeatureRow(strings.guideFeatureRanking, strings.guideFeatureRankingDesc)
                    CouiGuideFeatureRow(strings.guideFeatureDownload, strings.guideFeatureDownloadDesc)
                }
            }
        }

        item(key = "coui_guide_account") {
            CouiSettingSection(title = strings.settingSectionAccount) {
                val current = currentAccount
                when {
                    current != null && current.hasCredentials -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = strings.guideLoggedInStatus,
                            kind = CouiTextKind.ItemTitle,
                            color = scheme.accent,
                        )
                        Text(
                            text = "${current.name} (@${current.account})",
                            kind = CouiTextKind.ItemSummary,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        CouiTextAction(
                            text = strings.guideSwitchAccount,
                            color = scheme.accent,
                            onClick = onLoginClick,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    !isCheckingAccount -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = strings.guideNotLoggedIn,
                            kind = CouiTextKind.ItemTitle,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = strings.guideLoginBenefits,
                            kind = CouiTextKind.ItemSummary,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        CouiGuideButton(
                            text = strings.guideLoginNow,
                            onClick = {
                                onLoginClick()
                            },
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        CouiTextAction(
                            text = strings.guideGuestExplore,
                            onClick = onFinish,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item(key = "coui_guide_start") {
            CouiGuideButton(
                text = strings.guideStartJourney,
                onClick = onFinish,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

/** 步骤标题与说明。 */
@Composable
private fun CouiGuideStepHeader(
    title: String,
    description: String,
) {
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            kind = CouiTextKind.GroupTitle,
            color = scheme.onSurface,
        )
        Text(
            text = description,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
        )
    }
}

/** 特性行：标题 + 说明。 */
@Composable
private fun CouiGuideFeatureRow(
    title: String,
    summary: String,
) {
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = scheme.onSurface,
        )
        Text(
            text = summary,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** COUI 主按钮：实心强调色，按压反馈与登录页同口径。 */
@Composable
private fun CouiGuideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.accent)
            .couiPressFeedback(interactionSource, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = scheme.onAccent,
        )
    }
}

/** 底部步骤导航：伸缩胶囊指示器 + 上一步/下一步/完成。 */
@Composable
private fun CouiGuideBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentStep > 0) {
            CouiTextAction(
                text = strings.guidePrev,
                onClick = onPrevious,
            )
        } else {
            Spacer(modifier = Modifier.width(64.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalSteps) { step ->
                val isActive = step == currentStep
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isActive) 24.dp else 8.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(indicatorWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) scheme.accent else scheme.container),
                )
            }
        }

        CouiTextAction(
            text = if (currentStep == totalSteps - 1) strings.guideFinish else strings.guideNext,
            color = scheme.accent,
            onClick = onNext,
        )
    }
}
