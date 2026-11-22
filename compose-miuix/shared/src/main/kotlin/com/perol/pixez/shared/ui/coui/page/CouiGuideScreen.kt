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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.R
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiCompactScaffold
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiItemTitle
import com.perol.pixez.shared.ui.coui.core.CouiRadioOptionRow
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/** 引导页分步切换的转场时长（毫秒）。 */
private const val COUI_GUIDE_PAGE_TRANSITION_MILLIS = 300

/** 引导页总步数。 */
private const val COUI_GUIDE_STEP_COUNT = 3

/**
 * COUI 首次启动引导页：库骨架 + 三步内容 + 底部步骤导航。
 *
 * 三步的写入项（语言、图源、网络模式、完成标记）与 MIUIX 版完全一致；
 * 第三屏保留账号状态检测、登录入口与游客模式。
 * 页面只保留一个库 Scaffold 容器：顶栏由骨架提供，底部步骤导航与正文同处骨架正文，
 * 各步列表的顶部留白来自骨架下发的 padding，滚动时内容会进入顶栏覆盖区。
 *
 * 骨架选 [CouiCompactScaffold] 而非二级页骨架：core 里只有它的 onBack 可空
 * （`CouiSecondaryScaffold` 的 onBack 非空，`CouiMainScaffold` 固定无返回），
 * 才能做到"第一步不显示返回箭头"。同时它让 AnimatedContent 始终挂在同一个调用点上，
 * 0↔1 步切换不会因为骨架换型而重建子树、丢掉过场动画。
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

    val finishGuide: () -> Unit = {
        settingsRepository.hasCompletedGuide = true
        onFinish()
    }

    val stepTitle = when (currentStep) {
        0 -> strings.guideStepLanguage
        1 -> strings.guideStepNetwork
        else -> strings.guideStepWelcome
    }

    CouiCompactScaffold(
        title = stepTitle,
        // 第一步没有上一页：onBack 传 null，库骨架据此不渲染返回箭头。
        onBack = if (currentStep > 0) ({ currentStep-- }) else null,
    ) { padding ->
        val stepPadding = PaddingValues(
            top = padding.calculateTopPadding() + 4.dp,
            bottom = 16.dp,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        // 前进向左滑入、旧页向左滑出；后退反向，方向按步号大小自适应。
                        val direction = if (targetState > initialState) 1 else -1
                        val enter = slideInHorizontally(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)) { width ->
                            width * direction
                        } + fadeIn(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS))
                        val exit = slideOutHorizontally(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS)) { width ->
                            -width * direction
                        } + fadeOut(tween(COUI_GUIDE_PAGE_TRANSITION_MILLIS))
                        enter.togetherWith(exit)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { step ->
                    when (step) {
                        0 -> CouiGuideLanguageStep(
                            settingsRepository = settingsRepository,
                            contentPadding = stepPadding,
                        )

                        1 -> CouiGuideNetworkStep(
                            settingsRepository = settingsRepository,
                            contentPadding = stepPadding,
                        )

                        else -> CouiGuideWelcomeStep(
                            accountRepository = accountRepository,
                            onLoginClick = onLoginClick,
                            onFinish = finishGuide,
                            contentPadding = stepPadding,
                        )
                    }
                }

                CouiGuideBottomBar(
                    currentStep = currentStep,
                    totalSteps = COUI_GUIDE_STEP_COUNT,
                    onSkip = finishGuide,
                    onNext = {
                        if (currentStep < COUI_GUIDE_STEP_COUNT - 1) {
                            currentStep++
                        } else {
                            finishGuide()
                        }
                    },
                    modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                )
            }
        }
    }
}

/** 第一步：欢迎 hero 与界面语言。 */
@Composable
private fun CouiGuideLanguageStep(
    settingsRepository: SettingsRepository,
    contentPadding: PaddingValues,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    var selectedIndex by remember {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
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
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_pixez_logo),
                        contentDescription = strings.logoContentDescription,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Text(
                    text = strings.guideHeroWelcome,
                    kind = CouiTextKind.TopBar,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp, start = 20.dp, end = 20.dp),
                )
                Text(
                    text = strings.guideHeroWelcomeSub,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp),
                )
            }
        }

        item(key = "coui_guide_language") {
            CouiSettingsGroup(title = strings.settingLanguage) {
                LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                    if (index > 0) {
                        CouiRowDivider()
                    }
                    CouiRadioOptionRow(
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

/** 第二步：图片源与网络模式。 */
@Composable
private fun CouiGuideNetworkStep(
    settingsRepository: SettingsRepository,
    contentPadding: PaddingValues,
) {
    val strings = LocalStrings.current
    var isMirrorEnabled by remember {
        mutableStateOf(settingsRepository.pictureSource == PixivHosts.HOST_PIXIV_RE)
    }
    var currentNetworkMode by remember {
        mutableStateOf(settingsRepository.apiNetworkMode)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_guide_network_title") {
            CouiGuideStepHeader(
                title = strings.guideStepNetwork,
                description = strings.guideStepNetworkDesc,
            )
        }

        item(key = "coui_guide_image_source") {
            CouiSettingsGroup(title = strings.pictureSource) {
                CouiRadioOptionRow(
                    title = strings.guideImageMirror,
                    summary = "${strings.guideImageMirrorDesc} • ${strings.guideMirrorRecommendDirect}",
                    selected = isMirrorEnabled,
                    onClick = {
                        isMirrorEnabled = true
                        settingsRepository.pictureSource = PixivHosts.HOST_PIXIV_RE
                    },
                )
                CouiRowDivider()
                CouiRadioOptionRow(
                    title = strings.guideImageOfficial,
                    summary = "${strings.guideImageOfficialDesc} • ${strings.guideOfficialNeedProxy}",
                    selected = !isMirrorEnabled,
                    onClick = {
                        isMirrorEnabled = false
                        settingsRepository.pictureSource = PixivHosts.HOST_PXIMG
                    },
                )
            }
        }

        item(key = "coui_guide_network_mode") {
            CouiSettingsGroup(title = strings.apiNetworkMode) {
                CouiRadioOptionRow(
                    title = strings.guideNetworkDirect,
                    summary = strings.guideNetworkDirectDesc,
                    selected = currentNetworkMode == "direct",
                    onClick = {
                        currentNetworkMode = "direct"
                        settingsRepository.apiNetworkMode = "direct"
                        settingsRepository.oauthNetworkMode = "direct"
                    },
                )
                CouiRowDivider()
                CouiRadioOptionRow(
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

        item(key = "coui_guide_network_tip") {
            CouiSettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CouiItemTitle(text = strings.loginTroubleTip)
                    CouiItemSummary(text = strings.loginTroubleDesc)
                }
            }
        }
    }
}

/** 第三步：特性说明与账号状态。 */
@Composable
private fun CouiGuideWelcomeStep(
    accountRepository: AccountRepository,
    onLoginClick: () -> Unit,
    onFinish: () -> Unit,
    contentPadding: PaddingValues,
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
        contentPadding = contentPadding,
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_guide_welcome_title") {
            CouiGuideStepHeader(
                title = strings.guideStepWelcome,
                description = strings.guideStepWelcomeDesc,
            )
        }

        item(key = "coui_guide_features") {
            CouiSettingsGroup(title = strings.guidePixEzFeatures) {
                CouiGuideFeatureRow(strings.guideFeatureIllust, strings.guideFeatureIllustDesc)
                CouiRowDivider()
                CouiGuideFeatureRow(strings.guideFeatureRanking, strings.guideFeatureRankingDesc)
                CouiRowDivider()
                CouiGuideFeatureRow(strings.guideFeatureDownload, strings.guideFeatureDownloadDesc)
            }
        }

        item(key = "coui_guide_account") {
            CouiSettingsGroup(title = strings.settingSectionAccount) {
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
                            text = current.name + " (@" + current.account + ")",
                            kind = CouiTextKind.ItemSummary,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        CouiTextButton(
                            text = strings.guideSwitchAccount,
                            onClick = onLoginClick,
                            modifier = Modifier.padding(top = 10.dp),
                            filled = false,
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
                        CouiTextButton(
                            text = strings.guideLoginNow,
                            onClick = onLoginClick,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        CouiTextButton(
                            text = strings.guideGuestExplore,
                            onClick = onFinish,
                            modifier = Modifier.padding(top = 6.dp),
                            filled = false,
                        )
                    }
                }
            }
        }
    }
}

/** 步骤标题与说明：三步共用同一套居中版式。 */
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
            textAlign = TextAlign.Center,
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

/** 特性行：标题 + 说明，版式对齐官方 Item 的行内边距。 */
@Composable
private fun CouiGuideFeatureRow(
    title: String,
    summary: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CouiItemTitle(text = title)
        CouiItemSummary(text = summary)
    }
}

/**
 * 底部步骤导航：单行三槽，左右两槽等宽占位，中间胶囊指示器因此绝对居中。
 *
 * 左槽跳过（末步留空）、右槽主操作；返回上一页只走顶栏返回箭头，底栏不再提供"上一步"。
 */
@Composable
private fun CouiGuideBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val isLastStep = currentStep == totalSteps - 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (!isLastStep) {
                CouiTextButton(
                    text = strings.guideSkipLogin,
                    onClick = onSkip,
                    filled = false,
                )
            }
        }

        CouiGuideStepIndicator(
            currentStep = currentStep,
            totalSteps = totalSteps,
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            CouiTextButton(
                text = if (isLastStep) strings.guideFinish else strings.guideNext,
                onClick = onNext,
                filled = true,
            )
        }
    }
}

/** 胶囊步骤指示器：当前页拉长，切步时走弹簧动画。 */
@Composable
private fun CouiGuideStepIndicator(
    currentStep: Int,
    totalSteps: Int,
) {
    val scheme = CouiTheme.colorScheme
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
}
