package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.BoardInfo
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BoardRepository
import com.perol.pixez.shared.platform.isAndroidPlatform
import com.perol.pixez.shared.platform.openDefaultAppSettings
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.lazy.rememberLazyListState
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 设置页：分组展示账号、启动、通用、主题、交互、网络、屏蔽、隐私、收藏、分享、画质、保存、显示、下载、存储、关于等入口。
 *
 * 账号分组包含账号信息入口（已登录时）；通用分组包含语言设置、小部件推荐类型、历史记录；
 * 交互分组包含异形屏/H 限制/返回再退出/滑动切换作品开关；显示分组包含「跨适配设置」与「布局设置」；
 * 下载分组包含下载设置、下载历史、下载任务；存储分组包含清除缓存与应用数据；关于分组包含公告板、更新设置、关于 PixEz。
 */
@Composable
fun SettingsScreen(
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
    onAccountManageClick: () -> Unit = {},
    onNovelBrowseClick: () -> Unit = {},
    onHistoryClick: () -> Unit,
    onDownloadTaskClick: () -> Unit,
    onDataExportClick: () -> Unit,
    onBoardClick: () -> Unit,
    onGuideClick: () -> Unit,
    accountRepository: AccountRepository,
    boardRepository: BoardRepository,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalPlatformContext.current

    // 当前账号信息，登出后手动置 null。
    var account by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggingOut by rememberSaveable { mutableStateOf(false) }

    // 清除缓存的加载态与提示信息。
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(isClearingCache) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val imageLoader = SingletonImageLoader.get(context)
            cacheSizeBytes = imageLoader.diskCache?.size ?: 0L
        }
    }

    // 页面进入时加载一次账号信息。
    LaunchedEffect(accountRepository) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        account = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
    }

    // 公告板入口动态状态：仅在公告列表非空时显示。
    var boardList by remember { mutableStateOf<List<BoardInfo>?>(null) }
    val settingsRepository = LocalSettingsRepository.current
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }

    // 页面进入时异步加载公告列表与最新版本信息。
    LaunchedEffect(boardRepository) {
        boardList = suspendRunCatchingNonCancel { boardRepository.loadBoardList() }.getOrNull()
    }
    LaunchedEffect(Unit) {
        fetchLatestReleaseInfo().onSuccess { info ->
            releaseInfo = info
        }
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingsTitle,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = strings.back)
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surface)
                    .blurBackdropSource(backdrop),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = paddingValues,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                        .fillMaxWidth()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {

            // ── 1. 账号管理 ──
            item {
                SmallTitle(text = strings.settingSectionAccount)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    AccountSection(
                        account = account,
                        isLoggingOut = isLoggingOut,
                        onLoginClick = onLoginClick,
                        onLogoutClick = {
                            if (isLoggingOut) return@AccountSection
                            coroutineScope.launch {
                                try {
                                    isLoggingOut = true
                                    suspendRunCatchingNonCancel { accountRepository.logout() }
                                        .onSuccess { account = null }
                                } finally {
                                    isLoggingOut = false
                                }
                            }
                        },
                        strings = strings,
                    )
                    if (account != null) {
                        ArrowPreference(
                            title = strings.accountManageTitle,
                            summary = strings.accountSwitch,
                            onClick = onAccountManageClick,
                        )
                        ArrowPreference(
                            title = strings.settingAccountInfo,
                            summary = strings.settingAccountInfoSummary,
                            onClick = onAccountEditClick,
                        )
                    }
                }
            }

            // ── 2. 界面与个性化 ──
            item {
                SmallTitle(text = strings.settingSectionTheme)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ArrowPreference(
                        title = strings.settingTheme,
                        summary = strings.settingThemeSummary,
                        onClick = onThemeSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingLayout,
                        summary = strings.settingLayoutSummary,
                        onClick = onLayoutSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingLanguage,
                        summary = strings.settingLanguageSummary,
                        onClick = onLanguageSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingWelcomePage,
                        summary = strings.settingWelcomePageSummary,
                        onClick = onWelcomePageSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingShareFormat,
                        summary = strings.settingShareFormatSummary,
                        onClick = onCopyTextSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingGuideWizard,
                        summary = strings.settingGuideWizardSummary,
                        onClick = onGuideClick,
                    )
                }
            }

            // ── 3. 画质与保存 ──
            item {
                SmallTitle(text = strings.settingSectionQualitySave)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ArrowPreference(
                        title = strings.settingQuality,
                        summary = strings.settingQualitySummary,
                        onClick = onQualitySettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingDownload,
                        summary = strings.settingDownloadSummary,
                        onClick = onDownloadSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingDownloadTask,
                        summary = strings.settingDownloadTaskSummary,
                        onClick = onDownloadTaskClick,
                    )
                    ArrowPreference(
                        title = strings.settingDownloadHistory,
                        summary = strings.settingDownloadHistorySummary,
                        onClick = onDownloadHistoryClick,
                    )
                }
            }

            // ── 4. 浏览与交互 ──
            item {
                SmallTitle(text = strings.settingSectionGeneral)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ArrowPreference(
                        title = strings.interactionSettingTitle,
                        summary = strings.interactionSettingSwipeChange,
                        onClick = onInteractionSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingWidgetRecommend,
                        summary = strings.settingWidgetRecommendSummary,
                        onClick = onWidgetRecommendSettingClick,
                    )
                    ArrowPreference(
                        title = strings.settingHistory,
                        summary = strings.settingHistorySummary,
                        onClick = onHistoryClick,
                    )
                    ArrowPreference(
                        title = strings.novelBrowseTitle,
                        summary = "${strings.novelRecommend} / ${strings.novelRanking}",
                        onClick = onNovelBrowseClick,
                    )
                    ArrowPreference(
                        title = strings.settingBookTags,
                        summary = strings.settingBookTagsSummary,
                        onClick = onBookTagClick,
                    )
                }
            }

            // ── 5. 内容过滤与屏蔽 ──
            item {
                SmallTitle(text = strings.settingSectionShieldPrivacy)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ArrowPreference(
                        title = strings.settingShield,
                        summary = strings.settingShieldSummary,
                        onClick = onShieldClick,
                    )
                }
            }

            // ── 6. 系统与数据 ──
            item {
                SmallTitle(text = strings.settingSectionStorage)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (isAndroidPlatform()) {
                        BasicComponent(
                            title = strings.platformSettingDefaultOpenLinks,
                            summary = strings.platformSettingDefaultOpenLinksSummary,
                            onClick = { openDefaultAppSettings() },
                        )
                    }
                    ArrowPreference(
                        title = strings.settingNetwork,
                        summary = strings.settingNetworkSummary,
                        onClick = onNetworkSettingClick,
                    )
                    BasicComponent(
                        title = strings.settingClearCache,
                        summary = if (isClearingCache) {
                            strings.clearingCache
                        } else {
                            val sizeStr = formatCacheSize(cacheSizeBytes)
                            "${strings.settingClearCacheSummary} ($sizeStr)"
                        },
                        onClick = {
                            if (isClearingCache) return@BasicComponent
                            coroutineScope.launch {
                                isClearingCache = true
                                val result = try {
                                    val imageLoader = SingletonImageLoader.get(context)
                                    imageLoader.memoryCache?.clear()
                                    imageLoader.diskCache?.clear()
                                    cacheSizeBytes = 0L
                                    Result.success(Unit)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    Result.failure(e)
                                } finally {
                                    isClearingCache = false
                                }
                                toastMessage = if (result.isSuccess) {
                                    strings.cacheCleared
                                } else {
                                    "${strings.clearFailed}: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        },
                    )
                    ArrowPreference(
                        title = strings.settingDataExport,
                        summary = strings.settingDataExportSummary,
                        onClick = onDataExportClick,
                    )
                }
            }

            // ── 7. 关于与更新 ──
            item {
                SmallTitle(text = strings.settingSectionAbout)
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (!boardList.isNullOrEmpty()) {
                        ArrowPreference(
                            title = strings.settingBoard,
                            summary = strings.settingBoardSummary,
                            onClick = onBoardClick,
                        )
                    }
                    val hasNewUpdate = releaseInfo?.isNew == true && releaseInfo?.versionName != settingsRepository?.ignoreUpdateVersion
                    ArrowPreference(
                        title = strings.settingUpdate,
                        summary = if (hasNewUpdate) strings.updateFoundNew.format(releaseInfo?.versionName ?: "", AppInfo.VERSION_NAME) else strings.settingUpdateSummary,
                        onClick = onUpdateSettingClick,
                        endActions = {
                            if (hasNewUpdate) {
                                top.yukonga.miuix.kmp.basic.Badge(modifier = Modifier.padding(end = 4.dp))
                            }
                        },
                    )
                    ArrowPreference(
                        title = strings.settingAbout,
                        summary = "${strings.version} ${AppInfo.VERSION_NAME}",
                        onClick = onAboutClick,
                    )
                }
            }
        }
    }

    VerticalScrollBar(
        adapter = rememberScrollBarAdapter(listState),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight(),
        trackPadding = paddingValues,
    )
}

ToastMessage(
    message = toastMessage,
    onDismiss = { toastMessage = null },
)
}
}


/**
 * 账号分组：已登录展示头像与账号信息，未登录提供登录入口。
 */
@Composable
private fun AccountSection(
    account: AccountPersist?,
    isLoggingOut: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    strings: com.perol.pixez.shared.ui.i18n.AppStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (account != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title3,
                    )
                    Text(
                        text = "@${account.account}",
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    )
                    Text(
                        text = "ID: ${account.userId}",
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote2,
                    )
                }
            }
            Button(
                onClick = onLogoutClick,
                enabled = !isLoggingOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (isLoggingOut) strings.loggingOut else strings.logout)
            }
        } else {
            Text(
                text = strings.notLoggedIn,
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1,
            )
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = strings.goLogin)
            }
        }
    }
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${((kb * 10).toInt() / 10.0)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${((mb * 10).toInt() / 10.0)} MB"
    val gb = mb / 1024.0
    return "${((gb * 100).toInt() / 100.0)} GB"
}



