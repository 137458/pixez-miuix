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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.effect.BgEffectBackground
import com.perol.pixez.shared.ui.effect.isRuntimeShaderSupported
import com.perol.pixez.shared.ui.i18n.LocalStrings
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

    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isOs3Effect by remember { mutableStateOf(true) }

    var ignoredVersion by remember { mutableStateOf(settingsRepository.ignoreUpdateVersion) }
    var autoCheckUpdate by remember { mutableStateOf(settingsRepository.autoCheckUpdate) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var downloadedBytes by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var totalBytes by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
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
                        toastMessage = strings.updateLatest.format(AppInfo.VERSION_NAME)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: strings.loadFailed
                    toastMessage = "${strings.loadFailed}: $message"
                }
        } finally {
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        doCheck(userInitiated = false)
    }

    val scrollProgress by remember {
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

    val density = LocalDensity.current
    var logoHeightDp by remember { mutableStateOf(240.dp) }

    Scaffold(
        topBar = {
            val barColor = if (scrollProgress == 1f) MiuixTheme.colorScheme.surface else Color.Transparent
            val titleColor = MiuixTheme.colorScheme.onSurface.copy(
                alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
            )
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
        },
    ) { innerPadding ->
        BgEffectBackground(
            dynamicBackground = isRuntimeShaderSupported(),
            isOs3Effect = isOs3Effect,
            isFullSize = true,
            modifier = Modifier.fillMaxSize(),
            alpha = { 1f - scrollProgress },
        ) {
            // ── 顶部官方规范 Hero 视觉 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 24.dp)
                    .onSizeChanged { size ->
                        with(density) { logoHeightDp = size.height.toDp() }
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
                        contentDescription = "PixEz Logo",
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
                        text = strings.updateFoundNew.format(releaseInfo?.versionName ?: "", AppInfo.VERSION_NAME),
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

            // ── 滚动内容列表 ──
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
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
                        val changelogTitle = if (hasNew) {
                            strings.updateChangelogTitle.format(releaseInfo?.versionName ?: "")
                        } else {
                            strings.updateCurrentChangelog.format(releaseInfo?.versionName ?: "")
                        }
                        SmallTitle(text = changelogTitle)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = releaseInfo?.title ?: strings.updateNewRelease,
                                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                com.perol.pixez.shared.ui.components.MarkdownText(
                                    markdown = releaseInfo?.changelog ?: "",
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
                                            "${formatSize(downloadedBytes)} / ${formatSize(totalBytes)}"
                                        } else {
                                            formatSize(downloadedBytes)
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

                                if (downloadedFilePath != null) {
                                    TextButton(
                                        text = strings.updateInstallNow,
                                        onClick = {
                                            com.perol.pixez.shared.platform.AppInstaller().install(downloadedFilePath!!)
                                        },
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else if (!isDownloading) {
                                    TextButton(
                                        text = if (hasNew) strings.updateDownloadNow else strings.updateOpenInBrowser,
                                        onClick = {
                                            if (hasNew) {
                                                val downloadUrl = releaseInfo?.downloadUrl ?: releaseInfo?.releaseUrl.orEmpty()
                                                val fileName = releaseInfo?.fileName ?: "PixEz-MIUIX-${releaseInfo?.versionName}.apk"
                                                isDownloading = true
                                                downloadProgress = 0f
                                                coroutineScope.launch {
                                                    val result = com.perol.pixez.shared.platform.AppUpdateDownloader().download(
                                                        downloadUrl = downloadUrl,
                                                        fileName = fileName,
                                                        onProgress = { progress, downloaded, total ->
                                                            downloadProgress = progress
                                                            downloadedBytes = downloaded
                                                            totalBytes = total
                                                        },
                                                    )
                                                    isDownloading = false
                                                    result.onSuccess { path ->
                                                        downloadedFilePath = path
                                                        com.perol.pixez.shared.platform.AppInstaller().install(path)
                                                    }.onFailure { error ->
                                                        toastMessage = error.message ?: strings.updateDownloadFailed
                                                    }
                                                }
                                            } else {
                                                releaseInfo?.releaseUrl?.let { openBrowser(it) }
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── 更新设置 ──
                item(key = "settings") {
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
                                        autoCheckUpdate = checked
                                        coroutineScope.launch {
                                            withContext(Dispatchers.Default) {
                                                settingsRepository.autoCheckUpdate = checked
                                            }
                                        }
                                    },
                                )
                            },
                        )

                        BasicComponent(
                            title = strings.updateIgnoreVersion,
                            summary = when {
                                isChecking -> strings.loading
                                !hasNew -> strings.updateLatest.format(AppInfo.VERSION_NAME)
                                ignoredVersion == releaseInfo?.versionName -> strings.updateIgnoreVersionIgnored
                                else -> strings.updateIgnoreVersionSummary.format(releaseInfo?.versionName ?: "")
                            },
                            endActions = {
                                Switch(
                                    checked = hasNew && ignoredVersion == releaseInfo?.versionName,
                                    onCheckedChange = { checked ->
                                        val newValue = if (checked) releaseInfo?.versionName else null
                                        coroutineScope.launch {
                                            withContext(Dispatchers.Default) {
                                                settingsRepository.ignoreUpdateVersion = newValue
                                            }
                                            ignoredVersion = newValue
                                        }
                                    },
                                    enabled = hasNew,
                                )
                            },
                        )
                    }
                }

                // ── 版本通道与操作 ──
                item(key = "channel") {
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
                                coroutineScope.launch {
                                    doCheck(userInitiated = true)
                                }
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
                                openBrowser(AppConstants.Urls.GITHUB_RELEASES)
                            },
                        )

                        BasicComponent(
                            title = strings.updateHyperOs3Effect,
                            summary = strings.updateHyperOs3EffectSummary,
                            endActions = {
                                Switch(
                                    checked = isOs3Effect,
                                    onCheckedChange = { isOs3Effect = it },
                                )
                            },
                        )
                    }
                }
            }
        }

        // 官方 Miuix 风格更新弹窗
        if (showDialog && releaseInfo != null) {
            UpdateDialog(
                show = showDialog,
                releaseInfo = releaseInfo!!,
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
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${(gb * 10).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10).toInt() / 10.0} MB"
        kb >= 1.0 -> "${(kb * 10).toInt() / 10.0} KB"
        else -> "$bytes B"
    }
}

