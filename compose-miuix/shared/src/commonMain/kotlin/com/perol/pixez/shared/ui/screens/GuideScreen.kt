package com.perol.pixez.shared.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.CheckIndicator
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val MIRROR_IMAGE_HOST = "i.pixiv.re"

/**
 * 首次启动引导向导页（Onboarding Guide）：
 * Step 1: 语言与界面偏好（品牌 Hero 欢迎区、即时语言切换、贡献者感谢）
 * Step 2: 网络模式与加速源配置（镜像源直连免翻推荐、SNI 绕过与代理提示）
 * Step 3: 账号与开启旅程（特性高光卡片、登录状态同步、游客体验模式）
 */
@Composable
fun GuideScreen(
    settingsRepository: SettingsRepository,
    accountRepository: AccountRepository,
    onLoginClick: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var currentStep by remember { mutableIntStateOf(0) }

    val finishGuide: () -> Unit = {
        settingsRepository.hasCompletedGuide = true
        onFinish()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = when (currentStep) {
                    0 -> "1. ${strings.guideStepLanguage}"
                    1 -> "2. ${strings.guideStepNetwork}"
                    else -> "3. ${strings.guideStepWelcome}"
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.guidePrev,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        text = strings.guideSkipLogin,
                        onClick = finishGuide,
                    )
                },
            )
        },
        bottomBar = {
            GuideBottomBar(
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
        },
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
                } else {
                    (slideInHorizontally(tween(300)) { width -> -width } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300)))
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

/**
 * Step 1: 欢迎与语言选择
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_pixez_logo),
                            contentDescription = "PixEz Logo",
                            modifier = Modifier.size(56.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.guideHeroWelcome,
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LANGUAGE_OPTIONS.forEachIndexed { index, option ->
                        BasicComponent(
                            title = "${option.nativeName} (${option.displayName})",
                            summary = option.code,
                            onClick = {
                                selectedIndex = index
                                settingsRepository.languageNum = index
                            },
                            endActions = {
                                CheckIndicator(selected = selectedIndex == index)
                            },
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                val selectedOption = LANGUAGE_OPTIONS[selectedIndex]
                if (selectedOption.sponsors.isNotEmpty()) {
                    SmallTitle(text = strings.sponsor)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            selectedOption.sponsors.forEach { sponsor ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    AsyncImage(
                                        model = sponsor.avatar,
                                        contentDescription = sponsor.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                    )
                                    Text(
                                        text = sponsor.name,
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Step 2: 网络与镜像模式配置
 */
@Composable
private fun GuideNetworkStep(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var isMirrorEnabled by remember {
        mutableStateOf(settingsRepository.pictureSource == MIRROR_IMAGE_HOST)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
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

            item {
                SmallTitle(text = strings.guideImageSource)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BasicComponent(
                        title = strings.guideImageMirror,
                        summary = "${strings.guideImageMirrorDesc} • 推荐国内直连",
                        onClick = {
                            isMirrorEnabled = true
                            settingsRepository.pictureSource = MIRROR_IMAGE_HOST
                        },
                        endActions = {
                            CheckIndicator(selected = isMirrorEnabled)
                        },
                    )
                    BasicComponent(
                        title = strings.guideImageOfficial,
                        summary = "${strings.guideImageOfficialDesc} • 需配置网络代理",
                        onClick = {
                            isMirrorEnabled = false
                            settingsRepository.pictureSource = "i.pximg.net"
                        },
                        endActions = {
                            CheckIndicator(selected = !isMirrorEnabled)
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SmallTitle(text = strings.guideStepNetwork)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BasicComponent(
                        title = strings.guideNetworkDirect,
                        summary = strings.guideNetworkDirectDesc,
                        onClick = {
                            currentNetworkMode = "direct"
                            settingsRepository.apiNetworkMode = "direct"
                            settingsRepository.oauthNetworkMode = "direct"
                        },
                        endActions = {
                            CheckIndicator(selected = currentNetworkMode == "direct")
                        },
                    )
                    BasicComponent(
                        title = strings.guideNetworkSni,
                        summary = "${strings.guideNetworkSniDesc} • 推荐国内网络开启",
                        onClick = {
                            currentNetworkMode = "sni"
                            settingsRepository.apiNetworkMode = "sni"
                            settingsRepository.oauthNetworkMode = "sni"
                        },
                        endActions = {
                            CheckIndicator(selected = currentNetworkMode == "sni")
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
 * Step 3: 登录状态检测、特性展示与完成欢迎页
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
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
                        text = "✨ ${strings.guideStepWelcome}",
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.guideStepWelcomeDesc,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }

            // 核心特性高光卡片
            item {
                SmallTitle(text = "PixEz 特性")
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                if (current != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(0.9f),
                ) {
                    Text(
                        text = strings.guideStartJourney,
                        style = MiuixTheme.textStyles.button,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 底部向导导航栏：平滑伸缩胶囊指示器 + 上一步 / 下一步 / 完成按钮。
 */
@Composable
private fun GuideBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentStep > 0) {
            Button(
                onClick = onPrevious,
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Text(
                    text = strings.guidePrev,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(80.dp))
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

