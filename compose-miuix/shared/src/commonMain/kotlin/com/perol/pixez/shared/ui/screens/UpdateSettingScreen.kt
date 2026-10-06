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
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.effect.BgEffectBackground
import com.perol.pixez.shared.ui.effect.isRuntimeShaderSupported
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.formatFileSize
import io.ktor.client.HttpClient
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
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 官方 Miuix / HyperOS 视觉规范系统与应用更新页。
 */
@Composable
fun UpdateSettingScreen(
    settingsRepository: SettingsRepository,
    updateCheckClient: HttpClient = defaultUpdateCheckClient,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(getLocalReleaseInfo()) }
    var isChecking by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<ToastData?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isOs3Effect by remember { mutableStateOf(true) }

    var ignoredVersion by remember { mutableStateOf(settingsRepository.ignoreUpdateVersion) }
    var autoCheckUpdate by remember { mutableStateOf(settingsRepository.autoCheckUpdate) }

    var isDownloading by remember { mutableStateOf(false) }
    // 下载进度为高频更新状态：以 State 引用传入子 Section，保持原有重组作用域粒度。
    val downloadProgressState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var downloadProgress by downloadProgressState
    val downloadedBytesState = remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var downloadedBytes by downloadedBytesState
    val totalBytesState = remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var totalBytes by totalBytesState
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }

    val hasNew = releaseInfo?.isNew == true

    suspend fun doCheck(userInitiated: Boolean = false) {
        if (isChecking) return
        try {
            isChecking = true
            val result = fetchLatestReleaseInfo(updateCheckClient)
            result
                .onSuccess { info ->
                    releaseInfo = info
                    if (info.isNew && userInitiated) {
                        showDialog = true
                    } else if (!info.isNew && userInitiated) {
                        toastMessage = ToastData(strings.updateLatest.format(AppInfo.VERSION_NAME), ToastType.Normal)
                    }
                }
                .onFailure { error ->
                    if (userInitiated) {
                        val message = error.message ?: strings.loadFailed
                        toastMessage = ToastData("${strings.loadFailed}: $message", ToastType.Error)
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
                        // 下载/浏览器打开动作收敛在此定义，保持对父级高频状态的写入与原有顺序完全一致。
                        fun onUpdateAction() {
                            if (hasNew) {
                                val downloadUrl = releaseInfo?.downloadUrl
                                val fileName = releaseInfo?.fileName
                                if (downloadUrl == null || fileName == null) {
                                    toastMessage = ToastData(strings.updateDownloadFailed, ToastType.Error)
                                    return
                                }
                                isDownloading = true
                                downloadProgress = 0f
                                coroutineScope.launch {
                                    try {
                                        val result = com.perol.pixez.shared.platform.AppUpdateDownloader().download(
                                            downloadUrl = downloadUrl,
                                            fileName = fileName,
                                            sha256Digest = releaseInfo?.digest,
                                            onProgress = { progress, downloaded, total ->
                                                downloadProgress = progress
                                                downloadedBytes = downloaded
                                                totalBytes = total
                                            },
                                        )
                                        result.onSuccess { path ->
                                            downloadedFilePath = path
                                            com.perol.pixez.shared.platform.AppInstaller().install(path)
                                        }.onFailure { error ->
                                            toastMessage = ToastData(error.message ?: strings.updateDownloadFailed, ToastType.Error)
                                        }
                                    } finally {
                                        // 取消或异常时同样复位，避免下载按钮被永久禁用。
                                        isDownloading = false
                                    }
                                }
                            } else {
                                releaseInfo?.releaseUrl?.let { openBrowser(it) }
                            }
                        }
                        ChangelogCardSection(
                            hasNew = hasNew,
                            versionName = releaseInfo?.versionName ?: "",
                            title = releaseInfo?.title,
                            changelog = releaseInfo?.changelog,
                            strings = strings,
                            isDownloading = isDownloading,
                            downloadProgressState = downloadProgressState,
                            downloadedBytesState = downloadedBytesState,
                            totalBytesState = totalBytesState,
                            downloadedFilePath = downloadedFilePath,
                            onInstallClick = { path ->
                                com.perol.pixez.shared.platform.AppInstaller().install(path)
                            },
                            onUpdateActionClick = { onUpdateAction() },
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
                            openBrowser(AppConstants.Urls.GITHUB_RELEASES)
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
                    openBrowser(url)
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

        ToastMessage(
            toast = toastMessage,
            onDismiss = { toastMessage = null },
        )
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
 * 更新日志卡片：版本标题、Markdown 更新说明、下载进度与安装/下载操作按钮。
 * 下载进度相关高频状态以 State 引用传入，保持原 item 内的重组粒度。
 */
@Composable
private fun ChangelogCardSection(
    hasNew: Boolean,
    versionName: String,
    title: String?,
    changelog: String?,
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    isDownloading: Boolean,
    downloadProgressState: State<Float>,
    downloadedBytesState: State<Long>,
    totalBytesState: State<Long>,
    downloadedFilePath: String?,
    onInstallClick: (String) -> Unit,
    onUpdateActionClick: () -> Unit,
) {
    val downloadProgress by downloadProgressState
    val downloadedBytes by downloadedBytesState
    val totalBytes by totalBytesState

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
            com.perol.pixez.shared.ui.components.MarkdownText(
                markdown = displayChangelog,
                modifier = Modifier.fillMaxWidth(),
                baseFontSize = 14,
            )

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = strings.updateDownloading,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.primary,
                    )
                    val percent = if (downloadProgress >= 0f) "${(downloadProgress * 100).toInt()}%" else ""
                    val sizeText = if (totalBytes > 0) {
                        "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
                    } else {
                        formatFileSize(downloadedBytes)
                    }
                    Text(
                        text = if (percent.isNotEmpty()) "$sizeText ($percent)" else sizeText,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (downloadProgress >= 0f) {
                    top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val installFilePath = downloadedFilePath
            if (installFilePath != null) {
                TextButton(
                    text = strings.updateInstallNow,
                    onClick = {
                        onInstallClick(installFilePath)
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (!isDownloading) {
                TextButton(
                    text = if (hasNew) strings.updateDownloadNow else strings.updateOpenInBrowser,
                    onClick = {
                        onUpdateActionClick()
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
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
        BasicComponent(
            title = strings.updateAutoCheck,
            summary = strings.updateAutoCheckSummary,
            endActions = {
                Switch(
                    checked = autoCheckUpdate,
                    onCheckedChange = { checked ->
                        onAutoCheckChange(checked)
                    },
                )
            },
        )

        BasicComponent(
            title = strings.updateIgnoreVersion,
            summary = when {
                isChecking -> strings.loading
                !hasNew -> strings.updateLatest.format(AppInfo.VERSION_NAME)
                ignored -> strings.updateIgnoreVersionIgnored
                else -> strings.updateIgnoreVersionSummary.format(releaseVersionName)
            },
            endActions = {
                Switch(
                    checked = hasNew && ignored,
                    onCheckedChange = { checked ->
                        onIgnoreChange(checked)
                    },
                    enabled = hasNew,
                )
            },
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

        BasicComponent(
            title = strings.updateHyperOs3Effect,
            summary = strings.updateHyperOs3EffectSummary,
            endActions = {
                Switch(
                    checked = isOs3Effect,
                    onCheckedChange = { onOs3EffectChange(it) },
                )
            },
        )
    }
}
