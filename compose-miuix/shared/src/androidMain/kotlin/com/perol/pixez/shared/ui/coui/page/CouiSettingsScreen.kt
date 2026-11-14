package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.SingletonImageLoader
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.BoardInfo
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.isAndroidPlatform
import com.perol.pixez.shared.platform.openDefaultAppSettings
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.formatFileSize
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBoardRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUpdateRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 设置页：固定顶栏 + 分组卡片。
 *
 * 入口、分组顺序与业务行为与 MIUIX 版一致：账号登录/退出与账号管理入口（含凭证失效的
 * 重新登录提示）、界面个性化、画质与保存、浏览与交互、屏蔽、系统与数据（默认打开链接 /
 * 清除缓存并显示占用 / 数据导出）、关于与更新（公告入口按列表非空显示、新版本标记）。
 * 皮肤差异：顶栏为固定条（无折叠毛玻璃），分组标题与行样式改用 COUI 文本层级。
 */
@Composable
internal fun CouiSettingsScreen(
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
    onUserClick: (Long) -> Unit = {},
    onUserBookmarksClick: (Long) -> Unit = {},
    onAccountManageClick: () -> Unit = {},
    onNovelBrowseClick: () -> Unit = {},
) {
    val accountRepository = LocalAccountRepository.current
    val boardRepository = LocalBoardRepository.current
    val updateRepository = LocalUpdateRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current

    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggingOut by rememberSaveable { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var boardList by remember { mutableStateOf<List<BoardInfo>?>(null) }
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }

    LaunchedEffect(isClearingCache) {
        if (isClearingCache) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val imageLoader = SingletonImageLoader.get(context)
            cacheSizeBytes = imageLoader.diskCache?.size ?: 0L
        }
    }

    LaunchedEffect(Unit) {
        currentAccount = accountRepository.currentAccount()
        accountRepository.loginEventFlow.collect {
            currentAccount = accountRepository.currentAccount()
        }
    }

    LaunchedEffect(boardRepository) {
        boardList = suspendRunCatchingNonCancel { boardRepository.loadBoardList() }
            .onFailure { Napier.w(tag = "BoardList", throwable = it) { "加载公告板列表失败(CouiSettingsScreen)" } }
            .getOrNull()
    }
    LaunchedEffect(Unit) {
        updateRepository.fetchLatestReleaseInfo().onSuccess { info -> releaseInfo = info }
    }

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingsTitle,
            onBack = onBack,
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth(),
            ) {
                // ── 1. 账号管理 ──
                item {
                    CouiSettingSection(title = strings.settingSectionAccount) {
                        CouiAccountSection(
                            account = currentAccount,
                            isLoggingOut = isLoggingOut,
                            onLoginClick = onLoginClick,
                            onUserClick = onUserClick,
                            onLogoutClick = {
                                if (isLoggingOut) return@CouiAccountSection
                                coroutineScope.launch {
                                    try {
                                        isLoggingOut = true
                                        suspendRunCatchingNonCancel { accountRepository.logout() }
                                            .onSuccess { currentAccount = null }
                                    } finally {
                                        isLoggingOut = false
                                    }
                                }
                            },
                            strings = strings,
                        )
                        if (currentAccount.isLoggedIn) {
                            val currentUserId = currentAccount?.userId?.toLongOrNull()
                            if (currentUserId != null) {
                                CouiSettingRow(
                                    title = strings.settingMyProfileAndBookmarks,
                                    summary = strings.settingMyProfileAndBookmarksSummary,
                                    onClick = { onUserBookmarksClick(currentUserId) },
                                )
                            }
                            CouiSettingRow(
                                title = strings.accountManageTitle,
                                summary = strings.accountSwitch,
                                onClick = onAccountManageClick,
                            )
                            CouiSettingRow(
                                title = strings.settingAccountInfo,
                                summary = strings.settingAccountInfoSummary,
                                onClick = onAccountEditClick,
                            )
                        } else if (currentAccount != null) {
                            // 凭证失效但账号行保留：给出重新登录入口与账号切换。
                            CouiSettingRow(
                                title = strings.btnRelogin,
                                summary = strings.settingAccountExpiredHint,
                                onClick = onLoginClick,
                            )
                            CouiSettingRow(
                                title = strings.accountManageTitle,
                                summary = strings.accountSwitch,
                                onClick = onAccountManageClick,
                            )
                        }
                    }
                }

                // ── 2. 界面与个性化 ──
                item {
                    CouiSettingSection(title = strings.settingSectionTheme) {
                        CouiSettingRow(strings.settingTheme, strings.settingThemeSummary, onThemeSettingClick)
                        CouiSettingRow(strings.settingLayout, strings.settingLayoutSummary, onLayoutSettingClick)
                        CouiSettingRow(strings.settingLanguage, strings.settingLanguageSummary, onLanguageSettingClick)
                        CouiSettingRow(strings.settingWelcomePage, strings.settingWelcomePageSummary, onWelcomePageSettingClick)
                        CouiSettingRow(strings.settingShareFormat, strings.settingShareFormatSummary, onCopyTextSettingClick)
                        CouiSettingRow(strings.settingGuideWizard, strings.settingGuideWizardSummary, onGuideClick)
                    }
                }

                // ── 3. 画质与保存 ──
                item {
                    CouiSettingSection(title = strings.settingSectionQualitySave) {
                        CouiSettingRow(strings.settingQuality, strings.settingQualitySummary, onQualitySettingClick)
                        CouiSettingRow(strings.settingDownload, strings.settingDownloadSummary, onDownloadSettingClick)
                        CouiSettingRow(strings.settingDownloadTask, strings.settingDownloadTaskSummary, onDownloadTaskClick)
                        CouiSettingRow(strings.settingDownloadHistory, strings.settingDownloadHistorySummary, onDownloadHistoryClick)
                    }
                }

                // ── 4. 浏览与交互 ──
                item {
                    CouiSettingSection(title = strings.settingSectionGeneral) {
                        CouiSettingRow(
                            strings.interactionSettingTitle,
                            strings.interactionSettingSwipeChange,
                            onInteractionSettingClick,
                        )
                        if (isAndroidPlatform()) {
                            CouiSettingRow(
                                strings.settingWidgetRecommend,
                                strings.settingWidgetRecommendSummary,
                                onWidgetRecommendSettingClick,
                            )
                        }
                        CouiSettingRow(strings.settingHistory, strings.settingHistorySummary, onHistoryClick)
                        CouiSettingRow(
                            strings.novelBrowseTitle,
                            "${strings.novelRecommend} / ${strings.novelRanking}",
                            onNovelBrowseClick,
                        )
                        CouiSettingRow(strings.settingBookTags, strings.settingBookTagsSummary, onBookTagClick)
                    }
                }

                // ── 5. 内容过滤与屏蔽 ──
                item {
                    CouiSettingSection(title = strings.settingSectionShieldPrivacy) {
                        CouiSettingRow(strings.settingShield, strings.settingShieldSummary, onShieldClick)
                    }
                }

                // ── 6. 系统与数据 ──
                item {
                    CouiSettingSection(title = strings.settingSectionStorage) {
                        if (isAndroidPlatform()) {
                            CouiSettingRow(
                                strings.platformSettingDefaultOpenLinks,
                                strings.platformSettingDefaultOpenLinksSummary,
                                { openDefaultAppSettings() },
                            )
                        }
                        CouiSettingRow(strings.settingNetwork, strings.settingNetworkSummary, onNetworkSettingClick)
                        CouiSettingRow(
                            title = strings.settingClearCache,
                            summary = if (isClearingCache) {
                                strings.clearingCache
                            } else {
                                val sizeStr = formatFileSize(cacheSizeBytes, gbDecimals = 2)
                                "${strings.settingClearCacheSummary} ($sizeStr)"
                            },
                            onClick = {
                                if (isClearingCache) return@CouiSettingRow
                                coroutineScope.launch {
                                    isClearingCache = true
                                    val result = try {
                                        val imageLoader = SingletonImageLoader.get(context)
                                        imageLoader.memoryCache?.clear()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            imageLoader.diskCache?.clear()
                                        }
                                        cacheSizeBytes = 0L
                                        Result.success(Unit)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Throwable) {
                                        Result.failure(e)
                                    } finally {
                                        isClearingCache = false
                                    }
                                    toastSink(
                                        if (result.isSuccess) {
                                            ToastData(strings.cacheCleared, ToastType.Success)
                                        } else {
                                            ToastData(
                                                PixivApiErrorFormatter.format(result.exceptionOrNull(), strings),
                                                ToastType.Error,
                                            )
                                        },
                                    )
                                }
                            },
                        )
                        CouiSettingRow(strings.settingDataExport, strings.settingDataExportSummary, onDataExportClick)
                    }
                }

                // ── 7. 关于与更新 ──
                item {
                    CouiSettingSection(title = strings.settingSectionAbout) {
                        if (!boardList.isNullOrEmpty()) {
                            CouiSettingRow(strings.settingBoard, strings.settingBoardSummary, onBoardClick)
                        }
                        val hasNewUpdate = releaseInfo?.isNew == true &&
                            releaseInfo?.versionName != settingsRepository.ignoreUpdateVersion
                        CouiSettingRow(
                            title = strings.settingUpdate,
                            summary = if (hasNewUpdate) {
                                strings.updateFoundNew.format(
                                    releaseInfo?.versionName ?: "",
                                    AppInfo.VERSION_NAME,
                                )
                            } else {
                                strings.settingUpdateSummary
                            },
                            showDot = hasNewUpdate,
                            onClick = onUpdateSettingClick,
                        )
                        CouiSettingRow(
                            title = strings.settingAbout,
                            summary = "${strings.version} ${AppInfo.VERSION_NAME}",
                            onClick = onAboutClick,
                        )
                    }
                }
            }
        }
    }
}

/** 分组：标题 + COUI 卡片。 */
@Composable
internal fun CouiSettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
        CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
            content()
        }
    }
}

/** 设置行：标题 + 摘要 + 可选更新红点。 */
@Composable
private fun CouiSettingRow(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    showDot: Boolean = false,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .couiPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            summary?.let {
                Text(
                    text = it,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.accent),
            )
        }
    }
}

/** 账号分组：已登录展示头像与账号信息，未登录提供登录入口。 */
@Composable
private fun CouiAccountSection(
    account: AccountPersist?,
    isLoggingOut: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    strings: AppStrings,
) {
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (account != null) {
            val targetUserId = account.userId.toLongOrNull()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (targetUserId != null) Modifier.clickable { onUserClick(targetUserId) } else Modifier
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PixivAsyncImage(
                    model = account.userImage,
                    contentDescription = account.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = account.name,
                        kind = CouiTextKind.ItemTitle,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@${account.account}",
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = "ID: ${account.userId}",
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            CouiSettingsButton(
                text = if (isLoggingOut) strings.loggingOut else strings.logout,
                primary = false,
                enabled = !isLoggingOut,
                onClick = onLogoutClick,
            )
        } else {
            Text(
                text = strings.notLoggedIn,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
            )
            CouiSettingsButton(
                text = strings.goLogin,
                primary = true,
                enabled = true,
                onClick = onLoginClick,
            )
        }
    }
}

/** 账号区胶囊按钮：登录用强调色，退出用容器底色。 */
@Composable
private fun CouiSettingsButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) scheme.accent else scheme.container)
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = when {
                !enabled -> scheme.onSurfaceVariant
                primary -> scheme.onAccent
                else -> scheme.onSurface
            },
            maxLines = 1,
        )
    }
}
