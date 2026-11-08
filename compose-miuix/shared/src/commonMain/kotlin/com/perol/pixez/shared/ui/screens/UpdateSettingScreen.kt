package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.State
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.repository.getLocalReleaseInfo
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.platform.forAsset
import com.perol.pixez.shared.platform.installPathFor
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUpdateDownloader
import com.perol.pixez.shared.ui.LocalUpdateRepository
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.components.updateAsset
import com.perol.pixez.shared.ui.components.updateBrowserFallbackUrl
import com.perol.pixez.shared.ui.components.updateDialogNotesState
import com.perol.pixez.shared.ui.components.UPDATE_PAGE_PREVIEW_LINES
import com.perol.pixez.shared.ui.components.updatePrimaryCta
import com.perol.pixez.shared.ui.components.UpdatePrimaryAction
import com.perol.pixez.shared.ui.components.UpdateDownloadProgressSection
import com.perol.pixez.shared.ui.components.runUpdatePrimaryAction
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.effect.BgEffectBackground
import com.perol.pixez.shared.ui.effect.isRuntimeShaderSupported
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 官方 Miuix / HyperOS 视觉规范系统与应用更新页。
 */
@Composable
fun UpdateSettingScreen(
    onBack: () -> Unit,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val settingsRepository = LocalSettingsRepository.current
    val updateRepository = LocalUpdateRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(getLocalReleaseInfo()) }
    var isChecking by remember { mutableStateOf(false) }
    val toastSink = LocalToastSink.current
    var showDialog by remember { mutableStateOf(false) }
    var isOs3Effect by remember { mutableStateOf(true) }

    var ignoredVersion by remember { mutableStateOf(settingsRepository.ignoreUpdateVersion) }
    var autoCheckUpdate by remember { mutableStateOf(settingsRepository.autoCheckUpdate) }

    // 与更新弹窗共享同一个进程级下载器：页面不再自己起协程、也不再另存一套进度状态。
    val updateDownloader = LocalUpdateDownloader.current
    val rawDownloadState by updateDownloader.state.collectAsState()
    val hasNew = releaseInfo?.isNew == true
    val updateAsset = remember(releaseInfo) { releaseInfo?.updateAsset() }
    // 页面与弹窗同一口径：归属不一致的状态一律按 Idle 呈现，安装入口也只认当前版本。
    val downloadState = remember(rawDownloadState, updateAsset) { rawDownloadState.forAsset(updateAsset) }
    LaunchedEffect(updateAsset) { updateDownloader.syncTarget(updateAsset) }

    suspend fun doCheck(userInitiated: Boolean = false) {
        if (isChecking) return
        try {
            isChecking = true
            val result = updateRepository.fetchLatestReleaseInfo()
            result
                .onSuccess { info ->
                    releaseInfo = info
                    if (info.isNew && userInitiated) {
                        showDialog = true
                    } else if (!info.isNew && userInitiated) {
                        toastSink(ToastData(strings.updateLatest.format(AppInfo.VERSION_NAME), ToastType.Normal))
                    }
                }
                .onFailure { error ->
                    if (userInitiated) {
                        toastSink(ToastData(PixivApiErrorFormatter.format(error, strings), ToastType.Error))
                    }
                }
        } finally {
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        doCheck(userInitiated = false)
    }

    val scrollProgressState = remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }
    val scrollProgress by scrollProgressState

    val density = LocalDensity.current
    var logoHeightDp by remember { mutableStateOf(240.dp) }

    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        topBar = {
            val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
            val titleColor = colorScheme.onSurface.copy(
                alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
            )
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                SmallTopAppBar(
                    title = strings.settingUpdate,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    titleColor = titleColor,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(backdrop),
        ) {
        BgEffectBackground(
            dynamicBackground = isRuntimeShaderSupported(),
            isOs3Effect = isOs3Effect,
            isFullSize = true,
            modifier = Modifier.fillMaxSize(),
            alpha = { 1f - scrollProgress },
        ) {
            // ── 顶部官方规范 Hero 视觉 ──
            UpdateHeroSection(
                topPadding = innerPadding.calculateTopPadding(),
                scrollProgressState = scrollProgressState,
                isChecking = isChecking,
                hasNew = hasNew,
                releaseVersionName = releaseInfo?.versionName ?: "",
                strings = strings,
                onHeroSizeMeasured = { size ->
                    with(density) { logoHeightDp = size.height.toDp() }
                },
            )

            // ── 滚动内容列表 ──
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                item(key = "logoSpacer") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(logoHeightDp + 48.dp),
                    )
                }

                // ── 更新日志卡片（获取到版本信息后常驻展示，保证稳定可见） ──
                if (releaseInfo != null) {
                    item(key = "changelog") {
                        ChangelogCardSection(
                            hasNew = hasNew,
                            versionName = releaseInfo?.versionName ?: "",
                            title = releaseInfo?.title,
                            changelog = releaseInfo?.changelog,
                            strings = strings,
                            downloadState = downloadState,
                            hasAsset = updateAsset != null,
                            onPrimaryClick = { action ->
                                val error = runUpdatePrimaryAction(
                                    action = action,
                                    asset = updateAsset,
                                    downloader = updateDownloader,
                                    installPath = downloadState.installPathFor(updateAsset),
                                    installFailedMessage = strings.updateInstallFailed,
                                    openBrowser = {
                                        val url = if (!hasNew) {
                                            releaseInfo?.releaseUrl
                                        } else {
                                            releaseInfo?.let { updateBrowserFallbackUrl(it) }
                                        }
                                        if (!url.isNullOrBlank()) {
                                            openSafeUrl(url, strings, onError = { message ->
                                                toastSink(ToastData(message, ToastType.Error))
                                            })
                                        }
                                    },
                                )
                                if (error != null) {
                                    toastSink(ToastData(error, ToastType.Error))
                                }
                            },
                            onCancelClick = updateDownloader::cancel,
                            onBrowserClick = {
                                val url = releaseInfo?.let { updateBrowserFallbackUrl(it) }.orEmpty()
                                if (url.isNotBlank()) {
                                    openSafeUrl(url, strings, onError = { message ->
                                        toastSink(ToastData(message, ToastType.Error))
                                    })
                                }
                            },
                            onLinkClick = { url ->
                                openSafeUrl(url, strings, onError = { message ->
                                    toastSink(ToastData(message, ToastType.Error))
                                })
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── 更新设置 ──
                item(key = "settings") {
                    UpdateSettingsSection(
                        strings = strings,
                        autoCheckUpdate = autoCheckUpdate,
                        isChecking = isChecking,
                        hasNew = hasNew,
                        ignored = ignoredVersion == releaseInfo?.versionName,
                        releaseVersionName = releaseInfo?.versionName ?: "",
                        onAutoCheckChange = { checked ->
                            autoCheckUpdate = checked
                            coroutineScope.launch {
                                withContext(Dispatchers.Default) {
                                    settingsRepository.autoCheckUpdate = checked
                                }
                            }
                        },
                        onIgnoreChange = { checked ->
                            val newValue = if (checked) releaseInfo?.versionName else null
                            coroutineScope.launch {
                                withContext(Dispatchers.Default) {
                                    settingsRepository.ignoreUpdateVersion = newValue
                                }
                                ignoredVersion = newValue
                            }
                        },
                    )
                }

                // ── 版本通道与操作 ──
                item(key = "channel") {
                    UpdateChannelSection(
                        strings = strings,
                        isChecking = isChecking,
                        isOs3Effect = isOs3Effect,
                        onManualCheckClick = {
                            coroutineScope.launch {
                                doCheck(userInitiated = true)
                            }
                        },
                        onGithubClick = {
                            openSafeUrl(AppConstants.Urls.GITHUB_RELEASES, strings, onError = { message ->
                                toastSink(ToastData(message, ToastType.Error))
                            })
                        },
                        onOs3EffectChange = { isOs3Effect = it },
                    )
                }
            }
        }
        }

        // 官方 Miuix 风格更新弹窗
        val dialogReleaseInfo = releaseInfo
        if (showDialog && dialogReleaseInfo != null) {
            UpdateDialog(
                show = showDialog,
                releaseInfo = dialogReleaseInfo,
                onDismiss = { showDialog = false },
                onUpdate = { url ->
                    showDialog = false
                    openSafeUrl(url, strings, onError = { message ->
                        toastSink(ToastData(message, ToastType.Error))
                    })
                },
                onIgnore = { ver ->
                    coroutineScope.launch {
                        withContext(Dispatchers.Default) {
                            settingsRepository.ignoreUpdateVersion = ver
                        }
                        ignoredVersion = ver
                    }
                    showDialog = false
                },
            )
        }
    }
}

/**
 * 顶部官方规范 Hero 视觉：logo、名称与版本状态文案，随滚动进度做缩放与淡出。
 * scrollProgress 以 State 引用传入，保持 graphicsLayer 绘制期读取的原有语义。
 */
@Composable
private fun UpdateHeroSection(
    topPadding: Dp,
    scrollProgressState: State<Float>,
    isChecking: Boolean,
    hasNew: Boolean,
    releaseVersionName: String,
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    onHeroSizeMeasured: (IntSize) -> Unit,
) {
    val scrollProgress by scrollProgressState
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + 24.dp)
            .onSizeChanged { size ->
                onHeroSizeMeasured(size)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
                    clip = true
                    shape = RoundedCornerShape(24.dp)
                    alpha = 1 - iconProgress
                    scaleX = 1 - (iconProgress * 0.05f)
                    scaleY = 1 - (iconProgress * 0.05f)
                }
                .background(MiuixTheme.colorScheme.surfaceContainer),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_pixez_logo),
                contentDescription = strings.logoContentDescription,
                modifier = Modifier.size(72.dp),
            )
        }

        Text(
            text = "PixEz MIUIX",
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 4.dp)
                .graphicsLayer {
                    val nameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
                    alpha = 1 - nameProgress
                    scaleX = 1 - (nameProgress * 0.05f)
                    scaleY = 1 - (nameProgress * 0.05f)
                },
        )

        if (isChecking) {
            Text(
                text = strings.updateChecking,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val verProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - verProgress
                    },
            )
        } else if (hasNew) {
            Text(
                text = strings.updateFoundNew.format(releaseVersionName, AppInfo.VERSION_NAME),
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val verProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - verProgress
                    },
            )
        } else {
            Text(
                text = strings.updateLatest.format(AppInfo.VERSION_NAME),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val verProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - verProgress
                    },
            )
        }
    }
}

/**
 * 更新日志卡片：版本标题、Markdown 更新说明、下载进度与安装/下载操作。
 * 下载状态直接读进程级单例，与弹窗共享同一份事实源。
 */
@Composable
private fun ChangelogCardSection(
    hasNew: Boolean,
    versionName: String,
    title: String?,
    changelog: String?,
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    downloadState: UpdateDownloadState,
    hasAsset: Boolean,
    onPrimaryClick: (UpdatePrimaryAction) -> Unit,
    onCancelClick: () -> Unit,
    onBrowserClick: () -> Unit,
    onLinkClick: (String) -> Unit,
) {
    val changelogTitle = if (hasNew) {
        strings.updateChangelogTitle.format(versionName)
    } else {
        strings.updateCurrentChangelog.format(versionName)
    }
    SmallTitle(text = changelogTitle)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title?.takeIf { it.isNotBlank() } ?: strings.updateNewRelease,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val displayChangelog = when {
                !changelog.isNullOrBlank() -> changelog.orEmpty()
                !hasNew && AppInfo.CURRENT_CHANGELOG.isNotBlank() -> AppInfo.CURRENT_CHANGELOG
                else -> strings.updateChangelogEmpty
            }
            // 长说明先按行折叠：避免一次组合几百个 Markdown block 的 Text，展开后仍可完整阅读。
            var notesExpanded by remember(displayChangelog) { mutableStateOf(false) }
            val notesState = remember(displayChangelog, notesExpanded) {
                updateDialogNotesState(displayChangelog, notesExpanded, UPDATE_PAGE_PREVIEW_LINES)
            }
            com.perol.pixez.shared.ui.components.MarkdownText(
                markdown = notesState.visibleMarkdown,
                modifier = Modifier.fillMaxWidth(),
                baseFontSize = 14,
                onLinkClick = onLinkClick,
            )
            if (notesState.hiddenLineCount > 0 || notesExpanded) {
                TextButton(
                    text = if (notesExpanded) strings.collapse else strings.expand,
                    onClick = { notesExpanded = !notesExpanded },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (downloadState is UpdateDownloadState.Downloading) {
                Spacer(modifier = Modifier.height(12.dp))
                UpdateDownloadProgressSection(
                    state = downloadState,
                    label = strings.updateDownloading,
                )
            }

            if (downloadState is UpdateDownloadState.Failed) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.updateDownloadFailed,
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 与弹窗共用同一个「文案 + 动作」推导：hasNew=false 一定走浏览器，不在这里再猜。
            val primaryCta = updatePrimaryCta(hasNew, hasAsset, downloadState, strings)
            if (downloadState is UpdateDownloadState.Downloading) {
                TextButton(
                    text = strings.updateCancelDownload,
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(
                    text = primaryCta.text,
                    onClick = { onPrimaryClick(primaryCta.action) },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 浏览器入口保留为次要按钮：直链可用时优先走应用内下载。
            if (hasNew && hasAsset && downloadState !is UpdateDownloadState.Downloading) {
                TextButton(
                    text = strings.updateOpenInBrowser,
                    onClick = onBrowserClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 更新设置分组：自动检查更新与忽略当前新版本。
 */
@Composable
private fun UpdateSettingsSection(
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    autoCheckUpdate: Boolean,
    isChecking: Boolean,
    hasNew: Boolean,
    ignored: Boolean,
    releaseVersionName: String,
    onAutoCheckChange: (Boolean) -> Unit,
    onIgnoreChange: (Boolean) -> Unit,
) {
    SmallTitle(text = strings.updateSectionSettings)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        SwitchPreference(
            title = strings.updateAutoCheck,
            summary = strings.updateAutoCheckSummary,
            checked = autoCheckUpdate,
            onCheckedChange = onAutoCheckChange,
        )

        SwitchPreference(
            title = strings.updateIgnoreVersion,
            summary = when {
                isChecking -> strings.loading
                !hasNew -> strings.updateLatest.format(AppInfo.VERSION_NAME)
                ignored -> strings.updateIgnoreVersionIgnored
                else -> strings.updateIgnoreVersionSummary.format(releaseVersionName)
            },
            checked = hasNew && ignored,
            onCheckedChange = onIgnoreChange,
            enabled = hasNew,
        )
    }
}

/**
 * 版本通道与操作分组：手动检查更新、GitHub Releases 入口与 HyperOS 3 动效开关。
 */
@Composable
private fun UpdateChannelSection(
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    isChecking: Boolean,
    isOs3Effect: Boolean,
    onManualCheckClick: () -> Unit,
    onGithubClick: () -> Unit,
    onOs3EffectChange: (Boolean) -> Unit,
) {
    SmallTitle(text = strings.updateSectionChannel)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        BasicComponent(
            title = strings.updateManualCheck,
            summary = if (isChecking) strings.updateManualCheckSummaryChecking else strings.updateManualCheckSummaryIdle,
            onClick = {
                onManualCheckClick()
            },
            endActions = {
                if (isChecking) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        )

        BasicComponent(
            title = "GitHub Releases",
            summary = strings.updateGithubReleasesSummary,
            onClick = {
                onGithubClick()
            },
        )

        SwitchPreference(
            title = strings.updateHyperOs3Effect,
            summary = strings.updateHyperOs3EffectSummary,
            checked = isOs3Effect,
            onCheckedChange = onOs3EffectChange,
        )
    }
}
