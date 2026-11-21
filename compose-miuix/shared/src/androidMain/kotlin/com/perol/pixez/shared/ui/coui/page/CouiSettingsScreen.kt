package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.BoardInfo
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.platform.openDefaultAppSettings
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.formatFileSize
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBoardRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUpdateRepository
import com.perol.pixez.shared.ui.state.rememberAccountSession
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * COUI 设置页：设置骨架 + 官方 Group 分组卡片。
 *
 * 入口、分组顺序与业务行为与 MIUIX 版一致：账号登录/退出与账号管理入口（含凭证失效的
 * 重新登录提示）、界面个性化、画质与保存、浏览与交互、屏蔽、系统与数据（默认打开链接 /
 * 清除缓存并显示占用 / 数据导出）、关于与更新（公告入口按列表非空显示、新版本标记）。
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

    // 账号卡片的当前账号：经 rememberAccountSession 读取，账号存储异常时回落到未登录而不是让设置页崩溃；
    // 登出成功后要立即本地置空，故保留一份可变副本，由会话值回灌。
    val (sessionAccount, _) = rememberAccountSession()
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    LaunchedEffect(sessionAccount) { currentAccount = sessionAccount }

    LaunchedEffect(boardRepository) {
        boardList = suspendRunCatchingNonCancel { boardRepository.loadBoardList() }
            .onFailure { Napier.w(tag = "BoardList", throwable = it) { "加载公告板列表失败(CouiSettingsScreen)" } }
            .getOrNull()
    }
    LaunchedEffect(Unit) {
        updateRepository.fetchLatestReleaseInfo().onSuccess { info -> releaseInfo = info }
    }

    CouiSettingsScaffold(
        title = strings.settingsTitle,
        onBack = onBack,
    ) {
        // ── 1. 账号管理 ──
        item(key = "coui_settings_account", contentType = "section") {
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
                        CouiRowDivider()
                        CouiValueRow(
                            title = strings.settingMyProfileAndBookmarks,
                            summary = strings.settingMyProfileAndBookmarksSummary,
                            onClick = { onUserBookmarksClick(currentUserId) },
                        )
                    }
                    CouiRowDivider()
                    CouiValueRow(
                        title = strings.accountManageTitle,
                        summary = strings.accountSwitch,
                        onClick = onAccountManageClick,
                    )
                    CouiRowDivider()
                    CouiValueRow(
                        title = strings.settingAccountInfo,
                        summary = strings.settingAccountInfoSummary,
                        onClick = onAccountEditClick,
                    )
                } else if (currentAccount != null) {
                    // 凭证失效但账号行保留：给出重新登录入口与账号切换。
                    CouiRowDivider()
                    CouiValueRow(
                        title = strings.btnRelogin,
                        summary = strings.settingAccountExpiredHint,
                        onClick = onLoginClick,
                    )
                    CouiRowDivider()
                    CouiValueRow(
                        title = strings.accountManageTitle,
                        summary = strings.accountSwitch,
                        onClick = onAccountManageClick,
                    )
                }
            }
        }

        // ── 2. 界面与个性化 ──
        item(key = "coui_settings_theme", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionTheme) {
                CouiValueRow(title = strings.settingTheme, summary = strings.settingThemeSummary, onClick = onThemeSettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingLayout, summary = strings.settingLayoutSummary, onClick = onLayoutSettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingLanguage, summary = strings.settingLanguageSummary, onClick = onLanguageSettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingWelcomePage, summary = strings.settingWelcomePageSummary, onClick = onWelcomePageSettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingShareFormat, summary = strings.settingShareFormatSummary, onClick = onCopyTextSettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingGuideWizard, summary = strings.settingGuideWizardSummary, onClick = onGuideClick)
            }
        }

        // ── 3. 画质与保存 ──
        item(key = "coui_settings_quality", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionQualitySave) {
                CouiValueRow(title = strings.settingQuality, summary = strings.settingQualitySummary, onClick = onQualitySettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingDownload, summary = strings.settingDownloadSummary, onClick = onDownloadSettingClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingDownloadTask, summary = strings.settingDownloadTaskSummary, onClick = onDownloadTaskClick)
                CouiRowDivider()
                CouiValueRow(title = strings.settingDownloadHistory, summary = strings.settingDownloadHistorySummary, onClick = onDownloadHistoryClick)
            }
        }

        // ── 4. 浏览与交互 ──
        item(key = "coui_settings_general", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionGeneral) {
                CouiValueRow(
                    title = strings.interactionSettingTitle,
                    summary = strings.interactionSettingSwipeChange,
                    onClick = onInteractionSettingClick,
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.settingWidgetRecommend,
                    summary = strings.settingWidgetRecommendSummary,
                    onClick = onWidgetRecommendSettingClick,
                )
                CouiRowDivider()
                CouiValueRow(title = strings.settingHistory, summary = strings.settingHistorySummary, onClick = onHistoryClick)
                CouiRowDivider()
                CouiValueRow(
                    title = strings.novelBrowseTitle,
                    summary = "${strings.novelRecommend} / ${strings.novelRanking}",
                    onClick = onNovelBrowseClick,
                )
                CouiRowDivider()
                CouiValueRow(title = strings.settingBookTags, summary = strings.settingBookTagsSummary, onClick = onBookTagClick)
            }
        }

        // ── 5. 内容过滤与屏蔽 ──
        item(key = "coui_settings_shield", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionShieldPrivacy) {
                CouiValueRow(title = strings.settingShield, summary = strings.settingShieldSummary, onClick = onShieldClick)
            }
        }

        // ── 6. 系统与数据 ──
        item(key = "coui_settings_storage", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionStorage) {
                CouiValueRow(
                    title = strings.platformSettingDefaultOpenLinks,
                    summary = strings.platformSettingDefaultOpenLinksSummary,
                    onClick = { openDefaultAppSettings() },
                )
                CouiRowDivider()
                CouiValueRow(title = strings.settingNetwork, summary = strings.settingNetworkSummary, onClick = onNetworkSettingClick)
                CouiRowDivider()
                CouiValueRow(
                    title = strings.settingClearCache,
                    summary = if (isClearingCache) {
                        strings.clearingCache
                    } else {
                        val sizeStr = formatFileSize(cacheSizeBytes, gbDecimals = 2)
                        "${strings.settingClearCacheSummary} ($sizeStr)"
                    },
                    onClick = {
                        if (isClearingCache) return@CouiValueRow
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
                CouiRowDivider()
                CouiValueRow(title = strings.settingDataExport, summary = strings.settingDataExportSummary, onClick = onDataExportClick)
            }
        }

        // ── 7. 关于与更新 ──
        item(key = "coui_settings_about", contentType = "section") {
            CouiSettingSection(title = strings.settingSectionAbout) {
                if (!boardList.isNullOrEmpty()) {
                    CouiValueRow(title = strings.settingBoard, summary = strings.settingBoardSummary, onClick = onBoardClick)
                    CouiRowDivider()
                }
                val hasNewUpdate = releaseInfo?.isNew == true &&
                    releaseInfo?.versionName != settingsRepository.ignoreUpdateVersion
                CouiValueRow(
                    title = strings.settingUpdate,
                    summary = if (hasNewUpdate) {
                        strings.updateFoundNew.format(
                            releaseInfo?.versionName ?: "",
                            AppInfo.VERSION_NAME,
                        )
                    } else {
                        strings.settingUpdateSummary
                    },
                    onClick = onUpdateSettingClick,
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.settingAbout,
                    summary = "${strings.version} ${AppInfo.VERSION_NAME}",
                    onClick = onAboutClick,
                )
            }
        }
    }
}

/**
 * 分组：官方 Group（标题 + 卡片 + 内部分隔线），几何与间距全部交给库。
 *
 * 保留本函数名与签名，是因为 19 个页面共 53 处调用都指向它；实现已换成官方版式。
 */
@Composable
internal fun CouiSettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    CouiSettingsGroup(title = title, content = { content() })
}

/**
 * 下拉选择行：core 的 [CouiValueRow] 承载标题与当前值，点击后在行内锚点弹出官方 [Menu]。
 *
 * core 行原语没有下拉槽位，这里只借用 [CouiValueRow] 的官方 Item 版式与库的菜单锚点，
 * 选中结果仍由调用方按索引回写；视觉与同组其它行完全一致。
 */
@Composable
internal fun CouiMenuSelectRow(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val anchor = rememberCouiMenuAnchor()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.couiMenuAnchor(anchor)) {
        CouiValueRow(
            title = title,
            value = items.getOrNull(selectedIndex).orEmpty(),
            onClick = { anchor.open { expanded = true } },
        )
        Menu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchor = anchor,
            headerText = title,
            items = items.mapIndexed { index, label ->
                CouiMenuItem(value = index.toString(), text = label)
            },
            onItemClick = { item ->
                expanded = false
                item.value.toIntOrNull()?.let(onSelect)
            },
        )
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
                        if (targetUserId != null) Modifier.clickable { onUserClick(targetUserId) } else Modifier,
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
            .couiButtonFeedback(
                dark = CouiTheme.isDark,
                interactionSource = interactionSource,
                enabled = enabled,
                list = false,
            )
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
