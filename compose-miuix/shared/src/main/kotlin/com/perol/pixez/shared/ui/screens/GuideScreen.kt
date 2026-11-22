package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.SquircleShape
import com.perol.pixez.shared.ui.i18n.LANGUAGE_OPTIONS
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import androidx.compose.ui.res.painterResource
import com.perol.pixez.shared.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 引导页分步切换的转场时长（毫秒），四段位移动画与两段淡入淡出共用。 */
private const val GUIDE_PAGE_TRANSITION_MILLIS = 300

/** 引导页固定三步：语言 → 网络 → 完成/账号。顶栏标题、胶囊指示器与末步判定共用该常量。 */
private const val GUIDE_TOTAL_STEPS = 3

/** Hero 品牌图标底座的超椭圆圆角，按 72dp 图标尺度对齐 MIUIX 视觉比例。 */
private val GUIDE_HERO_LOGO_CORNER_RADIUS = 22.dp

/**
 * 首次启动引导向导页（Onboarding Guide），三步固定信息架构：
 * 语言与界面偏好（品牌 Hero 欢迎区、即时语言切换）
 * 网络模式与加速源配置（镜像源直连免翻推荐、SNI 绕过与代理提示）
 * 完成与账号（特性高光卡片、登录状态同步、游客体验模式）
 *
 * 返回能力只保留顶栏一处（step > 0 时出现），底栏为单行三槽：左跳过、中步骤指示器、右主操作。
 */
@Composable
fun GuideScreen(
    onLoginClick: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val settingsRepository = LocalSettingsRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    var currentStep by remember { mutableIntStateOf(0) }
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    val finishGuide: () -> Unit = {
        settingsRepository.hasCompletedGuide = true
        onFinish()
    }

    BlurTopBarScaffold(
        modifier = modifier.fillMaxSize(),
        title = when (currentStep) {
            0 -> strings.guideStepLanguage
            1 -> strings.guideStepNetwork
            else -> strings.guideStepWelcome
        },
        backdrop = backdrop,
        // 引导页内容为分步 AnimatedContent 中的 LazyColumn，未接 nestedScroll，故传 null 关闭滚动联动。
        scrollBehavior = null,
        onBack = if (currentStep > 0) ({ currentStep-- }) else null,
        bottomBar = {
            GuideBottomBar(
                currentStep = currentStep,
                totalSteps = GUIDE_TOTAL_STEPS,
                onSkip = finishGuide,
                onNext = {
                    if (currentStep < GUIDE_TOTAL_STEPS - 1) {
                        currentStep++
                    } else {
                        finishGuide()
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    // 前进向左推、后退向右推；步骤号为 Int，可直接比较确定方向。
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(GUIDE_PAGE_TRANSITION_MILLIS)) { width -> width } + fadeIn(tween(GUIDE_PAGE_TRANSITION_MILLIS)))
                            .togetherWith(slideOutHorizontally(tween(GUIDE_PAGE_TRANSITION_MILLIS)) { width -> -width } + fadeOut(tween(GUIDE_PAGE_TRANSITION_MILLIS)))
                    } else {
                        (slideInHorizontally(tween(GUIDE_PAGE_TRANSITION_MILLIS)) { width -> -width } + fadeIn(tween(GUIDE_PAGE_TRANSITION_MILLIS)))
                            .togetherWith(slideOutHorizontally(tween(GUIDE_PAGE_TRANSITION_MILLIS)) { width -> width } + fadeOut(tween(GUIDE_PAGE_TRANSITION_MILLIS)))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) { step ->
                when (step) {
                    0 -> GuideLanguageStep(settingsRepository = settingsRepository)
                    1 -> GuideNetworkStep(settingsRepository = settingsRepository)
                    2 -> GuideWelcomeStep(
                        accountRepository = accountRepository,
                        onLoginClick = onLoginClick,
                        onFinish = finishGuide,
                    )
                }
            }
        }
    }
}

/**
 * 第一步：品牌 Hero 欢迎区与语言选择（hero 变体头部，无额外步标题）
 */
@Composable
private fun GuideLanguageStep(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var selectedIndex by remember {
        mutableIntStateOf(settingsRepository.languageNum.coerceIn(0, LANGUAGE_OPTIONS.size - 1))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Hero 图标底座采用连续曲率超椭圆，与 MIUIX 图标造型一致。
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(SquircleShape(GUIDE_HERO_LOGO_CORNER_RADIUS))
                            .background(MiuixTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_pixez_logo),
                            contentDescription = strings.logoContentDescription,
                            modifier = Modifier.size(56.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.guideHeroWelcome,
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = strings.guideHeroWelcomeSub,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            item {
                SmallTitle(text = strings.settingLanguage)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                        RadioButtonPreference(
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 第二步：网络与镜像模式配置（步标题 + 说明的居中头部）
 */
@Composable
private fun GuideNetworkStep(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var isMirrorEnabled by remember {
        mutableStateOf(settingsRepository.pictureSource == PixivHosts.HOST_PIXIV_RE)
    }
    var currentNetworkMode by remember {
        mutableStateOf(settingsRepository.apiNetworkMode)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = strings.guideStepNetwork,
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.guideStepNetworkDesc,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            // 分组标题取自设置页既有词条，避免与页标题重复。
            item {
                SmallTitle(text = strings.pictureSource)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    RadioButtonPreference(
                        title = strings.guideImageMirror,
                        summary = "${strings.guideImageMirrorDesc} • ${strings.guideMirrorRecommendDirect}",
                        selected = isMirrorEnabled,
                        onClick = {
                            isMirrorEnabled = true
                            settingsRepository.pictureSource = PixivHosts.HOST_PIXIV_RE
                        },
                    )
                    RadioButtonPreference(
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SmallTitle(text = strings.apiNetworkMode)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    RadioButtonPreference(
                        title = strings.guideNetworkDirect,
                        summary = strings.guideNetworkDirectDesc,
                        selected = currentNetworkMode == "direct",
                        onClick = {
                            currentNetworkMode = "direct"
                            settingsRepository.apiNetworkMode = "direct"
                            settingsRepository.oauthNetworkMode = "direct"
                        },
                    )
                    RadioButtonPreference(
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.loginTroubleTip,
                            style = MiuixTheme.textStyles.title4,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.loginTroubleDesc,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 第三步：登录状态检测与完成欢迎页（头部样式与第二步一致，收束动作交给底栏主按钮）
 */
@Composable
private fun GuideWelcomeStep(
    accountRepository: AccountRepository,
    onLoginClick: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
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

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = strings.guideStepWelcome,
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.guideStepWelcomeDesc,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            // 核心特性高光卡片
            item {
                SmallTitle(text = strings.guidePixEzFeatures)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    BasicComponent(
                        title = strings.guideFeatureIllust,
                        summary = strings.guideFeatureIllustDesc,
                    )
                    BasicComponent(
                        title = strings.guideFeatureRanking,
                        summary = strings.guideFeatureRankingDesc,
                    )
                    BasicComponent(
                        title = strings.guideFeatureDownload,
                        summary = strings.guideFeatureDownloadDesc,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 账号状态卡片
            item {
                SmallTitle(text = strings.settingSectionAccount)
                val current = currentAccount
                if (current != null && current.hasCredentials) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = strings.guideLoggedInStatus,
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${current.name} (@${current.account})",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                text = strings.guideSwitchAccount,
                                onClick = onLoginClick,
                            )
                        }
                    }
                } else if (!isCheckingAccount) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = strings.guideNotLoggedIn,
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.guideLoginBenefits,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                onClick = onLoginClick,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(strings.guideLoginNow)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                text = strings.guideGuestExplore,
                                onClick = onFinish,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 底部向导导航栏：单行三槽布局（左跳过 / 中胶囊步骤指示器 / 右主操作）。
 *
 * 左右两槽等权重占位，使中间指示器在任意文案宽度下保持绝对居中；末步不显示跳过入口，左槽留空而非固定宽度占位。
 */
@Composable
private fun GuideBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (currentStep < totalSteps - 1) {
                TextButton(
                    text = strings.guideSkipLogin,
                    onClick = onSkip,
                )
            }
        }

        // 平滑伸缩胶囊指示器
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalSteps) { step ->
                val isActive = step == currentStep
                val targetWidth = if (isActive) 24.dp else 8.dp
                val indicatorWidth by animateDpAsState(
                    targetValue = targetWidth,
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
                        .background(
                            if (isActive) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.surfaceContainerHighest
                        ),
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onNext,
            ) {
                Text(
                    text = if (currentStep == totalSteps - 1) strings.guideFinish else strings.guideNext,
                )
            }
        }
    }
}
