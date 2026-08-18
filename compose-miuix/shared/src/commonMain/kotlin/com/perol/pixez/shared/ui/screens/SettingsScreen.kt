package com.perol.pixez.shared.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import com.perol.pixez.shared.ui.AppConstants

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

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
    onSaveSettingClick: () -> Unit,
    onCrossAdapterSettingClick: () -> Unit,
    onLayoutSettingClick: () -> Unit,
    onLanguageSettingClick: () -> Unit,
    onWidgetRecommendSettingClick: () -> Unit,
    onInteractionSettingClick: () -> Unit,
    onFeedSettingClick: () -> Unit,
    onQualitySettingClick: () -> Unit,
    onCopyTextSettingClick: () -> Unit,
    onPrivacySettingClick: () -> Unit,
    onWelcomePageSettingClick: () -> Unit,
    onPlatformSettingClick: () -> Unit,
    onBookTagClick: () -> Unit,
    onUpdateSettingClick: () -> Unit,
    onAccountEditClick: () -> Unit,
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
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // 页面进入时加载一次账号信息。
    LaunchedEffect(accountRepository) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        account = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
    }

    // 公告板入口动态状态：仅在公告列表非空时显示。
    var boardList by remember { mutableStateOf<List<BoardInfo>?>(null) }

    // 页面进入时异步加载公告列表，用于判断「公告板」入口是否显示。
    LaunchedEffect(boardRepository) {
        boardList = suspendRunCatchingNonCancel { boardRepository.loadBoardList() }.getOrNull()
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingsTitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {

            item {
                SmallTitle(text = "账号")
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
                    )
                    if (account != null) {
                        BasicComponent(
                            title = "账号信息",
                            summary = "修改密码、邮箱、Token 与账号注销",
                            onClick = onAccountEditClick,
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "启动")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "欢迎页",
                        summary = "设置启动应用时默认显示的页面",
                        onClick = onWelcomePageSettingClick,
                    )
                    BasicComponent(
                        title = "启动引导向导",
                        summary = "重新运行初次启动的语言与网络配置向导",
                        onClick = onGuideClick,
                    )
                }
            }

            item {
                SmallTitle(text = "通用")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "语言设置",
                        summary = "选择应用界面语言",
                        onClick = onLanguageSettingClick,
                    )
                    BasicComponent(
                        title = "小部件推荐类型",
                        summary = "桌面小部件展示的内容来源",
                        onClick = onWidgetRecommendSettingClick,
                    )
                    BasicComponent(
                        title = "历史记录",
                        summary = "查看本地插画浏览历史",
                        onClick = onHistoryClick,
                    )
                }
            }

            item {
                SmallTitle(text = "主题")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.settingTheme,
                        summary = "主题模式、AMOLED、动态颜色、种子色",
                        onClick = onThemeSettingClick,
                    )
                }
            }

            item {
                SmallTitle(text = "网络")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.settingNetwork,
                        summary = "网络模式、图片源",
                        onClick = onNetworkSettingClick,
                    )
                }
            }

            item {
                SmallTitle(text = "屏蔽与隐私")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.settingShield,
                        summary = "标签、画师、AI 作品过滤",
                        onClick = onShieldClick,
                    )
                    BasicComponent(
                        title = strings.settingPrivacy,
                        summary = "NSFW 遮罩、默认私密收藏",
                        onClick = onPrivacySettingClick,
                    )
                }
            }

            item {
                SmallTitle(text = "收藏与分享")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "收藏标签",
                        summary = "管理常用的收藏/搜索标签",
                        onClick = onBookTagClick,
                    )
                    BasicComponent(
                        title = "分享格式",
                        summary = "复制作品信息时的文本模板",
                        onClick = onCopyTextSettingClick,
                    )
                }
            }

            item {
                SmallTitle(text = "画质与保存")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.settingQuality,
                        summary = "Feed 预览、插画、漫画、大图缩放画质",
                        onClick = onQualitySettingClick,
                    )
                    BasicComponent(
                        title = strings.settingSave,
                        summary = "收藏/保存联动、长按确认、自动标签",
                        onClick = onSaveSettingClick,
                    )
                }
            }

            item {
                SmallTitle(text = "显示与布局")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.settingCrossAdapter,
                        summary = "竖屏/横屏按宽度自适应网格列数",
                        onClick = onCrossAdapterSettingClick,
                    )
                    BasicComponent(
                        title = strings.settingLayout,
                        summary = "平板模式、竖屏/横屏固定网格列数",
                        onClick = onLayoutSettingClick,
                    )
                }
            }

            item {
                SmallTitle(text = "下载")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = strings.settingDownload,
                        summary = "保存路径、同时下载任务数、单文件夹模式",
                        onClick = onDownloadSettingClick,
                    )
                    BasicComponent(
                        title = "下载历史",
                        summary = "查看已下载的作品记录",
                        onClick = onDownloadHistoryClick,
                    )
                    BasicComponent(
                        title = "下载任务",
                        summary = "管理下载队列与任务状态",
                        onClick = onDownloadTaskClick,
                    )
                }
            }

            if (isAndroidPlatform()) {
                item {
                    SmallTitle(text = "平台")
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BasicComponent(
                            title = strings.settingPlatform,
                            summary = "屏幕刷新率、图片选择器、默认打开链接",
                            onClick = onPlatformSettingClick,
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "存储")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "清除缓存",
                        summary = if (isClearingCache) "清理中…" else "释放图片缓存占用的空间",
                        onClick = {
                            if (isClearingCache) return@BasicComponent
                            coroutineScope.launch {
                                isClearingCache = true
                                val result = try {
                                    val imageLoader = SingletonImageLoader.get(context)
                                    imageLoader.memoryCache?.clear()
                                    imageLoader.diskCache?.clear()
                                    Result.success(Unit)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    Result.failure(e)
                                } finally {
                                    isClearingCache = false
                                }
                                toastMessage = if (result.isSuccess) {
                                    "缓存已清除"
                                } else {
                                    "清除失败: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        },
                    )
                    BasicComponent(
                        title = strings.settingDataExport,
                        summary = "导入/导出搜索历史、收藏标签、浏览历史等数据",
                        onClick = onDataExportClick,
                    )
                }
            }

            item {
                SmallTitle(text = "关于")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (!boardList.isNullOrEmpty()) {
                        BasicComponent(
                            title = "公告板",
                            summary = "查看官方公告与更新说明",
                            onClick = onBoardClick,
                        )
                    }
                    BasicComponent(
                        title = strings.settingUpdate,
                        summary = "忽略当前版本更新与手动检查更新",
                        onClick = onUpdateSettingClick,
                    )
                    BasicComponent(
                        title = strings.settingAbout,
                        summary = "版本 ${AppInfo.VERSION_NAME}",
                        onClick = onAboutClick,
                    )
                }
            }
        }
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
                Text(text = if (isLoggingOut) "退出中…" else "退出登录")
            }
        } else {
            Text(
                text = "未登录",
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1,
            )
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "去登录")
            }
        }
    }
}


