package com.perol.pixez.shared.ui.coui.page

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.data.repository.getLocalReleaseInfo
import com.perol.pixez.shared.platform.UpdateDownloadState
import com.perol.pixez.shared.platform.forAsset
import com.perol.pixez.shared.platform.installPathFor
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUpdateDownloader
import com.perol.pixez.shared.ui.LocalUpdateRepository
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.MarkdownText
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UPDATE_PAGE_PREVIEW_LINES
import com.perol.pixez.shared.ui.components.UpdatePrimaryAction
import com.perol.pixez.shared.ui.components.buildUpdateDialogPlan
import com.perol.pixez.shared.ui.components.runUpdatePrimaryAction
import com.perol.pixez.shared.ui.components.updateAsset
import com.perol.pixez.shared.ui.components.updateBrowserFallbackUrl
import com.perol.pixez.shared.ui.components.updateDialogNotesState
import com.perol.pixez.shared.ui.components.updatePrimaryCta
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 更新设置页：固定顶栏 + 应用信息、更新日志、更新设置与版本通道四段。
 *
 * 检查更新、忽略版本、下载/安装/浏览器兜底与进程级下载器口径完全复用 MIUIX 版的
 * 计划函数；COUI 侧只重排视觉，不重写状态机。HyperOS 3 动效开关只作用于 MIUIX 背景，
 * 不在 COUI 皮肤内提供。
 */
@Composable
internal fun CouiUpdateSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val updateRepository = LocalUpdateRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val toastSink = LocalToastSink.current

    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(getLocalReleaseInfo()) }
    var isChecking by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var ignoredVersion by remember { mutableStateOf(settingsRepository.ignoreUpdateVersion) }
    var autoCheckUpdate by remember { mutableStateOf(settingsRepository.autoCheckUpdate) }

    val updateDownloader = LocalUpdateDownloader.current
    val rawDownloadState by updateDownloader.state.collectAsState()
    val hasNew = releaseInfo?.isNew == true
    val updateAsset = remember(releaseInfo) { releaseInfo?.updateAsset() }
    val downloadState = remember(rawDownloadState, updateAsset) { rawDownloadState.forAsset(updateAsset) }
    LaunchedEffect(updateAsset) { updateDownloader.syncTarget(updateAsset) }

    suspend fun doCheck(userInitiated: Boolean = false) {
        if (isChecking) return
        try {
            isChecking = true
            updateRepository.fetchLatestReleaseInfo()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.settingUpdate,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_update_hero", contentType = "hero") {
                    CouiUpdateHero(
                        isChecking = isChecking,
                        hasNew = hasNew,
                        releaseVersionName = releaseInfo?.versionName.orEmpty(),
                    )
                }

                val changelogRelease = releaseInfo
                if (changelogRelease != null) {
                    item(key = "coui_update_changelog", contentType = "section") {
                        CouiChangelogSection(
                            releaseInfo = changelogRelease,
                            hasNew = hasNew,
                            downloadState = downloadState,
                            updateAsset = updateAsset,
                        )
                    }
                }

                item(key = "coui_update_settings", contentType = "section") {
                    CouiSettingSection(title = strings.updateSectionSettings) {
                        CouiSwitchRow(
                            title = strings.updateAutoCheck,
                            summary = strings.updateAutoCheckSummary,
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
                        CouiSwitchRow(
                            title = strings.updateIgnoreVersion,
                            summary = when {
                                isChecking -> strings.loading
                                !hasNew -> strings.updateLatest.format(AppInfo.VERSION_NAME)
                                ignoredVersion == releaseInfo?.versionName -> strings.updateIgnoreVersionIgnored
                                else -> strings.updateIgnoreVersionSummary.format(releaseInfo?.versionName.orEmpty())
                            },
                            checked = hasNew && ignoredVersion == releaseInfo?.versionName,
                            onCheckedChange = { checked ->
                                // COUI 开关无禁用态：无新版本时保持原状态，不写入忽略值。
                                if (hasNew) {
                                    val newValue = if (checked) releaseInfo?.versionName else null
                                    coroutineScope.launch {
                                        withContext(Dispatchers.Default) {
                                            settingsRepository.ignoreUpdateVersion = newValue
                                        }
                                        ignoredVersion = newValue
                                    }
                                }
                            },
                        )
                    }
                }

                item(key = "coui_update_channel", contentType = "section") {
                    CouiSettingSection(title = strings.updateSectionChannel) {
                        CouiNavigationRow(
                            title = strings.updateManualCheck,
                            summary = if (isChecking) {
                                strings.updateManualCheckSummaryChecking
                            } else {
                                strings.updateManualCheckSummaryIdle
                            },
                            onClick = {
                                coroutineScope.launch { doCheck(userInitiated = true) }
                            },
                        )
                        CouiNavigationRow(
                            title = "GitHub Releases",
                            summary = strings.updateGithubReleasesSummary,
                            onClick = {
                                openSafeUrl(AppConstants.Urls.GITHUB_RELEASES, strings, onError = { message ->
                                    toastSink(ToastData(message, ToastType.Error))
                                })
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDialog && releaseInfo != null) {
        CouiUpdateDialog(
            releaseInfo = releaseInfo!!,
            onDismiss = { showDialog = false },
            onIgnore = { version ->
                coroutineScope.launch {
                    withContext(Dispatchers.Default) {
                        settingsRepository.ignoreUpdateVersion = version
                    }
                    ignoredVersion = version
                }
                showDialog = false
            },
        )
    }
}

/** 应用信息与检查状态。 */
@Composable
private fun CouiUpdateHero(
    isChecking: Boolean,
    hasNew: Boolean,
    releaseVersionName: String,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
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
            text = "PixEz MIUIX",
            kind = CouiTextKind.GroupTitle,
            color = scheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        Text(
            text = when {
                isChecking -> strings.updateChecking
                hasNew -> strings.updateFoundNew.format(releaseVersionName, AppInfo.VERSION_NAME)
                else -> strings.updateLatest.format(AppInfo.VERSION_NAME)
            },
            kind = CouiTextKind.ItemSummary,
            color = if (hasNew) scheme.accent else scheme.onSurfaceVariant,
        )
    }
}

/** 更新日志卡片：Markdown 说明、下载进度与下载/安装/浏览器入口。 */
@Composable
private fun CouiChangelogSection(
    releaseInfo: ReleaseInfo,
    hasNew: Boolean,
    downloadState: UpdateDownloadState,
    updateAsset: com.perol.pixez.shared.platform.UpdateAsset?,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val toastSink = LocalToastSink.current
    val updateDownloader = LocalUpdateDownloader.current
    val changelogTitle = if (hasNew) {
        strings.updateChangelogTitle.format(releaseInfo.versionName)
    } else {
        strings.updateCurrentChangelog.format(releaseInfo.versionName)
    }

    CouiSettingSection(title = changelogTitle) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = releaseInfo.title?.takeIf { it.isNotBlank() } ?: strings.updateNewRelease,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
            )

            val displayChangelog = when {
                !releaseInfo.changelog.isNullOrBlank() -> releaseInfo.changelog.orEmpty()
                !hasNew && AppInfo.CURRENT_CHANGELOG.isNotBlank() -> AppInfo.CURRENT_CHANGELOG
                else -> strings.updateChangelogEmpty
            }
            var notesExpanded by remember(displayChangelog) { mutableStateOf(false) }
            val notesState = remember(displayChangelog, notesExpanded) {
                updateDialogNotesState(displayChangelog, notesExpanded, UPDATE_PAGE_PREVIEW_LINES)
            }
            MarkdownText(
                markdown = notesState.visibleMarkdown.ifBlank { strings.updateChangelogEmpty },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                baseFontSize = 14,
                onLinkClick = { url -> openSafeUrl(url, strings, onError = {}) },
            )
            if (notesState.hiddenLineCount > 0 || notesExpanded) {
                CouiTextAction(
                    text = if (notesExpanded) strings.collapse else strings.expand,
                    onClick = { notesExpanded = !notesExpanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            if (downloadState is UpdateDownloadState.Downloading) {
                CouiUpdateProgress(downloadState)
            }

            if (downloadState is UpdateDownloadState.Failed) {
                Text(
                    text = strings.updateDownloadFailed,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            val primaryCta = updatePrimaryCta(hasNew, updateAsset != null, downloadState, strings)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (downloadState is UpdateDownloadState.Downloading) {
                    CouiTextAction(
                        text = strings.updateCancelDownload,
                        onClick = updateDownloader::cancel,
                    )
                } else {
                    CouiTextAction(
                        text = primaryCta.text,
                        enabled = primaryCta.enabled,
                        color = scheme.accent,
                        onClick = {
                            val error = runUpdatePrimaryAction(
                                action = primaryCta.action,
                                asset = updateAsset,
                                downloader = updateDownloader,
                                installPath = downloadState.installPathFor(updateAsset),
                                installFailedMessage = strings.updateInstallFailed,
                                openBrowser = {
                                    val url = if (!hasNew) {
                                        releaseInfo.releaseUrl
                                    } else {
                                        updateBrowserFallbackUrl(releaseInfo)
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
                    )
                }
                if (hasNew && updateAsset != null && downloadState !is UpdateDownloadState.Downloading) {
                    CouiTextAction(
                        text = strings.updateOpenInBrowser,
                        onClick = {
                            openSafeUrl(updateBrowserFallbackUrl(releaseInfo), strings, onError = { message ->
                                toastSink(ToastData(message, ToastType.Error))
                            })
                        },
                    )
                }
            }
        }
    }
}

/** 下载进度：自绘轨道，避免引入 MIUIX 进度组件。 */
@Composable
private fun CouiUpdateProgress(
    state: UpdateDownloadState.Downloading,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val detail = if (state.total > 0L) {
        strings.updateDialogDownloadingPercent.format((state.progress * 100).toInt())
    } else {
        strings.updateDownloading
    }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = detail,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(scheme.container),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(scheme.accent),
            )
        }
    }
}

/** COUI 版本更新弹窗：说明、下载进度与主/次操作。 */
@Composable
private fun CouiUpdateDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onIgnore: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val downloader = LocalUpdateDownloader.current
    val rawDownloadState by downloader.state.collectAsState()
    val asset = remember(releaseInfo) { releaseInfo.updateAsset() }
    val downloadState = remember(rawDownloadState, asset) { rawDownloadState.forAsset(asset) }
    LaunchedEffect(asset) { downloader.syncTarget(asset) }

    var notesExpanded by remember(releaseInfo.tagName) { mutableStateOf(false) }
    var installErrorText by remember(releaseInfo.tagName) { mutableStateOf<String?>(null) }
    val notesState = remember(releaseInfo.changelog, notesExpanded) {
        updateDialogNotesState(releaseInfo.changelog, notesExpanded)
    }
    val plan = remember(releaseInfo, downloadState, strings) {
        buildUpdateDialogPlan(
            currentVersion = AppInfo.VERSION_NAME,
            release = releaseInfo,
            downloadState = downloadState,
            strings = strings,
            canIgnore = true,
            canCancelDownload = true,
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogNewVersionFound,
        message = plan.summary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = plan.primaryText,
            enabled = plan.primaryEnabled,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = {
                installErrorText = runUpdatePrimaryAction(
                    action = plan.primaryAction,
                    asset = asset,
                    downloader = downloader,
                    installPath = plan.installPath,
                    installFailedMessage = strings.updateInstallFailed,
                    openBrowser = { onDismiss() },
                )
            },
        ),
        dismissAction = CouiDialogAction(
            text = plan.dismissText,
            onClick = onDismiss,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (downloadState is UpdateDownloadState.Downloading) {
                CouiUpdateProgress(downloadState)
            }

            val errorText = installErrorText ?: plan.errorText
            if (errorText != null) {
                Text(
                    text = errorText,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            MarkdownText(
                markdown = notesState.visibleMarkdown.ifBlank { strings.updateChangelogEmpty },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                baseFontSize = 13,
                onLinkClick = { url -> openSafeUrl(url, strings, onError = {}) },
            )
            if (notesState.hiddenLineCount > 0 || notesExpanded) {
                CouiTextAction(
                    text = if (notesExpanded) strings.collapse else strings.expand,
                    onClick = { notesExpanded = !notesExpanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            if (plan.showCancelDownload) {
                CouiTextAction(
                    text = strings.updateCancelDownload,
                    onClick = downloader::cancel,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            if (plan.showBrowserFallback) {
                CouiTextAction(
                    text = strings.updateOpenInBrowser,
                    onClick = {
                        openSafeUrl(updateBrowserFallbackUrl(releaseInfo), strings, onError = {})
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            if (plan.showIgnore) {
                CouiTextAction(
                    text = strings.updateDialogIgnore,
                    color = scheme.accent,
                    onClick = { onIgnore(releaseInfo.versionName) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
